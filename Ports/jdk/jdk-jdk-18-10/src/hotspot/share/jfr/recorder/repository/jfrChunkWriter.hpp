/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_RECORDER_REPOSITORY_JFRCHUNKWRITER_HPP
#define SHARE_JFR_RECORDER_REPOSITORY_JFRCHUNKWRITER_HPP

#include "jfr/writers/jfrStorageAdapter.hpp"
#include "jfr/writers/jfrStreamWriterHost.inline.hpp"
#include "jfr/writers/jfrWriterHost.inline.hpp"

typedef MallocAdapter<M> JfrChunkBuffer; // 1 mb buffered writes
typedef StreamWriterHost<JfrChunkBuffer, JfrCHeapObj> JfrBufferedChunkWriter;
typedef WriterHost<BigEndianEncoder, CompressedIntegerEncoder, JfrBufferedChunkWriter> JfrChunkWriterBase;

class JfrChunk;
class JfrChunkHeadWriter;

class JfrChunkWriter : public JfrChunkWriterBase {
  friend class JfrChunkHeadWriter;
  friend class JfrRepository;
 private:
  JfrChunk* _chunk;
  void set_path(const char* path);
  int64_t flush_chunk(bool flushpoint);
  bool open();
  int64_t close();
  int64_t current_chunk_start_nanos() const;
  int64_t write_chunk_header_checkpoint(bool flushpoint);

 public:
  JfrChunkWriter();
  ~JfrChunkWriter();

  int64_t size_written() const;
  int64_t last_checkpoint_offset() const;
  void set_last_checkpoint_offset(int64_t offset);
  void set_last_metadata_offset(int64_t offset);

  bool has_metadata() const;
  void set_time_stamp();
  void mark_chunk_final();
};

#endif // SHARE_JFR_RECORDER_REPOSITORY_JFRCHUNKWRITER_HPP
