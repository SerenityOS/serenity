/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_RECORDER_CHECKPOINT_JFRCHECKPOINTMANAGER_HPP
#define SHARE_JFR_RECORDER_CHECKPOINT_JFRCHECKPOINTMANAGER_HPP

#include "jfr/recorder/storage/jfrBuffer.hpp"
#include "jfr/recorder/storage/jfrEpochStorage.hpp"
#include "jfr/recorder/storage/jfrMemorySpace.hpp"
#include "jfr/recorder/storage/jfrMemorySpaceRetrieval.hpp"
#include "jfr/utilities/jfrLinkedList.hpp"

class JfrCheckpointManager;
class JfrChunkWriter;
class Thread;

struct JfrCheckpointEntry {
  jlong size;
  jlong start_time;
  jlong duration;
  juint flushpoint;
  juint nof_segments;
};

typedef JfrMemorySpace<JfrCheckpointManager, JfrMspaceRetrieval, JfrLinkedList<JfrBuffer>, JfrLinkedList<JfrBuffer>, true > JfrCheckpointMspace;
typedef JfrEpochStorageHost<JfrBuffer, JfrMspaceRemoveRetrieval, true /* reclaim buffers eagerly*/ > JfrThreadLocalCheckpointMspace;

//
// Responsible for maintaining checkpoints and by implication types.
// A checkpoint is an event that has a payload consisting of constant types.
// A constant type is a binary relation, a set of key-value pairs.
//
class JfrCheckpointManager : public JfrCHeapObj {
 public:
  typedef JfrCheckpointMspace::Node Buffer;
  typedef JfrCheckpointMspace::NodePtr BufferPtr;
 private:
  JfrCheckpointMspace* _global_mspace;
  JfrThreadLocalCheckpointMspace* _thread_local_mspace;
  JfrChunkWriter& _chunkwriter;

  JfrCheckpointManager(JfrChunkWriter& cw);
  ~JfrCheckpointManager();
  static JfrCheckpointManager& instance();
  static JfrCheckpointManager* create(JfrChunkWriter& cw);
  bool initialize();
  static void destroy();

  static BufferPtr lease(Thread* thread, bool previous_epoch = false, size_t size = 0);
  static BufferPtr lease(BufferPtr old, Thread* thread, size_t size);

  static BufferPtr acquire_thread_local(size_t size, Thread* thread);
  static BufferPtr lease_thread_local(Thread* thread, size_t size = 0);

  static BufferPtr flush(BufferPtr old, size_t used, size_t requested, Thread* thread);

  size_t clear();
  size_t write();
  void notify_threads();

  size_t write_static_type_set(Thread* thread);
  size_t write_threads(JavaThread* thread);
  size_t write_static_type_set_and_threads();
  void clear_type_set();
  void write_type_set();

  void begin_epoch_shift();
  void end_epoch_shift();

  static void on_unloading_classes();
  void on_rotation();

  // mspace callback
  void register_full(BufferPtr buffer, Thread* thread);

 public:
  size_t flush_type_set();
  static void create_thread_blob(Thread* thread);
  static void write_thread_checkpoint(Thread* thread);

  friend class Jfr;
  friend class JfrRecorder;
  friend class JfrRecorderService;
  friend class JfrCheckpointFlush;
  friend class JfrCheckpointWriter;
  friend class JfrSerializer;
  template <typename, template <typename> class, typename, typename, bool>
  friend class JfrMemorySpace;
};

#endif // SHARE_JFR_RECORDER_CHECKPOINT_JFRCHECKPOINTMANAGER_HPP
