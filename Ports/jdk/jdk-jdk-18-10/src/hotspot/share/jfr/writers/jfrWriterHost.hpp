/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_WRITERS_JFRWRITERHOST_HPP
#define SHARE_JFR_WRITERS_JFRWRITERHOST_HPP

#include "jni.h"
#include "utilities/globalDefinitions.hpp"
#include "jfr/utilities/jfrTime.hpp"

class ClassLoaderData;
class Klass;
class Method;
class ModuleEntry;
class PackageEntry;
class Symbol;
class Thread;

// BE == Base Encoder
// IE == Integer Encoder
template <typename BE, typename IE, typename WriterPolicyImpl >
class WriterHost : public WriterPolicyImpl {
 private:
  const bool _compressed_integers;

  template <typename T>
  void write_padded(T value);
  template <typename T>
  void write_padded(const T* value, size_t len);
  template <typename T>
  u1* write_padded(const T* value, size_t len, u1* pos);
  template <typename T>
  void write(const T* value, size_t len);
  template <typename T>
  u1* write(const T* value, size_t len, u1* pos);
  void write_utf8(const char* value);
  void write_utf16(const jchar* value, jint len);

 protected:
  template <typename T>
  void be_write(T value);
  template <typename T>
  void be_write(const T* value, size_t len);
  template <typename StorageType>
  WriterHost(StorageType* storage, Thread* thread);
  template <typename StorageType>
  WriterHost(StorageType* storage, size_t size);
  WriterHost(Thread* thread);
  u1* ensure_size(size_t requested_size);

 public:
  template <typename T>
  void write(T value);
  void write(bool value);
  void write(float value);
  void write(double value);
  void write(const char* value);
  void write(char* value);
  void write(jstring value);
  void write(const ClassLoaderData* cld);
  void write(const Klass* klass);
  void write(const Method* method);
  void write(const ModuleEntry* module);
  void write(const PackageEntry* package);
  void write(const Symbol* symbol);
  void write(const Ticks& time);
  void write(const Tickspan& time);
  void write(const JfrTicks& time);
  void write(const JfrTickspan& time);
  void write_bytes(const void* buf, intptr_t len);
  void write_utf8_u2_len(const char* value);
  template <typename T>
  void write_padded_at_offset(T value, int64_t offset);
  template <typename T>
  void write_at_offset(T value, int64_t offset);
  template <typename T>
  void write_be_at_offset(T value, int64_t offset);
  int64_t reserve(size_t size);
};

#endif // SHARE_JFR_WRITERS_JFRWRITERHOST_HPP
