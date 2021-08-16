/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_LEAKPROFILER_CHECKPOINT_OBJECTSAMPLEDESCRIPTION_HPP
#define SHARE_JFR_LEAKPROFILER_CHECKPOINT_OBJECTSAMPLEDESCRIPTION_HPP

#define OBJECT_SAMPLE_DESCRIPTION_BUFFER_SIZE 100

#include "memory/allocation.hpp"

class outputStream;

class ObjectDescriptionBuilder : public StackObj {
private:
  char _buffer[OBJECT_SAMPLE_DESCRIPTION_BUFFER_SIZE];
  size_t _index;

public:
  ObjectDescriptionBuilder();

  void write_text(const char* text);
  void write_int(jint value);
  void reset();

  void print_description(outputStream* out);
  const char* description();
};

class ObjectSampleDescription : public StackObj {
private:
  ObjectDescriptionBuilder _description;
  oop _object;

  void write_text(const char* text);
  void write_int(jint value);

  void write_object_details();
  void write_size(jint size);
  void write_thread_name();
  void write_thread_group_name();
  void write_class_name();
  void write_object_to_buffer();
  bool is_class(Symbol* s1, const char* s2);
  void ensure_initialized();
  bool read_int_size(jint* result);

public:
  ObjectSampleDescription(oop object);
  void print_description(outputStream* out);
  const char* description();
};

#endif // SHARE_JFR_LEAKPROFILER_CHECKPOINT_OBJECTSAMPLEDESCRIPTION_HPP
