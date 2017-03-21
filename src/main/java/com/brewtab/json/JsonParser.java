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

import com.brewtab.json.JsonToken.Type;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A JSON parser. Reads {@link JsonValue JsonValues} from a character stream.
 */
public class JsonParser {

  private final JsonTokenizer tokenizer;
  private final int depthLimit = 50;
  private int depth = 0;

  public JsonParser(Reader reader) {
    this(new JsonTokenizer(reader));
  }

  public JsonParser(JsonTokenizer tokenizer) {
    this.tokenizer = tokenizer;
  }

  private JsonToken nextTokenRequired() throws IOException {
    JsonToken token = tokenizer.next();
    if (token == null) {
      throw new JsonException("end-of-input reached before complete value could be read");
    }
    return token;
  }

  private void checkTokenType(Type expected, JsonToken token) {
    if (expected != token.getType()) {
      throw new JsonException(expected + " expected at position " + token.getPosition()
          + ", but found " + token.getType());
    }
  }

  private JsonArray parseJsonArray(Position pos) throws IOException {
    List<JsonValue> values = new ArrayList<JsonValue>();
    JsonToken token = nextTokenRequired();

    while (token.getType() != Type.END_ARRAY) {
      values.add(parseValueFromToken(token));

      // Read "," or "]"
      token = nextTokenRequired();
      if (token.getType() == Type.VALUE_SEPARATOR) {
        token = nextTokenRequired();
        if (token.getType() == Type.END_ARRAY) {
          throw new JsonException("Illegal trailing ',' in array value");
        }
      } else {
        checkTokenType(Type.END_ARRAY, token);
      }
    }

    return new JsonArray(pos, values);
  }

  private JsonObject parseJsonObject(Position pos) throws IOException {
    Map<String, JsonValue> fields = new LinkedHashMap<String, JsonValue>();
    JsonToken token = nextTokenRequired();

    while (token.getType() != Type.END_OBJECT) {
      checkTokenType(Type.STRING, token);

      String key = token.stringValue();
      checkTokenType(Type.NAME_SEPARATOR, nextTokenRequired());
      JsonValue value = parseValueFromToken(nextTokenRequired());

      fields.put(key, value);

      // Read "," or "}"
      token = nextTokenRequired();
      if (token.getType() == Type.VALUE_SEPARATOR) {
        // Read the next field name to be processed on the next iteration
        token = nextTokenRequired();
        if (token.getType() == Type.END_OBJECT) {
          throw new JsonException("Illegal trailing ',' in object value");
        }
      } else {
        checkTokenType(Type.END_OBJECT, token);
      }
    }

    return new JsonObject(pos, fields);
  }

  /**
   * Returns the next JSON value from the input stream, or null if the end of the stream has been
   * reached.
   */
  public JsonValue parseValue() throws IOException {
    JsonToken token = tokenizer.next();
    if (token == null) {
      return null;
    } else {
      return parseValueFromToken(token);
    }
  }

  /**
   * Returns the JSON value for the input stream.
   *
   * @throws JsonException if no value can be read or additional (non-whitespace) data follows the
   * value
   */
  public JsonValue parseOnlyValue() throws IOException {
    JsonValue value = parseValue();

    if (value == null) {
      throw new JsonException("no value");
    }

    if (tokenizer.next() != null) {
      throw new JsonException("unexpected data following value");
    }

    return value;
  }

  private void increaseDepth() {
    if (++depth > depthLimit) {
      throw new JsonException("Nesting limit exceeded; limit=" + depthLimit);
    }
  }

  private void decreaseDepth() {
    --depth;
    assert depth >= 0;
  }

  private JsonValue parseValueFromToken(JsonToken token) throws IOException {
    try {
      increaseDepth();
      Position pos = token.getPosition();

      switch (token.getType()) {
        case BEGIN_ARRAY:
          return parseJsonArray(pos);

        case BEGIN_OBJECT:
          return parseJsonObject(pos);

        case TRUE:
          return new JsonTrue(pos);

        case FALSE:
          return new JsonFalse(pos);

        case NULL:
          return new JsonNull(pos);

        case STRING:
          return new JsonString(pos, token.stringValue());

        case NUMBER:
          return new JsonNumber(pos, token.numberValue());

        default:
          // Fall through
      }

      throw new JsonException("unexpected token '" + token + "' at position " + pos);
    } finally {
      decreaseDepth();
    }
  }

  public interface JsonValue {

    Position getPosition();
  }

  static abstract class AbstractJsonValue implements JsonValue {

    protected final Position position;

    protected AbstractJsonValue(Position position) {
      this.position = position;
    }

    @Override
    public Position getPosition() {
      return position;
    }
  }

  public static class JsonTrue extends AbstractJsonValue {

    public JsonTrue(Position position) {
      super(position);
    }

    @Override
    public String toString() {
      return "true";
    }
  }

  public static class JsonFalse extends AbstractJsonValue {

    public JsonFalse(Position position) {
      super(position);
    }

    @Override
    public String toString() {
      return "false";
    }
  }

  public static class JsonNull extends AbstractJsonValue {

    public JsonNull(Position position) {
      super(position);
    }

    @Override
    public String toString() {
      return "null";
    }
  }

  public static class JsonString extends AbstractJsonValue {

    private final String value;

    public JsonString(Position position, String value) {
      super(position);
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return Json.encode(value);
    }
  }

  public static class JsonNumber extends AbstractJsonValue {

    private final Number value;

    public JsonNumber(Position position, Number value) {
      super(position);
      this.value = value;
    }

    public Number getValue() {
      return value;
    }

    @Override
    public String toString() {
      return value.toString();
    }
  }

  public static class JsonArray extends AbstractJsonValue {

    private final List<JsonValue> values;

    protected JsonArray(Position position, List<JsonValue> values) {
      super(position);
      this.values = Collections.unmodifiableList(values);
    }

    public List<JsonValue> getValues() {
      return values;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      for (JsonValue value : values) {
        if (sb.length() > 1) {
          sb.append(",");
        }
        sb.append(value);
      }
      sb.append("]");
      return sb.toString();
    }
  }

  public static class JsonObject extends AbstractJsonValue {

    private final Map<String, JsonValue> fields;

    protected JsonObject(Position position, Map<String, JsonValue> fields) {
      super(position);
      this.fields = Collections.unmodifiableMap(fields);
    }

    public Map<String, JsonValue> getFields() {
      return fields;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      for (Entry<String, JsonValue> entry : fields.entrySet()) {
        if (sb.length() > 1) {
          sb.append(",");
        }
        sb.append(Json.encode(entry.getKey()));
        sb.append(":");
        sb.append(entry.getValue());
      }
      sb.append("}");
      return sb.toString();
    }
  }
}
