/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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


#ifndef SHARE_PRIMS_STACKWALK_HPP
#define SHARE_PRIMS_STACKWALK_HPP

#include "jvm.h"
#include "oops/oop.hpp"
#include "runtime/vframe.hpp"

// BaseFrameStream is an abstract base class for encapsulating the VM-side
// implementation of the StackWalker API.  There are two concrete subclasses:
// - JavaFrameStream:
//     -based on vframeStream; used in most instances
// - LiveFrameStream:
//     -based on javaVFrame; used for retrieving locals/monitors/operands for
//      LiveStackFrame
class BaseFrameStream : public StackObj {
private:
  enum {
    magic_pos = 0
  };

  JavaThread*           _thread;
  jlong                 _anchor;
protected:
  void fill_stackframe(Handle stackFrame, const methodHandle& method, TRAPS);
public:
  BaseFrameStream(JavaThread* thread) : _thread(thread), _anchor(0L) {}

  virtual void    next()=0;
  virtual bool    at_end()=0;

  virtual Method* method()=0;
  virtual int     bci()=0;

  virtual void    fill_frame(int index, objArrayHandle  frames_array,
                             const methodHandle& method, TRAPS)=0;

  void setup_magic_on_entry(objArrayHandle frames_array);
  bool check_magic(objArrayHandle frames_array);
  bool cleanup_magic_on_exit(objArrayHandle frames_array);

  bool is_valid_in(Thread* thread, objArrayHandle frames_array) {
    return (_thread == thread && check_magic(frames_array));
  }

  jlong address_value() {
    return (jlong) castable_address(this);
  }

  static BaseFrameStream* from_current(JavaThread* thread, jlong magic, objArrayHandle frames_array);
};

class JavaFrameStream : public BaseFrameStream {
private:
  vframeStream          _vfst;
  bool                  _need_method_info;
public:
  JavaFrameStream(JavaThread* thread, int mode);

  void next();
  bool at_end()    { return _vfst.at_end(); }

  Method* method() { return _vfst.method(); }
  int bci()        { return _vfst.bci(); }

  void fill_frame(int index, objArrayHandle  frames_array,
                  const methodHandle& method, TRAPS);
};

class LiveFrameStream : public BaseFrameStream {
private:
  enum {
    MODE_INTERPRETED = 0x01,
    MODE_COMPILED    = 0x02
  };

  javaVFrame*           _jvf;

  void fill_live_stackframe(Handle stackFrame, const methodHandle& method, TRAPS);
  static oop create_primitive_slot_instance(StackValueCollection* values,
                                            int i, BasicType type, TRAPS);
  static objArrayHandle monitors_to_object_array(GrowableArray<MonitorInfo*>* monitors,
                                                 TRAPS);
  static objArrayHandle values_to_object_array(StackValueCollection* values, TRAPS);
public:
  LiveFrameStream(JavaThread* thread, RegisterMap* rm) : BaseFrameStream(thread) {
    _jvf = thread->last_java_vframe(rm);
  }

  void next()      { _jvf = _jvf->java_sender(); }
  bool at_end()    { return _jvf == NULL; }

  Method* method() { return _jvf->method(); }
  int bci()        { return _jvf->bci(); }

  void fill_frame(int index, objArrayHandle  frames_array,
                  const methodHandle& method, TRAPS);
};

class StackWalk : public AllStatic {
private:
  static int fill_in_frames(jlong mode, BaseFrameStream& stream,
                            int max_nframes, int start_index,
                            objArrayHandle frames_array,
                            int& end_index, TRAPS);

  static inline bool get_caller_class(int mode) {
    return (mode & JVM_STACKWALK_GET_CALLER_CLASS) != 0;
  }
  static inline bool skip_hidden_frames(int mode) {
    return (mode & JVM_STACKWALK_SHOW_HIDDEN_FRAMES) == 0;
  }
  static inline bool live_frame_info(int mode) {
    return (mode & JVM_STACKWALK_FILL_LIVE_STACK_FRAMES) != 0;
  }

public:
  static inline bool need_method_info(int mode) {
    return (mode & JVM_STACKWALK_FILL_CLASS_REFS_ONLY) == 0;
  }
  static inline bool use_frames_array(int mode) {
    return (mode & JVM_STACKWALK_FILL_CLASS_REFS_ONLY) == 0;
  }
  static oop walk(Handle stackStream, jlong mode,
                  int skip_frames, int frame_count, int start_index,
                  objArrayHandle frames_array,
                  TRAPS);

  static oop fetchFirstBatch(BaseFrameStream& stream, Handle stackStream,
                             jlong mode, int skip_frames, int frame_count,
                             int start_index, objArrayHandle frames_array, TRAPS);

  static jint fetchNextBatch(Handle stackStream, jlong mode, jlong magic,
                             int frame_count, int start_index,
                             objArrayHandle frames_array, TRAPS);
};
#endif // SHARE_PRIMS_STACKWALK_HPP
