/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/jfr.hpp"
#include "jfr/jni/jfrJavaSupport.hpp"
#include "jfr/recorder/jfrRecorder.hpp"
#include "jfr/recorder/repository/jfrChunkWriter.hpp"
#include "jfr/recorder/repository/jfrEmergencyDump.hpp"
#include "jfr/recorder/repository/jfrRepository.hpp"
#include "jfr/recorder/service/jfrPostBox.hpp"
#include "logging/log.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/mutex.hpp"
#include "runtime/os.hpp"
#include "runtime/thread.inline.hpp"

static JfrRepository* _instance = NULL;

JfrRepository& JfrRepository::instance() {
  return *_instance;
}

static JfrChunkWriter* _chunkwriter = NULL;


JfrChunkWriter& JfrRepository::chunkwriter() {
  return *_chunkwriter;
}

JfrRepository::JfrRepository(JfrPostBox& post_box) : _path(NULL), _post_box(post_box) {}

bool JfrRepository::initialize() {
  assert(_chunkwriter == NULL, "invariant");
  _chunkwriter = new JfrChunkWriter();
  return _chunkwriter != NULL;
}

JfrRepository::~JfrRepository() {
  if (_path != NULL) {
    JfrCHeapObj::free(_path, strlen(_path) + 1);
    _path = NULL;
  }

  if (_chunkwriter != NULL) {
    delete _chunkwriter;
    _chunkwriter = NULL;
  }
}

JfrRepository* JfrRepository::create(JfrPostBox& post_box) {
  assert(_instance == NULL, "invariant");
  _instance = new JfrRepository(post_box);
  return _instance;
}

void JfrRepository::destroy() {
  assert(_instance != NULL, "invariant");
  delete _instance;
  _instance = NULL;
}

void JfrRepository::on_vm_error() {
  if (_path == NULL) {
    // completed already
    return;
  }
  JfrEmergencyDump::on_vm_error(_path);
}

void JfrRepository::on_vm_error_report(outputStream* st) {
  JfrEmergencyDump::on_vm_error_report(st, instance()._path);
}

bool JfrRepository::set_path(const char* path) {
  assert(path != NULL, "trying to set the repository path with a NULL string!");
  if (_path != NULL) {
    // delete existing
    JfrCHeapObj::free(_path, strlen(_path) + 1);
  }
  const size_t path_len = strlen(path);
  _path = JfrCHeapObj::new_array<char>(path_len + 1);
  if (_path == NULL) {
    return false;
  }
  strncpy(_path, path, path_len + 1);
  return true;
}

void JfrRepository::notify_on_new_chunk_path() {
  if (Jfr::is_recording()) {
    // rotations are synchronous, block until rotation completes
    instance()._post_box.post(MSG_ROTATE);
  }
}

void JfrRepository::set_chunk_path(const char* path) {
  chunkwriter().set_path(path);
}

void JfrRepository::mark_chunk_final() {
  chunkwriter().mark_chunk_final();
}

jlong JfrRepository::current_chunk_start_nanos() {
  return chunkwriter().current_chunk_start_nanos();
}

/**
* Sets the file where data should be written.
*
* Recording  Previous  Current  Action
* ==============================================
*   true     null      null     Ignore, keep recording in-memory
*   true     null      file1    Start disk recording
*   true     file      null     Copy out metadata to disk and continue in-memory recording
*   true     file1     file2    Copy out metadata and start with new File (file2)
*   false     *        null     Ignore, but start recording to memory
*   false     *        file     Ignore, but start recording to disk
*/
void JfrRepository::set_chunk_path(jstring path, JavaThread* jt) {
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(jt));
  ResourceMark rm(jt);
  const char* const canonical_chunk_path = JfrJavaSupport::c_str(path, jt);
  if (NULL == canonical_chunk_path && !_chunkwriter->is_valid()) {
    // new output is NULL and current output is NULL
    return;
  }
  instance().set_chunk_path(canonical_chunk_path);
  notify_on_new_chunk_path();
}

void JfrRepository::set_path(jstring location, JavaThread* jt) {
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(jt));
  ResourceMark rm(jt);
  const char* const path = JfrJavaSupport::c_str(location, jt);
  if (path != NULL) {
    instance().set_path(path);
  }
}

bool JfrRepository::open_chunk(bool vm_error /* false */) {
  if (vm_error) {
    _chunkwriter->set_path(JfrEmergencyDump::chunk_path(_path));
  }
  return _chunkwriter->open();
}

size_t JfrRepository::close_chunk() {
  return _chunkwriter->close();
}

void JfrRepository::flush(JavaThread* jt) {
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(jt));
  if (!Jfr::is_recording()) {
    return;
  }
  if (!_chunkwriter->is_valid()) {
    return;
  }
  instance()._post_box.post(MSG_FLUSHPOINT);
}

size_t JfrRepository::flush_chunk() {
  return _chunkwriter->flush_chunk(true);
}
