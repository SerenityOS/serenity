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
#include "jfr/recorder/service/jfrOptionSet.hpp"
#include "jfr/utilities/jfrTime.hpp"
#include "jfr/utilities/jfrTimeConverter.hpp"
#include "jfr/utilities/jfrTypes.hpp"
#include "runtime/os.hpp"

static const char* const MAGIC = "FLR";
static const u2 JFR_VERSION_MAJOR = 2;
static const u2 JFR_VERSION_MINOR = 1;

// strictly monotone
static jlong nanos_now() {
  static jlong last = 0;

  jlong seconds;
  jlong nanos;
  // Use same clock source as Instant.now() to ensure
  // that Recording::getStopTime() returns an Instant that
  // is in sync.
  os::javaTimeSystemUTC(seconds, nanos);
  const jlong now = seconds * 1000000000 + nanos;
  if (now > last) {
    last = now;
  }
  return last;
}

static jlong ticks_now() {
  return JfrTicks::now();
}

JfrChunk::JfrChunk() :
  _path(NULL),
  _start_ticks(0),
  _previous_start_ticks(invalid_time),
  _start_nanos(0),
  _previous_start_nanos(invalid_time),
  _last_update_nanos(0),
  _last_checkpoint_offset(0),
  _last_metadata_offset(0),
  _generation(1),
  _final(false) {}

JfrChunk::~JfrChunk() {
  reset();
}

void JfrChunk::reset() {
  if (_path != NULL) {
    JfrCHeapObj::free(_path, strlen(_path) + 1);
    _path = NULL;
  }
  _last_checkpoint_offset = _last_metadata_offset = 0;
  _generation = 1;
}

const char* JfrChunk::magic() const {
  return MAGIC;
}

u2 JfrChunk::major_version() const {
  return JFR_VERSION_MAJOR;
}

u2 JfrChunk::minor_version() const {
  return JFR_VERSION_MINOR;
}

void JfrChunk::mark_final() {
  _final = true;
}

u2 JfrChunk::flags() const {
  // chunk capabilities, CompressedIntegers etc
  u2 flags = 0;
  if (JfrOptionSet::compressed_integers()) {
    flags |= 1 << 0;
  }
  if (_final) {
    flags |= 1 << 1;
  }
  return flags;
}

int64_t JfrChunk::cpu_frequency() const {
  static const jlong frequency = JfrTime::frequency();
  return frequency;
}

void JfrChunk::set_last_checkpoint_offset(int64_t offset) {
  _last_checkpoint_offset = offset;
}

int64_t JfrChunk::last_checkpoint_offset() const {
  return _last_checkpoint_offset;
}

int64_t JfrChunk::start_ticks() const {
  assert(_start_ticks != 0, "invariant");
  return _start_ticks;
}

int64_t JfrChunk::start_nanos() const {
  return _start_nanos;
}

int64_t JfrChunk::previous_start_ticks() const {
  assert(_previous_start_ticks != invalid_time, "invariant");
  return _previous_start_ticks;
}

int64_t JfrChunk::previous_start_nanos() const {
  assert(_previous_start_nanos != invalid_time, "invariant");
  return _previous_start_nanos;
}

void JfrChunk::update_start_ticks() {
  _start_ticks = ticks_now();
}

void JfrChunk::update_start_nanos() {
  const jlong now = nanos_now();
  assert(now >= _start_nanos, "invariant");
  assert(now >= _last_update_nanos, "invariant");
  _start_nanos = _last_update_nanos = now;
}

void JfrChunk::update_current_nanos() {
  const jlong now = nanos_now();
  assert(now >= _last_update_nanos, "invariant");
  _last_update_nanos = now;
}

void JfrChunk::save_current_and_update_start_ticks() {
  _previous_start_ticks = _start_ticks;
  update_start_ticks();
}

void JfrChunk::save_current_and_update_start_nanos() {
  _previous_start_nanos = _start_nanos;
  update_start_nanos();
}

void JfrChunk::set_time_stamp() {
  save_current_and_update_start_nanos();
  save_current_and_update_start_ticks();
}

int64_t JfrChunk::last_chunk_duration() const {
  assert(_previous_start_nanos != invalid_time, "invariant");
  return _start_nanos - _previous_start_nanos;
}

static char* copy_path(const char* path) {
  assert(path != NULL, "invariant");
  const size_t path_len = strlen(path);
  char* new_path = JfrCHeapObj::new_array<char>(path_len + 1);
  strncpy(new_path, path, path_len + 1);
  return new_path;
}

void JfrChunk::set_path(const char* path) {
  if (_path != NULL) {
    JfrCHeapObj::free(_path, strlen(_path) + 1);
    _path = NULL;
  }
  if (path != NULL) {
    _path = copy_path(path);
  }
}

const char* JfrChunk::path() const {
  return _path;
}

bool JfrChunk::is_started() const {
  return _start_nanos != 0;
}

bool JfrChunk::is_finished() const {
  return 0 == _generation;
}

int64_t JfrChunk::duration() const {
  assert(_last_update_nanos >= _start_nanos, "invariant");
  return _last_update_nanos - _start_nanos;
}

int64_t JfrChunk::last_metadata_offset() const {
  return _last_metadata_offset;
}

void JfrChunk::set_last_metadata_offset(int64_t offset) {
  assert(offset > _last_metadata_offset, "invariant");
  _last_metadata_offset = offset;
}

bool JfrChunk::has_metadata() const {
  return 0 != _last_metadata_offset;
}

u1 JfrChunk::generation() const {
  assert(_generation > 0, "invariant");
  const u1 this_generation = _generation++;
  if (GUARD == _generation) {
    _generation = 1;
  }
  return this_generation;
}

u1 JfrChunk::next_generation() const {
  assert(_generation > 0, "invariant");
  const u1 next_gen = _generation;
  return GUARD == next_gen ? 1 : next_gen;
}
