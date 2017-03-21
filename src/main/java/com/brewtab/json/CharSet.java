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

/**
 * A set of characters. Not to be confused with a "Charset".
 */
class CharSet {
  private final boolean[] table;
  private final char min;
  private final char max;

  CharSet(boolean[] table, char min, char max) {
    this.table = table;
    this.min = min;
    this.max = max;
  }

  public boolean contains(char c) {
    return min <= c && c <= max && table[c - min];
  }

  public static CharSet of(String s) {
    char min = Character.MAX_VALUE;
    char max = Character.MIN_VALUE;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c < min) {
        min = c;
      }
      if (c > max) {
        max = c;
      }
    }

    boolean[] table = new boolean[1 + (max - min)];
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      table[c - min] = true;
    }

    return new CharSet(table, min, max);
  }
}
