/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/classFileStream.hpp"
#include "classfile/classLoader.hpp"
#include "classfile/vmSymbols.hpp"
#include "memory/resourceArea.hpp"

const bool ClassFileStream::verify = true;

void ClassFileStream::truncated_file_error(TRAPS) const {
  THROW_MSG(vmSymbols::java_lang_ClassFormatError(), "Truncated class file");
}

ClassFileStream::ClassFileStream(const u1* buffer,
                                 int length,
                                 const char* source,
                                 bool verify_stream,
                                 bool from_boot_loader_modules_image) :
  _buffer_start(buffer),
  _buffer_end(buffer + length),
  _current(buffer),
  _source(source),
  _need_verify(verify_stream),
  _from_boot_loader_modules_image(from_boot_loader_modules_image) {
    assert(buffer != NULL, "caller should throw NPE");
}

const u1* ClassFileStream::clone_buffer() const {
  u1* const new_buffer_start = NEW_RESOURCE_ARRAY(u1, length());
  memcpy(new_buffer_start, _buffer_start, length());
  return new_buffer_start;
}

const char* const ClassFileStream::clone_source() const {
  const char* const src = source();
  char* source_copy = NULL;
  if (src != NULL) {
    size_t source_len = strlen(src);
    source_copy = NEW_RESOURCE_ARRAY(char, source_len + 1);
    strncpy(source_copy, src, source_len + 1);
  }
  return source_copy;
}

// Caller responsible for ResourceMark
// clone stream with a rewound position
const ClassFileStream* ClassFileStream::clone() const {
  const u1* const new_buffer_start = clone_buffer();
  return new ClassFileStream(new_buffer_start,
                             length(),
                             clone_source(),
                             need_verify(),
                             from_boot_loader_modules_image());
}
