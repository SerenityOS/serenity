/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 */

#include "precompiled.hpp"
#include "jvm.h"
#include "memory/resourceArea.hpp"
#include "utilities/json.hpp"
#include "unittest.hpp"

class JSON_GTest : public JSON {
 public:
  static void test(const char* json, bool valid);
  char* get_output();

 private:
  JSON_GTest(const char* text);
  stringStream output;

  void log(uint level, const char* format, ...) ATTRIBUTE_PRINTF(3, 4);

  bool callback(JSON_TYPE t, JSON_VAL* v, uint level);
  JSON_TYPE prev;
};

char* JSON_GTest::get_output() {
  return output.as_string();
}

void JSON_GTest::test(const char* text, bool should_pass) {
  ResourceMark rm;
  JSON_GTest json(text);
  if (should_pass) {
    ASSERT_TRUE(json.valid()) << "failed on a valid json string"
            << std::endl << "debug output:" << std::endl << json.get_output();
  } else {
    ASSERT_FALSE(json.valid()) << "succeeded on an invalid json string"
            << std::endl << "debug output:" << std::endl << json.get_output();
  }
}

JSON_GTest::JSON_GTest(const char* text) : JSON(text, false, &output) {
  prev = JSON_NONE;
  parse();
}

TEST_VM(utilities, json_curly_braces) {
  JSON_GTest::test("{}", true);
}

TEST_VM(utilities, json_brackets) {
  JSON_GTest::test("[]", true);
}

TEST_VM(utilities, json_space_braces) {
  JSON_GTest::test("  {  }  ", true);
}

TEST_VM(utilities, json_space_bracketes) {
  JSON_GTest::test("  [  ]  ", true);
}

TEST_VM(utilities, json_quoted_error) {
  JSON_GTest::test("\"error\"", false);
}

TEST_VM(utilities, json_error_string) {
  JSON_GTest::test("error", false);
}

TEST_VM(utilities, json_simple_integer) {
  JSON_GTest::test("1", false);
}

TEST_VM(utilities, json_siple_float) {
  JSON_GTest::test("1.2", false);
}

TEST_VM(utilities, json_simple_boolean_true) {
  JSON_GTest::test("true", false);
}

TEST_VM(utilities, json_simple_boolean_false) {
  JSON_GTest::test("false", false);
}

TEST_VM(utilities, json_simple_null) {
  JSON_GTest::test("null", false);
}

TEST_VM(utilities, json_one_element_int_array) {
  JSON_GTest::test("[ 1 ]", true);
}

TEST_VM(utilities, json_int_array) {
  JSON_GTest::test("[ 1, ]", true);
}

TEST_VM(utilities, json_one_element_bool_array) {
  JSON_GTest::test("[ true ]", true);
}

TEST_VM(utilities, json_bool_array) {
  JSON_GTest::test("[ true, ]", true);
}

TEST_VM(utilities, json_one_element_false_array) {
  JSON_GTest::test("[ false ]", true);
}

TEST_VM(utilities, json_false_bool_array) {
  JSON_GTest::test("[ false, ]", true);
}

TEST_VM(utilities, json_one_null_array) {
  JSON_GTest::test("[ null ]", true);
}

TEST_VM(utilities, json_null_array) {
  JSON_GTest::test("[ null, ]", true);
}

TEST_VM(utilities, json_one_empty_string_array) {
  JSON_GTest::test("[ \"\" ]", true);
}

TEST_VM(utilities, json_empty_string_array) {
  JSON_GTest::test("[ \"\", ]", true);
}

TEST_VM(utilities, json_single_string_array) {
  JSON_GTest::test("[ \"elem1\" ]", true);
}

TEST_VM(utilities, json_string_comma_arrray) {
  JSON_GTest::test("[ \"elem1\", ]", true);
}

TEST_VM(utilities, json_two_strings_array) {
  JSON_GTest::test("[ \"elem1\", \"elem2\" ]", true);
}

TEST_VM(utilities, json_two_strings_comma_array) {
  JSON_GTest::test("[ \"elem1\", \"elem2\", ]", true);
}

TEST_VM(utilities, json_curly_braces_outside) {
  JSON_GTest::test("[ \"elem1\" ] { }", false);
}

TEST_VM(utilities, json_element_in_array) {
  JSON_GTest::test("[ elem1, \"elem2\" ]", false);
}

TEST_VM(utilities, json_incorrect_end_array) {
  JSON_GTest::test("[ \"elem1\"", false);
}

TEST_VM(utilities, json_incorrect_string_end) {
  JSON_GTest::test("[ \"elem1 ]", false);
}

TEST_VM(utilities, json_incorrect_end_of_two_elements_array) {
  JSON_GTest::test("[ \"elem1\", \"elem2\"", false);
}

TEST_VM(utilities, json_incorrect_bool_true_array) {
  JSON_GTest::test("[ truefoo ]", false);
}

TEST_VM(utilities, json_incorrect_bool_false_array) {
  JSON_GTest::test("[ falsefoo ]", false);
}

TEST_VM(utilities, json_incorrect_null_array) {
  JSON_GTest::test("[ nullfoo ]", false);
}

TEST_VM(utilities, json_key_pair) {
  JSON_GTest::test("{ key : 1 }", true);
}

TEST_VM(utilities, json_key_pair_comma) {
  JSON_GTest::test("{ key : 1, }", true);
}

TEST_VM(utilities, json_bool_true_key) {
  JSON_GTest::test("{ key : true }", true);
}

TEST_VM(utilities, json_bool_true_key_comma) {
  JSON_GTest::test("{ key : true, }", true);
}

TEST_VM(utilities, json_bool_false_key) {
  JSON_GTest::test("{ key : false }", true);
}

TEST_VM(utilities, json_bool_false_key_comma) {
  JSON_GTest::test("{ key : false, }", true);
}

TEST_VM(utilities, json_null_key) {
  JSON_GTest::test("{ key : null }", true);
}

TEST_VM(utilities, json_null_key_comma) {
  JSON_GTest::test("{ key : null, }", true);
}

TEST_VM(utilities, json_pair_of_empty_strings) {
  JSON_GTest::test("{ \"\" : \"\" }", true);
}

TEST_VM(utilities, json_pair_of_empty_strings_comma) {
  JSON_GTest::test("{ \"\" : \"\", }", true);
}

TEST_VM(utilities, json_pair_of_strings) {
  JSON_GTest::test("{ \"key1\" : \"val1\" }", true);
}

TEST_VM(utilities, json_pair_of_strings_comma) {
  JSON_GTest::test("{ \"key1\" : \"val1\", }", true);
}

TEST_VM(utilities, json_two_pairs_of_strings) {
  JSON_GTest::test("{ \"key1\" : \"val1\", \"key2\" : \"val2\" }", true);
}

TEST_VM(utilities, json_two_pairs_of_strings_comma) {
  JSON_GTest::test("{ \"key1\" : \"val1\", \"key2\" : \"val2\", }", true);
}

TEST_VM(utilities, json_array_outside) {
  JSON_GTest::test("{ \"key\" : \"val\" } [ \"error\" ]", false);
}

TEST_VM(utilities, json_incorrect_object_end) {
  JSON_GTest::test("{ \"key\" : \"val\" ", false);
}

TEST_VM(utilities, json_empty_comment) {
  JSON_GTest::test("/**/ { }", true);
}

TEST_VM(utilities, json_space_comment) {
  JSON_GTest::test("/* */ { }", true);
}

TEST_VM(utilities, json_comment) {
  JSON_GTest::test("/*foo*/ { }", true);
}

TEST_VM(utilities, json_star_comment) {
  JSON_GTest::test("/* *foo */ { }", true);
}

TEST_VM(utilities, json_stars_comment) {
  JSON_GTest::test("/* *foo* */ { }", true);
}

TEST_VM(utilities, json_special_comment) {
  JSON_GTest::test("/* /*foo */ { }", true);
}

TEST_VM(utilities, json_comment_after) {
  JSON_GTest::test("{ } /* foo */", true);
}

TEST_VM(utilities, json_comment_after_and_space) {
  JSON_GTest::test("{ } /* foo */ ", true);
}

TEST_VM(utilities, json_one_line_empty_comment_after) {
  JSON_GTest::test("{ } //", true);
}

TEST_VM(utilities, json_one_line_space_comment_after) {
  JSON_GTest::test("{ } // ", true);
}

TEST_VM(utilities, json_one_line_comment_after) {
  JSON_GTest::test("{ } // foo", true);
}

TEST_VM(utilities, json_incorrect_multiline_comment) {
  JSON_GTest::test("/* * / { }", false);
}

TEST_VM(utilities, json_incorrect_multiline_comment_begin) {
  JSON_GTest::test("/ * */ { }", false);
}

TEST_VM(utilities, json_oneline_comment_only) {
  JSON_GTest::test("// { }", false);
}

TEST_VM(utilities, json_multiline_comment_only) {
  JSON_GTest::test("/* { } */", false);
}

TEST_VM(utilities, json_multiline_comment_2) {
  JSON_GTest::test("/* { } */ ", false);
}

TEST_VM(utilities, json_incorrectly_commented_object) {
  JSON_GTest::test("/* { } ", false);
}

TEST_VM(utilities, json_missing_multiline_end) {
  JSON_GTest::test("{ } /* ", false);
}

TEST_VM(utilities, json_missing_multiline_slash) {
  JSON_GTest::test("/* { } *", false);
}

TEST_VM(utilities, json_commented_object_end) {
  JSON_GTest::test("{ /* } */", false);
}

TEST_VM(utilities, json_commented_array_end) {
  JSON_GTest::test("[ /* ] */", false);
}

TEST_VM(utilities, json_missing_object_end) {
  JSON_GTest::test("{ key : \"val\", /* } */", false);
}

TEST_VM(utilities, json_missing_array_end) {
  JSON_GTest::test("[ \"val\", /* ] */", false);
}

TEST_VM(utilities, json_key_values_1) {
  JSON_GTest::test("/* comment */{ key1 : { \"key2\" : { \"key3\" : [ \"elem1\", \"elem2\","
          "{ \"key4\" : null }, 3 , 2 , 1 , 0 , -1 , -2 , -3 , true, false, null, ] }, \"key5\""
          " : true }, \"key6\" : [ \"☃\" ], key7 : \"val\",}", true);
}

TEST_VM(utilities, json_key_values_2) {
  JSON_GTest::test("/* comment */ { \"key1\" : { \"key2\" : { \"key3\" : [ \"elem1\", \"elem2\","
          "{ \"key4\" : null }, 3 , 2 , 1 , 0 , -1 , -2 , -3 , true, false, null, ] }, \"key5\""
          " : true }, \"key6\" : [ \"☃\" ], key7 : \"val\",}", true);
}

TEST_VM(utilities, json_quoted_symbols) {
  JSON_GTest::test("/*comment*/{\"ff1 fsd\":{\"☃\":{\"☃\":[\"☃\",\"☃\"]},"
          "\"☃\":true},\"☃\":[\"☃\"],\"foo\":\"☃\",}", true);
}

TEST_VM(utilities, json_incorrect_key) {
  JSON_GTest::test("/* comment */ { key1 error : { \"☃\" : { \"☃\" : [ \"☃\","
          " \"☃\" ] }, \"☃\" : true }, \"baz\" : [ \"☃\" ], foo : \"☃\",}",
          false); // first key needs to be quoted since it contains a space
}

TEST_VM(utilities, json_array_with_newline) {
  JSON_GTest::test("[\n]", true);
}

TEST_VM(utilities, json_directives_file) {
  JSON_GTest::test(
          "[" "\n"
          "   {"
          "         // pattern to match against class+method+signature" "\n"
          "         // leading and trailing wildcard (*) allowed" "\n"
          "         match: \"foo.bar.*\"," "\n"
          " " "\n"
          "         // override defaults for specified compiler" "\n"
          "         // we may differentiate between levels too. TBD." "\n"
          "         c1:  {" "\n"
          "           //override c1 presets " "\n"
          "           array_bounds_check_removal: false" "\n"
          "         }," "\n"
          "" "\n"
          "         c2: {" "\n"
          "           // control inlining of method" "\n"
          "           // + force inline, - dont inline" "\n"
          "           inline : [ \"+java.util.*\", \"-com.sun.*\"]," "\n"
          "         }," "\n"
          "" "\n"
          "         // directives outside a specific preset applies to all compilers" "\n"
          "         inline : [ \"+java.util.*\", \"-com.sun.*\"]," "\n"
          "         print_assembly: true," "\n"
          "         verify_oopmaps: true," "\n"
          "         max_loop_unrolling: 5" "\n"
          "   }," "\n"
          "   {" "\n"
          "         // matching several patterns require an array" "\n"
          "         match: [\"baz.*\",\"frob*\"]," "\n"
          "" "\n"
          "         // only enable c1 for this directive" "\n"
          "         // all enabled by default. Command disables all not listed" "\n"
          "         enable: \"c1\"," "\n"
          "" "\n"
          "         // applies to all compilers" "\n"
          "         // + force inline, - dont inline" "\n"
          "         inline : [ \"+java.util.*\", \"-com.sun.*\"]," "\n"
          "         print_inlining: true," "\n"
          "" "\n"
          "         // force matching compiles to be blocking/syncronous" "\n"
          "         blocking_compile: true" "\n"
          "   }," "\n"
          "]" "\n", true);
}

void JSON_GTest::log(uint indent, const char* format, ...) {
  if (prev != JSON_KEY) {
    for (uint i = 0; i < indent; i++) {
      _st->print("  ");
    }
  }
  va_list args;
  va_start(args, format);
  _st->vprint(format, args);
  va_end(args);
}

bool JSON_GTest::callback(JSON_TYPE t, JSON_VAL* v, uint rlevel) {
  switch (t) {
    case JSON_OBJECT_BEGIN:
      log(rlevel, "{\n");
      prev = JSON_NONE; // Only care about JSON_KEY, to indent correctly
      return true;

    case JSON_OBJECT_END:
      log(rlevel, "},\n");
      prev = JSON_NONE;
      return true;

    case JSON_ARRAY_BEGIN:
      log(rlevel, "[\n");
      prev = JSON_NONE;
      return true;

    case JSON_ARRAY_END:
      log(rlevel, "],\n");
      prev = JSON_NONE;
      return true;

    case JSON_KEY:
      for (uint i = 0; i < rlevel; i++) {
        _st->print("  ");
      }
      _st->print("<key>");
      for (size_t i = 0; i < v->str.length; i++) {
        u_char c = v->str.start[i];
        if (c == 0) {
          return false;
        }
        _st->print("%c", c);
      }
      _st->print(" : ");
      prev = JSON_KEY;
      return true;

    case JSON_STRING:
      if (prev != JSON_KEY) {
        for (uint i = 0; i < rlevel; i++) {
          _st->print("  ");
        }
      }
      _st->print("<str>");
      for (size_t i = 0; i < v->str.length; i++) {
        u_char c = v->str.start[i];
        if (c == 0) {
          return false;
        }
        _st->print("%c", c);
      }
      _st->print(",\n");
      prev = JSON_NONE;
      return true;

    case JSON_NUMBER_INT:
      log(rlevel, "<int>%" PRId64 ",\n", v->int_value);
      prev = JSON_NONE;
      return true;

    case JSON_NUMBER_FLOAT:
      log(rlevel, "<double>%lf,\n", v->double_value);
      prev = JSON_NONE;
      return true;

    case JSON_TRUE:
      log(rlevel, "<true>,\n");
      prev = JSON_NONE;
      return true;

    case JSON_FALSE:
      log(rlevel, "<false>,\n");
      prev = JSON_NONE;
      return true;

    case JSON_NULL:
      log(rlevel, "<null>,\n");
      prev = JSON_NONE;
      return true;

    default:
      error(INTERNAL_ERROR, "unknown JSON type");
      return false;
  }
}
