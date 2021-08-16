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

#ifndef SHARE_JFR_RECORDER_CHECKPOINT_JFRCHECKPOINTWRITER_HPP
#define SHARE_JFR_RECORDER_CHECKPOINT_JFRCHECKPOINTWRITER_HPP

#include "jfr/recorder/storage/jfrBuffer.hpp"
#include "jfr/utilities/jfrBlob.hpp"
#include "jfr/utilities/jfrTime.hpp"
#include "jfr/utilities/jfrTypes.hpp"
#include "jfr/writers/jfrEventWriterHost.inline.hpp"
#include "jfr/writers/jfrMemoryWriterHost.inline.hpp"
#include "jfr/writers/jfrStorageAdapter.hpp"

class Thread;

class JfrCheckpointFlush : public StackObj {
 public:
  typedef JfrBuffer Type;
  JfrCheckpointFlush(Type* old, size_t used, size_t requested, Thread* t);
  Type* result() { return _result; }
 private:
  Type* _result;
};

typedef Adapter<JfrCheckpointFlush> JfrCheckpointAdapter;
typedef AcquireReleaseMemoryWriterHost<JfrCheckpointAdapter, StackObj > JfrTransactionalCheckpointWriter;
typedef EventWriterHost<BigEndianEncoder, CompressedIntegerEncoder, JfrTransactionalCheckpointWriter> JfrCheckpointWriterBase;

struct JfrCheckpointContext {
  int64_t offset;
  u4 count;
};

class JfrCheckpointWriter : public JfrCheckpointWriterBase {
  friend class JfrCheckpointManager;
  friend class JfrSerializerRegistration;
  friend class JfrTypeManager;
 private:
  JfrTicks _time;
  int64_t _offset;
  u4 _count;
  JfrCheckpointType _type;
  bool _header;

  u4 count() const;
  void set_count(u4 count);
  void increment();
  const u1* session_data(size_t* size, bool move = false, const JfrCheckpointContext* ctx = NULL);
  void release();
  JfrCheckpointWriter(bool previous_epoch, Thread* thread, JfrCheckpointType type = GENERIC);
 public:
  JfrCheckpointWriter(JfrCheckpointType type = GENERIC);
  JfrCheckpointWriter(Thread* thread, bool header = true, JfrCheckpointType mode = GENERIC, bool global_lease = true);
  ~JfrCheckpointWriter();
  void write_type(JfrTypeId type_id);
  void write_count(u4 nof_entries);
  void write_count(u4 nof_entries, int64_t offset);
  void write_key(u8 key);
  const JfrCheckpointContext context() const;
  void set_context(const JfrCheckpointContext ctx);
  bool has_data() const;
  JfrBlobHandle copy(const JfrCheckpointContext* ctx = NULL);
  JfrBlobHandle move(const JfrCheckpointContext* ctx = NULL);
};

#endif // SHARE_JFR_RECORDER_CHECKPOINT_JFRCHECKPOINTWRITER_HPP
