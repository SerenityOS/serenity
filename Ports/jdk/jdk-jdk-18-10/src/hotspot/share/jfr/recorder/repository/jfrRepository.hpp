/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
#ifndef SHARE_JFR_RECORDER_REPOSITORY_JFRREPOSITORY_HPP
#define SHARE_JFR_RECORDER_REPOSITORY_JFRREPOSITORY_HPP

#include "jni.h"
#include "jfr/utilities/jfrAllocation.hpp"

class JfrChunkWriter;
class JfrPostBox;

//
// Represents the location on disk where internal files, "chunks", are stored.
//
// A "chunk" is a self-contained file artifact storing events and metadata that
// has been moved out of process memory.
//
// Chunk files are associated with recordings and are managed at a higher level in Java.
// Java continously keeps the VM informed about new chunk locations via set_chunk_path().
//
// A JfrChunkWriter will open the next chunk file which it maintains as the current chunk.
// There is a rotation scheme in place for creating new chunks at certain intervals.
//
class JfrRepository : public JfrCHeapObj {
  friend class JfrRecorder;
  friend class JfrRecorderService;
 private:
  char* _path;
  JfrPostBox& _post_box;

  JfrRepository(JfrPostBox& post_box);
  ~JfrRepository();

  bool set_path(const char* path);
  void set_chunk_path(const char* path);
  bool open_chunk(bool vm_error = false);
  size_t close_chunk();
  size_t flush_chunk();
  void on_vm_error();

  static void notify_on_new_chunk_path();
  static JfrChunkWriter& chunkwriter();

  static JfrRepository& instance();
  static JfrRepository* create(JfrPostBox& post_box);
  bool initialize();
  static void destroy();

 public:
  static void set_path(jstring location, JavaThread* jt);
  static void set_chunk_path(jstring path, JavaThread* jt);
  static void mark_chunk_final();
  static void flush(JavaThread* jt);
  static jlong current_chunk_start_nanos();
  static void on_vm_error_report(outputStream* st);
};

#endif // SHARE_JFR_RECORDER_REPOSITORY_JFRREPOSITORY_HPP
