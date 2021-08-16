/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/recorder/repository/jfrChunk.hpp"
#include "jfr/recorder/repository/jfrChunkWriter.hpp"
#include "jfr/utilities/jfrTime.hpp"
#include "jfr/utilities/jfrTypes.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/os.hpp"

static const int64_t MAGIC_OFFSET = 0;
static const int64_t MAGIC_LEN = 4;
static const int64_t VERSION_OFFSET = MAGIC_LEN;
static const int64_t SIZE_OFFSET = 8;
static const int64_t SLOT_SIZE = 8;
static const int64_t CHECKPOINT_OFFSET = SIZE_OFFSET + SLOT_SIZE;
static const int64_t METADATA_OFFSET = CHECKPOINT_OFFSET + SLOT_SIZE;
static const int64_t START_NANOS_OFFSET = METADATA_OFFSET + SLOT_SIZE;
static const int64_t DURATION_NANOS_OFFSET = START_NANOS_OFFSET + SLOT_SIZE;
static const int64_t START_TICKS_OFFSET = DURATION_NANOS_OFFSET + SLOT_SIZE;
static const int64_t CPU_FREQUENCY_OFFSET = START_TICKS_OFFSET + SLOT_SIZE;
static const int64_t GENERATION_OFFSET = CPU_FREQUENCY_OFFSET + SLOT_SIZE;
static const int64_t FLAG_OFFSET = GENERATION_OFFSET + 2;
static const int64_t HEADER_SIZE = FLAG_OFFSET + 2;

static fio_fd open_chunk(const char* path) {
  return path != NULL ? os::open(path, O_CREAT | O_RDWR, S_IREAD | S_IWRITE) : invalid_fd;
}

#ifdef ASSERT
static void assert_writer_position(JfrChunkWriter* writer, int64_t offset) {
  assert(writer != NULL, "invariant");
  assert(offset == writer->current_offset(), "invariant");
}
#endif

class JfrChunkHeadWriter : public StackObj {
 private:
  JfrChunkWriter* _writer;
  JfrChunk* _chunk;
 public:
  void write_magic() {
    _writer->write_bytes(_chunk->magic(), MAGIC_LEN);
  }

  void write_version() {
    _writer->be_write(_chunk->major_version());
    _writer->be_write(_chunk->minor_version());
  }

  void write_size(int64_t size) {
    _writer->be_write(size);
  }

  void write_checkpoint() {
    _writer->be_write(_chunk->last_checkpoint_offset());
  }

  void write_metadata() {
    _writer->be_write(_chunk->last_metadata_offset());
  }

  void write_time(bool finalize) {
    if (finalize) {
      _writer->be_write(_chunk->previous_start_nanos());
      _writer->be_write(_chunk->last_chunk_duration());
      _writer->be_write(_chunk->previous_start_ticks());
      return;
    }
    _writer->be_write(_chunk->start_nanos());
    _writer->be_write(_chunk->duration());
    _writer->be_write(_chunk->start_ticks());
  }

  void write_cpu_frequency() {
    _writer->be_write(_chunk->cpu_frequency());
  }

  void write_generation(bool finalize) {
    _writer->be_write(finalize ? COMPLETE : _chunk->generation());
    _writer->be_write(PAD);
  }

  void write_next_generation(bool finalize) {
    _writer->be_write(finalize ? COMPLETE : _chunk->next_generation());
    _writer->be_write(PAD);
  }

  void write_guard() {
    _writer->be_write(GUARD);
    _writer->be_write(PAD);
  }

  void write_guard_flush() {
    write_guard();
    _writer->flush();
  }

  void write_flags() {
    _writer->be_write(_chunk->flags());
  }

  void write_size_to_generation(int64_t size, bool finalize) {
    write_size(size);
    write_checkpoint();
    write_metadata();
    write_time(finalize);
    write_cpu_frequency();
    write_generation(finalize);
  }

  void flush(int64_t size, bool finalize) {
    assert(_writer->is_valid(), "invariant");
    assert(_chunk != NULL, "invariant");
    DEBUG_ONLY(assert_writer_position(_writer, SIZE_OFFSET);)
    write_size_to_generation(size, finalize);
    write_flags();
    _writer->seek(size); // implicit flush
  }

  void initialize() {
    assert(_writer->is_valid(), "invariant");
    assert(_chunk != NULL, "invariant");
    DEBUG_ONLY(assert_writer_position(_writer, 0);)
    write_magic();
    write_version();
    write_size_to_generation(HEADER_SIZE, false);
    write_flags();
    DEBUG_ONLY(assert_writer_position(_writer, HEADER_SIZE);)
    _writer->flush();
  }

  JfrChunkHeadWriter(JfrChunkWriter* writer, int64_t offset, bool guard = true) : _writer(writer), _chunk(writer->_chunk) {
    assert(_writer != NULL, "invariant");
    assert(_writer->is_valid(), "invariant");
    assert(_chunk != NULL, "invariant");
    if (0 == _writer->current_offset()) {
      assert(HEADER_SIZE == offset, "invariant");
      initialize();
    } else {
      if (guard) {
        _writer->seek(GENERATION_OFFSET);
        write_guard();
        _writer->seek(offset);
      } else {
        _chunk->update_current_nanos();
      }
    }
    DEBUG_ONLY(assert_writer_position(_writer, offset);)
  }
};

static int64_t prepare_chunk_header_constant_pool(JfrChunkWriter& cw, int64_t event_offset, bool flushpoint) {
  const int64_t delta = cw.last_checkpoint_offset() == 0 ? 0 : cw.last_checkpoint_offset() - event_offset;
  const u4 checkpoint_type = flushpoint ? (u4)(FLUSH | HEADER) : (u4)HEADER;
  cw.reserve(sizeof(u4));
  cw.write<u8>(EVENT_CHECKPOINT);
  cw.write<u8>(JfrTicks::now().value());
  cw.write<u8>(0); // duration
  cw.write<u8>(delta); // to previous checkpoint
  cw.write<u4>(checkpoint_type);
  cw.write<u4>(1); // pool count
  cw.write<u8>(TYPE_CHUNKHEADER);
  cw.write<u4>(1); // count
  cw.write<u8>(1); // key
  cw.write<u4>(HEADER_SIZE); // length of byte array
  return cw.current_offset();
}

int64_t JfrChunkWriter::write_chunk_header_checkpoint(bool flushpoint) {
  assert(this->has_valid_fd(), "invariant");
  const int64_t event_size_offset = current_offset();
  const int64_t header_content_pos = prepare_chunk_header_constant_pool(*this, event_size_offset, flushpoint);
  JfrChunkHeadWriter head(this, header_content_pos, false);
  head.write_magic();
  head.write_version();
  const int64_t chunk_size_offset = reserve(sizeof(int64_t)); // size to be decided when we are done
  be_write(event_size_offset); // last checkpoint offset will be this checkpoint
  head.write_metadata();
  head.write_time(!flushpoint);
  head.write_cpu_frequency();
  head.write_next_generation(!flushpoint);
  head.write_flags();
  assert(current_offset() - header_content_pos == HEADER_SIZE, "invariant");
  const u4 checkpoint_size = current_offset() - event_size_offset;
  write_padded_at_offset<u4>(checkpoint_size, event_size_offset);
  set_last_checkpoint_offset(event_size_offset);
  const size_t sz_written = size_written();
  write_be_at_offset(sz_written, chunk_size_offset);
  return sz_written;
}

void JfrChunkWriter::mark_chunk_final() {
  assert(_chunk != NULL, "invariant");
  _chunk->mark_final();
}

int64_t JfrChunkWriter::flush_chunk(bool flushpoint) {
  assert(_chunk != NULL, "invariant");
  const int64_t sz_written = write_chunk_header_checkpoint(flushpoint);
  assert(size_written() == sz_written, "invariant");
  JfrChunkHeadWriter head(this, SIZE_OFFSET);
  head.flush(sz_written, !flushpoint);
  return sz_written;
}

JfrChunkWriter::JfrChunkWriter() : JfrChunkWriterBase(NULL), _chunk(new JfrChunk()) {}

JfrChunkWriter::~JfrChunkWriter() {
  assert(_chunk != NULL, "invariant");
  delete _chunk;
}

void JfrChunkWriter::set_path(const char* path) {
  assert(_chunk != NULL, "invariant");
  _chunk->set_path(path);
}

void JfrChunkWriter::set_time_stamp() {
  assert(_chunk != NULL, "invariant");
  _chunk->set_time_stamp();
}

int64_t JfrChunkWriter::size_written() const {
  return this->is_valid() ? this->current_offset() : 0;
}

int64_t JfrChunkWriter::last_checkpoint_offset() const {
  assert(_chunk != NULL, "invariant");
  return _chunk->last_checkpoint_offset();
}

int64_t JfrChunkWriter::current_chunk_start_nanos() const {
  assert(_chunk != NULL, "invariant");
  return _chunk->start_nanos();
}

void JfrChunkWriter::set_last_checkpoint_offset(int64_t offset) {
  assert(_chunk != NULL, "invariant");
  _chunk->set_last_checkpoint_offset(offset);
}

void JfrChunkWriter::set_last_metadata_offset(int64_t offset) {
  assert(_chunk != NULL, "invariant");
  _chunk->set_last_metadata_offset(offset);
}

bool JfrChunkWriter::has_metadata() const {
  assert(_chunk != NULL, "invariant");
  return _chunk->has_metadata();
}

bool JfrChunkWriter::open() {
  assert(_chunk != NULL, "invariant");
  JfrChunkWriterBase::reset(open_chunk(_chunk->path()));
  const bool is_open = this->has_valid_fd();
  if (is_open) {
    assert(0 == this->current_offset(), "invariant");
    _chunk->reset();
    JfrChunkHeadWriter head(this, HEADER_SIZE);
  }
  return is_open;
}

int64_t JfrChunkWriter::close() {
  assert(this->has_valid_fd(), "invariant");
  const int64_t size_written = flush_chunk(false);
  this->close_fd();
  assert(!this->is_valid(), "invariant");
  return size_written;
}
