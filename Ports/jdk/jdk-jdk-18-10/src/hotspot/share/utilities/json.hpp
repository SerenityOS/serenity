/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_JSON_HPP
#define SHARE_UTILITIES_JSON_HPP

#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ostream.hpp"

class JSON : public ResourceObj {
 protected:
  JSON(const char* text, bool silent, outputStream* st);
  void parse();
  bool valid();

  typedef enum {
    JSON_NONE,
    JSON_OBJECT_BEGIN,
    JSON_OBJECT_END,
    JSON_ARRAY_BEGIN,
    JSON_ARRAY_END,
    JSON_KEY,
    JSON_STRING,
    JSON_NUMBER_INT,
    JSON_NUMBER_FLOAT,
    JSON_TRUE,
    JSON_FALSE,
    JSON_NULL
  } JSON_TYPE;

  typedef union {
    int64_t int_value;
    uint64_t uint_value;
    double double_value;

    struct {
      const char* start;
      size_t length;
    } str;
  } JSON_VAL;

  typedef enum {
    INTERNAL_ERROR,
    SYNTAX_ERROR,
    KEY_ERROR,
    VALUE_ERROR
  } JSON_ERROR;

  void error(JSON_ERROR e, const char* format, ...) ATTRIBUTE_PRINTF(3, 4);
  outputStream* _st;

 private:
  const char* start;
  const char* pos;

  // For error printing
  const char* mark; // Error marker
  uint level;
  uint line;
  uint column;

  bool silent;
  bool _valid;

  bool parse_json_value();
  bool parse_json_object();
  bool parse_json_array();
  bool parse_json_string(bool key = false);
  bool parse_json_key();
  bool parse_json_number();
  bool parse_json_symbol(const char* name, JSON_TYPE symbol);

  virtual bool callback(JSON_TYPE t, JSON_VAL* v, uint level) = 0;

  void mark_pos();
  u_char next();
  u_char peek();
  u_char peek(size_t i);
  int expect_any(const char* valid_chars, const char* error_msg, JSON_ERROR e = SYNTAX_ERROR);
  bool expect_string(const char* expected_string, const char* error_msg = "", JSON_ERROR e = SYNTAX_ERROR);
  size_t skip(size_t i);
  int skip_to_token();
  u_char skip_to(u_char want);
  u_char skip_line_comment();
  int skip_block_comment();

  const char* strerror(JSON_ERROR e);
};

#endif // SHARE_UTILITIES_JSON_HPP
