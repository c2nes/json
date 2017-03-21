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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.brewtab.json.JsonParser.JsonArray;
import com.brewtab.json.JsonParser.JsonFalse;
import com.brewtab.json.JsonParser.JsonNull;
import com.brewtab.json.JsonParser.JsonNumber;
import com.brewtab.json.JsonParser.JsonObject;
import com.brewtab.json.JsonParser.JsonString;
import com.brewtab.json.JsonParser.JsonTrue;
import com.brewtab.json.JsonParser.JsonValue;
import com.brewtab.json.JsonToken.Type;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class JsonTest {

  @Test
  public void testTokenizerBasic() throws IOException {
    String json = "{"
        + "\"a\":\"hello\\nworld!\", "
        + "\"b\":12345678, "
        + "\"c\": 123.456e+789, "
        + "\"d\": true,"
        + "\"e\": false,"
        + "\"f\": null,"
        + "\"g\": [1, 2, 3]"
        + "}";

    System.out.println(json.length());
    JsonTokenizer tokenizer = new JsonTokenizer(new StringReader(json));

    // {
    assertNextToken(tokenizer, Type.BEGIN_OBJECT);

    // "a":"hello\nworld!",
    assertNextToken(tokenizer, Type.STRING, "a");
    assertNextToken(tokenizer, Type.NAME_SEPARATOR);
    assertNextToken(tokenizer, Type.STRING, "hello\nworld!");
    assertNextToken(tokenizer, Type.VALUE_SEPARATOR);

    // "b": 12345678,
    assertNextToken(tokenizer, Type.STRING, "b");
    assertNextToken(tokenizer, Type.NAME_SEPARATOR);
    assertNextToken(tokenizer, Type.NUMBER, 12345678L);
    assertNextToken(tokenizer, Type.VALUE_SEPARATOR);

    // "c": 123.456e789,
    assertNextToken(tokenizer, Type.STRING, "c");
    assertNextToken(tokenizer, Type.NAME_SEPARATOR);
    assertNextToken(tokenizer, Type.NUMBER, new BigDecimal("123.456e789"));
    assertNextToken(tokenizer, Type.VALUE_SEPARATOR);

    // "d": true,
    assertNextToken(tokenizer, Type.STRING, "d");
    assertNextToken(tokenizer, Type.NAME_SEPARATOR);
    assertNextToken(tokenizer, Type.TRUE);
    assertNextToken(tokenizer, Type.VALUE_SEPARATOR);

    // "e": false,
    assertNextToken(tokenizer, Type.STRING, "e");
    assertNextToken(tokenizer, Type.NAME_SEPARATOR);
    assertNextToken(tokenizer, Type.FALSE);
    assertNextToken(tokenizer, Type.VALUE_SEPARATOR);

    // "f": null,
    assertNextToken(tokenizer, Type.STRING, "f");
    assertNextToken(tokenizer, Type.NAME_SEPARATOR);
    assertNextToken(tokenizer, Type.NULL);
    assertNextToken(tokenizer, Type.VALUE_SEPARATOR);

    // "g": [1, 2, 3]
    assertNextToken(tokenizer, Type.STRING, "g");
    assertNextToken(tokenizer, Type.NAME_SEPARATOR);
    assertNextToken(tokenizer, Type.BEGIN_ARRAY);
    assertNextToken(tokenizer, Type.NUMBER, 1L);
    assertNextToken(tokenizer, Type.VALUE_SEPARATOR);
    assertNextToken(tokenizer, Type.NUMBER, 2L);
    assertNextToken(tokenizer, Type.VALUE_SEPARATOR);
    assertNextToken(tokenizer, Type.NUMBER, 3L);
    assertNextToken(tokenizer, Type.END_ARRAY);

    // }
    assertNextToken(tokenizer, Type.END_OBJECT);
  }

  @Test
  public void testParserBasic() throws IOException {
    String json = "{"
        + "\"a\":\"hello\\nworld!\", "
        + "\"b\":12345678, "
        + "\"c\": 123.456e+789, "
        + "\"d\": true,"
        + "\"e\": false,"
        + "\"f\": null,"
        + "\"g\": [1, 2, 3]"
        + "}";

    JsonParser parser = new JsonParser(new JsonTokenizer(new StringReader(json)));
    JsonValue value = parser.parseOnlyValue();
    JsonObject obj = (JsonObject) value;
    Map<String, JsonValue> fields = obj.getFields();

    assertEquals(
        ImmutableSet.of("a", "b", "c", "d", "e", "f", "g"),
        fields.keySet());

    assertTrue(fields.get("a") instanceof JsonString);
    assertTrue(fields.get("b") instanceof JsonNumber);
    assertTrue(fields.get("c") instanceof JsonNumber);
    assertTrue(fields.get("d") instanceof JsonTrue);
    assertTrue(fields.get("e") instanceof JsonFalse);
    assertTrue(fields.get("f") instanceof JsonNull);
    assertTrue(fields.get("g") instanceof JsonArray);

    JsonString a = (JsonString) fields.get("a");
    assertEquals("hello\nworld!", a.getValue());

    JsonNumber b = (JsonNumber) fields.get("b");
    assertEquals(12345678L, b.getValue());

    JsonNumber c = (JsonNumber) fields.get("c");
    assertEquals(new BigDecimal("123.456e789"), c.getValue());

    JsonArray g = (JsonArray) fields.get("g");
    List<JsonValue> gValues = g.getValues();
    assertEquals(3, gValues.size());

    assertTrue(gValues.get(0) instanceof JsonNumber);
    assertTrue(gValues.get(1) instanceof JsonNumber);
    assertTrue(gValues.get(2) instanceof JsonNumber);

    JsonNumber g0 = (JsonNumber) gValues.get(0);
    JsonNumber g1 = (JsonNumber) gValues.get(1);
    JsonNumber g2 = (JsonNumber) gValues.get(2);

    assertEquals(1L, g0.getValue());
    assertEquals(2L, g1.getValue());
    assertEquals(3L, g2.getValue());
  }

  public void assertNextToken(JsonTokenizer tokenizer, Type type) throws IOException {
    JsonToken token = tokenizer.next();
    System.out.println(token);
    assertNotNull(token);
    assertEquals(type, token.getType());
  }

  public void assertNextToken(JsonTokenizer tokenizer, Type type, String val) throws IOException {
    JsonToken token = tokenizer.next();
    System.out.println(token);
    assertNotNull(token);
    assertEquals(type, token.getType());
    assertEquals(val, token.stringValue());
  }

  public void assertNextToken(JsonTokenizer tokenizer, Type type, Number val) throws IOException {
    JsonToken token = tokenizer.next();
    System.out.println(token);
    assertNotNull(token);
    assertEquals(type, token.getType());
    assertEquals(val, token.numberValue());
  }

  public Reader dataset(String name) {
    String resource = "datasets/" + name + ".json";
    InputStream stream = getClass().getClassLoader()
        .getResourceAsStream(resource);
    if (stream == null) {
      throw new IllegalArgumentException(resource + ": not found");
    }
    return new InputStreamReader(stream);
  }
}
