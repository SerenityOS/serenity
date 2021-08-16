/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2021 SAP SE. All rights reserved.
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
#include "compiler/oopMap.hpp"
#include "interpreter/interpreter.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/markWord.hpp"
#include "oops/method.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/monitorChunk.hpp"
#include "runtime/os.inline.hpp"
#include "runtime/signature.hpp"
#include "runtime/stackWatermarkSet.hpp"
#include "runtime/stubCodeGenerator.hpp"
#include "runtime/stubRoutines.hpp"
#ifdef COMPILER1
#include "c1/c1_Runtime1.hpp"
#include "runtime/vframeArray.hpp"
#endif

#ifdef ASSERT
void RegisterMap::check_location_valid() {
}
#endif // ASSERT

bool frame::safe_for_sender(JavaThread *thread) {
  bool safe = false;
  address sp = (address)_sp;
  address fp = (address)_fp;
  address unextended_sp = (address)_unextended_sp;

  // consider stack guards when trying to determine "safe" stack pointers
  // sp must be within the usable part of the stack (not in guards)
  if (!thread->is_in_usable_stack(sp)) {
    return false;
  }

  // Unextended sp must be within the stack
  if (!thread->is_in_full_stack_checked(unextended_sp)) {
    return false;
  }

  // An fp must be within the stack and above (but not equal) sp.
  bool fp_safe = thread->is_in_stack_range_excl(fp, sp);
  // An interpreter fp must be within the stack and above (but not equal) sp.
  // Moreover, it must be at least the size of the ijava_state structure.
  bool fp_interp_safe = fp_safe && ((fp - sp) >= ijava_state_size);

  // We know sp/unextended_sp are safe, only fp is questionable here

  // If the current frame is known to the code cache then we can attempt to
  // to construct the sender and do some validation of it. This goes a long way
  // toward eliminating issues when we get in frame construction code

  if (_cb != NULL ){
    // Entry frame checks
    if (is_entry_frame()) {
      // An entry frame must have a valid fp.
      return fp_safe && is_entry_frame_valid(thread);
    }

    // Now check if the frame is complete and the test is
    // reliable. Unfortunately we can only check frame completeness for
    // runtime stubs and nmethods. Other generic buffer blobs are more
    // problematic so we just assume they are OK. Adapter blobs never have a
    // complete frame and are never OK
    if (!_cb->is_frame_complete_at(_pc)) {
      if (_cb->is_compiled() || _cb->is_adapter_blob() || _cb->is_runtime_stub()) {
        return false;
      }
    }

    // Could just be some random pointer within the codeBlob.
    if (!_cb->code_contains(_pc)) {
      return false;
    }

    if (is_interpreted_frame() && !fp_interp_safe) {
      return false;
    }

    abi_minframe* sender_abi = (abi_minframe*) fp;
    intptr_t* sender_sp = (intptr_t*) fp;
    address   sender_pc = (address) sender_abi->lr;;

    // We must always be able to find a recognizable pc.
    CodeBlob* sender_blob = CodeCache::find_blob_unsafe(sender_pc);
    if (sender_blob == NULL) {
      return false;
    }

    // Could be a zombie method
    if (sender_blob->is_zombie() || sender_blob->is_unloaded()) {
      return false;
    }

    // It should be safe to construct the sender though it might not be valid.

    frame sender(sender_sp, sender_pc);

    // Do we have a valid fp?
    address sender_fp = (address) sender.fp();

    // sender_fp must be within the stack and above (but not
    // equal) current frame's fp.
    if (!thread->is_in_stack_range_excl(sender_fp, fp)) {
        return false;
    }

    // If the potential sender is the interpreter then we can do some more checking.
    if (Interpreter::contains(sender_pc)) {
      return sender.is_interpreted_frame_valid(thread);
    }

    // Could just be some random pointer within the codeBlob.
    if (!sender.cb()->code_contains(sender_pc)) {
      return false;
    }

    // We should never be able to see an adapter if the current frame is something from code cache.
    if (sender_blob->is_adapter_blob()) {
      return false;
    }

    if (sender.is_entry_frame()) {
      return sender.is_entry_frame_valid(thread);
    }

    // Frame size is always greater than zero. If the sender frame size is zero or less,
    // something is really weird and we better give up.
    if (sender_blob->frame_size() <= 0) {
      return false;
    }

    return true;
  }

  // Must be native-compiled frame. Since sender will try and use fp to find
  // linkages it must be safe

  if (!fp_safe) {
    return false;
  }

  return true;
}

bool frame::is_interpreted_frame() const  {
  return Interpreter::contains(pc());
}

frame frame::sender_for_entry_frame(RegisterMap *map) const {
  assert(map != NULL, "map must be set");
  // Java frame called from C; skip all C frames and return top C
  // frame of that chunk as the sender.
  JavaFrameAnchor* jfa = entry_frame_call_wrapper()->anchor();
  assert(!entry_frame_is_first(), "next Java fp must be non zero");
  assert(jfa->last_Java_sp() > _sp, "must be above this frame on stack");
  map->clear();
  assert(map->include_argument_oops(), "should be set by clear");

  if (jfa->last_Java_pc() != NULL) {
    frame fr(jfa->last_Java_sp(), jfa->last_Java_pc());
    return fr;
  }
  // Last_java_pc is not set, if we come here from compiled code. The
  // constructor retrieves the PC from the stack.
  frame fr(jfa->last_Java_sp());
  return fr;
}

OptimizedEntryBlob::FrameData* OptimizedEntryBlob::frame_data_for_frame(const frame& frame) const {
  ShouldNotCallThis();
  return nullptr;
}

bool frame::optimized_entry_frame_is_first() const {
  ShouldNotCallThis();
  return false;
}

frame frame::sender_for_interpreter_frame(RegisterMap *map) const {
  // Pass callers initial_caller_sp as unextended_sp.
  return frame(sender_sp(), sender_pc(), (intptr_t*)get_ijava_state()->sender_sp);
}

frame frame::sender_for_compiled_frame(RegisterMap *map) const {
  assert(map != NULL, "map must be set");

  // Frame owned by compiler.
  address pc = *compiled_sender_pc_addr(_cb);
  frame caller(compiled_sender_sp(_cb), pc);

  // Now adjust the map.

  // Get the rest.
  if (map->update_map()) {
    // Tell GC to use argument oopmaps for some runtime stubs that need it.
    map->set_include_argument_oops(_cb->caller_must_gc_arguments(map->thread()));
    if (_cb->oop_maps() != NULL) {
      OopMapSet::update_register_map(this, map);
    }
  }

  return caller;
}

intptr_t* frame::compiled_sender_sp(CodeBlob* cb) const {
  return sender_sp();
}

address* frame::compiled_sender_pc_addr(CodeBlob* cb) const {
  return sender_pc_addr();
}

frame frame::sender_raw(RegisterMap* map) const {
  // Default is we do have to follow them. The sender_for_xxx will
  // update it accordingly.
  map->set_include_argument_oops(false);

  if (is_entry_frame())       return sender_for_entry_frame(map);
  if (is_interpreted_frame()) return sender_for_interpreter_frame(map);
  assert(_cb == CodeCache::find_blob(pc()),"Must be the same");

  if (_cb != NULL) {
    return sender_for_compiled_frame(map);
  }
  // Must be native-compiled frame, i.e. the marshaling code for native
  // methods that exists in the core system.
  return frame(sender_sp(), sender_pc());
}

frame frame::sender(RegisterMap* map) const {
  frame result = sender_raw(map);

  if (map->process_frames()) {
    StackWatermarkSet::on_iteration(map->thread(), result);
  }

  return result;
}

void frame::patch_pc(Thread* thread, address pc) {
  assert(_cb == CodeCache::find_blob(pc), "unexpected pc");
  if (TracePcPatching) {
    tty->print_cr("patch_pc at address " PTR_FORMAT " [" PTR_FORMAT " -> " PTR_FORMAT "]",
                  p2i(&((address*) _sp)[-1]), p2i(((address*) _sp)[-1]), p2i(pc));
  }
  own_abi()->lr = (uint64_t)pc;
  if (_cb != NULL && _cb->is_nmethod() && ((nmethod*)_cb)->is_deopt_pc(_pc)) {
    address orig = (((nmethod*)_cb)->get_original_pc(this));
    assert(orig == _pc, "expected original to be stored before patching");
    _deopt_state = is_deoptimized;
    // Leave _pc as is.
  } else {
    _deopt_state = not_deoptimized;
    _pc = pc;
  }
}

bool frame::is_interpreted_frame_valid(JavaThread* thread) const {
  // Is there anything to do?
  assert(is_interpreted_frame(), "Not an interpreted frame");
  return true;
}

BasicType frame::interpreter_frame_result(oop* oop_result, jvalue* value_result) {
  assert(is_interpreted_frame(), "interpreted frame expected");
  Method* method = interpreter_frame_method();
  BasicType type = method->result_type();

  if (method->is_native()) {
    // Prior to calling into the runtime to notify the method exit the possible
    // result value is saved into the interpreter frame.
    address lresult = (address)&(get_ijava_state()->lresult);
    address fresult = (address)&(get_ijava_state()->fresult);

    switch (method->result_type()) {
      case T_OBJECT:
      case T_ARRAY: {
        *oop_result = JNIHandles::resolve(*(jobject*)lresult);
        break;
      }
      // We use std/stfd to store the values.
      case T_BOOLEAN : value_result->z = (jboolean) *(unsigned long*)lresult; break;
      case T_INT     : value_result->i = (jint)     *(long*)lresult;          break;
      case T_CHAR    : value_result->c = (jchar)    *(unsigned long*)lresult; break;
      case T_SHORT   : value_result->s = (jshort)   *(long*)lresult;          break;
      case T_BYTE    : value_result->z = (jbyte)    *(long*)lresult;          break;
      case T_LONG    : value_result->j = (jlong)    *(long*)lresult;          break;
      case T_FLOAT   : value_result->f = (jfloat)   *(double*)fresult;        break;
      case T_DOUBLE  : value_result->d = (jdouble)  *(double*)fresult;        break;
      case T_VOID    : /* Nothing to do */ break;
      default        : ShouldNotReachHere();
    }
  } else {
    intptr_t* tos_addr = interpreter_frame_tos_address();
    switch (method->result_type()) {
      case T_OBJECT:
      case T_ARRAY: {
        oop obj = *(oop*)tos_addr;
        assert(Universe::is_in_heap_or_null(obj), "sanity check");
        *oop_result = obj;
      }
      case T_BOOLEAN : value_result->z = (jboolean) *(jint*)tos_addr; break;
      case T_BYTE    : value_result->b = (jbyte) *(jint*)tos_addr; break;
      case T_CHAR    : value_result->c = (jchar) *(jint*)tos_addr; break;
      case T_SHORT   : value_result->s = (jshort) *(jint*)tos_addr; break;
      case T_INT     : value_result->i = *(jint*)tos_addr; break;
      case T_LONG    : value_result->j = *(jlong*)tos_addr; break;
      case T_FLOAT   : value_result->f = *(jfloat*)tos_addr; break;
      case T_DOUBLE  : value_result->d = *(jdouble*)tos_addr; break;
      case T_VOID    : /* Nothing to do */ break;
      default        : ShouldNotReachHere();
    }
  }
  return type;
}

#ifndef PRODUCT

void frame::describe_pd(FrameValues& values, int frame_no) {
  if (is_interpreted_frame()) {
#define DESCRIBE_ADDRESS(name) \
  values.describe(frame_no, (intptr_t*)&(get_ijava_state()->name), #name);

      DESCRIBE_ADDRESS(method);
      DESCRIBE_ADDRESS(mirror);
      DESCRIBE_ADDRESS(locals);
      DESCRIBE_ADDRESS(monitors);
      DESCRIBE_ADDRESS(cpoolCache);
      DESCRIBE_ADDRESS(bcp);
      DESCRIBE_ADDRESS(esp);
      DESCRIBE_ADDRESS(mdx);
      DESCRIBE_ADDRESS(top_frame_sp);
      DESCRIBE_ADDRESS(sender_sp);
      DESCRIBE_ADDRESS(oop_tmp);
      DESCRIBE_ADDRESS(lresult);
      DESCRIBE_ADDRESS(fresult);
  }
}
#endif

intptr_t *frame::initial_deoptimization_info() {
  // unused... but returns fp() to minimize changes introduced by 7087445
  return fp();
}

#ifndef PRODUCT
// This is a generic constructor which is only used by pns() in debug.cpp.
frame::frame(void* sp, void* fp, void* pc) : _sp((intptr_t*)sp), _unextended_sp((intptr_t*)sp) {
  find_codeblob_and_set_pc_and_deopt_state((address)pc); // also sets _fp and adjusts _unextended_sp
}

void frame::pd_ps() {}
#endif
