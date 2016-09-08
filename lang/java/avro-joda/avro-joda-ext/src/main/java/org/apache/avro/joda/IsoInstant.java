/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.avro.joda;

import java.util.Collections;
import java.util.Set;
import org.apache.avro.AbstractLogicalType;
import org.apache.avro.Schema;
import org.joda.time.Instant;

public class IsoInstant extends AbstractLogicalType {


  public IsoInstant(Schema.Type type) {
    super(type, Collections.EMPTY_SET, "isoinstant", Collections.EMPTY_MAP);
    // validate the type
    if (type != Schema.Type.LONG) {
      throw new IllegalArgumentException(
              "Logical type " + this + " must be backed by long or string");
    }
  }

  @Override
  public void validate(Schema schema) {
    // validate the type
    if (schema.getType() != Schema.Type.LONG) {
      throw new IllegalArgumentException(
              "Logical type " + this + " must be backed by long or string");
    }
  }

  @Override
  public Set<String> reserved() {
    return Collections.EMPTY_SET;
  }

  @Override
  public Class<?> getLogicalJavaType() {
    return Instant.class;
  }


  @Override
  public Object deserialize(Object object) {
    return new Instant((Long) object);
  }

  @Override
  public Object serialize(Object object) {
    return ((Instant) object).getMillis();
  }

}
