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
#ifndef SHARE_JFR_RECORDER_STORAGE_JFRSTORAGE_HPP
#define SHARE_JFR_RECORDER_STORAGE_JFRSTORAGE_HPP

#include "jfr/recorder/storage/jfrBuffer.hpp"
#include "jfr/recorder/storage/jfrFullStorage.hpp"
#include "jfr/recorder/storage/jfrMemorySpace.hpp"
#include "jfr/recorder/storage/jfrMemorySpaceRetrieval.hpp"
#include "jfr/utilities/jfrConcurrentQueue.hpp"
#include "jfr/utilities/jfrLinkedList.hpp"
#include "jfr/utilities/jfrNode.hpp"
#include "jfr/utilities/jfrRelation.hpp"

class JfrChunkWriter;
class JfrPostBox;
class JfrStorage;
class JfrStorageControl;

typedef JfrMemorySpace<JfrStorage, JfrMspaceRetrieval, JfrLinkedList<JfrBuffer> > JfrStorageMspace;
typedef JfrMemorySpace<JfrStorage, JfrMspaceRemoveRetrieval, JfrConcurrentQueue<JfrBuffer>, JfrLinkedList<JfrBuffer> > JfrThreadLocalMspace;
typedef JfrFullStorage<JfrBuffer*, JfrValueNode> JfrFullList;

//
// Responsible for providing backing storage for writing events.
//
class JfrStorage : public JfrCHeapObj {
 public:
  typedef JfrStorageMspace::Node    Buffer;
  typedef JfrStorageMspace::NodePtr BufferPtr;

 private:
  JfrStorageControl* _control;
  JfrStorageMspace* _global_mspace;
  JfrThreadLocalMspace* _thread_local_mspace;
  JfrFullList* _full_list;
  JfrChunkWriter& _chunkwriter;
  JfrPostBox& _post_box;

  BufferPtr acquire_large(size_t size, Thread* thread);
  BufferPtr acquire_transient(size_t size, Thread* thread);
  bool flush_regular_buffer(BufferPtr buffer, Thread* thread);
  BufferPtr flush_regular(BufferPtr cur, const u1* cur_pos, size_t used, size_t req, bool native, Thread* thread);
  BufferPtr flush_large(BufferPtr cur, const u1* cur_pos, size_t used, size_t req, bool native, Thread* thread);
  BufferPtr provision_large(BufferPtr cur, const u1* cur_pos, size_t used, size_t req, bool native, Thread* thread);
  void release(BufferPtr buffer, Thread* thread);

  size_t clear();
  size_t clear_full();
  size_t write_full();
  size_t write_at_safepoint();

  JfrStorage(JfrChunkWriter& cw, JfrPostBox& post_box);
  ~JfrStorage();

  static JfrStorage& instance();
  static JfrStorage* create(JfrChunkWriter& chunkwriter, JfrPostBox& post_box);
  bool initialize();
  static void destroy();

  // mspace callback
  void register_full(BufferPtr buffer, Thread* thread);

 public:
  static BufferPtr acquire_thread_local(Thread* thread, size_t size = 0);
  static void release_thread_local(BufferPtr buffer, Thread* thread);
  void release_large(BufferPtr buffer, Thread* thread);
  static BufferPtr flush(BufferPtr cur, size_t used, size_t req, bool native, Thread* thread);
  void discard_oldest(Thread* thread);
  static JfrStorageControl& control();
  size_t write();

  friend class JfrRecorder;
  friend class JfrRecorderService;
  template <typename, template <typename> class, typename, typename, bool>
  friend class JfrMemorySpace;
};

#endif // SHARE_JFR_RECORDER_STORAGE_JFRSTORAGE_HPP
