/*
 * Copyright 2017 Chris Thunes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.brewtab.json;

public class JsonToken {

  private final Type type;
  private final Position position;
  private final String serializedValue;

  private final String string;
  private final Number number;

  private JsonToken(
      Type type,
      Position position,
      String serializedValue,
      String string,
      Number number
  ) {
    this.type = type;
    this.position = position;
    this.serializedValue = serializedValue;
    this.string = string;
    this.number = number;
  }

  public Type getType() {
    return type;
  }

  public Position getPosition() {
    return position;
  }

  /**
   * Returns the token as it appeared in the source document.
   */
  public String serializedValue() {
    return serializedValue;
  }

  /**
   * Returns the string value of this token. For {@code STRING} tokens this is the decoded string
   * value. For all other tokens, this returns the {@link #serializedValue() serialized value}.
   */
  public String stringValue() {
    return string == null ? serializedValue : string;
  }

  /**
   * Returns the numeric value of this {@link Type#NUMBER NUMBER} token. For all other types
   * throws {@code UnsupportedOperationException}.
   */
  public Number numberValue() {
    if (number == null) {
      throw new UnsupportedOperationException("not a number");
    }
    return number;
  }

  @Override
  public String toString() {
    return "JsonToken{"
        + "type=" + type
        + ", serialized=" + serializedValue
        + ", pos=" + position
        + "}";
  }

  public static JsonToken newBeginArrayToken(Position position) {
    return new JsonToken(Type.BEGIN_ARRAY, position, "[", null, null);
  }

  public static JsonToken newBeginObjectToken(Position position) {
    return new JsonToken(Type.BEGIN_OBJECT, position, "{", null, null);
  }

  public static JsonToken newEndArrayToken(Position position) {
    return new JsonToken(Type.END_ARRAY, position, "]", null, null);
  }

  public static JsonToken newEndObjectToken(Position position) {
    return new JsonToken(Type.END_OBJECT, position, "}", null, null);
  }

  public static JsonToken newNameSeparatorToken(Position position) {
    return new JsonToken(Type.NAME_SEPARATOR, position, ":", null, null);
  }

  public static JsonToken newValueSeparatorToken(Position position) {
    return new JsonToken(Type.VALUE_SEPARATOR, position, ",", null, null);
  }

  public static JsonToken newTrueToken(Position position) {
    return new JsonToken(Type.TRUE, position, "true", null, null);
  }

  public static JsonToken newFalseToken(Position position) {
    return new JsonToken(Type.FALSE, position, "false", null, null);
  }

  public static JsonToken newNullToken(Position position) {
    return new JsonToken(Type.NULL, position, "null", null, null);
  }

  public static JsonToken newStringToken(Position position, String serializedValue, String value) {
    return new JsonToken(Type.STRING, position, serializedValue, value, null);
  }

  public static JsonToken newNumberToken(Position position, String serializedValue, Number value) {
    return new JsonToken(Type.NUMBER, position, serializedValue, null, value);
  }

  public enum Type {
    BEGIN_ARRAY,
    BEGIN_OBJECT,
    END_ARRAY,
    END_OBJECT,
    NAME_SEPARATOR,
    VALUE_SEPARATOR,
    TRUE,
    FALSE,
    NULL,
    STRING,
    NUMBER
  }
}
