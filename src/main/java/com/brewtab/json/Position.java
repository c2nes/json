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
 * A position in a file or stream. Provides {@link #getOffset() offset} (characters from beginning
 * of the file/stream), {@link #getLine() line number} and {@link #getColumn() column} (characters
 * from the beginning of the line). All values start at zero (i.e. offset 0 is at line 0 and column
 * 0).
 */
public class Position {

  private final int offset;
  private final int line;
  private final int column;

  Position(int offset, int line, int column) {
    this.offset = offset;
    this.line = line;
    this.column = column;
  }

  public int getOffset() {
    return offset;
  }

  public int getLine() {
    return line;
  }

  public int getColumn() {
    return column;
  }

  @Override
  public String toString() {
    return line + ":" + column;
  }
}
