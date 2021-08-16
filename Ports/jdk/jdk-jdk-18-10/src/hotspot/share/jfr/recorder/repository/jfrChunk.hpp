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

#ifndef SHARE_VM_JFR_RECORDER_REPOSITORY_JFRRCHUNK_HPP
#define SHARE_VM_JFR_RECORDER_REPOSITORY_JFRRCHUNK_HPP

#include "jfr/utilities/jfrAllocation.hpp"

const u1 COMPLETE = 0;
const u1 GUARD = 0xff;
const u1 PAD = 0;

class JfrChunk : public JfrCHeapObj {
  friend class JfrChunkWriter;
  friend class JfrChunkHeadWriter;
 private:
  char* _path;
  int64_t _start_ticks;
  int64_t _previous_start_ticks;
  int64_t _start_nanos;
  int64_t _previous_start_nanos;
  int64_t _last_update_nanos;
  int64_t _last_checkpoint_offset;
  int64_t _last_metadata_offset;
  mutable u1 _generation;
  bool _final;

  JfrChunk();
  ~JfrChunk();
  void reset();

  const char* magic() const;
  u2 major_version() const;
  u2 minor_version() const;
  int64_t cpu_frequency() const;
  u2 flags() const;

  void mark_final();

  void update_start_ticks();
  void update_start_nanos();
  void save_current_and_update_start_ticks();
  void save_current_and_update_start_nanos();

  int64_t last_checkpoint_offset() const;
  void set_last_checkpoint_offset(int64_t offset);

  int64_t last_metadata_offset() const;
  void set_last_metadata_offset(int64_t offset);
  bool has_metadata() const;

  int64_t start_ticks() const;
  int64_t start_nanos() const;

  int64_t previous_start_ticks() const;
  int64_t previous_start_nanos() const;
  int64_t last_chunk_duration() const;

  void set_time_stamp();
  void update_current_nanos();

  void set_path(const char* path);
  const char* path() const;

  bool is_started() const;
  bool is_finished() const;

  int64_t duration() const;
  u1 generation() const;
  u1 next_generation() const;
};

#endif // SHARE_VM_JFR_RECORDER_REPOSITORY_JFRRCHUNK_HPP
