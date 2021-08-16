/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_RECORDER_SERVICE_JFRRECORDERSERVICE_HPP
#define SHARE_JFR_RECORDER_SERVICE_JFRRECORDERSERVICE_HPP

#include "jfr/utilities/jfrAllocation.hpp"

class JfrCheckpointManager;
class JfrChunkWriter;
class JfrRepository;
class JfrStackTraceRepository;
class JfrStorage;
class JfrStringPool;

class JfrRecorderService : public StackObj {
 private:
  JfrCheckpointManager& _checkpoint_manager;
  JfrChunkWriter& _chunkwriter;
  JfrRepository& _repository;
  JfrStackTraceRepository& _stack_trace_repository;
  JfrStorage& _storage;
  JfrStringPool& _string_pool;

  void open_new_chunk(bool vm_error = false);
  void chunk_rotation();
  void in_memory_rotation();
  void finalize_current_chunk();
  void vm_error_rotation();
  void invoke_flush();

  void clear();
  void pre_safepoint_clear();
  void safepoint_clear();
  void invoke_safepoint_clear();
  void post_safepoint_clear();

  void write();
  void pre_safepoint_write();
  void safepoint_write();
  void invoke_safepoint_write();
  void post_safepoint_write();

 public:
  JfrRecorderService();
  void start();
  size_t flush();
  void rotate(int msgs);
  void flushpoint();
  void process_full_buffers();
  void scavenge();
  void evaluate_chunk_size_for_rotation();
  static bool is_recording();
};

#endif // SHARE_JFR_RECORDER_SERVICE_JFRRECORDERSERVICE_HPP
