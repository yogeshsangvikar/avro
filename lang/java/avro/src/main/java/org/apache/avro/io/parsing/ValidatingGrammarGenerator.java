/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.avro.io.parsing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;

/**
 * The class that generates validating grammar.
 */
public class ValidatingGrammarGenerator {


  private static final  ValidatingGrammarGenerator INSTANCE = new ValidatingGrammarGenerator();

  static final boolean DISABLE_SYMBOL_CACHE = Boolean.getBoolean("avro.disableSymbolCache");

  private static final Function<Schema, Symbol> IMPL = DISABLE_SYMBOL_CACHE
            ? ValidatingGrammarGenerator::generateRoot : Cache::getCachedRootSymbol;

  public static Symbol generateRoot(final Schema schema) {
    return INSTANCE.generate(schema);
  }

  public static Symbol getRootSymbol(final Schema schema) {
    return IMPL.apply(schema);
  }

  private static class Cache {
    private static final ConcurrentMap<Schema, Symbol> ROOT_SYMBOL_CACHE
            = new ConcurrentHashMap<>(16);

    private static Symbol getCachedRootSymbol(final Schema schema) {
      return ROOT_SYMBOL_CACHE.computeIfAbsent(schema, ValidatingGrammarGenerator::generateRoot);
    }

  }

  /**
   * Returns the non-terminal that is the start symbol
   * for the grammar for the given schema <tt>sc</tt>.
   */
  public Symbol generate(Schema schema) {
    return Symbol.root(generate(schema, new HashMap<LitS, Symbol>()));
  }

  /**
   * Returns the non-terminal that is the start symbol
   * for the grammar for the given schema <tt>sc</tt>. If there is already an entry
   * for the given schema in the given map <tt>seen</tt> then
   * that entry is returned. Otherwise a new symbol is generated and
   * an entry is inserted into the map.
   * @param sc    The schema for which the start symbol is required
   * @param seen  A map of schema to symbol mapping done so far.
   * @return      The start symbol for the schema
   */
  public Symbol generate(Schema sc, Map<LitS, Symbol> seen) {
    switch (sc.getType()) {
    case NULL:
      return Symbol.NULL;
    case BOOLEAN:
      return Symbol.BOOLEAN;
    case INT:
      return Symbol.INT;
    case LONG:
      return Symbol.LONG;
    case FLOAT:
      return Symbol.FLOAT;
    case DOUBLE:
      return Symbol.DOUBLE;
    case STRING:
      return Symbol.STRING;
    case BYTES:
      return Symbol.BYTES;
    case FIXED:
      return Symbol.seq(Symbol.intCheckAction(sc.getFixedSize()),
          Symbol.FIXED);
    case ENUM:
      return Symbol.seq(Symbol.intCheckAction(sc.getEnumSymbols().size()),
          Symbol.ENUM);
    case ARRAY:
      return Symbol.seq(Symbol.repeat(Symbol.ARRAY_END, generate(sc.getElementType(), seen)),
          Symbol.ARRAY_START);
    case MAP:
      return Symbol.seq(Symbol.repeat(Symbol.MAP_END,
              generate(sc.getValueType(), seen), Symbol.STRING),
          Symbol.MAP_START);
    case RECORD: {
      LitS wsc = new LitS(sc);
      Symbol rresult = seen.get(wsc);
      if (rresult == null) {
        List<Field> fields = sc.getFields();
        Symbol[] production = new Symbol[fields.size()];

        /**
         * We construct a symbol without filling the array. Please see
         * {@link Symbol#production} for the reason.
         */
        rresult = Symbol.seq(production);
        seen.put(wsc, rresult);

        int i = production.length;
        for (Field f : fields) {
          production[--i] = generate(f.schema(), seen);
        }
      }
      return rresult;
    }
    case UNION:
      List<Schema> subs = sc.getTypes();
      int size = subs.size();
      Symbol[] symbols = new Symbol[size];
      String[] labels = new String[size];

      int i = 0;
      for (Schema b : subs) {
        symbols[i] = generate(b, seen);
        labels[i] = b.getFullName();
        i++;
      }
      return Symbol.seq(Symbol.alt(symbols, labels), Symbol.UNION);

    default:
      throw new RuntimeException("Unexpected schema type: " + sc);
    }
  }

  /** A wrapper around Schema that does "==" equality. */
  static class LitS {
    public final Schema actual;
    public LitS(Schema actual) { this.actual = actual; }

    /**
     * Two LitS are equal if and only if their underlying schema is
     * the same (not merely equal).
     */
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }
      if (LitS.class != o.getClass()) return false;
      return actual == ((LitS)o).actual;
    }

    public int hashCode() {
      return actual.hashCode();
    }
  }
}

