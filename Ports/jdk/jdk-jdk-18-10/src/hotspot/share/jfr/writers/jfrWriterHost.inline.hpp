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

#ifndef SHARE_JFR_WRITERS_JFRWRITERHOST_INLINE_HPP
#define SHARE_JFR_WRITERS_JFRWRITERHOST_INLINE_HPP

#include "jfr/writers/jfrWriterHost.hpp"

#include "classfile/javaClasses.hpp"
#include "jfr/recorder/checkpoint/types/traceid/jfrTraceId.inline.hpp"
#include "jfr/recorder/service/jfrOptionSet.hpp"
#include "jfr/writers/jfrEncoding.hpp"
#include "memory/resourceArea.hpp"
#include "oops/oop.hpp"
#include "oops/symbol.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "runtime/jniHandles.hpp"

inline bool compressed_integers() {
  static const bool comp_integers = JfrOptionSet::compressed_integers();
  return comp_integers;
}

template <typename BE, typename IE, typename WriterPolicyImpl >
template <typename T>
inline void WriterHost<BE, IE, WriterPolicyImpl>::write_padded(T value) {
  write_padded(&value, 1);
}

template <typename BE, typename IE, typename WriterPolicyImpl >
template <typename T>
inline void WriterHost<BE, IE, WriterPolicyImpl>::write_padded(const T* value, size_t len) {
  assert(value != NULL, "invariant");
  assert(len > 0, "invariant");
  u1* const pos = ensure_size(sizeof(T) * len);
  if (pos) {
    this->set_current_pos(write_padded(value, len, pos));
  }
}

template <typename BE, typename IE, typename WriterPolicyImpl >
template <typename T>
inline u1* WriterHost<BE, IE, WriterPolicyImpl>::write_padded(const T* value, size_t len, u1* pos) {
  assert(value != NULL, "invariant");
  assert(len > 0, "invariant");
  assert(pos != NULL, "invariant");
  return _compressed_integers ? IE::write_padded(value, len, pos) : BE::write_padded(value, len, pos);
}

template <typename BE, typename IE, typename WriterPolicyImpl >
template <typename T>
inline void WriterHost<BE, IE, WriterPolicyImpl>::write(const T* value, size_t len) {
  assert(value != NULL, "invariant");
  assert(len > 0, "invariant");
  // Might need T + 1 size
  u1* const pos = ensure_size(sizeof(T) * len + len);
  if (pos) {
    this->set_current_pos(write(value, len, pos));
  }
}

template <typename BE, typename IE, typename WriterPolicyImpl >
template <typename T>
inline u1* WriterHost<BE, IE, WriterPolicyImpl>::write(const T* value, size_t len, u1* pos) {
  assert(value != NULL, "invariant");
  assert(len > 0, "invariant");
  assert(pos != NULL, "invariant");
  return _compressed_integers ? IE::write(value, len, pos) : BE::write(value, len, pos);
}

template <typename BE, typename IE, typename WriterPolicyImpl>
void WriterHost<BE, IE, WriterPolicyImpl>::write_utf8(const char* value) {
  if (NULL == value) {
    // only write encoding byte indicating NULL string
    write<u1>(NULL_STRING);
    return;
  }
  write<u1>(UTF8); // designate encoding
  const jint len = MIN2<jint>(max_jint, (jint)strlen(value));
  write(len);
  if (len > 0) {
    be_write(value, len);
  }
}

template <typename BE, typename IE, typename WriterPolicyImpl>
void WriterHost<BE, IE, WriterPolicyImpl>::write_utf16(const jchar* value, jint len) {
  assert(value != NULL, "invariant");
  write((u1)UTF16); // designate encoding
  write(len);
  if (len > 0) {
    write(value, len);
  }
}

template <typename BE, typename IE, typename WriterPolicyImpl >
template <typename T>
inline void WriterHost<BE, IE, WriterPolicyImpl>::be_write(T value) {
  be_write(&value, 1);
}

template <typename BE, typename IE, typename WriterPolicyImpl >
template <typename T>
inline void WriterHost<BE, IE, WriterPolicyImpl>::be_write(const T* value, size_t len) {
  assert(value != NULL, "invariant");
  assert(len > 0, "invariant");
  // Might need T + 1 size
  u1* const pos = ensure_size(sizeof(T) * len + len);
  if (pos) {
    this->set_current_pos(BE::be_write(value, len, pos));
  }
}

template <typename BE, typename IE, typename WriterPolicyImpl >
template <typename StorageType>
inline WriterHost<BE, IE, WriterPolicyImpl>::WriterHost(StorageType* storage, Thread* thread) :
  WriterPolicyImpl(storage, thread),
  _compressed_integers(compressed_integers()) {
}

// Extra size added as a safety cushion when dimensioning memory.
// With varint encoding, the worst case is
// associated with writing negative values.
// For example, writing a negative s1 (-1)
// will encode as 0xff 0x0f (2 bytes).
static const size_t size_safety_cushion = 1;

template <typename BE, typename IE, typename WriterPolicyImpl >
template <typename StorageType>
inline WriterHost<BE, IE, WriterPolicyImpl>::WriterHost(StorageType* storage, size_t size) :
  WriterPolicyImpl(storage, size + size_safety_cushion),
  _compressed_integers(compressed_integers()) {
}

template <typename BE, typename IE, typename WriterPolicyImpl >
inline WriterHost<BE, IE, WriterPolicyImpl>::WriterHost(Thread* thread) :
  WriterPolicyImpl(thread),
  _compressed_integers(compressed_integers()) {
}

template <typename BE, typename IE, typename WriterPolicyImpl>
inline u1* WriterHost<BE, IE, WriterPolicyImpl>::ensure_size(size_t requested_size) {
  if (!this->is_valid()) {
    // cancelled
    return NULL;
  }
  if (this->available_size() < requested_size) {
    if (!this->accommodate(this->used_size(), requested_size)) {
      assert(!this->is_valid(), "invariant");
      return NULL;
    }
  }
  assert(requested_size <= this->available_size(), "invariant");
  return this->current_pos();
}

template <typename BE, typename IE, typename WriterPolicyImpl>
template <typename T>
inline void WriterHost<BE, IE, WriterPolicyImpl>::write(T value) {
  write(&value, 1);
}

template <typename BE, typename IE, typename WriterPolicyImpl>
inline void WriterHost<BE, IE, WriterPolicyImpl>::write(bool value) {
  be_write((u1)value);
}

template <typename BE, typename IE, typename WriterPolicyImpl>
inline void WriterHost<BE, IE, WriterPolicyImpl>::write(float value) {
  be_write(*(u4*)&(value));
}

template <typename BE, typename IE, typename WriterPolicyImpl>
inline void WriterHost<BE, IE, WriterPolicyImpl>::write(double value) {
  be_write(*(u8*)&(value));
}

template <typename BE, typename IE, typename WriterPolicyImpl>
inline void WriterHost<BE, IE, WriterPolicyImpl>::write(const char* value) {
  // UTF-8, max_jint len
  write_utf8(value);
}

template <typename BE, typename IE, typename WriterPolicyImpl>
inline void WriterHost<BE, IE, WriterPolicyImpl>::write(char* value) {
  write(const_cast<const char*>(value));
}

template <typename BE, typename IE, typename WriterPolicyImpl>
inline void WriterHost<BE, IE, WriterPolicyImpl>::write(jstring string) {
  if (string == NULL) {
    write<u1>(NULL_STRING);
    return;
  }
  const oop string_oop = JNIHandles::resolve_external_guard(string);
  assert(string_oop != NULL, "invariant");
  const size_t length = (size_t)java_lang_String::length(string_oop);
  if (0 == length) {
    write<u1>(EMPTY_STRING);
    return;
  }
  const bool is_latin1_encoded = java_lang_String::is_latin1(string_oop);
  const typeArrayOop value = java_lang_String::value(string_oop);
  assert(value != NULL, "invariant");
  if (is_latin1_encoded) {
    write<u1>(LATIN1);
    write<u4>((u4)length);
    be_write(value->byte_at_addr(0), length);
  } else {
    write<u1>(UTF16);
    write<u4>((u4)length);
    write(value->char_at_addr(0), length);
  }
}

template <typename Writer, typename T>
inline void tag_write(Writer* w, const T* t) {
  assert(w != NULL, "invariant");
  const traceid id = t == NULL ? 0 : JfrTraceId::load(t);
  w->write(id);
}

template <typename BE, typename IE, typename WriterPolicyImpl>
void WriterHost<BE, IE, WriterPolicyImpl>::write(const ClassLoaderData* cld) {
  tag_write(this, cld);
}

template <typename BE, typename IE, typename WriterPolicyImpl>
void WriterHost<BE, IE, WriterPolicyImpl>::write(const Klass* klass) {
  tag_write(this, klass);
}

template <typename BE, typename IE, typename WriterPolicyImpl>
void WriterHost<BE, IE, WriterPolicyImpl>::write(const Method* method) {
  tag_write(this, method);
}

template <typename BE, typename IE, typename WriterPolicyImpl>
void WriterHost<BE, IE, WriterPolicyImpl>::write(const ModuleEntry* module) {
  tag_write(this, module);
}

template <typename BE, typename IE, typename WriterPolicyImpl>
void WriterHost<BE, IE, WriterPolicyImpl>::write(const PackageEntry* package) {
  tag_write(this, package);
}

template <typename BE, typename IE, typename WriterPolicyImpl>
void WriterHost<BE, IE, WriterPolicyImpl>::write(const Symbol* symbol) {
  ResourceMark rm;
  write_utf8(symbol != NULL ? symbol->as_C_string() : NULL);
}

template <typename BE, typename IE, typename WriterPolicyImpl>
void WriterHost<BE, IE, WriterPolicyImpl>::write(const Ticks& time) {
  write(JfrTime::is_ft_enabled() ? time.ft_value() : time.value());
}

template <typename BE, typename IE, typename WriterPolicyImpl>
void WriterHost<BE, IE, WriterPolicyImpl>::write(const Tickspan& time) {
  write(JfrTime::is_ft_enabled() ? time.ft_value() : time.value());
}

template <typename BE, typename IE, typename WriterPolicyImpl>
void WriterHost<BE, IE, WriterPolicyImpl>::write(const JfrTicks& time) {
  write(time.value());
}

template <typename BE, typename IE, typename WriterPolicyImpl>
void WriterHost<BE, IE, WriterPolicyImpl>::write(const JfrTickspan& time) {
  write(time.value());
}

template <typename BE, typename IE, typename WriterPolicyImpl>
void WriterHost<BE, IE, WriterPolicyImpl>::write_bytes(const void* buf, intptr_t len) {
  assert(len >= 0, "invariant");
  u1* const pos = this->ensure_size((size_t)len);
  if (pos != NULL) {
    WriterPolicyImpl::write_bytes(pos, buf, len); // WriterPolicyImpl responsible for position update
  }
}

// UTF-8 for use with classfile/bytecodes
template <typename BE, typename IE, typename WriterPolicyImpl>
inline void WriterHost<BE, IE, WriterPolicyImpl>::write_utf8_u2_len(const char* value) {
  u2 len = 0;
  if (value != NULL) {
    len = MIN2<u2>(max_jushort, (u2)strlen(value));
  }
  write(len);
  if (len > 0) {
    be_write(value, len);
  }
}

template <typename BE, typename IE, typename WriterPolicyImpl>
inline int64_t WriterHost<BE, IE, WriterPolicyImpl>::reserve(size_t size) {
  if (ensure_size(size) != NULL) {
    const int64_t reserved_offset = this->current_offset();
    this->set_current_pos(size);
    return reserved_offset;
  }
  this->cancel();
  return 0;
}

template <typename BE, typename IE, typename WriterPolicyImpl>
template <typename T>
inline void WriterHost<BE, IE, WriterPolicyImpl>::write_padded_at_offset(T value, int64_t offset) {
  if (this->is_valid()) {
    const int64_t current = this->current_offset();
    this->seek(offset);
    write_padded(value);
    this->seek(current); // restore
  }
}

template <typename BE, typename IE, typename WriterPolicyImpl>
template <typename T>
inline void WriterHost<BE, IE, WriterPolicyImpl>::write_at_offset(T value, int64_t offset) {
  if (this->is_valid()) {
    const int64_t current = this->current_offset();
    this->seek(offset);
    write(value);
    this->seek(current); // restore
  }
}

template <typename BE, typename IE, typename WriterPolicyImpl>
template <typename T>
inline void WriterHost<BE, IE, WriterPolicyImpl>::write_be_at_offset(T value, int64_t offset) {
  if (this->is_valid()) {
    const int64_t current = this->current_offset();
    this->seek(offset);
    be_write(value);
    this->seek(current); // restore
  }
}

#endif // SHARE_JFR_WRITERS_JFRWRITERHOST_INLINE_HPP
