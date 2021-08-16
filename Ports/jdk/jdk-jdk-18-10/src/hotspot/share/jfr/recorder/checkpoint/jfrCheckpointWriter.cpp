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

#include "precompiled.hpp"
#include "jfr/recorder/checkpoint/jfrCheckpointManager.hpp"
#include "jfr/recorder/checkpoint/jfrCheckpointWriter.hpp"
#include "jfr/utilities/jfrBlob.hpp"
#include "jfr/writers/jfrBigEndianWriter.hpp"

JfrCheckpointFlush::JfrCheckpointFlush(Type* old, size_t used, size_t requested, Thread* t) :
  _result(JfrCheckpointManager::flush(old, used, requested, t)) {}

JfrCheckpointWriter::JfrCheckpointWriter(JfrCheckpointType type /* GENERIC */) :
  JfrCheckpointWriterBase(JfrCheckpointManager::lease(Thread::current()), Thread::current()),
  _time(JfrTicks::now()),
  _offset(0),
  _count(0),
  _type(type),
  _header(true) {
  assert(this->is_acquired(), "invariant");
  assert(0 == this->current_offset(), "invariant");
  if (_header) {
    reserve(sizeof(JfrCheckpointEntry));
  }
}

JfrCheckpointWriter::JfrCheckpointWriter(Thread* thread, bool header /* true */, JfrCheckpointType type /* GENERIC */, bool global_lease /* true */) :
  JfrCheckpointWriterBase(global_lease ? JfrCheckpointManager::lease(thread) : JfrCheckpointManager::lease_thread_local(thread), thread),
  _time(JfrTicks::now()),
  _offset(0),
  _count(0),
  _type(type),
  _header(header) {
  assert(this->is_acquired(), "invariant");
  assert(0 == this->current_offset(), "invariant");
  if (_header) {
    reserve(sizeof(JfrCheckpointEntry));
  }
}

JfrCheckpointWriter::JfrCheckpointWriter(bool previous_epoch, Thread* thread, JfrCheckpointType type /* GENERIC */) :
  JfrCheckpointWriterBase(JfrCheckpointManager::lease(thread, previous_epoch), thread),
  _time(JfrTicks::now()),
  _offset(0),
  _count(0),
  _type(type),
  _header(true) {
  assert(this->is_acquired(), "invariant");
  assert(0 == this->current_offset(), "invariant");
  if (_header) {
    reserve(sizeof(JfrCheckpointEntry));
  }
}

static void write_checkpoint_header(u1* pos, int64_t size, jlong time, u4 checkpoint_type, u4 type_count) {
  assert(pos != NULL, "invariant");
  JfrBigEndianWriter be_writer(pos, sizeof(JfrCheckpointEntry));
  be_writer.write(size);
  be_writer.write(time);
  be_writer.write(JfrTicks::now().value() - time);
  be_writer.write(checkpoint_type);
  be_writer.write(type_count);
  assert(be_writer.is_valid(), "invariant");
}

JfrCheckpointWriter::~JfrCheckpointWriter() {
  assert(this->is_acquired(), "invariant");
  if (!this->is_valid() || !_header) {
    release();
    return;
  }
  if (0 == count()) {
    assert(this->used_size() == sizeof(JfrCheckpointEntry), "invariant");
    this->seek(_offset);
    release();
    return;
  }
  assert(_header, "invariant");
  assert(this->is_valid(), "invariant");
  assert(count() > 0, "invariant");
  assert(this->used_size() > sizeof(JfrCheckpointEntry), "invariant");
  const int64_t size = this->current_offset();
  assert(size + this->start_pos() == this->current_pos(), "invariant");
  write_checkpoint_header(const_cast<u1*>(this->start_pos()), size, _time, (u4)_type, count());
  release();
}

u4 JfrCheckpointWriter::count() const {
  return _count;
}

void JfrCheckpointWriter::set_count(u4 count) {
  _count = count;
}

void JfrCheckpointWriter::release() {
  assert(this->is_acquired(), "invariant");
  if (!this->is_valid() || this->used_size() == 0) {
    return;
  }
  assert(this->used_size() > 0, "invariant");
  // write through to backing storage
  this->commit();
  assert(0 == this->current_offset(), "invariant");
}

void JfrCheckpointWriter::write_type(JfrTypeId type_id) {
  assert(type_id <= LAST_TYPE_ID, "type id overflow invariant");
  assert(type_id >= FIRST_TYPE_ID, "type id underflow invariant");
  write<u8>(type_id);
  increment();
}

void JfrCheckpointWriter::write_key(u8 key) {
  write(key);
}

void JfrCheckpointWriter::increment() {
  ++_count;
}

void JfrCheckpointWriter::write_count(u4 nof_entries) {
  write(nof_entries);
}

void JfrCheckpointWriter::write_count(u4 nof_entries, int64_t offset) {
  write_padded_at_offset(nof_entries, offset);
}

const u1* JfrCheckpointWriter::session_data(size_t* size, bool move /* false */, const JfrCheckpointContext* ctx /* 0 */) {
  assert(this->is_acquired(), "wrong state!");
  if (!this->is_valid()) {
    *size = 0;
    return NULL;
  }
  if (ctx != NULL) {
    const u1* session_start_pos = this->start_pos() + ctx->offset;
    *size = this->current_pos() - session_start_pos;
    return session_start_pos;
  }
  *size = this->used_size();
  assert(this->start_pos() + *size == this->current_pos(), "invariant");
  write_checkpoint_header(const_cast<u1*>(this->start_pos()), this->used_offset(), _time, (u4)_type, count());
  _header = false; // the header was just written
  if (move) {
    this->seek(_offset);
  }
  return this->start_pos();
}

const JfrCheckpointContext JfrCheckpointWriter::context() const {
  JfrCheckpointContext ctx;
  ctx.offset = this->current_offset();
  ctx.count = this->count();
  return ctx;
}

void JfrCheckpointWriter::set_context(const JfrCheckpointContext ctx) {
  this->seek(ctx.offset);
  set_count(ctx.count);
}
bool JfrCheckpointWriter::has_data() const {
  return this->used_size() > sizeof(JfrCheckpointEntry);
}

JfrBlobHandle JfrCheckpointWriter::copy(const JfrCheckpointContext* ctx /* 0 */) {
  size_t size = 0;
  const u1* data = session_data(&size, false, ctx);
  return JfrBlob::make(data, size);
}

JfrBlobHandle JfrCheckpointWriter::move(const JfrCheckpointContext* ctx /* 0 */) {
  size_t size = 0;
  const u1* data = session_data(&size, true, ctx);
  JfrBlobHandle blob = JfrBlob::make(data, size);
  if (ctx != NULL) {
    const_cast<JfrCheckpointContext*>(ctx)->count = 0;
    set_context(*ctx);
  }
  return blob;
}
