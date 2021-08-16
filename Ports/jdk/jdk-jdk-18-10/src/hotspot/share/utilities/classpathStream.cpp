/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "memory/allocation.inline.hpp"
#include "runtime/os.hpp"
#include "utilities/classpathStream.hpp"

const char* ClasspathStream::get_next() {
  while (_class_path[_end] != '\0' && _class_path[_end] != os::path_separator()[0]) {
    _end++;
  }
  int path_len = _end - _start;
  char* path = NEW_RESOURCE_ARRAY(char, path_len + 1);
  strncpy(path, &_class_path[_start], path_len);
  path[path_len] = '\0';

  while (_class_path[_end] == os::path_separator()[0]) {
    _end++;
  }
  _start = _end;
  return path;
}
