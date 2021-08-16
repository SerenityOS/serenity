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

#ifndef SHARE_JFR_RECORDER_STACKTRACE_JFRSTACKTRACE_HPP
#define SHARE_JFR_RECORDER_STACKTRACE_JFRSTACKTRACE_HPP

#include "jfr/utilities/jfrAllocation.hpp"
#include "jfr/utilities/jfrTypes.hpp"

class frame;
class InstanceKlass;
class JavaThread;
class JfrCheckpointWriter;
class JfrChunkWriter;

class JfrStackFrame {
  friend class ObjectSampleCheckpoint;
 private:
  const InstanceKlass* _klass;
  traceid _methodid;
  mutable int _line;
  int _bci;
  u1 _type;

 public:
  JfrStackFrame(const traceid& id, int bci, int type, const InstanceKlass* klass);
  JfrStackFrame(const traceid& id, int bci, int type, int lineno, const InstanceKlass* klass);

  bool equals(const JfrStackFrame& rhs) const;
  void write(JfrChunkWriter& cw) const;
  void write(JfrCheckpointWriter& cpw) const;
  void resolve_lineno() const;

  enum {
    FRAME_INTERPRETER = 0,
    FRAME_JIT,
    FRAME_INLINE,
    FRAME_NATIVE,
    NUM_FRAME_TYPES
  };
};

class JfrStackTrace : public JfrCHeapObj {
  friend class JfrNativeSamplerCallback;
  friend class JfrStackTraceRepository;
  friend class ObjectSampleCheckpoint;
  friend class ObjectSampler;
  friend class OSThreadSampler;
  friend class StackTraceResolver;
 private:
  const JfrStackTrace* _next;
  JfrStackFrame* _frames;
  traceid _id;
  unsigned int _hash;
  u4 _nr_of_frames;
  u4 _max_frames;
  bool _frames_ownership;
  bool _reached_root;
  mutable bool _lineno;
  mutable bool _written;

  const JfrStackTrace* next() const { return _next; }

  bool should_write() const { return !_written; }
  void write(JfrChunkWriter& cw) const;
  void write(JfrCheckpointWriter& cpw) const;
  bool equals(const JfrStackTrace& rhs) const;

  void set_id(traceid id) { _id = id; }
  void set_nr_of_frames(u4 nr_of_frames) { _nr_of_frames = nr_of_frames; }
  void set_hash(unsigned int hash) { _hash = hash; }
  void set_reached_root(bool reached_root) { _reached_root = reached_root; }
  void resolve_linenos() const;

  bool record_thread(JavaThread& thread, frame& frame);
  bool record_safe(JavaThread* thread, int skip);

  bool have_lineno() const { return _lineno; }
  bool full_stacktrace() const { return _reached_root; }

  JfrStackTrace(traceid id, const JfrStackTrace& trace, const JfrStackTrace* next);
  JfrStackTrace(JfrStackFrame* frames, u4 max_frames);
  ~JfrStackTrace();

 public:
  unsigned int hash() const { return _hash; }
  traceid id() const { return _id; }
};

#endif // SHARE_JFR_RECORDER_STACKTRACE_JFRSTACKTRACE_HPP
