/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, 2020, Red Hat Inc. All rights reserved.
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
#include "prims/methodHandles.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/monitorChunk.hpp"
#include "runtime/os.inline.hpp"
#include "runtime/signature.hpp"
#include "runtime/stackWatermarkSet.hpp"
#include "runtime/stubCodeGenerator.hpp"
#include "runtime/stubRoutines.hpp"
#include "vmreg_aarch64.inline.hpp"
#ifdef COMPILER1
#include "c1/c1_Runtime1.hpp"
#include "runtime/vframeArray.hpp"
#endif

#ifdef ASSERT
void RegisterMap::check_location_valid() {
}
#endif


// Profiling/safepoint support

bool frame::safe_for_sender(JavaThread *thread) {
  address   sp = (address)_sp;
  address   fp = (address)_fp;
  address   unextended_sp = (address)_unextended_sp;

  // consider stack guards when trying to determine "safe" stack pointers
  // sp must be within the usable part of the stack (not in guards)
  if (!thread->is_in_usable_stack(sp)) {
    return false;
  }

  // When we are running interpreted code the machine stack pointer, SP, is
  // set low enough so that the Java expression stack can grow and shrink
  // without ever exceeding the machine stack bounds.  So, ESP >= SP.

  // When we call out of an interpreted method, SP is incremented so that
  // the space between SP and ESP is removed.  The SP saved in the callee's
  // frame is the SP *before* this increment.  So, when we walk a stack of
  // interpreter frames the sender's SP saved in a frame might be less than
  // the SP at the point of call.

  // So unextended sp must be within the stack but we need not to check
  // that unextended sp >= sp
  if (!thread->is_in_full_stack_checked(unextended_sp)) {
    return false;
  }

  // an fp must be within the stack and above (but not equal) sp
  // second evaluation on fp+ is added to handle situation where fp is -1
  bool fp_safe = thread->is_in_stack_range_excl(fp, sp) &&
                 thread->is_in_full_stack_checked(fp + (return_addr_offset * sizeof(void*)));

  // We know sp/unextended_sp are safe only fp is questionable here

  // If the current frame is known to the code cache then we can attempt to
  // to construct the sender and do some validation of it. This goes a long way
  // toward eliminating issues when we get in frame construction code

  if (_cb != NULL ) {

    // First check if frame is complete and tester is reliable
    // Unfortunately we can only check frame complete for runtime stubs and nmethod
    // other generic buffer blobs are more problematic so we just assume they are
    // ok. adapter blobs never have a frame complete and are never ok.

    if (!_cb->is_frame_complete_at(_pc)) {
      if (_cb->is_nmethod() || _cb->is_adapter_blob() || _cb->is_runtime_stub()) {
        return false;
      }
    }

    // Could just be some random pointer within the codeBlob
    if (!_cb->code_contains(_pc)) {
      return false;
    }

    // Entry frame checks
    if (is_entry_frame()) {
      // an entry frame must have a valid fp.
      return fp_safe && is_entry_frame_valid(thread);
    }

    intptr_t* sender_sp = NULL;
    intptr_t* sender_unextended_sp = NULL;
    address   sender_pc = NULL;
    intptr_t* saved_fp =  NULL;

    if (is_interpreted_frame()) {
      // fp must be safe
      if (!fp_safe) {
        return false;
      }

      sender_pc = (address) this->fp()[return_addr_offset];
      // for interpreted frames, the value below is the sender "raw" sp,
      // which can be different from the sender unextended sp (the sp seen
      // by the sender) because of current frame local variables
      sender_sp = (intptr_t*) addr_at(sender_sp_offset);
      sender_unextended_sp = (intptr_t*) this->fp()[interpreter_frame_sender_sp_offset];
      saved_fp = (intptr_t*) this->fp()[link_offset];

    } else {
      // must be some sort of compiled/runtime frame
      // fp does not have to be safe (although it could be check for c1?)

      // check for a valid frame_size, otherwise we are unlikely to get a valid sender_pc
      if (_cb->frame_size() <= 0) {
        return false;
      }

      sender_sp = _unextended_sp + _cb->frame_size();
      // Is sender_sp safe?
      if (!thread->is_in_full_stack_checked((address)sender_sp)) {
        return false;
      }
      sender_unextended_sp = sender_sp;
      sender_pc = (address) *(sender_sp-1);
      // Note: frame::sender_sp_offset is only valid for compiled frame
      saved_fp = (intptr_t*) *(sender_sp - frame::sender_sp_offset);
    }


    // If the potential sender is the interpreter then we can do some more checking
    if (Interpreter::contains(sender_pc)) {

      // fp is always saved in a recognizable place in any code we generate. However
      // only if the sender is interpreted/call_stub (c1 too?) are we certain that the saved fp
      // is really a frame pointer.

      if (!thread->is_in_stack_range_excl((address)saved_fp, (address)sender_sp)) {
        return false;
      }

      // construct the potential sender

      frame sender(sender_sp, sender_unextended_sp, saved_fp, sender_pc);

      return sender.is_interpreted_frame_valid(thread);

    }

    // We must always be able to find a recognizable pc
    CodeBlob* sender_blob = CodeCache::find_blob_unsafe(sender_pc);
    if (sender_pc == NULL ||  sender_blob == NULL) {
      return false;
    }

    // Could be a zombie method
    if (sender_blob->is_zombie() || sender_blob->is_unloaded()) {
      return false;
    }

    // Could just be some random pointer within the codeBlob
    if (!sender_blob->code_contains(sender_pc)) {
      return false;
    }

    // We should never be able to see an adapter if the current frame is something from code cache
    if (sender_blob->is_adapter_blob()) {
      return false;
    }

    // Could be the call_stub
    if (StubRoutines::returns_to_call_stub(sender_pc)) {
      if (!thread->is_in_stack_range_excl((address)saved_fp, (address)sender_sp)) {
        return false;
      }

      // construct the potential sender

      frame sender(sender_sp, sender_unextended_sp, saved_fp, sender_pc);

      // Validate the JavaCallWrapper an entry frame must have
      address jcw = (address)sender.entry_frame_call_wrapper();

      return thread->is_in_stack_range_excl(jcw, (address)sender.fp());
    }

    CompiledMethod* nm = sender_blob->as_compiled_method_or_null();
    if (nm != NULL) {
      if (nm->is_deopt_mh_entry(sender_pc) || nm->is_deopt_entry(sender_pc) ||
          nm->method()->is_method_handle_intrinsic()) {
        return false;
      }
    }

    // If the frame size is 0 something (or less) is bad because every nmethod has a non-zero frame size
    // because the return address counts against the callee's frame.

    if (sender_blob->frame_size() <= 0) {
      assert(!sender_blob->is_compiled(), "should count return address at least");
      return false;
    }

    // We should never be able to see anything here except an nmethod. If something in the
    // code cache (current frame) is called by an entity within the code cache that entity
    // should not be anything but the call stub (already covered), the interpreter (already covered)
    // or an nmethod.

    if (!sender_blob->is_compiled()) {
        return false;
    }

    // Could put some more validation for the potential non-interpreted sender
    // frame we'd create by calling sender if I could think of any. Wait for next crash in forte...

    // One idea is seeing if the sender_pc we have is one that we'd expect to call to current cb

    // We've validated the potential sender that would be created
    return true;
  }

  // Must be native-compiled frame. Since sender will try and use fp to find
  // linkages it must be safe

  if (!fp_safe) {
    return false;
  }

  // Will the pc we fetch be non-zero (which we'll find at the oldest frame)

  if ( (address) this->fp()[return_addr_offset] == NULL) return false;


  // could try and do some more potential verification of native frame if we could think of some...

  return true;

}

void frame::patch_pc(Thread* thread, address pc) {
  assert(_cb == CodeCache::find_blob(pc), "unexpected pc");
  address* pc_addr = &(((address*) sp())[-1]);
  if (TracePcPatching) {
    tty->print_cr("patch_pc at address " INTPTR_FORMAT " [" INTPTR_FORMAT " -> " INTPTR_FORMAT "]",
                  p2i(pc_addr), p2i(*pc_addr), p2i(pc));
  }

  // Only generated code frames should be patched, therefore the return address will not be signed.
  assert(pauth_ptr_is_raw(*pc_addr), "cannot be signed");
  // Either the return address is the original one or we are going to
  // patch in the same address that's already there.
  assert(_pc == *pc_addr || pc == *pc_addr, "must be");
  *pc_addr = pc;
  address original_pc = CompiledMethod::get_deopt_original_pc(this);
  if (original_pc != NULL) {
    assert(original_pc == _pc, "expected original PC to be stored before patching");
    _deopt_state = is_deoptimized;
    // leave _pc as is
  } else {
    _deopt_state = not_deoptimized;
    _pc = pc;
  }
}

bool frame::is_interpreted_frame() const  {
  return Interpreter::contains(pc());
}

int frame::frame_size(RegisterMap* map) const {
  frame sender = this->sender(map);
  return sender.sp() - sp();
}

intptr_t* frame::entry_frame_argument_at(int offset) const {
  // convert offset to index to deal with tsi
  int index = (Interpreter::expr_offset_in_bytes(offset)/wordSize);
  // Entry frame's arguments are always in relation to unextended_sp()
  return &unextended_sp()[index];
}

// sender_sp
intptr_t* frame::interpreter_frame_sender_sp() const {
  assert(is_interpreted_frame(), "interpreted frame expected");
  return (intptr_t*) at(interpreter_frame_sender_sp_offset);
}

void frame::set_interpreter_frame_sender_sp(intptr_t* sender_sp) {
  assert(is_interpreted_frame(), "interpreted frame expected");
  ptr_at_put(interpreter_frame_sender_sp_offset, (intptr_t) sender_sp);
}


// monitor elements

BasicObjectLock* frame::interpreter_frame_monitor_begin() const {
  return (BasicObjectLock*) addr_at(interpreter_frame_monitor_block_bottom_offset);
}

BasicObjectLock* frame::interpreter_frame_monitor_end() const {
  BasicObjectLock* result = (BasicObjectLock*) *addr_at(interpreter_frame_monitor_block_top_offset);
  // make sure the pointer points inside the frame
  assert(sp() <= (intptr_t*) result, "monitor end should be above the stack pointer");
  assert((intptr_t*) result < fp(),  "monitor end should be strictly below the frame pointer");
  return result;
}

void frame::interpreter_frame_set_monitor_end(BasicObjectLock* value) {
  *((BasicObjectLock**)addr_at(interpreter_frame_monitor_block_top_offset)) = value;
}

// Used by template based interpreter deoptimization
void frame::interpreter_frame_set_last_sp(intptr_t* sp) {
    *((intptr_t**)addr_at(interpreter_frame_last_sp_offset)) = sp;
}

frame frame::sender_for_entry_frame(RegisterMap* map) const {
  assert(map != NULL, "map must be set");
  // Java frame called from C; skip all C frames and return top C
  // frame of that chunk as the sender
  JavaFrameAnchor* jfa = entry_frame_call_wrapper()->anchor();
  assert(!entry_frame_is_first(), "next Java fp must be non zero");
  assert(jfa->last_Java_sp() > sp(), "must be above this frame on stack");
  // Since we are walking the stack now this nested anchor is obviously walkable
  // even if it wasn't when it was stacked.
  if (!jfa->walkable()) {
    // Capture _last_Java_pc (if needed) and mark anchor walkable.
    jfa->capture_last_Java_pc();
  }
  map->clear();
  assert(map->include_argument_oops(), "should be set by clear");
  vmassert(jfa->last_Java_pc() != NULL, "not walkable");
  frame fr(jfa->last_Java_sp(), jfa->last_Java_fp(), jfa->last_Java_pc());

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

frame frame::sender_for_optimized_entry_frame(RegisterMap* map) const {
  ShouldNotCallThis();
  return {};
}

//------------------------------------------------------------------------------
// frame::verify_deopt_original_pc
//
// Verifies the calculated original PC of a deoptimization PC for the
// given unextended SP.
#ifdef ASSERT
void frame::verify_deopt_original_pc(CompiledMethod* nm, intptr_t* unextended_sp) {
  frame fr;

  // This is ugly but it's better than to change {get,set}_original_pc
  // to take an SP value as argument.  And it's only a debugging
  // method anyway.
  fr._unextended_sp = unextended_sp;

  address original_pc = nm->get_original_pc(&fr);
  assert(nm->insts_contains_inclusive(original_pc),
         "original PC must be in the main code section of the the compiled method (or must be immediately following it)");
}
#endif

//------------------------------------------------------------------------------
// frame::adjust_unextended_sp
void frame::adjust_unextended_sp() {
  // On aarch64, sites calling method handle intrinsics and lambda forms are treated
  // as any other call site. Therefore, no special action is needed when we are
  // returning to any of these call sites.

  if (_cb != NULL) {
    CompiledMethod* sender_cm = _cb->as_compiled_method_or_null();
    if (sender_cm != NULL) {
      // If the sender PC is a deoptimization point, get the original PC.
      if (sender_cm->is_deopt_entry(_pc) ||
          sender_cm->is_deopt_mh_entry(_pc)) {
        DEBUG_ONLY(verify_deopt_original_pc(sender_cm, _unextended_sp));
      }
    }
  }
}

//------------------------------------------------------------------------------
// frame::update_map_with_saved_link
void frame::update_map_with_saved_link(RegisterMap* map, intptr_t** link_addr) {
  // The interpreter and compiler(s) always save fp in a known
  // location on entry. We must record where that location is
  // so that if fp was live on callout from c2 we can find
  // the saved copy no matter what it called.

  // Since the interpreter always saves fp if we record where it is then
  // we don't have to always save fp on entry and exit to c2 compiled
  // code, on entry will be enough.
  map->set_location(rfp->as_VMReg(), (address) link_addr);
  // this is weird "H" ought to be at a higher address however the
  // oopMaps seems to have the "H" regs at the same address and the
  // vanilla register.
  // XXXX make this go away
  if (true) {
    map->set_location(rfp->as_VMReg()->next(), (address) link_addr);
  }
}


//------------------------------------------------------------------------------
// frame::sender_for_interpreter_frame
frame frame::sender_for_interpreter_frame(RegisterMap* map) const {
  // SP is the raw SP from the sender after adapter or interpreter
  // extension.
  intptr_t* sender_sp = this->sender_sp();

  // This is the sp before any possible extension (adapter/locals).
  intptr_t* unextended_sp = interpreter_frame_sender_sp();

#if COMPILER2_OR_JVMCI
  if (map->update_map()) {
    update_map_with_saved_link(map, (intptr_t**) addr_at(link_offset));
  }
#endif // COMPILER2_OR_JVMCI

  // Use the raw version of pc - the interpreter should not have signed it.

  return frame(sender_sp, unextended_sp, link(), sender_pc_maybe_signed());
}


//------------------------------------------------------------------------------
// frame::sender_for_compiled_frame
frame frame::sender_for_compiled_frame(RegisterMap* map) const {
  // we cannot rely upon the last fp having been saved to the thread
  // in C2 code but it will have been pushed onto the stack. so we
  // have to find it relative to the unextended sp

  assert(_cb->frame_size() >= 0, "must have non-zero frame size");
  intptr_t* l_sender_sp = unextended_sp() + _cb->frame_size();
  intptr_t* unextended_sp = l_sender_sp;

  // the return_address is always the word on the stack
  address sender_pc = (address) *(l_sender_sp-1);

  intptr_t** saved_fp_addr = (intptr_t**) (l_sender_sp - frame::sender_sp_offset);

  // assert (sender_sp() == l_sender_sp, "should be");
  // assert (*saved_fp_addr == link(), "should be");

  if (map->update_map()) {
    // Tell GC to use argument oopmaps for some runtime stubs that need it.
    // For C1, the runtime stub might not have oop maps, so set this flag
    // outside of update_register_map.
    map->set_include_argument_oops(_cb->caller_must_gc_arguments(map->thread()));
    if (_cb->oop_maps() != NULL) {
      OopMapSet::update_register_map(this, map);
    }

    // Since the prolog does the save and restore of FP there is no
    // oopmap for it so we must fill in its location as if there was
    // an oopmap entry since if our caller was compiled code there
    // could be live jvm state in it.
    update_map_with_saved_link(map, saved_fp_addr);
  }

  return frame(l_sender_sp, unextended_sp, *saved_fp_addr, sender_pc);
}

//------------------------------------------------------------------------------
// frame::sender_raw
frame frame::sender_raw(RegisterMap* map) const {
  // Default is we done have to follow them. The sender_for_xxx will
  // update it accordingly
   map->set_include_argument_oops(false);

  if (is_entry_frame())
    return sender_for_entry_frame(map);
  if (is_interpreted_frame())
    return sender_for_interpreter_frame(map);
  assert(_cb == CodeCache::find_blob(pc()),"Must be the same");

  // This test looks odd: why is it not is_compiled_frame() ?  That's
  // because stubs also have OOP maps.
  if (_cb != NULL) {
    return sender_for_compiled_frame(map);
  }

  // Must be native-compiled frame, i.e. the marshaling code for native
  // methods that exists in the core system.

  return frame(sender_sp(), link(), sender_pc());
}

frame frame::sender(RegisterMap* map) const {
  frame result = sender_raw(map);

  if (map->process_frames()) {
    StackWatermarkSet::on_iteration(map->thread(), result);
  }

  return result;
}

bool frame::is_interpreted_frame_valid(JavaThread* thread) const {
  assert(is_interpreted_frame(), "Not an interpreted frame");
  // These are reasonable sanity checks
  if (fp() == 0 || (intptr_t(fp()) & (wordSize-1)) != 0) {
    return false;
  }
  if (sp() == 0 || (intptr_t(sp()) & (wordSize-1)) != 0) {
    return false;
  }
  if (fp() + interpreter_frame_initial_sp_offset < sp()) {
    return false;
  }
  // These are hacks to keep us out of trouble.
  // The problem with these is that they mask other problems
  if (fp() <= sp()) {        // this attempts to deal with unsigned comparison above
    return false;
  }

  // do some validation of frame elements

  // first the method

  Method* m = *interpreter_frame_method_addr();

  // validate the method we'd find in this potential sender
  if (!Method::is_valid_method(m)) return false;

  // stack frames shouldn't be much larger than max_stack elements
  // this test requires the use of unextended_sp which is the sp as seen by
  // the current frame, and not sp which is the "raw" pc which could point
  // further because of local variables of the callee method inserted after
  // method arguments
  if (fp() - unextended_sp() > 1024 + m->max_stack()*Interpreter::stackElementSize) {
    return false;
  }

  // validate bci/bcx

  address  bcp    = interpreter_frame_bcp();
  if (m->validate_bci_from_bcp(bcp) < 0) {
    return false;
  }

  // validate constantPoolCache*
  ConstantPoolCache* cp = *interpreter_frame_cache_addr();
  if (MetaspaceObj::is_valid(cp) == false) return false;

  // validate locals

  address locals =  (address) *interpreter_frame_locals_addr();
  return thread->is_in_stack_range_incl(locals, (address)fp());
}

BasicType frame::interpreter_frame_result(oop* oop_result, jvalue* value_result) {
  assert(is_interpreted_frame(), "interpreted frame expected");
  Method* method = interpreter_frame_method();
  BasicType type = method->result_type();

  intptr_t* tos_addr;
  if (method->is_native()) {
    // TODO : ensure AARCH64 does the same as Intel here i.e. push v0 then r0
    // Prior to calling into the runtime to report the method_exit the possible
    // return value is pushed to the native stack. If the result is a jfloat/jdouble
    // then ST0 is saved before EAX/EDX. See the note in generate_native_result
    tos_addr = (intptr_t*)sp();
    if (type == T_FLOAT || type == T_DOUBLE) {
      // This is times two because we do a push(ltos) after pushing XMM0
      // and that takes two interpreter stack slots.
      tos_addr += 2 * Interpreter::stackElementWords;
    }
  } else {
    tos_addr = (intptr_t*)interpreter_frame_tos_address();
  }

  switch (type) {
    case T_OBJECT  :
    case T_ARRAY   : {
      oop obj;
      if (method->is_native()) {
        obj = cast_to_oop(at(interpreter_frame_oop_temp_offset));
      } else {
        oop* obj_p = (oop*)tos_addr;
        obj = (obj_p == NULL) ? (oop)NULL : *obj_p;
      }
      assert(Universe::is_in_heap_or_null(obj), "sanity check");
      *oop_result = obj;
      break;
    }
    case T_BOOLEAN : value_result->z = *(jboolean*)tos_addr; break;
    case T_BYTE    : value_result->b = *(jbyte*)tos_addr; break;
    case T_CHAR    : value_result->c = *(jchar*)tos_addr; break;
    case T_SHORT   : value_result->s = *(jshort*)tos_addr; break;
    case T_INT     : value_result->i = *(jint*)tos_addr; break;
    case T_LONG    : value_result->j = *(jlong*)tos_addr; break;
    case T_FLOAT   : {
        value_result->f = *(jfloat*)tos_addr;
      break;
    }
    case T_DOUBLE  : value_result->d = *(jdouble*)tos_addr; break;
    case T_VOID    : /* Nothing to do */ break;
    default        : ShouldNotReachHere();
  }

  return type;
}


intptr_t* frame::interpreter_frame_tos_at(jint offset) const {
  int index = (Interpreter::expr_offset_in_bytes(offset)/wordSize);
  return &interpreter_frame_tos_address()[index];
}

#ifndef PRODUCT

#define DESCRIBE_FP_OFFSET(name) \
  values.describe(frame_no, fp() + frame::name##_offset, #name)

void frame::describe_pd(FrameValues& values, int frame_no) {
  if (is_interpreted_frame()) {
    DESCRIBE_FP_OFFSET(interpreter_frame_sender_sp);
    DESCRIBE_FP_OFFSET(interpreter_frame_last_sp);
    DESCRIBE_FP_OFFSET(interpreter_frame_method);
    DESCRIBE_FP_OFFSET(interpreter_frame_mdp);
    DESCRIBE_FP_OFFSET(interpreter_frame_mirror);
    DESCRIBE_FP_OFFSET(interpreter_frame_cache);
    DESCRIBE_FP_OFFSET(interpreter_frame_locals);
    DESCRIBE_FP_OFFSET(interpreter_frame_bcp);
    DESCRIBE_FP_OFFSET(interpreter_frame_initial_sp);
  }
}
#endif

intptr_t *frame::initial_deoptimization_info() {
  // Not used on aarch64, but we must return something.
  return NULL;
}

intptr_t* frame::real_fp() const {
  if (_cb != NULL) {
    // use the frame size if valid
    int size = _cb->frame_size();
    if (size > 0) {
      return unextended_sp() + size;
    }
  }
  // else rely on fp()
  assert(! is_compiled_frame(), "unknown compiled frame size");
  return fp();
}

#undef DESCRIBE_FP_OFFSET

#define DESCRIBE_FP_OFFSET(name)                     \
  {                                                  \
    uintptr_t *p = (uintptr_t *)fp;                  \
    printf(INTPTR_FORMAT " " INTPTR_FORMAT " %s\n",  \
           (uintptr_t)(p + frame::name##_offset),    \
           p[frame::name##_offset], #name);          \
  }

static THREAD_LOCAL uintptr_t nextfp;
static THREAD_LOCAL uintptr_t nextpc;
static THREAD_LOCAL uintptr_t nextsp;
static THREAD_LOCAL RegisterMap *reg_map;

static void printbc(Method *m, intptr_t bcx) {
  const char *name;
  char buf[16];
  if (m->validate_bci_from_bcp((address)bcx) < 0
      || !m->contains((address)bcx)) {
    name = "???";
    snprintf(buf, sizeof buf, "(bad)");
  } else {
    int bci = m->bci_from((address)bcx);
    snprintf(buf, sizeof buf, "%d", bci);
    name = Bytecodes::name(m->code_at(bci));
  }
  ResourceMark rm;
  printf("%s : %s ==> %s\n", m->name_and_sig_as_C_string(), buf, name);
}

void internal_pf(uintptr_t sp, uintptr_t fp, uintptr_t pc, uintptr_t bcx) {
  if (! fp)
    return;

  DESCRIBE_FP_OFFSET(return_addr);
  DESCRIBE_FP_OFFSET(link);
  DESCRIBE_FP_OFFSET(interpreter_frame_sender_sp);
  DESCRIBE_FP_OFFSET(interpreter_frame_last_sp);
  DESCRIBE_FP_OFFSET(interpreter_frame_method);
  DESCRIBE_FP_OFFSET(interpreter_frame_mdp);
  DESCRIBE_FP_OFFSET(interpreter_frame_cache);
  DESCRIBE_FP_OFFSET(interpreter_frame_locals);
  DESCRIBE_FP_OFFSET(interpreter_frame_bcp);
  DESCRIBE_FP_OFFSET(interpreter_frame_initial_sp);
  uintptr_t *p = (uintptr_t *)fp;

  // We want to see all frames, native and Java.  For compiled and
  // interpreted frames we have special information that allows us to
  // unwind them; for everything else we assume that the native frame
  // pointer chain is intact.
  frame this_frame((intptr_t*)sp, (intptr_t*)fp, (address)pc);
  if (this_frame.is_compiled_frame() ||
      this_frame.is_interpreted_frame()) {
    frame sender = this_frame.sender(reg_map);
    nextfp = (uintptr_t)sender.fp();
    nextpc = (uintptr_t)sender.pc();
    nextsp = (uintptr_t)sender.unextended_sp();
  } else {
    nextfp = p[frame::link_offset];
    nextpc = p[frame::return_addr_offset];
    nextsp = (uintptr_t)&p[frame::sender_sp_offset];
  }

  if (bcx == -1ULL)
    bcx = p[frame::interpreter_frame_bcp_offset];

  if (Interpreter::contains((address)pc)) {
    Method* m = (Method*)p[frame::interpreter_frame_method_offset];
    if(m && m->is_method()) {
      printbc(m, bcx);
    } else
      printf("not a Method\n");
  } else {
    CodeBlob *cb = CodeCache::find_blob((address)pc);
    if (cb != NULL) {
      if (cb->is_nmethod()) {
        ResourceMark rm;
        nmethod* nm = (nmethod*)cb;
        printf("nmethod %s\n", nm->method()->name_and_sig_as_C_string());
      } else if (cb->name()) {
        printf("CodeBlob %s\n", cb->name());
      }
    }
  }
}

extern "C" void npf() {
  CodeBlob *cb = CodeCache::find_blob((address)nextpc);
  // C2 does not always chain the frame pointers when it can, instead
  // preferring to use fixed offsets from SP, so a simple leave() does
  // not work.  Instead, it adds the frame size to SP then pops FP and
  // LR.  We have to do the same thing to get a good call chain.
  if (cb && cb->frame_size())
    nextfp = nextsp + wordSize * (cb->frame_size() - 2);
  internal_pf (nextsp, nextfp, nextpc, -1);
}

extern "C" void pf(uintptr_t sp, uintptr_t fp, uintptr_t pc,
                   uintptr_t bcx, uintptr_t thread) {
  if (!reg_map) {
    reg_map = NEW_C_HEAP_OBJ(RegisterMap, mtInternal);
    ::new (reg_map) RegisterMap((JavaThread*)thread, false);
  } else {
    *reg_map = RegisterMap((JavaThread*)thread, false);
  }

  {
    CodeBlob *cb = CodeCache::find_blob((address)pc);
    if (cb && cb->frame_size())
      fp = sp + wordSize * (cb->frame_size() - 2);
  }
  internal_pf(sp, fp, pc, bcx);
}

// support for printing out where we are in a Java method
// needs to be passed current fp and bcp register values
// prints method name, bc index and bytecode name
extern "C" void pm(uintptr_t fp, uintptr_t bcx) {
  DESCRIBE_FP_OFFSET(interpreter_frame_method);
  uintptr_t *p = (uintptr_t *)fp;
  Method* m = (Method*)p[frame::interpreter_frame_method_offset];
  printbc(m, bcx);
}

#ifndef PRODUCT
// This is a generic constructor which is only used by pns() in debug.cpp.
frame::frame(void* sp, void* fp, void* pc) {
  init((intptr_t*)sp, (intptr_t*)fp, (address)pc);
}

void frame::pd_ps() {}
#endif

void JavaFrameAnchor::make_walkable(JavaThread* thread) {
  // last frame set?
  if (last_Java_sp() == NULL) return;
  // already walkable?
  if (walkable()) return;
  vmassert(Thread::current() == (Thread*)thread, "not current thread");
  vmassert(last_Java_sp() != NULL, "not called from Java code?");
  vmassert(last_Java_pc() == NULL, "already walkable");
  capture_last_Java_pc();
  vmassert(walkable(), "something went wrong");
}

void JavaFrameAnchor::capture_last_Java_pc() {
  vmassert(_last_Java_sp != NULL, "no last frame set");
  vmassert(_last_Java_pc == NULL, "already walkable");
  _last_Java_pc = (address)_last_Java_sp[-1];
}
