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

#include "precompiled.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/oopFactory.hpp"
#include "memory/universe.hpp"
#include "oops/klass.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "prims/stackwalk.hpp"
#include "runtime/globals.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/keepStackGCProcessed.hpp"
#include "runtime/stackWatermarkSet.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/vframe.inline.hpp"
#include "utilities/formatBuffer.hpp"
#include "utilities/globalDefinitions.hpp"

// setup and cleanup actions
void BaseFrameStream::setup_magic_on_entry(objArrayHandle frames_array) {
  frames_array->obj_at_put(magic_pos, _thread->threadObj());
  _anchor = address_value();
  assert(check_magic(frames_array), "invalid magic");
}

bool BaseFrameStream::check_magic(objArrayHandle frames_array) {
  oop   m1 = frames_array->obj_at(magic_pos);
  jlong m2 = _anchor;
  if (m1 == _thread->threadObj() && m2 == address_value())  return true;
  return false;
}

bool BaseFrameStream::cleanup_magic_on_exit(objArrayHandle frames_array) {
  bool ok = check_magic(frames_array);
  frames_array->obj_at_put(magic_pos, NULL);
  _anchor = 0L;
  return ok;
}

JavaFrameStream::JavaFrameStream(JavaThread* thread, int mode)
  : BaseFrameStream(thread), _vfst(thread) {
  _need_method_info = StackWalk::need_method_info(mode);
}

void JavaFrameStream::next() { _vfst.next();}

// Returns the BaseFrameStream for the current stack being traversed.
//
// Parameters:
//  thread         Current Java thread.
//  magic          Magic value used for each stack walking
//  frames_array   User-supplied buffers.  The 0th element is reserved
//                 for this BaseFrameStream to use
//
BaseFrameStream* BaseFrameStream::from_current(JavaThread* thread, jlong magic,
                                               objArrayHandle frames_array)
{
  oop m1 = frames_array->obj_at(magic_pos);
  if (m1 != thread->threadObj()) return NULL;
  if (magic == 0L)                    return NULL;
  BaseFrameStream* stream = (BaseFrameStream*) (intptr_t) magic;
  if (!stream->is_valid_in(thread, frames_array))   return NULL;
  return stream;
}

// Unpacks one or more frames into user-supplied buffers.
// Updates the end index, and returns the number of unpacked frames.
// Always start with the existing vfst.method and bci.
// Do not call vfst.next to advance over the last returned value.
// In other words, do not leave any stale data in the vfst.
//
// Parameters:
//   mode             Restrict which frames to be decoded.
//   BaseFrameStream  stream of frames
//   max_nframes      Maximum number of frames to be filled.
//   start_index      Start index to the user-supplied buffers.
//   frames_array     Buffer to store Class or StackFrame in, starting at start_index.
//                    frames array is a Class<?>[] array when only getting caller
//                    reference, and a StackFrameInfo[] array (or derivative)
//                    otherwise. It should never be null.
//   end_index        End index to the user-supplied buffers with unpacked frames.
//
// Returns the number of frames whose information was transferred into the buffers.
//
int StackWalk::fill_in_frames(jlong mode, BaseFrameStream& stream,
                              int max_nframes, int start_index,
                              objArrayHandle  frames_array,
                              int& end_index, TRAPS) {
  log_debug(stackwalk)("fill_in_frames limit=%d start=%d frames length=%d",
                       max_nframes, start_index, frames_array->length());
  assert(max_nframes > 0, "invalid max_nframes");
  assert(start_index + max_nframes <= frames_array->length(), "oob");

  int frames_decoded = 0;
  for (; !stream.at_end(); stream.next()) {
    Method* method = stream.method();

    if (method == NULL) continue;

    // skip hidden frames for default StackWalker option (i.e. SHOW_HIDDEN_FRAMES
    // not set) and when StackWalker::getCallerClass is called
    if (!ShowHiddenFrames && (skip_hidden_frames(mode) || get_caller_class(mode))) {
      if (method->is_hidden()) {
        LogTarget(Debug, stackwalk) lt;
        if (lt.is_enabled()) {
          ResourceMark rm(THREAD);
          LogStream ls(lt);
          ls.print("  hidden method: ");
          method->print_short_name(&ls);
          ls.cr();
        }
        continue;
      }
    }

    int index = end_index++;
    LogTarget(Debug, stackwalk) lt;
    if (lt.is_enabled()) {
      ResourceMark rm(THREAD);
      LogStream ls(lt);
      ls.print("  %d: frame method: ", index);
      method->print_short_name(&ls);
      ls.print_cr(" bci=%d", stream.bci());
    }

    if (!need_method_info(mode) && get_caller_class(mode) &&
          index == start_index && method->caller_sensitive()) {
      ResourceMark rm(THREAD);
      THROW_MSG_0(vmSymbols::java_lang_UnsupportedOperationException(),
        err_msg("StackWalker::getCallerClass called from @CallerSensitive '%s' method",
                method->external_name()));
    }
    // fill in StackFrameInfo and initialize MemberName
    stream.fill_frame(index, frames_array, methodHandle(THREAD, method), CHECK_0);
    if (++frames_decoded >= max_nframes)  break;
  }
  return frames_decoded;
}

// Fill in the LiveStackFrameInfo at the given index in frames_array
void LiveFrameStream::fill_frame(int index, objArrayHandle  frames_array,
                                 const methodHandle& method, TRAPS) {
  HandleMark hm(THREAD);
  Handle stackFrame(THREAD, frames_array->obj_at(index));
  fill_live_stackframe(stackFrame, method, CHECK);
}

// Fill in the StackFrameInfo at the given index in frames_array
void JavaFrameStream::fill_frame(int index, objArrayHandle  frames_array,
                                 const methodHandle& method, TRAPS) {
  if (_need_method_info) {
    HandleMark hm(THREAD);
    Handle stackFrame(THREAD, frames_array->obj_at(index));
    fill_stackframe(stackFrame, method, CHECK);
  } else {
    frames_array->obj_at_put(index, method->method_holder()->java_mirror());
  }
}

// Create and return a LiveStackFrame.PrimitiveSlot (if needed) for the
// StackValue at the given index. 'type' is expected to be T_INT, T_LONG,
// T_OBJECT, or T_CONFLICT.
oop LiveFrameStream::create_primitive_slot_instance(StackValueCollection* values,
                                                    int i, BasicType type, TRAPS) {
  Klass* k = vmClasses::LiveStackFrameInfo_klass();
  InstanceKlass* ik = InstanceKlass::cast(k);

  JavaValue result(T_OBJECT);
  JavaCallArguments args;
  Symbol* signature = NULL;

  // ## TODO: type is only available in LocalVariable table, if present.
  // ## StackValue type is T_INT or T_OBJECT (or converted to T_LONG on 64-bit)
  switch (type) {
    case T_INT:
      args.push_int(values->int_at(i));
      signature = vmSymbols::asPrimitive_int_signature();
      break;

    case T_LONG:
      args.push_long(values->long_at(i));
      signature = vmSymbols::asPrimitive_long_signature();
      break;

    case T_FLOAT:
    case T_DOUBLE:
    case T_BYTE:
    case T_SHORT:
    case T_CHAR:
    case T_BOOLEAN:
      THROW_MSG_(vmSymbols::java_lang_InternalError(), "Unexpected StackValue type", NULL);

    case T_OBJECT:
      return values->obj_at(i)();

    case T_CONFLICT:
      // put a non-null slot
      #ifdef _LP64
        args.push_long(0);
        signature = vmSymbols::asPrimitive_long_signature();
      #else
        args.push_int(0);
        signature = vmSymbols::asPrimitive_int_signature();
      #endif

      break;

    default: ShouldNotReachHere();
  }
  JavaCalls::call_static(&result,
                         ik,
                         vmSymbols::asPrimitive_name(),
                         signature,
                         &args,
                         CHECK_NULL);
  return (instanceOop) result.get_oop();
}

objArrayHandle LiveFrameStream::values_to_object_array(StackValueCollection* values, TRAPS) {
  objArrayHandle empty;
  int length = values->size();
  objArrayOop array_oop = oopFactory::new_objArray(vmClasses::Object_klass(),
                                                   length, CHECK_(empty));
  objArrayHandle array_h(THREAD, array_oop);
  for (int i = 0; i < values->size(); i++) {
    StackValue* st = values->at(i);
    BasicType type = st->type();
    int index = i;
#ifdef _LP64
    if (type != T_OBJECT && type != T_CONFLICT) {
        intptr_t ret = st->get_int(); // read full 64-bit slot
        type = T_LONG;                // treat as long
        index--;                      // undo +1 in StackValueCollection::long_at
    }
#endif
    oop obj = create_primitive_slot_instance(values, index, type, CHECK_(empty));
    if (obj != NULL) {
      array_h->obj_at_put(i, obj);
    }
  }
  return array_h;
}

objArrayHandle LiveFrameStream::monitors_to_object_array(GrowableArray<MonitorInfo*>* monitors, TRAPS) {
  int length = monitors->length();
  objArrayOop array_oop = oopFactory::new_objArray(vmClasses::Object_klass(),
                                                   length, CHECK_(objArrayHandle()));
  objArrayHandle array_h(THREAD, array_oop);
  for (int i = 0; i < length; i++) {
    MonitorInfo* monitor = monitors->at(i);
    array_h->obj_at_put(i, monitor->owner());
  }
  return array_h;
}

// Fill StackFrameInfo with bci and initialize memberName
void BaseFrameStream::fill_stackframe(Handle stackFrame, const methodHandle& method, TRAPS) {
  java_lang_StackFrameInfo::set_method_and_bci(stackFrame, method, bci(), THREAD);
}

// Fill LiveStackFrameInfo with locals, monitors, and expressions
void LiveFrameStream::fill_live_stackframe(Handle stackFrame,
                                           const methodHandle& method, TRAPS) {
  fill_stackframe(stackFrame, method, CHECK);
  if (_jvf != NULL) {
    ResourceMark rm(THREAD);
    HandleMark hm(THREAD);

    StackValueCollection* locals = _jvf->locals();
    StackValueCollection* expressions = _jvf->expressions();
    GrowableArray<MonitorInfo*>* monitors = _jvf->monitors();

    int mode = 0;
    if (_jvf->is_interpreted_frame()) {
      mode = MODE_INTERPRETED;
    } else if (_jvf->is_compiled_frame()) {
      mode = MODE_COMPILED;
    }

    if (!locals->is_empty()) {
      objArrayHandle locals_h = values_to_object_array(locals, CHECK);
      java_lang_LiveStackFrameInfo::set_locals(stackFrame(), locals_h());
    }
    if (!expressions->is_empty()) {
      objArrayHandle expressions_h = values_to_object_array(expressions, CHECK);
      java_lang_LiveStackFrameInfo::set_operands(stackFrame(), expressions_h());
    }
    if (monitors->length() > 0) {
      objArrayHandle monitors_h = monitors_to_object_array(monitors, CHECK);
      java_lang_LiveStackFrameInfo::set_monitors(stackFrame(), monitors_h());
    }
    java_lang_LiveStackFrameInfo::set_mode(stackFrame(), mode);
  }
}

// Begins stack walking.
//
// Parameters:
//   stackStream    StackStream object
//   mode           Stack walking mode.
//   skip_frames    Number of frames to be skipped.
//   frame_count    Number of frames to be traversed.
//   start_index    Start index to the user-supplied buffers.
//   frames_array   Buffer to store StackFrame in, starting at start_index.
//                  frames array is a Class<?>[] array when only getting caller
//                  reference, and a StackFrameInfo[] array (or derivative)
//                  otherwise. It should never be null.
//
// Returns Object returned from AbstractStackWalker::doStackWalk call.
//
oop StackWalk::walk(Handle stackStream, jlong mode,
                    int skip_frames, int frame_count, int start_index,
                    objArrayHandle frames_array,
                    TRAPS) {
  ResourceMark rm(THREAD);
  JavaThread* jt = THREAD;
  log_debug(stackwalk)("Start walking: mode " JLONG_FORMAT " skip %d frames batch size %d",
                       mode, skip_frames, frame_count);

  if (frames_array.is_null()) {
    THROW_MSG_(vmSymbols::java_lang_NullPointerException(), "frames_array is NULL", NULL);
  }

  // Setup traversal onto my stack.
  if (live_frame_info(mode)) {
    assert (use_frames_array(mode), "Bad mode for get live frame");
    RegisterMap regMap(jt, true);
    LiveFrameStream stream(jt, &regMap);
    return fetchFirstBatch(stream, stackStream, mode, skip_frames, frame_count,
                           start_index, frames_array, THREAD);
  } else {
    JavaFrameStream stream(jt, mode);
    return fetchFirstBatch(stream, stackStream, mode, skip_frames, frame_count,
                           start_index, frames_array, THREAD);
  }
}

oop StackWalk::fetchFirstBatch(BaseFrameStream& stream, Handle stackStream,
                               jlong mode, int skip_frames, int frame_count,
                               int start_index, objArrayHandle frames_array, TRAPS) {
  methodHandle m_doStackWalk(THREAD, Universe::do_stack_walk_method());

  {
    Klass* stackWalker_klass = vmClasses::StackWalker_klass();
    Klass* abstractStackWalker_klass = vmClasses::AbstractStackWalker_klass();
    while (!stream.at_end()) {
      InstanceKlass* ik = stream.method()->method_holder();
      if (ik != stackWalker_klass &&
            ik != abstractStackWalker_klass && ik->super() != abstractStackWalker_klass)  {
        break;
      }

      LogTarget(Debug, stackwalk) lt;
      if (lt.is_enabled()) {
        ResourceMark rm(THREAD);
        LogStream ls(lt);
        ls.print("  skip ");
        stream.method()->print_short_name(&ls);
        ls.cr();
      }
      stream.next();
    }

    // stack frame has been traversed individually and resume stack walk
    // from the stack frame at depth == skip_frames.
    for (int n=0; n < skip_frames && !stream.at_end(); stream.next(), n++) {
      LogTarget(Debug, stackwalk) lt;
      if (lt.is_enabled()) {
        ResourceMark rm(THREAD);
        LogStream ls(lt);
        ls.print("  skip ");
        stream.method()->print_short_name(&ls);
        ls.cr();
      }
    }
  }

  int end_index = start_index;
  int numFrames = 0;
  if (!stream.at_end()) {
    KeepStackGCProcessedMark keep_stack(THREAD);
    numFrames = fill_in_frames(mode, stream, frame_count, start_index,
                               frames_array, end_index, CHECK_NULL);
    if (numFrames < 1) {
      THROW_MSG_(vmSymbols::java_lang_InternalError(), "stack walk: decode failed", NULL);
    }
  }

  // JVM_CallStackWalk walks the stack and fills in stack frames, then calls to
  // Java method java.lang.StackStreamFactory.AbstractStackWalker::doStackWalk
  // which calls the implementation to consume the stack frames.
  // When JVM_CallStackWalk returns, it invalidates the stack stream.
  JavaValue result(T_OBJECT);
  JavaCallArguments args(stackStream);
  args.push_long(stream.address_value());
  args.push_int(skip_frames);
  args.push_int(frame_count);
  args.push_int(start_index);
  args.push_int(end_index);

  // Link the thread and vframe stream into the callee-visible object
  stream.setup_magic_on_entry(frames_array);

  JavaCalls::call(&result, m_doStackWalk, &args, THREAD);

  // Do this before anything else happens, to disable any lingering stream objects
  bool ok = stream.cleanup_magic_on_exit(frames_array);

  // Throw pending exception if we must
  (void) (CHECK_NULL);

  if (!ok) {
    THROW_MSG_(vmSymbols::java_lang_InternalError(), "doStackWalk: corrupted buffers on exit", NULL);
  }

  // Return normally
  return result.get_oop();
}

// Walk the next batch of stack frames
//
// Parameters:
//   stackStream    StackStream object
//   mode           Stack walking mode.
//   magic          Must be valid value to continue the stack walk
//   frame_count    Number of frames to be decoded.
//   start_index    Start index to the user-supplied buffers.
//   frames_array   Buffer to store StackFrame in, starting at start_index.
//
// Returns the end index of frame filled in the buffer.
//
jint StackWalk::fetchNextBatch(Handle stackStream, jlong mode, jlong magic,
                               int frame_count, int start_index,
                               objArrayHandle frames_array,
                               TRAPS)
{
  JavaThread* jt = THREAD;
  BaseFrameStream* existing_stream = BaseFrameStream::from_current(jt, magic, frames_array);
  if (existing_stream == NULL) {
    THROW_MSG_(vmSymbols::java_lang_InternalError(), "doStackWalk: corrupted buffers", 0L);
  }

  if (frames_array.is_null()) {
    THROW_MSG_(vmSymbols::java_lang_NullPointerException(), "frames_array is NULL", 0L);
  }

  log_debug(stackwalk)("StackWalk::fetchNextBatch frame_count %d existing_stream "
                       PTR_FORMAT " start %d frames %d",
                       frame_count, p2i(existing_stream), start_index, frames_array->length());
  int end_index = start_index;
  if (frame_count <= 0) {
    return end_index;        // No operation.
  }

  int count = frame_count + start_index;
  assert (frames_array->length() >= count, "not enough space in buffers");

  BaseFrameStream& stream = (*existing_stream);
  if (!stream.at_end()) {
    // If we have to get back here for even more frames, then 1) the user did not supply
    // an accurate hint suggesting the depth of the stack walk, and 2) we are not just
    // peeking  at a few frames. Take the cost of flushing out any pending deferred GC
    // processing of the stack.
    KeepStackGCProcessedMark keep_stack(jt);
    stream.next(); // advance past the last frame decoded in previous batch
    if (!stream.at_end()) {
      int n = fill_in_frames(mode, stream, frame_count, start_index,
                             frames_array, end_index, CHECK_0);
      if (n < 1) {
        THROW_MSG_(vmSymbols::java_lang_InternalError(), "doStackWalk: later decode failed", 0L);
      }
      return end_index;
    }
  }
  return end_index;
}
