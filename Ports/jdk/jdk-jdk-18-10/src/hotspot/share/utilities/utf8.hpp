/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_UTF8_HPP
#define SHARE_UTILITIES_UTF8_HPP

#include "memory/allocation.hpp"

// Low-level interface for UTF8 strings

class UTF8 : AllStatic {
 public:
  // returns the unicode length of a 0-terminated utf8 string
  static int unicode_length(const char* utf8_str) {
    bool is_latin1, has_multibyte;
    return unicode_length(utf8_str, is_latin1, has_multibyte);
  }
  static int unicode_length(const char* utf8_str, bool& is_latin1, bool& has_multibyte);

  // returns the unicode length of a non-0-terminated utf8 string
  static int unicode_length(const char* utf8_str, int len) {
    bool is_latin1, has_multibyte;
    return unicode_length(utf8_str, len, is_latin1, has_multibyte);
  }
  static int unicode_length(const char* utf8_str, int len, bool& is_latin1, bool& has_multibyte);

  // converts a utf8 string to a unicode string
  template<typename T> static void convert_to_unicode(const char* utf8_str, T* unicode_str, int unicode_length);

  // returns the quoted ascii length of a utf8 string
  static int quoted_ascii_length(const char* utf8_str, int utf8_length);

  // converts a utf8 string to quoted ascii
  static void as_quoted_ascii(const char* utf8_str, int utf8_length, char* buf, int buflen);

#ifndef PRODUCT
  // converts a quoted ascii string to utf8 string.  returns the original
  // string unchanged if nothing needs to be done.
  static const char* from_quoted_ascii(const char* quoted_ascii_string);
#endif

  // decodes the current utf8 character, stores the result in value,
  // and returns the end of the current utf8 chararacter.
  template<typename T> static char* next(const char* str, T* value);

  // decodes the current utf8 character, gets the supplementary character instead of
  // the surrogate pair when seeing a supplementary character in string,
  // stores the result in value, and returns the end of the current utf8 chararacter.
  static char* next_character(const char* str, jint* value);

  // Utility methods

  // Returns NULL if 'c' it not found. This only works as long
  // as 'c' is an ASCII character
  static const jbyte* strrchr(const jbyte* base, int length, jbyte c) {
    assert(length >= 0, "sanity check");
    assert(c >= 0, "does not work for non-ASCII characters");
    // Skip backwards in string until 'c' is found or end is reached
    while(--length >= 0 && base[length] != c);
    return (length < 0) ? NULL : &base[length];
  }
  static bool   equal(const jbyte* base1, int length1, const jbyte* base2,int length2);
  static bool   is_supplementary_character(const unsigned char* str);
  static jint   get_supplementary_character(const unsigned char* str);

  static bool   is_legal_utf8(const unsigned char* buffer, int length,
                              bool version_leq_47);
};


// Low-level interface for UNICODE strings

// A unicode string represents a string in the UTF-16 format in which supplementary
// characters are represented by surrogate pairs. Index values refer to char code
// units, so a supplementary character uses two positions in a unicode string.

class UNICODE : AllStatic {
 public:
  // checks if the given unicode character can be encoded as latin1
  static bool is_latin1(jchar c);

  // checks if the given string can be encoded as latin1
  static bool is_latin1(const jchar* base, int length);

  // returns the utf8 size of a unicode character
  static int utf8_size(jchar c);
  static int utf8_size(jbyte c);

  // returns the utf8 length of a unicode string
  template<typename T> static int utf8_length(const T* base, int length);

  // converts a unicode string to utf8 string
  static void convert_to_utf8(const jchar* base, int length, char* utf8_buffer);

  // converts a unicode string to a utf8 string; result is allocated
  // in resource area unless a buffer is provided. The unicode 'length'
  // parameter is set to the length of the result utf8 string.
  template<typename T> static char* as_utf8(const T* base, int& length);
  static char* as_utf8(const jchar* base, int length, char* buf, int buflen);
  static char* as_utf8(const jbyte* base, int length, char* buf, int buflen);

  // returns the quoted ascii length of a unicode string
  template<typename T> static int quoted_ascii_length(const T* base, int length);

  // converts a unicode string to quoted ascii
  template<typename T> static void as_quoted_ascii(const T* base, int length, char* buf, int buflen);
};

#endif // SHARE_UTILITIES_UTF8_HPP
