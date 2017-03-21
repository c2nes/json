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

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * A JSON tokenizer. Reads {@link JsonToken JsonTokens} from a character stream.
 */
public class JsonTokenizer {

  private static final CharSet VALID_NUMBER_CHARS = CharSet.of("0123456789eE+-.");
  private static final CharSet WS_CHARS = CharSet.of(" \t\r\n");

  private static final char[] TRUE_SUFFIX = {'r', 'u', 'e'};
  private static final char[] FALSE_SUFFIX = {'a', 'l', 's', 'e'};
  private static final char[] NULL_SUFFIX = {'u', 'l', 'l'};

  private final Reader reader;
  private char[] buffer = new char[4000];
  private int pos = 0;
  private int limit = 0;

  // Position
  private int offset = 0;
  private int line = 0;
  private int column = 0;

  public JsonTokenizer(Reader reader) {
    this.reader = reader;
  }

  /**
   * Returns the next JSON token in the stream, or null if the end of the stream is reached.
   *
   * @throws IOException if an I/O error occurs while reading from the underlying reader
   * @throws JsonException if an invalid token is encountered
   */
  public JsonToken next() throws IOException {
    Position position;
    char c;

    try {
      consumeWhitespace();
      position = position();
      c = read();
    } catch (JsonEOFException e) {
      return null;
    }

    switch (c) {
      case '{':
        return JsonToken.newBeginObjectToken(position);

      case '}':
        return JsonToken.newEndObjectToken(position);

      case '[':
        return JsonToken.newBeginArrayToken(position);

      case ']':
        return JsonToken.newEndArrayToken(position);

      case ':':
        return JsonToken.newNameSeparatorToken(position);

      case ',':
        return JsonToken.newValueSeparatorToken(position);

      case '"':
        // Push the quote back and read the whole string
        unread();
        return readString();

      case 't':
        // "true"
        if (tryRead(TRUE_SUFFIX)) {
          return JsonToken.newTrueToken(position);
        } else {
          break;
        }

      case 'f':
        // "false"
        if (tryRead(FALSE_SUFFIX)) {
          return JsonToken.newFalseToken(position);
        } else {
          break;
        }

      case 'n':
        // "null"
        if (tryRead(NULL_SUFFIX)) {
          return JsonToken.newNullToken(position);
        } else {
          break;
        }

      default:
        if (c == '-' || ('0' <= c && c <= '9')) {
          // Push the hyphen or digit back and read the whole number
          unread();
          return readNumber(position);
        } else {
          break;
        }
    }

    throw new JsonException(
        String.format("syntax error at character '%c' at position %s", c, position));
  }

  /**
   * Returns the current position in the input.
   */
  private Position position() {
    return new Position(offset, line, column);
  }

  /**
   * Reads additional data into the buffer from the input.
   *
   * @throws JsonEOFException if no more data is available from the input
   */
  private void doRead() throws IOException {
    int n = reader.read(buffer, limit, buffer.length - limit);
    if (n < 0) {
      throw new JsonEOFException("end of input");
    }
    limit += n;
  }

  /**
   * Fills the buffer to ensure the given number of characters are available. That is,
   * a call {@code fillBuffer(N)} ensures {@code limit - pos >= N}.
   *
   * @throws JsonEOFException if end-of-stream is reached without buffering the required number of
   *     characters
   */
  private void fillBuffer(int required) throws IOException {
    if (limit - pos >= required) {
      return;
    }

    if (pos == limit) {
      pos = 0;
      limit = 0;
    }

    // If there isn't enough space left at the end of the buffer we will
    // either compact the buffer (if the buffer itself is large enough and
    // we can simply move data to make room) or grow *and* compact the
    // buffer simultaneously.
    if (buffer.length - pos < required) {
      // Save this value now. It will be used for the compaction steps
      // and will be our new "limit" afterwards.
      final int len = limit - pos;

      if (required <= buffer.length) {
        // Buffer is big enough. Just compact to make the free space accessible.
        System.arraycopy(buffer, pos, buffer, 0, len);
      } else {
        // The buffer isn't big enough. Grow and compact.
        char[] newBuffer = new char[buffer.length << 1];
        System.arraycopy(buffer, pos, newBuffer, 0, len);
        buffer = newBuffer;
      }

      // Compaction complete. Move pos to beginning of the buffer and limit
      // to the end of the data.
      pos = 0;
      limit = len;
    }

    while (limit - pos < required) {
      doRead();
    }
  }

  /**
   * Advances the input to the next non-whitespace character. Updates {@link #line} and
   * {@link #column} appropriately as newline characters are consumed.
   *
   * @see #WS_CHARS
   */
  private void consumeWhitespace() throws IOException {
    fillBuffer(1);

    char c = buffer[pos];
    while (WS_CHARS.contains(c)) {
      offset++;

      if (c == '\n') {
        line++;
        column = 0;
      } else {
        column++;
      }

      pos++;
      fillBuffer(1);
      c = buffer[pos];
    }
  }

  /**
   * Undoes the most recent {@link #read()} call.
   */
  private void unread() {
    pos--;
    column--;
    offset--;
  }

  /**
   * Returns true and advances the stream if (and only if) the next characters
   * in the input reader match those provided. Otherwise returns false and leaves
   * the input reader position unmodified. Note, this method does not track
   * newlines so should only be called after a call to {@link #consumeWhitespace()}
   * or if it is known that a newline will not be encountered.
   */
  private boolean tryRead(char... cs) throws IOException {
    try {
      fillBuffer(cs.length);
    } catch (JsonEOFException e) {
      return false;
    }

    for (int i = 0; i < cs.length; i++) {
      if (cs[i] != buffer[pos + i]) {
        return false;
      }
    }

    pos += cs.length;
    offset += cs.length;
    column += cs.length;

    return true;
  }

  /**
   * Returns the next character from the input. This method does not track
   * newlines so should only be called after a call to {@link #consumeWhitespace()}
   * or if it is known that a newline will not be encountered.
   */
  private char read() throws IOException {
    fillBuffer(1);
    offset++;
    column++;
    return buffer[pos++];
  }

  private JsonToken readString() throws IOException {
    Position start = position();
    boolean escaped = false;
    int len = 1;

    fillBuffer(len + 1);
    char c = buffer[pos + len];

    // In this first pass we just find the end of the string. We decode
    // escape sequences later if necessary.
    while (c != '"') {
      if (c < 0x20) {
        throw new JsonException("unescaped control character in string");
      } else if (c == '\\') {
        escaped = true;
        len += 2;
      } else {
        len += 1;
      }

      fillBuffer(len + 1);
      c = buffer[pos + len];
    }

    // Step over the closing quote
    len++;

    String rawString = new String(buffer, pos, len);
    String decoded = escaped
        ? decode(rawString)
        : new String(buffer, pos + 1, len - 2);

    pos += len;
    offset += len;
    column += len;

    return JsonToken.newStringToken(start, rawString, decoded);
  }

  private JsonToken readNumber(Position position) throws IOException {
    int len = 0;
    while (VALID_NUMBER_CHARS.contains(buffer[pos + len])) {
      len++;

      try {
        fillBuffer(len + 1);
      } catch (JsonEOFException e) {
        break;
      }
    }

    String raw = new String(buffer, pos, len);

    pos += len;
    offset += len;
    column += len;

    /*
     * Okay, lets review the format of numbers,
     *
     *       number = [ minus ] int [ frac ] [ exp ]
     *       decimal-point = %x2E       ; .
     *       digit1-9 = %x31-39         ; 1-9
     *       e = %x65 / %x45            ; e E
     *       exp = e [ minus / plus ] 1*DIGIT
     *       frac = decimal-point 1*DIGIT
     *       int = zero / ( digit1-9 *DIGIT )
     *       minus = %x2D               ; -
     *       plus = %x2B                ; +
     *       zero = %x30                ; 0
     */

    int fracStart = raw.indexOf('.');

    int expStart = raw.indexOf('e', fracStart + 1);
    if (expStart < 0) {
      expStart = raw.indexOf('E', fracStart + 1);
    }

    int expEnd = raw.length();
    if (expStart < 0) {
      expStart = expEnd;
    }

    int fracEnd = expStart;
    if (fracStart < 0) {
      fracStart = fracEnd;
    }

    int intSign = raw.charAt(0) == '-' ? -1 : 1;
    int intStart = (intSign == -1) ? 1 : 0;
    int intEnd = fracStart;
    int intLen = intEnd - intStart;

    // Validate integer part
    if (intLen < 1) {
      throw new JsonException("Invalid number. Integer part required; val=" + raw);
    } else if (intLen > 1 && raw.charAt(intStart) == '0') {
      throw new JsonException("Invalid number. Leading 0s not permitted; val=" + raw);
    } else {
      for (int i = intStart; i < intEnd; i++) {
        if (!isDigit(raw.charAt(i))) {
          throw new JsonException("Invalid number. Illegal character in integer component;"
              + " val=" + raw);
        }
      }
    }

    // Validate fractional part
    if (fracEnd > fracStart) {
      // Skip over '.'
      fracStart++;
      if (fracStart == fracEnd) {
        throw new JsonException("Invalid number. Empty fractional part; val=" + raw);
      }
      for (int i = fracStart; i < fracEnd; i++) {
        if (!isDigit(raw.charAt(i))) {
          throw new JsonException("Invalid number. Illegal character in fractional component;"
              + " val=" + raw);
        }
      }
    }

    // Validate exp part
    int expSign = 1;
    if (expEnd > expStart) {
      // Skip over "e"/"E" and optional "-"/"+"
      expStart++;
      if (expStart < expEnd) {
        if (raw.charAt(expStart) == '-') {
          expSign = -1;
          expStart++;
        } else if (raw.charAt(expStart) == '+') {
          expStart++;
        }
      }

      if (expStart == expEnd) {
        throw new JsonException("Invalid number. Empty exponent part; val=" + raw);
      }

      for (int i = expStart; i < expEnd; i++) {
        if (!isDigit(raw.charAt(i))) {
          throw new JsonException("Invalid number. Illegal character in exponent component;"
              + " val=" + raw);
        }
      }
    }

    // If it doesn't have a fractional port or exponent try to convert
    // to a long falling back to BigInteger. For anything with a fractional
    // part or exponent we always use a BigDecimal.

    Number value;
    if (intEnd == raw.length()) {
      try {
        value = Long.valueOf(raw);
      } catch (NumberFormatException e) {
        value = new BigInteger(raw);
      }
    } else {
      // Exponent may be approaching BigDecimal limit of Integer.MAX_VALUE.
      // Integer.MAX_VALUE = 2_147_483_647
      if ((expEnd - expStart) > 9) {
        BigInteger exp = new BigInteger(raw.substring(expStart, expEnd));
        if (exp.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
          throw new JsonException("Number exceeds supported range (exp > 2147483647)");
        }
      }
      value = new BigDecimal(raw);
    }

    return JsonToken.newNumberToken(position, raw, value);
  }

  private static String decode(String serializedValue) {
    StringBuilder decoded = new StringBuilder(serializedValue.length() - 2);

    int i = 1;
    int j = serializedValue.indexOf('\\', 1);

    while (j != -1) {
      decoded.append(serializedValue.substring(i, j));
      char c = serializedValue.charAt(j + 1);
      if (c == '"' || c == '/' || c == '\\') {
        decoded.append(c);
      } else if (c == 'n') {
        decoded.append('\n');
      } else if (c == 't') {
        decoded.append('\t');
      } else if (c == 'r') {
        decoded.append('\r');
      } else if (c == 'f') {
        decoded.append('\f');
      } else if (c == 'b') {
        decoded.append('\b');
      } else if (c == 'u') {
        int codepoint = 0;
        for (int ui = j + 2; ui < j + 6; ui++) {
          if (ui >= serializedValue.length()) {
            throw new JsonException("Incomplete unicode escape in string");
          }
          char uc = serializedValue.charAt(ui);
          int ud = "0123456789abcdef".indexOf(Character.toLowerCase(uc));
          if (ud < 0) {
            throw new JsonException("Illegal character '" + uc + "' in unicode escape");
          }
          codepoint = (codepoint << 4) | (ud & 0xF);
        }
        decoded.append((char) codepoint);
        j = j + 4;
      } else {
        throw new JsonException("Illegal escape sequence '\\" + c + "'");
      }

      i = j + 2;
      j = serializedValue.indexOf('\\', i);
    }
    decoded.append(serializedValue.substring(i, serializedValue.length() - 1));
    return decoded.toString();
  }

  private static boolean isDigit(char c) {
    return '0' <= c && c <= '9';
  }
}
