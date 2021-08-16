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

#ifndef SHARE_JFR_RECORDER_STRINGPOOL_JFRSTRINGPOOL_HPP
#define SHARE_JFR_RECORDER_STRINGPOOL_JFRSTRINGPOOL_HPP

#include "jni.h"
#include "jfr/recorder/storage/jfrMemorySpace.hpp"
#include "jfr/recorder/storage/jfrMemorySpaceRetrieval.hpp"
#include "jfr/recorder/stringpool/jfrStringPoolBuffer.hpp"
#include "jfr/utilities/jfrLinkedList.hpp"

class JavaThread;
class JfrChunkWriter;
class JfrStringPool;

typedef JfrMemorySpace<JfrStringPool, JfrMspaceRetrieval, JfrLinkedList<JfrStringPoolBuffer> > JfrStringPoolMspace;

//
// Although called JfrStringPool, a more succinct description would be
// "backing storage for the string pool located in Java"
//
// There are no lookups in native, only the encoding of string constants to the stream.
//
class JfrStringPool : public JfrCHeapObj {
 public:
  size_t write();
  size_t clear();
  static jboolean add(jlong id, jstring string, JavaThread* jt);

  typedef JfrStringPoolMspace::Node    Buffer;
  typedef JfrStringPoolMspace::NodePtr BufferPtr;

 private:
  JfrStringPoolMspace* _mspace;
  JfrChunkWriter& _chunkwriter;

  static BufferPtr lease(Thread* thread, size_t size = 0);
  static BufferPtr flush(BufferPtr old, size_t used, size_t requested, Thread* thread);

  JfrStringPool(JfrChunkWriter& cw);
  ~JfrStringPool();

  static JfrStringPool& instance();
  static JfrStringPool* create(JfrChunkWriter& cw);
  bool initialize();
  static void destroy();
  static bool is_modified();

  // mspace callback
  void register_full(BufferPtr buffer, Thread* thread);

  friend class JfrRecorder;
  friend class JfrRecorderService;
  friend class JfrStringPoolFlush;
  friend class JfrStringPoolWriter;
  template <typename, template <typename> class, typename, typename, bool>
  friend class JfrMemorySpace;
};

#endif // SHARE_JFR_RECORDER_STRINGPOOL_JFRSTRINGPOOL_HPP
