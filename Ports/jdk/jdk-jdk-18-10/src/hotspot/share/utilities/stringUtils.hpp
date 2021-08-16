/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_STRINGUTILS_HPP
#define SHARE_UTILITIES_STRINGUTILS_HPP

#include "memory/allocation.hpp"

class StringUtils : AllStatic {
public:
  // Replace the substring <from> with another string <to>. <to> must be
  // no longer than <from>. The input string is modified in-place.
  //
  // Replacement is done in a single pass left-to-right. So replace_no_expand("aaa", "aa", "a")
  // will result in "aa", not "a".
  //
  // Returns the count of substrings that have been replaced.
  static int replace_no_expand(char* string, const char* from, const char* to);

  // Compute string similarity based on Dice's coefficient
  static double similarity(const char* str1, size_t len1, const char* str2, size_t len2);
};

#endif // SHARE_UTILITIES_STRINGUTILS_HPP
