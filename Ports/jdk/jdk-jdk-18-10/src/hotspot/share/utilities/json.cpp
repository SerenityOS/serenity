/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

/*
 * This is not really json in the state it is now.
 * Some differences:
 * - Double quotes around the key in an object is not enforced.
 *     i.e you can write: { foo : "bar" } instead of { "foo" : "bar" }.
 * - Comments are allowed.
 * - The last element in an object or array can have an ending comma.
 */

#include "precompiled.hpp"
#include "utilities/json.hpp"
#include "utilities/ostream.hpp"
#include <math.h>

const char* strchrnul_(const char *s, int c) {
  const char* tmp = strchr(s, c);
  return tmp == NULL ? s + strlen(s) : tmp;
}

JSON::JSON(const char* text, bool silent, outputStream* st)
: _st(st), start(text), pos(text), mark(text),
  level(0), line(1), column(0), silent(silent), _valid(true)
{
}

void JSON::parse() {
  assert(start != NULL, "Need something to parse");
  if (start == NULL) {
    _valid = false;
    error(INTERNAL_ERROR, "JSON parser was called with a string that was NULL.");
  } else {
    _valid = parse_json_value();
  }
}

bool JSON::valid() {
  return _valid;
}

bool JSON::parse_json_value() {
  int c;

  c = skip_to_token();
  if (c == -1) {
    return false;
  }

  // Must start with object or array
  if (level == 0) {

    switch (c) {
    case '{':
      if (parse_json_object() == false) {
        return false;
      }
      c = skip_to_token();
      if (c > 0) {
        mark_pos();
        error(SYNTAX_ERROR, "Only one top level object/array is allowed.");
        return false;
      } else if (c < 0) {
        return false;
      }
      return true;

    case '[':
      if (parse_json_array() == false) {
        return false;
      }
      c = skip_to_token();
      if (c > 0) {
        mark_pos();
        error(SYNTAX_ERROR, "Only one top level object/array is allowed.");
        return false;
      } else if (c < 0) {
        return false;
      }
      return true;

    case 0:
      error(SYNTAX_ERROR, "EOS was encountered before any json declarations");
      return false;

    default:
      error(SYNTAX_ERROR, "Json must start with an object or an array.");
      return false;
    }
  } else { // level > 0
    switch (c) {
    case '{':
      return parse_json_object();

    case '[':
      return parse_json_array();

    case '"':
      return parse_json_string();

    case '-': case '0':
    case '1': case '2': case '3':
    case '4': case '5': case '6':
    case '7': case '8': case '9':
      return parse_json_number();

    case 't':
      return parse_json_symbol("true", JSON_TRUE);

    case 'f':
      return parse_json_symbol("false", JSON_FALSE);

    case 'n':
      return parse_json_symbol("null", JSON_NULL);

    case 0:
      error(SYNTAX_ERROR, "EOS was encountered when expecting a json value.");
      return false;

    default:
      error(SYNTAX_ERROR, "Could not parse as a json value (did you forget to quote your strings?).");
      return false;
    }
  }
}

// Should only be called when we actually have the start of an object
// Otherwise it is an internal error
bool JSON::parse_json_object() {
  NOT_PRODUCT(const char* prev_pos);
  int c;

  mark_pos();
  // Check that we are not called in error
  if (expect_any("{", "object start", INTERNAL_ERROR) <= 0) {
    return false;
  }

  if (!callback(JSON_OBJECT_BEGIN, NULL, level++)) {
    return false;
  }

  for (;;) {
    mark_pos();
    c = skip_to_token();
    if (c == 0) {
      error(SYNTAX_ERROR, "EOS when expecting an object key or object end");
      return false;
    } else if (c < 0) {
      return false;
    } else if (c == '}') {
      // We got here from either empty object "{}" or ending comma "{a:1,}"
      next();
      break;
    }

    NOT_PRODUCT(prev_pos = pos);
    if (parse_json_key() == false) {
      return false;
    }
    assert(pos > prev_pos, "parsing stalled");

    skip_to_token();
    mark_pos();
    if (expect_any(":", "object key-value separator") <= 0) {
      return false;
    }

    skip_to_token();
    mark_pos();
    NOT_PRODUCT(prev_pos = pos);
    if (parse_json_value() == false) {
      return false;
    }
    assert(pos > prev_pos, "parsing stalled");

    c = skip_to_token();
    mark_pos();
    if (expect_any(",}", "value separator or object end") <= 0) {
      return false;
    }
    if (c == '}') {
      break;
    }
  }

  assert(c == '}', "array parsing ended without object end token ('}')");
  return callback(JSON_OBJECT_END, NULL, --level);
}

// Should only be called when we actually have the start of an array
// Otherwise it is an internal error
bool JSON::parse_json_array() {
  NOT_PRODUCT(const char* prev_pos);
  int c;

  mark_pos();
  // Check that we are not called in error
  if (expect_any("[", "array start character", INTERNAL_ERROR) <= 0) {
    return false;
  }

  if (!callback(JSON_ARRAY_BEGIN, NULL, level++)) {
    return false;
  }

  for (;;) {
    mark_pos();
    c = skip_to_token();
    if (c == 0) {
      error(SYNTAX_ERROR, "EOS when expecting a json value or array end");
      return false;
    } else if (c < 0) {
      return false;
    } else if (c == ']') {
      // We got here from either empty array "[]" or ending comma "[1,]"
      next();
      break;
    }

    mark_pos();
    NOT_PRODUCT(prev_pos = pos);
    if (parse_json_value() == false) {
      return false;
    }
    assert(pos > prev_pos, "parsing stalled");

    c = skip_to_token();
    mark_pos();
    if (expect_any(",]", "value separator or array end") <= 0) {
      return false;
    }
    if (c == ']') {
      break;
    }
  }

  assert(c == ']', "array parsing ended without array end token (']')");
  return callback(JSON_ARRAY_END, NULL, --level);
}

bool JSON::parse_json_string(bool key) {
  const char* end;
  JSON_VAL v;

  mark_pos();
  if (expect_any("\"", "string start character", INTERNAL_ERROR) <= 0) {
    return false;
  }

  end = strchr(pos, '"'); // TODO: escapes
  if (end == NULL) {
    error(SYNTAX_ERROR, "String started here never ended. Expected \'\"\' before EOS.");
    return false;
  }

  v.str.start = pos;
  v.str.length = end - pos;
  skip(end - pos);

  if (expect_any("\"", "string end character", INTERNAL_ERROR) <= 0) {
    return false;
  }

  if (key == true) {
    return callback(JSON_KEY, &v, level);
  } else {
    return callback(JSON_STRING, &v, level);
  }
}

// TODO: hotspot equivalents?
static bool is_alpha(u_char c) {
  return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
}
static bool is_numeric(u_char c) {
  return (c >= '0' && c <= '9');
}
static bool is_alnum(u_char c) {
  return is_alpha(c) || is_numeric(c);
}
static bool is_word(u_char c) {
  return c == '_' || is_alnum(c);
}

// Allow object keys to be without quotation,
// but then restrict to ([a-zA-Z0-9_])+
bool JSON::parse_json_key() {
  const char* begin;
  JSON_VAL v;
  u_char c;

  mark_pos();
  c = peek();
  if (c == '"') {
    return parse_json_string(true);
  }

  begin = pos;
  c = peek();
  if (c == 0) {
    error(SYNTAX_ERROR, "Got EOS when expecting an object key.");
    return false;
  } else if (is_word(c) == false) {
    error(SYNTAX_ERROR, "Expected an object key, which can be a double-quoted (\") string or a simple string (only alphanumeric characters and underscore, separated by whitespace) that doesn't need to be quoted.");
    return false;
  }

  for (;;) {
    c = peek();
    // Allow the key to be delimited by control characters and the object key-value separator ':'
    if (c <= ' ' || c == ':') {
      break;
    } else if (is_word(c) == false) {
      error(SYNTAX_ERROR, "Object key need to be quoted, or consist entirely of alphanumeric characters and underscores.");
      return false;
    }
    next();
  }

  v.str.start = begin;
  v.str.length = pos - begin;
  return callback(JSON_KEY, &v, level);
}

bool JSON::parse_json_number() {
  double double_value;
  int tokens, read;
  JSON_VAL v;

  mark_pos();

  // Parsing number - for simplicity ints are limited to 2**53
  // sscanf as a double and check if part is 0.
  tokens = sscanf(pos, "%lf%n", &double_value, &read);
  assert(tokens <= 1, "scanf implementation that counts as a token, parsing json numbers will always fail");
  if (tokens == 1) {
    assert(read > 0, "sanity");

    if (floor(double_value) == double_value) {
      // No exponent - treat as an int
      v.int_value = (int)double_value;
      if (!callback(JSON_NUMBER_INT, &v, level)) {
        return false;
      }
    } else {
      v.double_value = double_value;
      if (!callback(JSON_NUMBER_FLOAT, &v, level)) {
        return false;
      }
    }
    skip(read);
    return true;
  }

  error(SYNTAX_ERROR, "Couldn't parse json number (note that exponents are not supported).");
  return false;
}

bool JSON::parse_json_symbol(const char* name, JSON_TYPE symbol) {
  if (expect_string(name, "maybe you forgot to quote your strings?") == false) {
    mark_pos();
    return false;
  }
  return callback(symbol, NULL, level);
}

void JSON::mark_pos() {
  assert((mark == start || *(mark - 1)) != 0, "buffer overrun");
  assert(mark <= pos, "mark runahead");

  u_char c;

  while (mark < pos) {
    c = *mark;
    assert(c != 0, "pos buffer overrun?");
    if (c != 0) {
      mark++;
      column++;
    }
    if (c == '\n') {
      line++;
      column = 0;
    }
  }

  assert(mark <= pos, "mark runahead");
}

u_char JSON::next() {
  assert((pos == start || *(pos - 1)) != 0, "buffer overrun");

  u_char c = *pos;
  if (c != 0) {
    pos++;
  }
  return c;
}

u_char JSON::peek() {
  return *pos;
}

// Peek ahead i chars (0 is same as peek())
u_char JSON::peek(size_t i) {
  u_char c;
  const char* p;

  p = pos;
  c = *p;
  while (i > 0 && c != 0) {
    i--;
    p++;
    c = *p;
  }
  return c;
}

/*
 * Check that one of the expected characters is next in the stream.
 * If not, it is an error.
 * Returns 0 if EOS is encountered.
 * Returns -1 if the next character was not one of the expected.
 * Otherwise consumes and returns the expected character that was encountered.
 */
int JSON::expect_any(const char* valid_chars, const char* error_msg, JSON_ERROR e) {
  size_t len;
  u_char c;

  len = strlen(valid_chars);
  assert(len > 0, "need non-empty string");

  c = peek();
  if (c == 0) {
    error(e, "Got EOS when expecting %s (%s\'%s\').", error_msg, len > 1 ? "one of " : "", valid_chars);
    return 0;
  }
  for (size_t i = 0; i < len; i++) {
    if (c == valid_chars[i]) {
      return next();
    }
  }
  error(e, "Expected %s (%s\'%s\').", error_msg, len > 1 ? "one of " : "", valid_chars);
  return -1;
}

/*
 * Check that the expected string is next in the stream.
 * If not, it is an error.
 * Consumes the expected characters if they are present.
 * Returns true if the expected characters were present, otherwise false.
 */
bool JSON::expect_string(const char* expected_string, const char* error_msg, JSON_ERROR e) {
  u_char c, expected_char;
  size_t len;

  assert(expected_string != NULL, "need non-null string");
  len = strlen(expected_string);
  assert(len > 0, "need non-empty string");

  for (size_t i = 0; i < len; i++) {
    expected_char = expected_string[i];
    assert(expected_char > ' ', "not sane for control characters");
    if (expected_char <= ' ') {
      error(INTERNAL_ERROR, "expect got a control char");
    }
    c = pos[i];
    if (c == 0) {
      error(e, "EOS encountered when expecting %s (\"%s\")", error_msg, expected_string);
      return false;
    } else if (c != expected_char) {
      error(e, "Expected \"%s\" (%s)", expected_string, error_msg);
      return false;
    }
  }
  skip(len);
  return true;
}

/*
 * Skip i characters.
 * Returns number of characters skipped.
 */
size_t JSON::skip(size_t i) {
  u_char c;
  size_t j;

  c = peek();
  for (j = i; c != 0 && j > 0; j--) {
    c = next();
  }
  return i - j;
}

/*
 * Skip whitespace and comments.
 * Returns the first token after whitespace/comments without consuming it
 * Returns 0 if EOS is encountered.
 * Returns -1 if there is an error
 */
int JSON::skip_to_token() {
  for (;;) {
    int c = peek(0);
    if (c == '/') {
      u_char c2 = peek(1);
      if (c2 == '/') {
        c = skip_line_comment();
      } else if (c2 == '*') {
        c = skip_block_comment();
        if (c < 0) {
          return -1;
        }
      }
      // Fall through to keep checking if there
      // are more whitespace / comments to skip
    }
    if (c == 0 || c > ' ') {
      return c;
    }
    next();
  }
  return 0;
}

/*
 * Skip to, and return the wanted char without consuming it
 * Returns 0 if EOS is encountered.
 */
u_char JSON::skip_to(u_char want) {
  // We want the bookkeeping done in next().
  // Otherwise strchr could have been used.
  u_char c;
  for(;;) {
    c = peek();
    if (c == 0 || c == want) {
      return c;
    }
    next();
  }
}

/*
 * Should only be called when we actually have a line comment to skip.
 * Otherwise it is an internal error.
 *
 * Will return the first token after the line comment without consuming it.
 * Returns 0 if EOS is encoutered.
 */
u_char JSON::skip_line_comment() {
  u_char c;

  // Check that we are not called in error
  expect_any("/", "line comment start", INTERNAL_ERROR);
  expect_any("/", "line comment start", INTERNAL_ERROR);

  c = skip_to('\n');
  if (c == 0) {
    return 0;
  }
  next();
  return next();
}

/*
 * Should only be called when we actually have a block comment to skip.
 * Otherwise it is an internal error.
 *
 * Returns the first token after the block comment without consuming it.
 * Returns -1 if EOS is encountered in the middle of a comment.
 */
int JSON::skip_block_comment() {
  const char* current;

  // Check that we are not called in error.
  if (peek() != '/' || peek(1) != '*') {
    // Let expect handle EOS.
    expect_string("/*", "block comment start", INTERNAL_ERROR);
    return 0;
  }

  current = pos;
  for (;;) {
    current = strchrnul_(current, '*');

    if (current[0] == 0 || current[1] == 0) {
      // Advance error marker to start of block comment
      mark_pos();
      error(SYNTAX_ERROR, "Block comment started here never ended. Expected \"*/\" before EOS.");
      return -1;
    }

    if (current[1] == '/') {
      pos = current;
      if (expect_string("*/", "block comment end", INTERNAL_ERROR) == false) {
        return -1;
      }
      // Found block comment end
      return peek();
    }
    current++;
  }
}

const char* JSON::strerror(JSON_ERROR e) {
  switch (e) {
  case SYNTAX_ERROR:
    return "Syntax error";
  case INTERNAL_ERROR:
    return "Internal error";
  case KEY_ERROR:
    return "Key error";
  case VALUE_ERROR:
    return "Value error";
  default:
    ShouldNotReachHere();
    return "Unknown error";
  }
}

void JSON::error(JSON_ERROR e, const char* format, ...) {
  _valid = false;

  if (!silent) {
    const char* line_start;
    const char* tmp;
    size_t line_length;
    va_list args;
    u_char c;

    _st->print("%s on line %u byte %u: ", JSON::strerror(e), line, column + 1);
    va_start(args, format);
    _st->vprint(format, args);
    _st->cr();
    va_end(args);

    line_start = mark - column;
    assert(line_start >= start, "out of bounds");
    assert(line_start <= mark, "out of bounds");
    assert(line_start == start || line_start[-1] == '\n', "line counting error");

    c = *pos;
    if (c == 0) {
      _st->print("  Got ");
      _st->print_cr("EOS.");
    }
    tmp = mark;
    c = *tmp;
    if (c > ' ') {
      _st->print("  At ");
      _st->print("'");
      while (c > ' ') {
        _st->print("%c", c);
        tmp++;
        c = *tmp;
      }
      _st->print_cr("'.");
    }

    // Skip to newline or EOS
    tmp = strchrnul_(mark, '\n');
    line_length = tmp - line_start;

    _st->print_cr("%s", line_start);
  }
}

