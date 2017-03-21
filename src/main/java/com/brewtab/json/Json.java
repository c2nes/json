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

import com.brewtab.json.JsonParser.JsonValue;
import java.io.IOException;
import java.io.StringReader;

public class Json {

  /**
   * Returns the given string parsed as a JSON value.
   */
  public static JsonValue parse(String json) {
    if (json == null) {
      throw new NullPointerException();
    }

    try {
      return new JsonParser(new JsonTokenizer(new StringReader(json))).parseValue();
    } catch (IOException e) {
      throw new RuntimeException("Unexpected IOException", e);
    }
  }

  /**
   * Returns the JSON representation of the given String.
   */
  public static String encode(String s) {
    return '"' + escape(s) + '"';
  }

  /**
   * Escape the given string for use in a JSON document. Unlike {@link #encode(String)} the return
   * value is unquoted.
   */
  public static String escape(String s) {
    StringBuilder sb = null;

    int i = 0;
    for (int j = 0; j < s.length(); j++) {
      char c = s.charAt(j);
      if (isEscapingRequired(c)) {
        if (sb == null) {
          sb = new StringBuilder(s.length());
        }
        sb.append(s.substring(i, j));
        sb.append(escape(c));
        i = j + 1;
      }
    }

    if (sb == null) {
      return s;
    } else {
      return sb.append(s.substring(i)).toString();
    }
  }

  /**
   * Returns the JSON-escaped version of the given character.
   */
  public static String escape(char c) {
    switch (c) {
      case '"':
        return "\\\"";
      case '\\':
        return "\\\\";
      case '\n':
        return "\\n";
      case '\t':
        return "\\t";
      case '\r':
        return "\\r";
      case '\f':
        return "\\f";
      case '\b':
        return "\\b";
      default:
    }

    return (c < 0x20)
        ? String.format("\\u%04x", (int) c)
        : String.valueOf(c);
  }

  private static boolean isEscapingRequired(char c) {
    return c == '"' || c == '\\' || c < 0x20;
  }
}
