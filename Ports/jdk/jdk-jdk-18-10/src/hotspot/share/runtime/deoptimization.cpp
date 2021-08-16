/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm.h"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmClasses.hpp"
#include "code/codeCache.hpp"
#include "code/debugInfoRec.hpp"
#include "code/nmethod.hpp"
#include "code/pcDesc.hpp"
#include "code/scopeDesc.hpp"
#include "compiler/compilationPolicy.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "interpreter/bytecode.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/oopMapCache.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/constantPool.hpp"
#include "oops/method.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/fieldStreams.inline.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "oops/verifyOopClosure.hpp"
#include "prims/jvmtiDeferredUpdates.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/jvmtiThreadState.hpp"
#include "prims/vectorSupport.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/atomic.hpp"
#include "runtime/deoptimization.hpp"
#include "runtime/escapeBarrier.hpp"
#include "runtime/fieldDescriptor.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/keepStackGCProcessed.hpp"
#include "runtime/objectMonitor.inline.hpp"
#include "runtime/osThread.hpp"
#include "runtime/safepointVerifiers.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/signature.hpp"
#include "runtime/stackFrameStream.inline.hpp"
#include "runtime/stackWatermarkSet.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/thread.hpp"
#include "runtime/threadSMR.hpp"
#include "runtime/threadWXSetters.inline.hpp"
#include "runtime/vframe.hpp"
#include "runtime/vframeArray.hpp"
#include "runtime/vframe_hp.hpp"
#include "runtime/vmOperations.hpp"
#include "utilities/events.hpp"
#include "utilities/macros.hpp"
#include "utilities/preserveException.hpp"
#include "utilities/xmlstream.hpp"
#if INCLUDE_JFR
#include "jfr/jfrEvents.hpp"
#include "jfr/metadata/jfrSerializer.hpp"
#endif

bool DeoptimizationMarker::_is_active = false;

Deoptimization::UnrollBlock::UnrollBlock(int  size_of_deoptimized_frame,
                                         int  caller_adjustment,
                                         int  caller_actual_parameters,
                                         int  number_of_frames,
                                         intptr_t* frame_sizes,
                                         address* frame_pcs,
                                         BasicType return_type,
                                         int exec_mode) {
  _size_of_deoptimized_frame = size_of_deoptimized_frame;
  _caller_adjustment         = caller_adjustment;
  _caller_actual_parameters  = caller_actual_parameters;
  _number_of_frames          = number_of_frames;
  _frame_sizes               = frame_sizes;
  _frame_pcs                 = frame_pcs;
  _register_block            = NEW_C_HEAP_ARRAY(intptr_t, RegisterMap::reg_count * 2, mtCompiler);
  _return_type               = return_type;
  _initial_info              = 0;
  // PD (x86 only)
  _counter_temp              = 0;
  _unpack_kind               = exec_mode;
  _sender_sp_temp            = 0;

  _total_frame_sizes         = size_of_frames();
  assert(exec_mode >= 0 && exec_mode < Unpack_LIMIT, "Unexpected exec_mode");
}


Deoptimization::UnrollBlock::~UnrollBlock() {
  FREE_C_HEAP_ARRAY(intptr_t, _frame_sizes);
  FREE_C_HEAP_ARRAY(intptr_t, _frame_pcs);
  FREE_C_HEAP_ARRAY(intptr_t, _register_block);
}


intptr_t* Deoptimization::UnrollBlock::value_addr_at(int register_number) const {
  assert(register_number < RegisterMap::reg_count, "checking register number");
  return &_register_block[register_number * 2];
}



int Deoptimization::UnrollBlock::size_of_frames() const {
  // Acount first for the adjustment of the initial frame
  int result = _caller_adjustment;
  for (int index = 0; index < number_of_frames(); index++) {
    result += frame_sizes()[index];
  }
  return result;
}


void Deoptimization::UnrollBlock::print() {
  ttyLocker ttyl;
  tty->print_cr("UnrollBlock");
  tty->print_cr("  size_of_deoptimized_frame = %d", _size_of_deoptimized_frame);
  tty->print(   "  frame_sizes: ");
  for (int index = 0; index < number_of_frames(); index++) {
    tty->print(INTX_FORMAT " ", frame_sizes()[index]);
  }
  tty->cr();
}


// In order to make fetch_unroll_info work properly with escape
// analysis, the method was changed from JRT_LEAF to JRT_BLOCK_ENTRY.
// The actual reallocation of previously eliminated objects occurs in realloc_objects,
// which is called from the method fetch_unroll_info_helper below.
JRT_BLOCK_ENTRY(Deoptimization::UnrollBlock*, Deoptimization::fetch_unroll_info(JavaThread* current, int exec_mode))
  // fetch_unroll_info() is called at the beginning of the deoptimization
  // handler. Note this fact before we start generating temporary frames
  // that can confuse an asynchronous stack walker. This counter is
  // decremented at the end of unpack_frames().
  if (TraceDeoptimization) {
    tty->print_cr("Deoptimizing thread " INTPTR_FORMAT, p2i(current));
  }
  current->inc_in_deopt_handler();

  if (exec_mode == Unpack_exception) {
    // When we get here, a callee has thrown an exception into a deoptimized
    // frame. That throw might have deferred stack watermark checking until
    // after unwinding. So we deal with such deferred requests here.
    StackWatermarkSet::after_unwind(current);
  }

  return fetch_unroll_info_helper(current, exec_mode);
JRT_END

#if COMPILER2_OR_JVMCI
static bool rematerialize_objects(JavaThread* thread, int exec_mode, CompiledMethod* compiled_method,
                                  frame& deoptee, RegisterMap& map, GrowableArray<compiledVFrame*>* chunk,
                                  bool& deoptimized_objects) {
  bool realloc_failures = false;
  assert (chunk->at(0)->scope() != NULL,"expect only compiled java frames");

  JavaThread* deoptee_thread = chunk->at(0)->thread();
  assert(exec_mode == Deoptimization::Unpack_none || (deoptee_thread == thread),
         "a frame can only be deoptimized by the owner thread");

  GrowableArray<ScopeValue*>* objects = chunk->at(0)->scope()->objects();

  // The flag return_oop() indicates call sites which return oop
  // in compiled code. Such sites include java method calls,
  // runtime calls (for example, used to allocate new objects/arrays
  // on slow code path) and any other calls generated in compiled code.
  // It is not guaranteed that we can get such information here only
  // by analyzing bytecode in deoptimized frames. This is why this flag
  // is set during method compilation (see Compile::Process_OopMap_Node()).
  // If the previous frame was popped or if we are dispatching an exception,
  // we don't have an oop result.
  bool save_oop_result = chunk->at(0)->scope()->return_oop() && !thread->popframe_forcing_deopt_reexecution() && (exec_mode == Deoptimization::Unpack_deopt);
  Handle return_value;
  if (save_oop_result) {
    // Reallocation may trigger GC. If deoptimization happened on return from
    // call which returns oop we need to save it since it is not in oopmap.
    oop result = deoptee.saved_oop_result(&map);
    assert(oopDesc::is_oop_or_null(result), "must be oop");
    return_value = Handle(thread, result);
    assert(Universe::heap()->is_in_or_null(result), "must be heap pointer");
    if (TraceDeoptimization) {
      ttyLocker ttyl;
      tty->print_cr("SAVED OOP RESULT " INTPTR_FORMAT " in thread " INTPTR_FORMAT, p2i(result), p2i(thread));
    }
  }
  if (objects != NULL) {
    if (exec_mode == Deoptimization::Unpack_none) {
      assert(thread->thread_state() == _thread_in_vm, "assumption");
      JavaThread* THREAD = thread; // For exception macros.
      // Clear pending OOM if reallocation fails and return true indicating allocation failure
      realloc_failures = Deoptimization::realloc_objects(thread, &deoptee, &map, objects, CHECK_AND_CLEAR_(true));
      deoptimized_objects = true;
    } else {
      JavaThread* current = thread; // For JRT_BLOCK
      JRT_BLOCK
      realloc_failures = Deoptimization::realloc_objects(thread, &deoptee, &map, objects, THREAD);
      JRT_END
    }
    bool skip_internal = (compiled_method != NULL) && !compiled_method->is_compiled_by_jvmci();
    Deoptimization::reassign_fields(&deoptee, &map, objects, realloc_failures, skip_internal);
#ifndef PRODUCT
    if (TraceDeoptimization) {
      ttyLocker ttyl;
      tty->print_cr("REALLOC OBJECTS in thread " INTPTR_FORMAT, p2i(deoptee_thread));
      Deoptimization::print_objects(objects, realloc_failures);
    }
#endif
  }
  if (save_oop_result) {
    // Restore result.
    deoptee.set_saved_oop_result(&map, return_value());
  }
  return realloc_failures;
}

static void restore_eliminated_locks(JavaThread* thread, GrowableArray<compiledVFrame*>* chunk, bool realloc_failures,
                                     frame& deoptee, int exec_mode, bool& deoptimized_objects) {
  JavaThread* deoptee_thread = chunk->at(0)->thread();
  assert(!EscapeBarrier::objs_are_deoptimized(deoptee_thread, deoptee.id()), "must relock just once");
  assert(thread == Thread::current(), "should be");
  HandleMark hm(thread);
#ifndef PRODUCT
  bool first = true;
#endif
  for (int i = 0; i < chunk->length(); i++) {
    compiledVFrame* cvf = chunk->at(i);
    assert (cvf->scope() != NULL,"expect only compiled java frames");
    GrowableArray<MonitorInfo*>* monitors = cvf->monitors();
    if (monitors->is_nonempty()) {
      bool relocked = Deoptimization::relock_objects(thread, monitors, deoptee_thread, deoptee,
                                                     exec_mode, realloc_failures);
      deoptimized_objects = deoptimized_objects || relocked;
#ifndef PRODUCT
      if (PrintDeoptimizationDetails) {
        ttyLocker ttyl;
        for (int j = 0; j < monitors->length(); j++) {
          MonitorInfo* mi = monitors->at(j);
          if (mi->eliminated()) {
            if (first) {
              first = false;
              tty->print_cr("RELOCK OBJECTS in thread " INTPTR_FORMAT, p2i(thread));
            }
            if (exec_mode == Deoptimization::Unpack_none) {
              ObjectMonitor* monitor = deoptee_thread->current_waiting_monitor();
              if (monitor != NULL && monitor->object() == mi->owner()) {
                tty->print_cr("     object <" INTPTR_FORMAT "> DEFERRED relocking after wait", p2i(mi->owner()));
                continue;
              }
            }
            if (mi->owner_is_scalar_replaced()) {
              Klass* k = java_lang_Class::as_Klass(mi->owner_klass());
              tty->print_cr("     failed reallocation for klass %s", k->external_name());
            } else {
              tty->print_cr("     object <" INTPTR_FORMAT "> locked", p2i(mi->owner()));
            }
          }
        }
      }
#endif // !PRODUCT
    }
  }
}

// Deoptimize objects, that is reallocate and relock them, just before they escape through JVMTI.
// The given vframes cover one physical frame.
bool Deoptimization::deoptimize_objects_internal(JavaThread* thread, GrowableArray<compiledVFrame*>* chunk,
                                                 bool& realloc_failures) {
  frame deoptee = chunk->at(0)->fr();
  JavaThread* deoptee_thread = chunk->at(0)->thread();
  CompiledMethod* cm = deoptee.cb()->as_compiled_method_or_null();
  RegisterMap map(chunk->at(0)->register_map());
  bool deoptimized_objects = false;

  bool const jvmci_enabled = JVMCI_ONLY(UseJVMCICompiler) NOT_JVMCI(false);

  // Reallocate the non-escaping objects and restore their fields.
  if (jvmci_enabled COMPILER2_PRESENT(|| (DoEscapeAnalysis && EliminateAllocations)
                                      || EliminateAutoBox || EnableVectorAggressiveReboxing)) {
    realloc_failures = rematerialize_objects(thread, Unpack_none, cm, deoptee, map, chunk, deoptimized_objects);
  }

  // MonitorInfo structures used in eliminate_locks are not GC safe.
  NoSafepointVerifier no_safepoint;

  // Now relock objects if synchronization on them was eliminated.
  if (jvmci_enabled COMPILER2_PRESENT(|| ((DoEscapeAnalysis || EliminateNestedLocks) && EliminateLocks))) {
    restore_eliminated_locks(thread, chunk, realloc_failures, deoptee, Unpack_none, deoptimized_objects);
  }
  return deoptimized_objects;
}
#endif // COMPILER2_OR_JVMCI

// This is factored, since it is both called from a JRT_LEAF (deoptimization) and a JRT_ENTRY (uncommon_trap)
Deoptimization::UnrollBlock* Deoptimization::fetch_unroll_info_helper(JavaThread* current, int exec_mode) {
  // When we get here we are about to unwind the deoptee frame. In order to
  // catch not yet safe to use frames, the following stack watermark barrier
  // poll will make such frames safe to use.
  StackWatermarkSet::before_unwind(current);

  // Note: there is a safepoint safety issue here. No matter whether we enter
  // via vanilla deopt or uncommon trap we MUST NOT stop at a safepoint once
  // the vframeArray is created.
  //

  // Allocate our special deoptimization ResourceMark
  DeoptResourceMark* dmark = new DeoptResourceMark(current);
  assert(current->deopt_mark() == NULL, "Pending deopt!");
  current->set_deopt_mark(dmark);

  frame stub_frame = current->last_frame(); // Makes stack walkable as side effect
  RegisterMap map(current, true);
  RegisterMap dummy_map(current, false);
  // Now get the deoptee with a valid map
  frame deoptee = stub_frame.sender(&map);
  // Set the deoptee nmethod
  assert(current->deopt_compiled_method() == NULL, "Pending deopt!");
  CompiledMethod* cm = deoptee.cb()->as_compiled_method_or_null();
  current->set_deopt_compiled_method(cm);

  if (VerifyStack) {
    current->validate_frame_layout();
  }

  // Create a growable array of VFrames where each VFrame represents an inlined
  // Java frame.  This storage is allocated with the usual system arena.
  assert(deoptee.is_compiled_frame(), "Wrong frame type");
  GrowableArray<compiledVFrame*>* chunk = new GrowableArray<compiledVFrame*>(10);
  vframe* vf = vframe::new_vframe(&deoptee, &map, current);
  while (!vf->is_top()) {
    assert(vf->is_compiled_frame(), "Wrong frame type");
    chunk->push(compiledVFrame::cast(vf));
    vf = vf->sender();
  }
  assert(vf->is_compiled_frame(), "Wrong frame type");
  chunk->push(compiledVFrame::cast(vf));

  bool realloc_failures = false;

#if COMPILER2_OR_JVMCI
  bool const jvmci_enabled = JVMCI_ONLY(EnableJVMCI) NOT_JVMCI(false);

  // Reallocate the non-escaping objects and restore their fields. Then
  // relock objects if synchronization on them was eliminated.
  if (jvmci_enabled COMPILER2_PRESENT( || (DoEscapeAnalysis && EliminateAllocations)
                                       || EliminateAutoBox || EnableVectorAggressiveReboxing )) {
    bool unused;
    realloc_failures = rematerialize_objects(current, exec_mode, cm, deoptee, map, chunk, unused);
  }
#endif // COMPILER2_OR_JVMCI

  // Ensure that no safepoint is taken after pointers have been stored
  // in fields of rematerialized objects.  If a safepoint occurs from here on
  // out the java state residing in the vframeArray will be missed.
  // Locks may be rebaised in a safepoint.
  NoSafepointVerifier no_safepoint;

#if COMPILER2_OR_JVMCI
  if ((jvmci_enabled COMPILER2_PRESENT( || ((DoEscapeAnalysis || EliminateNestedLocks) && EliminateLocks) ))
      && !EscapeBarrier::objs_are_deoptimized(current, deoptee.id())) {
    bool unused;
    restore_eliminated_locks(current, chunk, realloc_failures, deoptee, exec_mode, unused);
  }
#endif // COMPILER2_OR_JVMCI

  ScopeDesc* trap_scope = chunk->at(0)->scope();
  Handle exceptionObject;
  if (trap_scope->rethrow_exception()) {
    if (PrintDeoptimizationDetails) {
      tty->print_cr("Exception to be rethrown in the interpreter for method %s::%s at bci %d", trap_scope->method()->method_holder()->name()->as_C_string(), trap_scope->method()->name()->as_C_string(), trap_scope->bci());
    }
    GrowableArray<ScopeValue*>* expressions = trap_scope->expressions();
    guarantee(expressions != NULL && expressions->length() > 0, "must have exception to throw");
    ScopeValue* topOfStack = expressions->top();
    exceptionObject = StackValue::create_stack_value(&deoptee, &map, topOfStack)->get_obj();
    guarantee(exceptionObject() != NULL, "exception oop can not be null");
  }

  vframeArray* array = create_vframeArray(current, deoptee, &map, chunk, realloc_failures);
#if COMPILER2_OR_JVMCI
  if (realloc_failures) {
    pop_frames_failed_reallocs(current, array);
  }
#endif

  assert(current->vframe_array_head() == NULL, "Pending deopt!");
  current->set_vframe_array_head(array);

  // Now that the vframeArray has been created if we have any deferred local writes
  // added by jvmti then we can free up that structure as the data is now in the
  // vframeArray

  JvmtiDeferredUpdates::delete_updates_for_frame(current, array->original().id());

  // Compute the caller frame based on the sender sp of stub_frame and stored frame sizes info.
  CodeBlob* cb = stub_frame.cb();
  // Verify we have the right vframeArray
  assert(cb->frame_size() >= 0, "Unexpected frame size");
  intptr_t* unpack_sp = stub_frame.sp() + cb->frame_size();

  // If the deopt call site is a MethodHandle invoke call site we have
  // to adjust the unpack_sp.
  nmethod* deoptee_nm = deoptee.cb()->as_nmethod_or_null();
  if (deoptee_nm != NULL && deoptee_nm->is_method_handle_return(deoptee.pc()))
    unpack_sp = deoptee.unextended_sp();

#ifdef ASSERT
  assert(cb->is_deoptimization_stub() ||
         cb->is_uncommon_trap_stub() ||
         strcmp("Stub<DeoptimizationStub.deoptimizationHandler>", cb->name()) == 0 ||
         strcmp("Stub<UncommonTrapStub.uncommonTrapHandler>", cb->name()) == 0,
         "unexpected code blob: %s", cb->name());
#endif

  // This is a guarantee instead of an assert because if vframe doesn't match
  // we will unpack the wrong deoptimized frame and wind up in strange places
  // where it will be very difficult to figure out what went wrong. Better
  // to die an early death here than some very obscure death later when the
  // trail is cold.
  // Note: on ia64 this guarantee can be fooled by frames with no memory stack
  // in that it will fail to detect a problem when there is one. This needs
  // more work in tiger timeframe.
  guarantee(array->unextended_sp() == unpack_sp, "vframe_array_head must contain the vframeArray to unpack");

  int number_of_frames = array->frames();

  // Compute the vframes' sizes.  Note that frame_sizes[] entries are ordered from outermost to innermost
  // virtual activation, which is the reverse of the elements in the vframes array.
  intptr_t* frame_sizes = NEW_C_HEAP_ARRAY(intptr_t, number_of_frames, mtCompiler);
  // +1 because we always have an interpreter return address for the final slot.
  address* frame_pcs = NEW_C_HEAP_ARRAY(address, number_of_frames + 1, mtCompiler);
  int popframe_extra_args = 0;
  // Create an interpreter return address for the stub to use as its return
  // address so the skeletal frames are perfectly walkable
  frame_pcs[number_of_frames] = Interpreter::deopt_entry(vtos, 0);

  // PopFrame requires that the preserved incoming arguments from the recently-popped topmost
  // activation be put back on the expression stack of the caller for reexecution
  if (JvmtiExport::can_pop_frame() && current->popframe_forcing_deopt_reexecution()) {
    popframe_extra_args = in_words(current->popframe_preserved_args_size_in_words());
  }

  // Find the current pc for sender of the deoptee. Since the sender may have been deoptimized
  // itself since the deoptee vframeArray was created we must get a fresh value of the pc rather
  // than simply use array->sender.pc(). This requires us to walk the current set of frames
  //
  frame deopt_sender = stub_frame.sender(&dummy_map); // First is the deoptee frame
  deopt_sender = deopt_sender.sender(&dummy_map);     // Now deoptee caller

  // It's possible that the number of parameters at the call site is
  // different than number of arguments in the callee when method
  // handles are used.  If the caller is interpreted get the real
  // value so that the proper amount of space can be added to it's
  // frame.
  bool caller_was_method_handle = false;
  if (deopt_sender.is_interpreted_frame()) {
    methodHandle method(current, deopt_sender.interpreter_frame_method());
    Bytecode_invoke cur = Bytecode_invoke_check(method, deopt_sender.interpreter_frame_bci());
    if (cur.is_invokedynamic() || cur.is_invokehandle()) {
      // Method handle invokes may involve fairly arbitrary chains of
      // calls so it's impossible to know how much actual space the
      // caller has for locals.
      caller_was_method_handle = true;
    }
  }

  //
  // frame_sizes/frame_pcs[0] oldest frame (int or c2i)
  // frame_sizes/frame_pcs[1] next oldest frame (int)
  // frame_sizes/frame_pcs[n] youngest frame (int)
  //
  // Now a pc in frame_pcs is actually the return address to the frame's caller (a frame
  // owns the space for the return address to it's caller).  Confusing ain't it.
  //
  // The vframe array can address vframes with indices running from
  // 0.._frames-1. Index  0 is the youngest frame and _frame - 1 is the oldest (root) frame.
  // When we create the skeletal frames we need the oldest frame to be in the zero slot
  // in the frame_sizes/frame_pcs so the assembly code can do a trivial walk.
  // so things look a little strange in this loop.
  //
  int callee_parameters = 0;
  int callee_locals = 0;
  for (int index = 0; index < array->frames(); index++ ) {
    // frame[number_of_frames - 1 ] = on_stack_size(youngest)
    // frame[number_of_frames - 2 ] = on_stack_size(sender(youngest))
    // frame[number_of_frames - 3 ] = on_stack_size(sender(sender(youngest)))
    frame_sizes[number_of_frames - 1 - index] = BytesPerWord * array->element(index)->on_stack_size(callee_parameters,
                                                                                                    callee_locals,
                                                                                                    index == 0,
                                                                                                    popframe_extra_args);
    // This pc doesn't have to be perfect just good enough to identify the frame
    // as interpreted so the skeleton frame will be walkable
    // The correct pc will be set when the skeleton frame is completely filled out
    // The final pc we store in the loop is wrong and will be overwritten below
    frame_pcs[number_of_frames - 1 - index ] = Interpreter::deopt_entry(vtos, 0) - frame::pc_return_offset;

    callee_parameters = array->element(index)->method()->size_of_parameters();
    callee_locals = array->element(index)->method()->max_locals();
    popframe_extra_args = 0;
  }

  // Compute whether the root vframe returns a float or double value.
  BasicType return_type;
  {
    methodHandle method(current, array->element(0)->method());
    Bytecode_invoke invoke = Bytecode_invoke_check(method, array->element(0)->bci());
    return_type = invoke.is_valid() ? invoke.result_type() : T_ILLEGAL;
  }

  // Compute information for handling adapters and adjusting the frame size of the caller.
  int caller_adjustment = 0;

  // Compute the amount the oldest interpreter frame will have to adjust
  // its caller's stack by. If the caller is a compiled frame then
  // we pretend that the callee has no parameters so that the
  // extension counts for the full amount of locals and not just
  // locals-parms. This is because without a c2i adapter the parm
  // area as created by the compiled frame will not be usable by
  // the interpreter. (Depending on the calling convention there
  // may not even be enough space).

  // QQQ I'd rather see this pushed down into last_frame_adjust
  // and have it take the sender (aka caller).

  if (deopt_sender.is_compiled_frame() || caller_was_method_handle) {
    caller_adjustment = last_frame_adjust(0, callee_locals);
  } else if (callee_locals > callee_parameters) {
    // The caller frame may need extending to accommodate
    // non-parameter locals of the first unpacked interpreted frame.
    // Compute that adjustment.
    caller_adjustment = last_frame_adjust(callee_parameters, callee_locals);
  }

  // If the sender is deoptimized the we must retrieve the address of the handler
  // since the frame will "magically" show the original pc before the deopt
  // and we'd undo the deopt.

  frame_pcs[0] = deopt_sender.raw_pc();

  assert(CodeCache::find_blob_unsafe(frame_pcs[0]) != NULL, "bad pc");

#if INCLUDE_JVMCI
  if (exceptionObject() != NULL) {
    current->set_exception_oop(exceptionObject());
    exec_mode = Unpack_exception;
  }
#endif

  if (current->frames_to_pop_failed_realloc() > 0 && exec_mode != Unpack_uncommon_trap) {
    assert(current->has_pending_exception(), "should have thrown OOME");
    current->set_exception_oop(current->pending_exception());
    current->clear_pending_exception();
    exec_mode = Unpack_exception;
  }

#if INCLUDE_JVMCI
  if (current->frames_to_pop_failed_realloc() > 0) {
    current->set_pending_monitorenter(false);
  }
#endif

  UnrollBlock* info = new UnrollBlock(array->frame_size() * BytesPerWord,
                                      caller_adjustment * BytesPerWord,
                                      caller_was_method_handle ? 0 : callee_parameters,
                                      number_of_frames,
                                      frame_sizes,
                                      frame_pcs,
                                      return_type,
                                      exec_mode);
  // On some platforms, we need a way to pass some platform dependent
  // information to the unpacking code so the skeletal frames come out
  // correct (initial fp value, unextended sp, ...)
  info->set_initial_info((intptr_t) array->sender().initial_deoptimization_info());

  if (array->frames() > 1) {
    if (VerifyStack && TraceDeoptimization) {
      ttyLocker ttyl;
      tty->print_cr("Deoptimizing method containing inlining");
    }
  }

  array->set_unroll_block(info);
  return info;
}

// Called to cleanup deoptimization data structures in normal case
// after unpacking to stack and when stack overflow error occurs
void Deoptimization::cleanup_deopt_info(JavaThread *thread,
                                        vframeArray *array) {

  // Get array if coming from exception
  if (array == NULL) {
    array = thread->vframe_array_head();
  }
  thread->set_vframe_array_head(NULL);

  // Free the previous UnrollBlock
  vframeArray* old_array = thread->vframe_array_last();
  thread->set_vframe_array_last(array);

  if (old_array != NULL) {
    UnrollBlock* old_info = old_array->unroll_block();
    old_array->set_unroll_block(NULL);
    delete old_info;
    delete old_array;
  }

  // Deallocate any resource creating in this routine and any ResourceObjs allocated
  // inside the vframeArray (StackValueCollections)

  delete thread->deopt_mark();
  thread->set_deopt_mark(NULL);
  thread->set_deopt_compiled_method(NULL);


  if (JvmtiExport::can_pop_frame()) {
    // Regardless of whether we entered this routine with the pending
    // popframe condition bit set, we should always clear it now
    thread->clear_popframe_condition();
  }

  // unpack_frames() is called at the end of the deoptimization handler
  // and (in C2) at the end of the uncommon trap handler. Note this fact
  // so that an asynchronous stack walker can work again. This counter is
  // incremented at the beginning of fetch_unroll_info() and (in C2) at
  // the beginning of uncommon_trap().
  thread->dec_in_deopt_handler();
}

// Moved from cpu directories because none of the cpus has callee save values.
// If a cpu implements callee save values, move this to deoptimization_<cpu>.cpp.
void Deoptimization::unwind_callee_save_values(frame* f, vframeArray* vframe_array) {

  // This code is sort of the equivalent of C2IAdapter::setup_stack_frame back in
  // the days we had adapter frames. When we deoptimize a situation where a
  // compiled caller calls a compiled caller will have registers it expects
  // to survive the call to the callee. If we deoptimize the callee the only
  // way we can restore these registers is to have the oldest interpreter
  // frame that we create restore these values. That is what this routine
  // will accomplish.

  // At the moment we have modified c2 to not have any callee save registers
  // so this problem does not exist and this routine is just a place holder.

  assert(f->is_interpreted_frame(), "must be interpreted");
}

// Return BasicType of value being returned
JRT_LEAF(BasicType, Deoptimization::unpack_frames(JavaThread* thread, int exec_mode))

  // We are already active in the special DeoptResourceMark any ResourceObj's we
  // allocate will be freed at the end of the routine.

  // JRT_LEAF methods don't normally allocate handles and there is a
  // NoHandleMark to enforce that. It is actually safe to use Handles
  // in a JRT_LEAF method, and sometimes desirable, but to do so we
  // must use ResetNoHandleMark to bypass the NoHandleMark, and
  // then use a HandleMark to ensure any Handles we do create are
  // cleaned up in this scope.
  ResetNoHandleMark rnhm;
  HandleMark hm(thread);

  frame stub_frame = thread->last_frame();

  // Since the frame to unpack is the top frame of this thread, the vframe_array_head
  // must point to the vframeArray for the unpack frame.
  vframeArray* array = thread->vframe_array_head();

#ifndef PRODUCT
  if (TraceDeoptimization) {
    ttyLocker ttyl;
    tty->print_cr("DEOPT UNPACKING thread " INTPTR_FORMAT " vframeArray " INTPTR_FORMAT " mode %d",
                  p2i(thread), p2i(array), exec_mode);
  }
#endif
  Events::log_deopt_message(thread, "DEOPT UNPACKING pc=" INTPTR_FORMAT " sp=" INTPTR_FORMAT " mode %d",
              p2i(stub_frame.pc()), p2i(stub_frame.sp()), exec_mode);

  UnrollBlock* info = array->unroll_block();

  // We set the last_Java frame. But the stack isn't really parsable here. So we
  // clear it to make sure JFR understands not to try and walk stacks from events
  // in here.
  intptr_t* sp = thread->frame_anchor()->last_Java_sp();
  thread->frame_anchor()->set_last_Java_sp(NULL);

  // Unpack the interpreter frames and any adapter frame (c2 only) we might create.
  array->unpack_to_stack(stub_frame, exec_mode, info->caller_actual_parameters());

  thread->frame_anchor()->set_last_Java_sp(sp);

  BasicType bt = info->return_type();

  // If we have an exception pending, claim that the return type is an oop
  // so the deopt_blob does not overwrite the exception_oop.

  if (exec_mode == Unpack_exception)
    bt = T_OBJECT;

  // Cleanup thread deopt data
  cleanup_deopt_info(thread, array);

#ifndef PRODUCT
  if (VerifyStack) {
    ResourceMark res_mark;
    // Clear pending exception to not break verification code (restored afterwards)
    PreserveExceptionMark pm(thread);

    thread->validate_frame_layout();

    // Verify that the just-unpacked frames match the interpreter's
    // notions of expression stack and locals
    vframeArray* cur_array = thread->vframe_array_last();
    RegisterMap rm(thread, false);
    rm.set_include_argument_oops(false);
    bool is_top_frame = true;
    int callee_size_of_parameters = 0;
    int callee_max_locals = 0;
    for (int i = 0; i < cur_array->frames(); i++) {
      vframeArrayElement* el = cur_array->element(i);
      frame* iframe = el->iframe();
      guarantee(iframe->is_interpreted_frame(), "Wrong frame type");

      // Get the oop map for this bci
      InterpreterOopMap mask;
      int cur_invoke_parameter_size = 0;
      bool try_next_mask = false;
      int next_mask_expression_stack_size = -1;
      int top_frame_expression_stack_adjustment = 0;
      methodHandle mh(thread, iframe->interpreter_frame_method());
      OopMapCache::compute_one_oop_map(mh, iframe->interpreter_frame_bci(), &mask);
      BytecodeStream str(mh, iframe->interpreter_frame_bci());
      int max_bci = mh->code_size();
      // Get to the next bytecode if possible
      assert(str.bci() < max_bci, "bci in interpreter frame out of bounds");
      // Check to see if we can grab the number of outgoing arguments
      // at an uncommon trap for an invoke (where the compiler
      // generates debug info before the invoke has executed)
      Bytecodes::Code cur_code = str.next();
      if (Bytecodes::is_invoke(cur_code)) {
        Bytecode_invoke invoke(mh, iframe->interpreter_frame_bci());
        cur_invoke_parameter_size = invoke.size_of_parameters();
        if (i != 0 && !invoke.is_invokedynamic() && MethodHandles::has_member_arg(invoke.klass(), invoke.name())) {
          callee_size_of_parameters++;
        }
      }
      if (str.bci() < max_bci) {
        Bytecodes::Code next_code = str.next();
        if (next_code >= 0) {
          // The interpreter oop map generator reports results before
          // the current bytecode has executed except in the case of
          // calls. It seems to be hard to tell whether the compiler
          // has emitted debug information matching the "state before"
          // a given bytecode or the state after, so we try both
          if (!Bytecodes::is_invoke(cur_code) && cur_code != Bytecodes::_athrow) {
            // Get expression stack size for the next bytecode
            InterpreterOopMap next_mask;
            OopMapCache::compute_one_oop_map(mh, str.bci(), &next_mask);
            next_mask_expression_stack_size = next_mask.expression_stack_size();
            if (Bytecodes::is_invoke(next_code)) {
              Bytecode_invoke invoke(mh, str.bci());
              next_mask_expression_stack_size += invoke.size_of_parameters();
            }
            // Need to subtract off the size of the result type of
            // the bytecode because this is not described in the
            // debug info but returned to the interpreter in the TOS
            // caching register
            BasicType bytecode_result_type = Bytecodes::result_type(cur_code);
            if (bytecode_result_type != T_ILLEGAL) {
              top_frame_expression_stack_adjustment = type2size[bytecode_result_type];
            }
            assert(top_frame_expression_stack_adjustment >= 0, "stack adjustment must be positive");
            try_next_mask = true;
          }
        }
      }

      // Verify stack depth and oops in frame
      // This assertion may be dependent on the platform we're running on and may need modification (tested on x86 and sparc)
      if (!(
            /* SPARC */
            (iframe->interpreter_frame_expression_stack_size() == mask.expression_stack_size() + callee_size_of_parameters) ||
            /* x86 */
            (iframe->interpreter_frame_expression_stack_size() == mask.expression_stack_size() + callee_max_locals) ||
            (try_next_mask &&
             (iframe->interpreter_frame_expression_stack_size() == (next_mask_expression_stack_size -
                                                                    top_frame_expression_stack_adjustment))) ||
            (is_top_frame && (exec_mode == Unpack_exception) && iframe->interpreter_frame_expression_stack_size() == 0) ||
            (is_top_frame && (exec_mode == Unpack_uncommon_trap || exec_mode == Unpack_reexecute || el->should_reexecute()) &&
             (iframe->interpreter_frame_expression_stack_size() == mask.expression_stack_size() + cur_invoke_parameter_size))
            )) {
        {
          ttyLocker ttyl;

          // Print out some information that will help us debug the problem
          tty->print_cr("Wrong number of expression stack elements during deoptimization");
          tty->print_cr("  Error occurred while verifying frame %d (0..%d, 0 is topmost)", i, cur_array->frames() - 1);
          tty->print_cr("  Fabricated interpreter frame had %d expression stack elements",
                        iframe->interpreter_frame_expression_stack_size());
          tty->print_cr("  Interpreter oop map had %d expression stack elements", mask.expression_stack_size());
          tty->print_cr("  try_next_mask = %d", try_next_mask);
          tty->print_cr("  next_mask_expression_stack_size = %d", next_mask_expression_stack_size);
          tty->print_cr("  callee_size_of_parameters = %d", callee_size_of_parameters);
          tty->print_cr("  callee_max_locals = %d", callee_max_locals);
          tty->print_cr("  top_frame_expression_stack_adjustment = %d", top_frame_expression_stack_adjustment);
          tty->print_cr("  exec_mode = %d", exec_mode);
          tty->print_cr("  cur_invoke_parameter_size = %d", cur_invoke_parameter_size);
          tty->print_cr("  Thread = " INTPTR_FORMAT ", thread ID = %d", p2i(thread), thread->osthread()->thread_id());
          tty->print_cr("  Interpreted frames:");
          for (int k = 0; k < cur_array->frames(); k++) {
            vframeArrayElement* el = cur_array->element(k);
            tty->print_cr("    %s (bci %d)", el->method()->name_and_sig_as_C_string(), el->bci());
          }
          cur_array->print_on_2(tty);
        } // release tty lock before calling guarantee
        guarantee(false, "wrong number of expression stack elements during deopt");
      }
      VerifyOopClosure verify;
      iframe->oops_interpreted_do(&verify, &rm, false);
      callee_size_of_parameters = mh->size_of_parameters();
      callee_max_locals = mh->max_locals();
      is_top_frame = false;
    }
  }
#endif /* !PRODUCT */

  return bt;
JRT_END

class DeoptimizeMarkedClosure : public HandshakeClosure {
 public:
  DeoptimizeMarkedClosure() : HandshakeClosure("Deoptimize") {}
  void do_thread(Thread* thread) {
    JavaThread* jt = JavaThread::cast(thread);
    jt->deoptimize_marked_methods();
  }
};

void Deoptimization::deoptimize_all_marked(nmethod* nmethod_only) {
  ResourceMark rm;
  DeoptimizationMarker dm;

  // Make the dependent methods not entrant
  if (nmethod_only != NULL) {
    nmethod_only->mark_for_deoptimization();
    nmethod_only->make_not_entrant();
  } else {
    MutexLocker mu(SafepointSynchronize::is_at_safepoint() ? NULL : CodeCache_lock, Mutex::_no_safepoint_check_flag);
    CodeCache::make_marked_nmethods_not_entrant();
  }

  DeoptimizeMarkedClosure deopt;
  if (SafepointSynchronize::is_at_safepoint()) {
    Threads::java_threads_do(&deopt);
  } else {
    Handshake::execute(&deopt);
  }
}

Deoptimization::DeoptAction Deoptimization::_unloaded_action
  = Deoptimization::Action_reinterpret;

#if COMPILER2_OR_JVMCI
template<typename CacheType>
class BoxCacheBase : public CHeapObj<mtCompiler> {
protected:
  static InstanceKlass* find_cache_klass(Symbol* klass_name) {
    ResourceMark rm;
    char* klass_name_str = klass_name->as_C_string();
    InstanceKlass* ik = SystemDictionary::find_instance_klass(klass_name, Handle(), Handle());
    guarantee(ik != NULL, "%s must be loaded", klass_name_str);
    guarantee(ik->is_initialized(), "%s must be initialized", klass_name_str);
    CacheType::compute_offsets(ik);
    return ik;
  }
};

template<typename PrimitiveType, typename CacheType, typename BoxType> class BoxCache  : public BoxCacheBase<CacheType> {
  PrimitiveType _low;
  PrimitiveType _high;
  jobject _cache;
protected:
  static BoxCache<PrimitiveType, CacheType, BoxType> *_singleton;
  BoxCache(Thread* thread) {
    InstanceKlass* ik = BoxCacheBase<CacheType>::find_cache_klass(CacheType::symbol());
    objArrayOop cache = CacheType::cache(ik);
    assert(cache->length() > 0, "Empty cache");
    _low = BoxType::value(cache->obj_at(0));
    _high = _low + cache->length() - 1;
    _cache = JNIHandles::make_global(Handle(thread, cache));
  }
  ~BoxCache() {
    JNIHandles::destroy_global(_cache);
  }
public:
  static BoxCache<PrimitiveType, CacheType, BoxType>* singleton(Thread* thread) {
    if (_singleton == NULL) {
      BoxCache<PrimitiveType, CacheType, BoxType>* s = new BoxCache<PrimitiveType, CacheType, BoxType>(thread);
      if (!Atomic::replace_if_null(&_singleton, s)) {
        delete s;
      }
    }
    return _singleton;
  }
  oop lookup(PrimitiveType value) {
    if (_low <= value && value <= _high) {
      int offset = value - _low;
      return objArrayOop(JNIHandles::resolve_non_null(_cache))->obj_at(offset);
    }
    return NULL;
  }
  oop lookup_raw(intptr_t raw_value) {
    // Have to cast to avoid little/big-endian problems.
    if (sizeof(PrimitiveType) > sizeof(jint)) {
      jlong value = (jlong)raw_value;
      return lookup(value);
    }
    PrimitiveType value = (PrimitiveType)*((jint*)&raw_value);
    return lookup(value);
  }
};

typedef BoxCache<jint, java_lang_Integer_IntegerCache, java_lang_Integer> IntegerBoxCache;
typedef BoxCache<jlong, java_lang_Long_LongCache, java_lang_Long> LongBoxCache;
typedef BoxCache<jchar, java_lang_Character_CharacterCache, java_lang_Character> CharacterBoxCache;
typedef BoxCache<jshort, java_lang_Short_ShortCache, java_lang_Short> ShortBoxCache;
typedef BoxCache<jbyte, java_lang_Byte_ByteCache, java_lang_Byte> ByteBoxCache;

template<> BoxCache<jint, java_lang_Integer_IntegerCache, java_lang_Integer>* BoxCache<jint, java_lang_Integer_IntegerCache, java_lang_Integer>::_singleton = NULL;
template<> BoxCache<jlong, java_lang_Long_LongCache, java_lang_Long>* BoxCache<jlong, java_lang_Long_LongCache, java_lang_Long>::_singleton = NULL;
template<> BoxCache<jchar, java_lang_Character_CharacterCache, java_lang_Character>* BoxCache<jchar, java_lang_Character_CharacterCache, java_lang_Character>::_singleton = NULL;
template<> BoxCache<jshort, java_lang_Short_ShortCache, java_lang_Short>* BoxCache<jshort, java_lang_Short_ShortCache, java_lang_Short>::_singleton = NULL;
template<> BoxCache<jbyte, java_lang_Byte_ByteCache, java_lang_Byte>* BoxCache<jbyte, java_lang_Byte_ByteCache, java_lang_Byte>::_singleton = NULL;

class BooleanBoxCache : public BoxCacheBase<java_lang_Boolean> {
  jobject _true_cache;
  jobject _false_cache;
protected:
  static BooleanBoxCache *_singleton;
  BooleanBoxCache(Thread *thread) {
    InstanceKlass* ik = find_cache_klass(java_lang_Boolean::symbol());
    _true_cache = JNIHandles::make_global(Handle(thread, java_lang_Boolean::get_TRUE(ik)));
    _false_cache = JNIHandles::make_global(Handle(thread, java_lang_Boolean::get_FALSE(ik)));
  }
  ~BooleanBoxCache() {
    JNIHandles::destroy_global(_true_cache);
    JNIHandles::destroy_global(_false_cache);
  }
public:
  static BooleanBoxCache* singleton(Thread* thread) {
    if (_singleton == NULL) {
      BooleanBoxCache* s = new BooleanBoxCache(thread);
      if (!Atomic::replace_if_null(&_singleton, s)) {
        delete s;
      }
    }
    return _singleton;
  }
  oop lookup_raw(intptr_t raw_value) {
    // Have to cast to avoid little/big-endian problems.
    jboolean value = (jboolean)*((jint*)&raw_value);
    return lookup(value);
  }
  oop lookup(jboolean value) {
    if (value != 0) {
      return JNIHandles::resolve_non_null(_true_cache);
    }
    return JNIHandles::resolve_non_null(_false_cache);
  }
};

BooleanBoxCache* BooleanBoxCache::_singleton = NULL;

oop Deoptimization::get_cached_box(AutoBoxObjectValue* bv, frame* fr, RegisterMap* reg_map, TRAPS) {
   Klass* k = java_lang_Class::as_Klass(bv->klass()->as_ConstantOopReadValue()->value()());
   BasicType box_type = vmClasses::box_klass_type(k);
   if (box_type != T_OBJECT) {
     StackValue* value = StackValue::create_stack_value(fr, reg_map, bv->field_at(box_type == T_LONG ? 1 : 0));
     switch(box_type) {
       case T_INT:     return IntegerBoxCache::singleton(THREAD)->lookup_raw(value->get_int());
       case T_CHAR:    return CharacterBoxCache::singleton(THREAD)->lookup_raw(value->get_int());
       case T_SHORT:   return ShortBoxCache::singleton(THREAD)->lookup_raw(value->get_int());
       case T_BYTE:    return ByteBoxCache::singleton(THREAD)->lookup_raw(value->get_int());
       case T_BOOLEAN: return BooleanBoxCache::singleton(THREAD)->lookup_raw(value->get_int());
       case T_LONG:    return LongBoxCache::singleton(THREAD)->lookup_raw(value->get_int());
       default:;
     }
   }
   return NULL;
}

bool Deoptimization::realloc_objects(JavaThread* thread, frame* fr, RegisterMap* reg_map, GrowableArray<ScopeValue*>* objects, TRAPS) {
  Handle pending_exception(THREAD, thread->pending_exception());
  const char* exception_file = thread->exception_file();
  int exception_line = thread->exception_line();
  thread->clear_pending_exception();

  bool failures = false;

  for (int i = 0; i < objects->length(); i++) {
    assert(objects->at(i)->is_object(), "invalid debug information");
    ObjectValue* sv = (ObjectValue*) objects->at(i);

    Klass* k = java_lang_Class::as_Klass(sv->klass()->as_ConstantOopReadValue()->value()());
    oop obj = NULL;

    if (k->is_instance_klass()) {
      if (sv->is_auto_box()) {
        AutoBoxObjectValue* abv = (AutoBoxObjectValue*) sv;
        obj = get_cached_box(abv, fr, reg_map, THREAD);
        if (obj != NULL) {
          // Set the flag to indicate the box came from a cache, so that we can skip the field reassignment for it.
          abv->set_cached(true);
        }
      }

      InstanceKlass* ik = InstanceKlass::cast(k);
      if (obj == NULL) {
#ifdef COMPILER2
        if (EnableVectorSupport && VectorSupport::is_vector(ik)) {
          obj = VectorSupport::allocate_vector(ik, fr, reg_map, sv, THREAD);
        } else {
          obj = ik->allocate_instance(THREAD);
        }
#else
        obj = ik->allocate_instance(THREAD);
#endif // COMPILER2
      }
    } else if (k->is_typeArray_klass()) {
      TypeArrayKlass* ak = TypeArrayKlass::cast(k);
      assert(sv->field_size() % type2size[ak->element_type()] == 0, "non-integral array length");
      int len = sv->field_size() / type2size[ak->element_type()];
      obj = ak->allocate(len, THREAD);
    } else if (k->is_objArray_klass()) {
      ObjArrayKlass* ak = ObjArrayKlass::cast(k);
      obj = ak->allocate(sv->field_size(), THREAD);
    }

    if (obj == NULL) {
      failures = true;
    }

    assert(sv->value().is_null(), "redundant reallocation");
    assert(obj != NULL || HAS_PENDING_EXCEPTION, "allocation should succeed or we should get an exception");
    CLEAR_PENDING_EXCEPTION;
    sv->set_value(obj);
  }

  if (failures) {
    THROW_OOP_(Universe::out_of_memory_error_realloc_objects(), failures);
  } else if (pending_exception.not_null()) {
    thread->set_pending_exception(pending_exception(), exception_file, exception_line);
  }

  return failures;
}

#if INCLUDE_JVMCI
/**
 * For primitive types whose kind gets "erased" at runtime (shorts become stack ints),
 * we need to somehow be able to recover the actual kind to be able to write the correct
 * amount of bytes.
 * For that purpose, this method assumes that, for an entry spanning n bytes at index i,
 * the entries at index n + 1 to n + i are 'markers'.
 * For example, if we were writing a short at index 4 of a byte array of size 8, the
 * expected form of the array would be:
 *
 * {b0, b1, b2, b3, INT, marker, b6, b7}
 *
 * Thus, in order to get back the size of the entry, we simply need to count the number
 * of marked entries
 *
 * @param virtualArray the virtualized byte array
 * @param i index of the virtual entry we are recovering
 * @return The number of bytes the entry spans
 */
static int count_number_of_bytes_for_entry(ObjectValue *virtualArray, int i) {
  int index = i;
  while (++index < virtualArray->field_size() &&
           virtualArray->field_at(index)->is_marker()) {}
  return index - i;
}

/**
 * If there was a guarantee for byte array to always start aligned to a long, we could
 * do a simple check on the parity of the index. Unfortunately, that is not always the
 * case. Thus, we check alignment of the actual address we are writing to.
 * In the unlikely case index 0 is 5-aligned for example, it would then be possible to
 * write a long to index 3.
 */
static jbyte* check_alignment_get_addr(typeArrayOop obj, int index, int expected_alignment) {
    jbyte* res = obj->byte_at_addr(index);
    assert((((intptr_t) res) % expected_alignment) == 0, "Non-aligned write");
    return res;
}

static void byte_array_put(typeArrayOop obj, intptr_t val, int index, int byte_count) {
  switch (byte_count) {
    case 1:
      obj->byte_at_put(index, (jbyte) *((jint *) &val));
      break;
    case 2:
      *((jshort *) check_alignment_get_addr(obj, index, 2)) = (jshort) *((jint *) &val);
      break;
    case 4:
      *((jint *) check_alignment_get_addr(obj, index, 4)) = (jint) *((jint *) &val);
      break;
    case 8:
      *((jlong *) check_alignment_get_addr(obj, index, 8)) = (jlong) *((jlong *) &val);
      break;
    default:
      ShouldNotReachHere();
  }
}
#endif // INCLUDE_JVMCI


// restore elements of an eliminated type array
void Deoptimization::reassign_type_array_elements(frame* fr, RegisterMap* reg_map, ObjectValue* sv, typeArrayOop obj, BasicType type) {
  int index = 0;
  intptr_t val;

  for (int i = 0; i < sv->field_size(); i++) {
    StackValue* value = StackValue::create_stack_value(fr, reg_map, sv->field_at(i));
    switch(type) {
    case T_LONG: case T_DOUBLE: {
      assert(value->type() == T_INT, "Agreement.");
      StackValue* low =
        StackValue::create_stack_value(fr, reg_map, sv->field_at(++i));
#ifdef _LP64
      jlong res = (jlong)low->get_int();
#else
      jlong res = jlong_from((jint)value->get_int(), (jint)low->get_int());
#endif
      obj->long_at_put(index, res);
      break;
    }

    // Have to cast to INT (32 bits) pointer to avoid little/big-endian problem.
    case T_INT: case T_FLOAT: { // 4 bytes.
      assert(value->type() == T_INT, "Agreement.");
      bool big_value = false;
      if (i + 1 < sv->field_size() && type == T_INT) {
        if (sv->field_at(i)->is_location()) {
          Location::Type type = ((LocationValue*) sv->field_at(i))->location().type();
          if (type == Location::dbl || type == Location::lng) {
            big_value = true;
          }
        } else if (sv->field_at(i)->is_constant_int()) {
          ScopeValue* next_scope_field = sv->field_at(i + 1);
          if (next_scope_field->is_constant_long() || next_scope_field->is_constant_double()) {
            big_value = true;
          }
        }
      }

      if (big_value) {
        StackValue* low = StackValue::create_stack_value(fr, reg_map, sv->field_at(++i));
  #ifdef _LP64
        jlong res = (jlong)low->get_int();
  #else
        jlong res = jlong_from((jint)value->get_int(), (jint)low->get_int());
  #endif
        obj->int_at_put(index, (jint)*((jint*)&res));
        obj->int_at_put(++index, (jint)*(((jint*)&res) + 1));
      } else {
        val = value->get_int();
        obj->int_at_put(index, (jint)*((jint*)&val));
      }
      break;
    }

    case T_SHORT:
      assert(value->type() == T_INT, "Agreement.");
      val = value->get_int();
      obj->short_at_put(index, (jshort)*((jint*)&val));
      break;

    case T_CHAR:
      assert(value->type() == T_INT, "Agreement.");
      val = value->get_int();
      obj->char_at_put(index, (jchar)*((jint*)&val));
      break;

    case T_BYTE: {
      assert(value->type() == T_INT, "Agreement.");
      // The value we get is erased as a regular int. We will need to find its actual byte count 'by hand'.
      val = value->get_int();
#if INCLUDE_JVMCI
      int byte_count = count_number_of_bytes_for_entry(sv, i);
      byte_array_put(obj, val, index, byte_count);
      // According to byte_count contract, the values from i + 1 to i + byte_count are illegal values. Skip.
      i += byte_count - 1; // Balance the loop counter.
      index += byte_count;
      // index has been updated so continue at top of loop
      continue;
#else
      obj->byte_at_put(index, (jbyte)*((jint*)&val));
      break;
#endif // INCLUDE_JVMCI
    }

    case T_BOOLEAN: {
      assert(value->type() == T_INT, "Agreement.");
      val = value->get_int();
      obj->bool_at_put(index, (jboolean)*((jint*)&val));
      break;
    }

      default:
        ShouldNotReachHere();
    }
    index++;
  }
}

// restore fields of an eliminated object array
void Deoptimization::reassign_object_array_elements(frame* fr, RegisterMap* reg_map, ObjectValue* sv, objArrayOop obj) {
  for (int i = 0; i < sv->field_size(); i++) {
    StackValue* value = StackValue::create_stack_value(fr, reg_map, sv->field_at(i));
    assert(value->type() == T_OBJECT, "object element expected");
    obj->obj_at_put(i, value->get_obj()());
  }
}

class ReassignedField {
public:
  int _offset;
  BasicType _type;
public:
  ReassignedField() {
    _offset = 0;
    _type = T_ILLEGAL;
  }
};

int compare(ReassignedField* left, ReassignedField* right) {
  return left->_offset - right->_offset;
}

// Restore fields of an eliminated instance object using the same field order
// returned by HotSpotResolvedObjectTypeImpl.getInstanceFields(true)
static int reassign_fields_by_klass(InstanceKlass* klass, frame* fr, RegisterMap* reg_map, ObjectValue* sv, int svIndex, oop obj, bool skip_internal) {
  GrowableArray<ReassignedField>* fields = new GrowableArray<ReassignedField>();
  InstanceKlass* ik = klass;
  while (ik != NULL) {
    for (AllFieldStream fs(ik); !fs.done(); fs.next()) {
      if (!fs.access_flags().is_static() && (!skip_internal || !fs.access_flags().is_internal())) {
        ReassignedField field;
        field._offset = fs.offset();
        field._type = Signature::basic_type(fs.signature());
        fields->append(field);
      }
    }
    ik = ik->superklass();
  }
  fields->sort(compare);
  for (int i = 0; i < fields->length(); i++) {
    intptr_t val;
    ScopeValue* scope_field = sv->field_at(svIndex);
    StackValue* value = StackValue::create_stack_value(fr, reg_map, scope_field);
    int offset = fields->at(i)._offset;
    BasicType type = fields->at(i)._type;
    switch (type) {
      case T_OBJECT: case T_ARRAY:
        assert(value->type() == T_OBJECT, "Agreement.");
        obj->obj_field_put(offset, value->get_obj()());
        break;

      // Have to cast to INT (32 bits) pointer to avoid little/big-endian problem.
      case T_INT: case T_FLOAT: { // 4 bytes.
        assert(value->type() == T_INT, "Agreement.");
        bool big_value = false;
        if (i+1 < fields->length() && fields->at(i+1)._type == T_INT) {
          if (scope_field->is_location()) {
            Location::Type type = ((LocationValue*) scope_field)->location().type();
            if (type == Location::dbl || type == Location::lng) {
              big_value = true;
            }
          }
          if (scope_field->is_constant_int()) {
            ScopeValue* next_scope_field = sv->field_at(svIndex + 1);
            if (next_scope_field->is_constant_long() || next_scope_field->is_constant_double()) {
              big_value = true;
            }
          }
        }

        if (big_value) {
          i++;
          assert(i < fields->length(), "second T_INT field needed");
          assert(fields->at(i)._type == T_INT, "T_INT field needed");
        } else {
          val = value->get_int();
          obj->int_field_put(offset, (jint)*((jint*)&val));
          break;
        }
      }
        /* no break */

      case T_LONG: case T_DOUBLE: {
        assert(value->type() == T_INT, "Agreement.");
        StackValue* low = StackValue::create_stack_value(fr, reg_map, sv->field_at(++svIndex));
#ifdef _LP64
        jlong res = (jlong)low->get_int();
#else
        jlong res = jlong_from((jint)value->get_int(), (jint)low->get_int());
#endif
        obj->long_field_put(offset, res);
        break;
      }

      case T_SHORT:
        assert(value->type() == T_INT, "Agreement.");
        val = value->get_int();
        obj->short_field_put(offset, (jshort)*((jint*)&val));
        break;

      case T_CHAR:
        assert(value->type() == T_INT, "Agreement.");
        val = value->get_int();
        obj->char_field_put(offset, (jchar)*((jint*)&val));
        break;

      case T_BYTE:
        assert(value->type() == T_INT, "Agreement.");
        val = value->get_int();
        obj->byte_field_put(offset, (jbyte)*((jint*)&val));
        break;

      case T_BOOLEAN:
        assert(value->type() == T_INT, "Agreement.");
        val = value->get_int();
        obj->bool_field_put(offset, (jboolean)*((jint*)&val));
        break;

      default:
        ShouldNotReachHere();
    }
    svIndex++;
  }
  return svIndex;
}

// restore fields of all eliminated objects and arrays
void Deoptimization::reassign_fields(frame* fr, RegisterMap* reg_map, GrowableArray<ScopeValue*>* objects, bool realloc_failures, bool skip_internal) {
  for (int i = 0; i < objects->length(); i++) {
    ObjectValue* sv = (ObjectValue*) objects->at(i);
    Klass* k = java_lang_Class::as_Klass(sv->klass()->as_ConstantOopReadValue()->value()());
    Handle obj = sv->value();
    assert(obj.not_null() || realloc_failures, "reallocation was missed");
    if (PrintDeoptimizationDetails) {
      tty->print_cr("reassign fields for object of type %s!", k->name()->as_C_string());
    }
    if (obj.is_null()) {
      continue;
    }

    // Don't reassign fields of boxes that came from a cache. Caches may be in CDS.
    if (sv->is_auto_box() && ((AutoBoxObjectValue*) sv)->is_cached()) {
      continue;
    }
#ifdef COMPILER2
    if (EnableVectorSupport && VectorSupport::is_vector(k)) {
      assert(sv->field_size() == 1, "%s not a vector", k->name()->as_C_string());
      ScopeValue* payload = sv->field_at(0);
      if (payload->is_location() &&
          payload->as_LocationValue()->location().type() == Location::vector) {
        if (PrintDeoptimizationDetails) {
          tty->print_cr("skip field reassignment for this vector - it should be assigned already");
          if (Verbose) {
            Handle obj = sv->value();
            k->oop_print_on(obj(), tty);
          }
        }
        continue; // Such vector's value was already restored in VectorSupport::allocate_vector().
      }
      // Else fall-through to do assignment for scalar-replaced boxed vector representation
      // which could be restored after vector object allocation.
    }
#endif
    if (k->is_instance_klass()) {
      InstanceKlass* ik = InstanceKlass::cast(k);
      reassign_fields_by_klass(ik, fr, reg_map, sv, 0, obj(), skip_internal);
    } else if (k->is_typeArray_klass()) {
      TypeArrayKlass* ak = TypeArrayKlass::cast(k);
      reassign_type_array_elements(fr, reg_map, sv, (typeArrayOop) obj(), ak->element_type());
    } else if (k->is_objArray_klass()) {
      reassign_object_array_elements(fr, reg_map, sv, (objArrayOop) obj());
    }
  }
}


// relock objects for which synchronization was eliminated
bool Deoptimization::relock_objects(JavaThread* thread, GrowableArray<MonitorInfo*>* monitors,
                                    JavaThread* deoptee_thread, frame& fr, int exec_mode, bool realloc_failures) {
  bool relocked_objects = false;
  for (int i = 0; i < monitors->length(); i++) {
    MonitorInfo* mon_info = monitors->at(i);
    if (mon_info->eliminated()) {
      assert(!mon_info->owner_is_scalar_replaced() || realloc_failures, "reallocation was missed");
      relocked_objects = true;
      if (!mon_info->owner_is_scalar_replaced()) {
        Handle obj(thread, mon_info->owner());
        markWord mark = obj->mark();
        if (exec_mode == Unpack_none) {
          if (mark.has_locker() && fr.sp() > (intptr_t*)mark.locker()) {
            // With exec_mode == Unpack_none obj may be thread local and locked in
            // a callee frame. Make the lock in the callee a recursive lock and restore the displaced header.
            markWord dmw = mark.displaced_mark_helper();
            mark.locker()->set_displaced_header(markWord::encode((BasicLock*) NULL));
            obj->set_mark(dmw);
          }
          if (mark.has_monitor()) {
            // defer relocking if the deoptee thread is currently waiting for obj
            ObjectMonitor* waiting_monitor = deoptee_thread->current_waiting_monitor();
            if (waiting_monitor != NULL && waiting_monitor->object() == obj()) {
              assert(fr.is_deoptimized_frame(), "frame must be scheduled for deoptimization");
              mon_info->lock()->set_displaced_header(markWord::unused_mark());
              JvmtiDeferredUpdates::inc_relock_count_after_wait(deoptee_thread);
              continue;
            }
          }
        }
        BasicLock* lock = mon_info->lock();
        ObjectSynchronizer::enter(obj, lock, deoptee_thread);
        assert(mon_info->owner()->is_locked(), "object must be locked now");
      }
    }
  }
  return relocked_objects;
}


#ifndef PRODUCT
// print information about reallocated objects
void Deoptimization::print_objects(GrowableArray<ScopeValue*>* objects, bool realloc_failures) {
  fieldDescriptor fd;

  for (int i = 0; i < objects->length(); i++) {
    ObjectValue* sv = (ObjectValue*) objects->at(i);
    Klass* k = java_lang_Class::as_Klass(sv->klass()->as_ConstantOopReadValue()->value()());
    Handle obj = sv->value();

    tty->print("     object <" INTPTR_FORMAT "> of type ", p2i(sv->value()()));
    k->print_value();
    assert(obj.not_null() || realloc_failures, "reallocation was missed");
    if (obj.is_null()) {
      tty->print(" allocation failed");
    } else {
      tty->print(" allocated (%d bytes)", obj->size() * HeapWordSize);
    }
    tty->cr();

    if (Verbose && !obj.is_null()) {
      k->oop_print_on(obj(), tty);
    }
  }
}
#endif
#endif // COMPILER2_OR_JVMCI

vframeArray* Deoptimization::create_vframeArray(JavaThread* thread, frame fr, RegisterMap *reg_map, GrowableArray<compiledVFrame*>* chunk, bool realloc_failures) {
  Events::log_deopt_message(thread, "DEOPT PACKING pc=" INTPTR_FORMAT " sp=" INTPTR_FORMAT, p2i(fr.pc()), p2i(fr.sp()));

#ifndef PRODUCT
  if (PrintDeoptimizationDetails) {
    ttyLocker ttyl;
    tty->print("DEOPT PACKING thread " INTPTR_FORMAT " ", p2i(thread));
    fr.print_on(tty);
    tty->print_cr("     Virtual frames (innermost first):");
    for (int index = 0; index < chunk->length(); index++) {
      compiledVFrame* vf = chunk->at(index);
      tty->print("       %2d - ", index);
      vf->print_value();
      int bci = chunk->at(index)->raw_bci();
      const char* code_name;
      if (bci == SynchronizationEntryBCI) {
        code_name = "sync entry";
      } else {
        Bytecodes::Code code = vf->method()->code_at(bci);
        code_name = Bytecodes::name(code);
      }
      tty->print(" - %s", code_name);
      tty->print_cr(" @ bci %d ", bci);
      if (Verbose) {
        vf->print();
        tty->cr();
      }
    }
  }
#endif

  // Register map for next frame (used for stack crawl).  We capture
  // the state of the deopt'ing frame's caller.  Thus if we need to
  // stuff a C2I adapter we can properly fill in the callee-save
  // register locations.
  frame caller = fr.sender(reg_map);
  int frame_size = caller.sp() - fr.sp();

  frame sender = caller;

  // Since the Java thread being deoptimized will eventually adjust it's own stack,
  // the vframeArray containing the unpacking information is allocated in the C heap.
  // For Compiler1, the caller of the deoptimized frame is saved for use by unpack_frames().
  vframeArray* array = vframeArray::allocate(thread, frame_size, chunk, reg_map, sender, caller, fr, realloc_failures);

  // Compare the vframeArray to the collected vframes
  assert(array->structural_compare(thread, chunk), "just checking");

#ifndef PRODUCT
  if (PrintDeoptimizationDetails) {
    ttyLocker ttyl;
    tty->print_cr("     Created vframeArray " INTPTR_FORMAT, p2i(array));
  }
#endif // PRODUCT

  return array;
}

#if COMPILER2_OR_JVMCI
void Deoptimization::pop_frames_failed_reallocs(JavaThread* thread, vframeArray* array) {
  // Reallocation of some scalar replaced objects failed. Record
  // that we need to pop all the interpreter frames for the
  // deoptimized compiled frame.
  assert(thread->frames_to_pop_failed_realloc() == 0, "missed frames to pop?");
  thread->set_frames_to_pop_failed_realloc(array->frames());
  // Unlock all monitors here otherwise the interpreter will see a
  // mix of locked and unlocked monitors (because of failed
  // reallocations of synchronized objects) and be confused.
  for (int i = 0; i < array->frames(); i++) {
    MonitorChunk* monitors = array->element(i)->monitors();
    if (monitors != NULL) {
      for (int j = 0; j < monitors->number_of_monitors(); j++) {
        BasicObjectLock* src = monitors->at(j);
        if (src->obj() != NULL) {
          ObjectSynchronizer::exit(src->obj(), src->lock(), thread);
        }
      }
      array->element(i)->free_monitors(thread);
#ifdef ASSERT
      array->element(i)->set_removed_monitors();
#endif
    }
  }
}
#endif

void Deoptimization::deoptimize_single_frame(JavaThread* thread, frame fr, Deoptimization::DeoptReason reason) {
  assert(fr.can_be_deoptimized(), "checking frame type");

  gather_statistics(reason, Action_none, Bytecodes::_illegal);

  if (LogCompilation && xtty != NULL) {
    CompiledMethod* cm = fr.cb()->as_compiled_method_or_null();
    assert(cm != NULL, "only compiled methods can deopt");

    ttyLocker ttyl;
    xtty->begin_head("deoptimized thread='" UINTX_FORMAT "' reason='%s' pc='" INTPTR_FORMAT "'",(uintx)thread->osthread()->thread_id(), trap_reason_name(reason), p2i(fr.pc()));
    cm->log_identity(xtty);
    xtty->end_head();
    for (ScopeDesc* sd = cm->scope_desc_at(fr.pc()); ; sd = sd->sender()) {
      xtty->begin_elem("jvms bci='%d'", sd->bci());
      xtty->method(sd->method());
      xtty->end_elem();
      if (sd->is_top())  break;
    }
    xtty->tail("deoptimized");
  }

  // Patch the compiled method so that when execution returns to it we will
  // deopt the execution state and return to the interpreter.
  fr.deoptimize(thread);
}

void Deoptimization::deoptimize(JavaThread* thread, frame fr, DeoptReason reason) {
  // Deoptimize only if the frame comes from compile code.
  // Do not deoptimize the frame which is already patched
  // during the execution of the loops below.
  if (!fr.is_compiled_frame() || fr.is_deoptimized_frame()) {
    return;
  }
  ResourceMark rm;
  DeoptimizationMarker dm;
  deoptimize_single_frame(thread, fr, reason);
}

#if INCLUDE_JVMCI
address Deoptimization::deoptimize_for_missing_exception_handler(CompiledMethod* cm) {
  // there is no exception handler for this pc => deoptimize
  cm->make_not_entrant();

  // Use Deoptimization::deoptimize for all of its side-effects:
  // gathering traps statistics, logging...
  // it also patches the return pc but we do not care about that
  // since we return a continuation to the deopt_blob below.
  JavaThread* thread = JavaThread::current();
  RegisterMap reg_map(thread, false);
  frame runtime_frame = thread->last_frame();
  frame caller_frame = runtime_frame.sender(&reg_map);
  assert(caller_frame.cb()->as_compiled_method_or_null() == cm, "expect top frame compiled method");
  vframe* vf = vframe::new_vframe(&caller_frame, &reg_map, thread);
  compiledVFrame* cvf = compiledVFrame::cast(vf);
  ScopeDesc* imm_scope = cvf->scope();
  MethodData* imm_mdo = get_method_data(thread, methodHandle(thread, imm_scope->method()), true);
  if (imm_mdo != NULL) {
    ProfileData* pdata = imm_mdo->allocate_bci_to_data(imm_scope->bci(), NULL);
    if (pdata != NULL && pdata->is_BitData()) {
      BitData* bit_data = (BitData*) pdata;
      bit_data->set_exception_seen();
    }
  }

  Deoptimization::deoptimize(thread, caller_frame, Deoptimization::Reason_not_compiled_exception_handler);

  MethodData* trap_mdo = get_method_data(thread, methodHandle(thread, cm->method()), true);
  if (trap_mdo != NULL) {
    trap_mdo->inc_trap_count(Deoptimization::Reason_not_compiled_exception_handler);
  }

  return SharedRuntime::deopt_blob()->unpack_with_exception_in_tls();
}
#endif

void Deoptimization::deoptimize_frame_internal(JavaThread* thread, intptr_t* id, DeoptReason reason) {
  assert(thread == Thread::current() ||
         thread->is_handshake_safe_for(Thread::current()) ||
         SafepointSynchronize::is_at_safepoint(),
         "can only deoptimize other thread at a safepoint/handshake");
  // Compute frame and register map based on thread and sp.
  RegisterMap reg_map(thread, false);
  frame fr = thread->last_frame();
  while (fr.id() != id) {
    fr = fr.sender(&reg_map);
  }
  deoptimize(thread, fr, reason);
}


void Deoptimization::deoptimize_frame(JavaThread* thread, intptr_t* id, DeoptReason reason) {
  Thread* current = Thread::current();
  if (thread == current || thread->is_handshake_safe_for(current)) {
    Deoptimization::deoptimize_frame_internal(thread, id, reason);
  } else {
    VM_DeoptimizeFrame deopt(thread, id, reason);
    VMThread::execute(&deopt);
  }
}

void Deoptimization::deoptimize_frame(JavaThread* thread, intptr_t* id) {
  deoptimize_frame(thread, id, Reason_constraint);
}

// JVMTI PopFrame support
JRT_LEAF(void, Deoptimization::popframe_preserve_args(JavaThread* thread, int bytes_to_save, void* start_address))
{
  thread->popframe_preserve_args(in_ByteSize(bytes_to_save), start_address);
}
JRT_END

MethodData*
Deoptimization::get_method_data(JavaThread* thread, const methodHandle& m,
                                bool create_if_missing) {
  JavaThread* THREAD = thread; // For exception macros.
  MethodData* mdo = m()->method_data();
  if (mdo == NULL && create_if_missing && !HAS_PENDING_EXCEPTION) {
    // Build an MDO.  Ignore errors like OutOfMemory;
    // that simply means we won't have an MDO to update.
    Method::build_interpreter_method_data(m, THREAD);
    if (HAS_PENDING_EXCEPTION) {
      // Only metaspace OOM is expected. No Java code executed.
      assert((PENDING_EXCEPTION->is_a(vmClasses::OutOfMemoryError_klass())), "we expect only an OOM error here");
      CLEAR_PENDING_EXCEPTION;
    }
    mdo = m()->method_data();
  }
  return mdo;
}

#if COMPILER2_OR_JVMCI
void Deoptimization::load_class_by_index(const constantPoolHandle& constant_pool, int index, TRAPS) {
  // In case of an unresolved klass entry, load the class.
  // This path is exercised from case _ldc in Parse::do_one_bytecode,
  // and probably nowhere else.
  // Even that case would benefit from simply re-interpreting the
  // bytecode, without paying special attention to the class index.
  // So this whole "class index" feature should probably be removed.

  if (constant_pool->tag_at(index).is_unresolved_klass()) {
    Klass* tk = constant_pool->klass_at(index, THREAD);
    if (HAS_PENDING_EXCEPTION) {
      // Exception happened during classloading. We ignore the exception here, since it
      // is going to be rethrown since the current activation is going to be deoptimized and
      // the interpreter will re-execute the bytecode.
      // Do not clear probable Async Exceptions.
      CLEAR_PENDING_NONASYNC_EXCEPTION;
      // Class loading called java code which may have caused a stack
      // overflow. If the exception was thrown right before the return
      // to the runtime the stack is no longer guarded. Reguard the
      // stack otherwise if we return to the uncommon trap blob and the
      // stack bang causes a stack overflow we crash.
      JavaThread* jt = THREAD;
      bool guard_pages_enabled = jt->stack_overflow_state()->reguard_stack_if_needed();
      assert(guard_pages_enabled, "stack banging in uncommon trap blob may cause crash");
    }
    return;
  }

  assert(!constant_pool->tag_at(index).is_symbol(),
         "no symbolic names here, please");
}

#if INCLUDE_JFR

class DeoptReasonSerializer : public JfrSerializer {
 public:
  void serialize(JfrCheckpointWriter& writer) {
    writer.write_count((u4)(Deoptimization::Reason_LIMIT + 1)); // + Reason::many (-1)
    for (int i = -1; i < Deoptimization::Reason_LIMIT; ++i) {
      writer.write_key((u8)i);
      writer.write(Deoptimization::trap_reason_name(i));
    }
  }
};

class DeoptActionSerializer : public JfrSerializer {
 public:
  void serialize(JfrCheckpointWriter& writer) {
    static const u4 nof_actions = Deoptimization::Action_LIMIT;
    writer.write_count(nof_actions);
    for (u4 i = 0; i < Deoptimization::Action_LIMIT; ++i) {
      writer.write_key(i);
      writer.write(Deoptimization::trap_action_name((int)i));
    }
  }
};

static void register_serializers() {
  static int critical_section = 0;
  if (1 == critical_section || Atomic::cmpxchg(&critical_section, 0, 1) == 1) {
    return;
  }
  JfrSerializer::register_serializer(TYPE_DEOPTIMIZATIONREASON, true, new DeoptReasonSerializer());
  JfrSerializer::register_serializer(TYPE_DEOPTIMIZATIONACTION, true, new DeoptActionSerializer());
}

static void post_deoptimization_event(CompiledMethod* nm,
                                      const Method* method,
                                      int trap_bci,
                                      int instruction,
                                      Deoptimization::DeoptReason reason,
                                      Deoptimization::DeoptAction action) {
  assert(nm != NULL, "invariant");
  assert(method != NULL, "invariant");
  if (EventDeoptimization::is_enabled()) {
    static bool serializers_registered = false;
    if (!serializers_registered) {
      register_serializers();
      serializers_registered = true;
    }
    EventDeoptimization event;
    event.set_compileId(nm->compile_id());
    event.set_compiler(nm->compiler_type());
    event.set_method(method);
    event.set_lineNumber(method->line_number_from_bci(trap_bci));
    event.set_bci(trap_bci);
    event.set_instruction(instruction);
    event.set_reason(reason);
    event.set_action(action);
    event.commit();
  }
}

#endif // INCLUDE_JFR

JRT_ENTRY(void, Deoptimization::uncommon_trap_inner(JavaThread* current, jint trap_request)) {
  HandleMark hm(current);

  // uncommon_trap() is called at the beginning of the uncommon trap
  // handler. Note this fact before we start generating temporary frames
  // that can confuse an asynchronous stack walker. This counter is
  // decremented at the end of unpack_frames().
  current->inc_in_deopt_handler();

#if INCLUDE_JVMCI
  // JVMCI might need to get an exception from the stack, which in turn requires the register map to be valid
  RegisterMap reg_map(current, true);
#else
  RegisterMap reg_map(current, false);
#endif
  frame stub_frame = current->last_frame();
  frame fr = stub_frame.sender(&reg_map);
  // Make sure the calling nmethod is not getting deoptimized and removed
  // before we are done with it.
  nmethodLocker nl(fr.pc());

  // Log a message
  Events::log_deopt_message(current, "Uncommon trap: trap_request=" PTR32_FORMAT " fr.pc=" INTPTR_FORMAT " relative=" INTPTR_FORMAT,
              trap_request, p2i(fr.pc()), fr.pc() - fr.cb()->code_begin());

  {
    ResourceMark rm;

    DeoptReason reason = trap_request_reason(trap_request);
    DeoptAction action = trap_request_action(trap_request);
#if INCLUDE_JVMCI
    int debug_id = trap_request_debug_id(trap_request);
#endif
    jint unloaded_class_index = trap_request_index(trap_request); // CP idx or -1

    vframe*  vf  = vframe::new_vframe(&fr, &reg_map, current);
    compiledVFrame* cvf = compiledVFrame::cast(vf);

    CompiledMethod* nm = cvf->code();

    ScopeDesc*      trap_scope  = cvf->scope();

    bool is_receiver_constraint_failure = COMPILER2_PRESENT(VerifyReceiverTypes &&) (reason == Deoptimization::Reason_receiver_constraint);

    if (TraceDeoptimization || is_receiver_constraint_failure) {
      ttyLocker ttyl;
      tty->print_cr("  bci=%d pc=" INTPTR_FORMAT ", relative_pc=" INTPTR_FORMAT ", method=%s" JVMCI_ONLY(", debug_id=%d"), trap_scope->bci(), p2i(fr.pc()), fr.pc() - nm->code_begin(), trap_scope->method()->name_and_sig_as_C_string()
#if INCLUDE_JVMCI
          , debug_id
#endif
          );
    }

    methodHandle    trap_method(current, trap_scope->method());
    int             trap_bci    = trap_scope->bci();
#if INCLUDE_JVMCI
    jlong           speculation = current->pending_failed_speculation();
    if (nm->is_compiled_by_jvmci()) {
      nm->as_nmethod()->update_speculation(current);
    } else {
      assert(speculation == 0, "There should not be a speculation for methods compiled by non-JVMCI compilers");
    }

    if (trap_bci == SynchronizationEntryBCI) {
      trap_bci = 0;
      current->set_pending_monitorenter(true);
    }

    if (reason == Deoptimization::Reason_transfer_to_interpreter) {
      current->set_pending_transfer_to_interpreter(true);
    }
#endif

    Bytecodes::Code trap_bc     = trap_method->java_code_at(trap_bci);
    // Record this event in the histogram.
    gather_statistics(reason, action, trap_bc);

    // Ensure that we can record deopt. history:
    // Need MDO to record RTM code generation state.
    bool create_if_missing = ProfileTraps || UseCodeAging RTM_OPT_ONLY( || UseRTMLocking );

    methodHandle profiled_method;
#if INCLUDE_JVMCI
    if (nm->is_compiled_by_jvmci()) {
      profiled_method = methodHandle(current, nm->method());
    } else {
      profiled_method = trap_method;
    }
#else
    profiled_method = trap_method;
#endif

    MethodData* trap_mdo =
      get_method_data(current, profiled_method, create_if_missing);

    JFR_ONLY(post_deoptimization_event(nm, trap_method(), trap_bci, trap_bc, reason, action);)

    // Log a message
    Events::log_deopt_message(current, "Uncommon trap: reason=%s action=%s pc=" INTPTR_FORMAT " method=%s @ %d %s",
                              trap_reason_name(reason), trap_action_name(action), p2i(fr.pc()),
                              trap_method->name_and_sig_as_C_string(), trap_bci, nm->compiler_name());

    // Print a bunch of diagnostics, if requested.
    if (TraceDeoptimization || LogCompilation || is_receiver_constraint_failure) {
      ResourceMark rm;
      ttyLocker ttyl;
      char buf[100];
      if (xtty != NULL) {
        xtty->begin_head("uncommon_trap thread='" UINTX_FORMAT "' %s",
                         os::current_thread_id(),
                         format_trap_request(buf, sizeof(buf), trap_request));
#if INCLUDE_JVMCI
        if (speculation != 0) {
          xtty->print(" speculation='" JLONG_FORMAT "'", speculation);
        }
#endif
        nm->log_identity(xtty);
      }
      Symbol* class_name = NULL;
      bool unresolved = false;
      if (unloaded_class_index >= 0) {
        constantPoolHandle constants (current, trap_method->constants());
        if (constants->tag_at(unloaded_class_index).is_unresolved_klass()) {
          class_name = constants->klass_name_at(unloaded_class_index);
          unresolved = true;
          if (xtty != NULL)
            xtty->print(" unresolved='1'");
        } else if (constants->tag_at(unloaded_class_index).is_symbol()) {
          class_name = constants->symbol_at(unloaded_class_index);
        }
        if (xtty != NULL)
          xtty->name(class_name);
      }
      if (xtty != NULL && trap_mdo != NULL && (int)reason < (int)MethodData::_trap_hist_limit) {
        // Dump the relevant MDO state.
        // This is the deopt count for the current reason, any previous
        // reasons or recompiles seen at this point.
        int dcnt = trap_mdo->trap_count(reason);
        if (dcnt != 0)
          xtty->print(" count='%d'", dcnt);
        ProfileData* pdata = trap_mdo->bci_to_data(trap_bci);
        int dos = (pdata == NULL)? 0: pdata->trap_state();
        if (dos != 0) {
          xtty->print(" state='%s'", format_trap_state(buf, sizeof(buf), dos));
          if (trap_state_is_recompiled(dos)) {
            int recnt2 = trap_mdo->overflow_recompile_count();
            if (recnt2 != 0)
              xtty->print(" recompiles2='%d'", recnt2);
          }
        }
      }
      if (xtty != NULL) {
        xtty->stamp();
        xtty->end_head();
      }
      if (TraceDeoptimization) {  // make noise on the tty
        tty->print("Uncommon trap occurred in");
        nm->method()->print_short_name(tty);
        tty->print(" compiler=%s compile_id=%d", nm->compiler_name(), nm->compile_id());
#if INCLUDE_JVMCI
        if (nm->is_nmethod()) {
          const char* installed_code_name = nm->as_nmethod()->jvmci_name();
          if (installed_code_name != NULL) {
            tty->print(" (JVMCI: installed code name=%s) ", installed_code_name);
          }
        }
#endif
        tty->print(" (@" INTPTR_FORMAT ") thread=" UINTX_FORMAT " reason=%s action=%s unloaded_class_index=%d" JVMCI_ONLY(" debug_id=%d"),
                   p2i(fr.pc()),
                   os::current_thread_id(),
                   trap_reason_name(reason),
                   trap_action_name(action),
                   unloaded_class_index
#if INCLUDE_JVMCI
                   , debug_id
#endif
                   );
        if (class_name != NULL) {
          tty->print(unresolved ? " unresolved class: " : " symbol: ");
          class_name->print_symbol_on(tty);
        }
        tty->cr();
      }
      if (xtty != NULL) {
        // Log the precise location of the trap.
        for (ScopeDesc* sd = trap_scope; ; sd = sd->sender()) {
          xtty->begin_elem("jvms bci='%d'", sd->bci());
          xtty->method(sd->method());
          xtty->end_elem();
          if (sd->is_top())  break;
        }
        xtty->tail("uncommon_trap");
      }
    }
    // (End diagnostic printout.)

    if (is_receiver_constraint_failure) {
      fatal("missing receiver type check");
    }

    // Load class if necessary
    if (unloaded_class_index >= 0) {
      constantPoolHandle constants(current, trap_method->constants());
      load_class_by_index(constants, unloaded_class_index, THREAD);
    }

    // Flush the nmethod if necessary and desirable.
    //
    // We need to avoid situations where we are re-flushing the nmethod
    // because of a hot deoptimization site.  Repeated flushes at the same
    // point need to be detected by the compiler and avoided.  If the compiler
    // cannot avoid them (or has a bug and "refuses" to avoid them), this
    // module must take measures to avoid an infinite cycle of recompilation
    // and deoptimization.  There are several such measures:
    //
    //   1. If a recompilation is ordered a second time at some site X
    //   and for the same reason R, the action is adjusted to 'reinterpret',
    //   to give the interpreter time to exercise the method more thoroughly.
    //   If this happens, the method's overflow_recompile_count is incremented.
    //
    //   2. If the compiler fails to reduce the deoptimization rate, then
    //   the method's overflow_recompile_count will begin to exceed the set
    //   limit PerBytecodeRecompilationCutoff.  If this happens, the action
    //   is adjusted to 'make_not_compilable', and the method is abandoned
    //   to the interpreter.  This is a performance hit for hot methods,
    //   but is better than a disastrous infinite cycle of recompilations.
    //   (Actually, only the method containing the site X is abandoned.)
    //
    //   3. In parallel with the previous measures, if the total number of
    //   recompilations of a method exceeds the much larger set limit
    //   PerMethodRecompilationCutoff, the method is abandoned.
    //   This should only happen if the method is very large and has
    //   many "lukewarm" deoptimizations.  The code which enforces this
    //   limit is elsewhere (class nmethod, class Method).
    //
    // Note that the per-BCI 'is_recompiled' bit gives the compiler one chance
    // to recompile at each bytecode independently of the per-BCI cutoff.
    //
    // The decision to update code is up to the compiler, and is encoded
    // in the Action_xxx code.  If the compiler requests Action_none
    // no trap state is changed, no compiled code is changed, and the
    // computation suffers along in the interpreter.
    //
    // The other action codes specify various tactics for decompilation
    // and recompilation.  Action_maybe_recompile is the loosest, and
    // allows the compiled code to stay around until enough traps are seen,
    // and until the compiler gets around to recompiling the trapping method.
    //
    // The other actions cause immediate removal of the present code.

    // Traps caused by injected profile shouldn't pollute trap counts.
    bool injected_profile_trap = trap_method->has_injected_profile() &&
                                 (reason == Reason_intrinsic || reason == Reason_unreached);

    bool update_trap_state = (reason != Reason_tenured) && !injected_profile_trap;
    bool make_not_entrant = false;
    bool make_not_compilable = false;
    bool reprofile = false;
    switch (action) {
    case Action_none:
      // Keep the old code.
      update_trap_state = false;
      break;
    case Action_maybe_recompile:
      // Do not need to invalidate the present code, but we can
      // initiate another
      // Start compiler without (necessarily) invalidating the nmethod.
      // The system will tolerate the old code, but new code should be
      // generated when possible.
      break;
    case Action_reinterpret:
      // Go back into the interpreter for a while, and then consider
      // recompiling form scratch.
      make_not_entrant = true;
      // Reset invocation counter for outer most method.
      // This will allow the interpreter to exercise the bytecodes
      // for a while before recompiling.
      // By contrast, Action_make_not_entrant is immediate.
      //
      // Note that the compiler will track null_check, null_assert,
      // range_check, and class_check events and log them as if they
      // had been traps taken from compiled code.  This will update
      // the MDO trap history so that the next compilation will
      // properly detect hot trap sites.
      reprofile = true;
      break;
    case Action_make_not_entrant:
      // Request immediate recompilation, and get rid of the old code.
      // Make them not entrant, so next time they are called they get
      // recompiled.  Unloaded classes are loaded now so recompile before next
      // time they are called.  Same for uninitialized.  The interpreter will
      // link the missing class, if any.
      make_not_entrant = true;
      break;
    case Action_make_not_compilable:
      // Give up on compiling this method at all.
      make_not_entrant = true;
      make_not_compilable = true;
      break;
    default:
      ShouldNotReachHere();
    }

    // Setting +ProfileTraps fixes the following, on all platforms:
    // 4852688: ProfileInterpreter is off by default for ia64.  The result is
    // infinite heroic-opt-uncommon-trap/deopt/recompile cycles, since the
    // recompile relies on a MethodData* to record heroic opt failures.

    // Whether the interpreter is producing MDO data or not, we also need
    // to use the MDO to detect hot deoptimization points and control
    // aggressive optimization.
    bool inc_recompile_count = false;
    ProfileData* pdata = NULL;
    if (ProfileTraps && CompilerConfig::is_c2_or_jvmci_compiler_enabled() && update_trap_state && trap_mdo != NULL) {
      assert(trap_mdo == get_method_data(current, profiled_method, false), "sanity");
      uint this_trap_count = 0;
      bool maybe_prior_trap = false;
      bool maybe_prior_recompile = false;
      pdata = query_update_method_data(trap_mdo, trap_bci, reason, true,
#if INCLUDE_JVMCI
                                   nm->is_compiled_by_jvmci() && nm->is_osr_method(),
#endif
                                   nm->method(),
                                   //outputs:
                                   this_trap_count,
                                   maybe_prior_trap,
                                   maybe_prior_recompile);
      // Because the interpreter also counts null, div0, range, and class
      // checks, these traps from compiled code are double-counted.
      // This is harmless; it just means that the PerXTrapLimit values
      // are in effect a little smaller than they look.

      DeoptReason per_bc_reason = reason_recorded_per_bytecode_if_any(reason);
      if (per_bc_reason != Reason_none) {
        // Now take action based on the partially known per-BCI history.
        if (maybe_prior_trap
            && this_trap_count >= (uint)PerBytecodeTrapLimit) {
          // If there are too many traps at this BCI, force a recompile.
          // This will allow the compiler to see the limit overflow, and
          // take corrective action, if possible.  The compiler generally
          // does not use the exact PerBytecodeTrapLimit value, but instead
          // changes its tactics if it sees any traps at all.  This provides
          // a little hysteresis, delaying a recompile until a trap happens
          // several times.
          //
          // Actually, since there is only one bit of counter per BCI,
          // the possible per-BCI counts are {0,1,(per-method count)}.
          // This produces accurate results if in fact there is only
          // one hot trap site, but begins to get fuzzy if there are
          // many sites.  For example, if there are ten sites each
          // trapping two or more times, they each get the blame for
          // all of their traps.
          make_not_entrant = true;
        }

        // Detect repeated recompilation at the same BCI, and enforce a limit.
        if (make_not_entrant && maybe_prior_recompile) {
          // More than one recompile at this point.
          inc_recompile_count = maybe_prior_trap;
        }
      } else {
        // For reasons which are not recorded per-bytecode, we simply
        // force recompiles unconditionally.
        // (Note that PerMethodRecompilationCutoff is enforced elsewhere.)
        make_not_entrant = true;
      }

      // Go back to the compiler if there are too many traps in this method.
      if (this_trap_count >= per_method_trap_limit(reason)) {
        // If there are too many traps in this method, force a recompile.
        // This will allow the compiler to see the limit overflow, and
        // take corrective action, if possible.
        // (This condition is an unlikely backstop only, because the
        // PerBytecodeTrapLimit is more likely to take effect first,
        // if it is applicable.)
        make_not_entrant = true;
      }

      // Here's more hysteresis:  If there has been a recompile at
      // this trap point already, run the method in the interpreter
      // for a while to exercise it more thoroughly.
      if (make_not_entrant && maybe_prior_recompile && maybe_prior_trap) {
        reprofile = true;
      }
    }

    // Take requested actions on the method:

    // Recompile
    if (make_not_entrant) {
      if (!nm->make_not_entrant()) {
        return; // the call did not change nmethod's state
      }

      if (pdata != NULL) {
        // Record the recompilation event, if any.
        int tstate0 = pdata->trap_state();
        int tstate1 = trap_state_set_recompiled(tstate0, true);
        if (tstate1 != tstate0)
          pdata->set_trap_state(tstate1);
      }

#if INCLUDE_RTM_OPT
      // Restart collecting RTM locking abort statistic if the method
      // is recompiled for a reason other than RTM state change.
      // Assume that in new recompiled code the statistic could be different,
      // for example, due to different inlining.
      if ((reason != Reason_rtm_state_change) && (trap_mdo != NULL) &&
          UseRTMDeopt && (nm->as_nmethod()->rtm_state() != ProfileRTM)) {
        trap_mdo->atomic_set_rtm_state(ProfileRTM);
      }
#endif
      // For code aging we count traps separately here, using make_not_entrant()
      // as a guard against simultaneous deopts in multiple threads.
      if (reason == Reason_tenured && trap_mdo != NULL) {
        trap_mdo->inc_tenure_traps();
      }
    }

    if (inc_recompile_count) {
      trap_mdo->inc_overflow_recompile_count();
      if ((uint)trap_mdo->overflow_recompile_count() >
          (uint)PerBytecodeRecompilationCutoff) {
        // Give up on the method containing the bad BCI.
        if (trap_method() == nm->method()) {
          make_not_compilable = true;
        } else {
          trap_method->set_not_compilable("overflow_recompile_count > PerBytecodeRecompilationCutoff", CompLevel_full_optimization);
          // But give grace to the enclosing nm->method().
        }
      }
    }

    // Reprofile
    if (reprofile) {
      CompilationPolicy::reprofile(trap_scope, nm->is_osr_method());
    }

    // Give up compiling
    if (make_not_compilable && !nm->method()->is_not_compilable(CompLevel_full_optimization)) {
      assert(make_not_entrant, "consistent");
      nm->method()->set_not_compilable("give up compiling", CompLevel_full_optimization);
    }

  } // Free marked resources

}
JRT_END

ProfileData*
Deoptimization::query_update_method_data(MethodData* trap_mdo,
                                         int trap_bci,
                                         Deoptimization::DeoptReason reason,
                                         bool update_total_trap_count,
#if INCLUDE_JVMCI
                                         bool is_osr,
#endif
                                         Method* compiled_method,
                                         //outputs:
                                         uint& ret_this_trap_count,
                                         bool& ret_maybe_prior_trap,
                                         bool& ret_maybe_prior_recompile) {
  bool maybe_prior_trap = false;
  bool maybe_prior_recompile = false;
  uint this_trap_count = 0;
  if (update_total_trap_count) {
    uint idx = reason;
#if INCLUDE_JVMCI
    if (is_osr) {
      idx += Reason_LIMIT;
    }
#endif
    uint prior_trap_count = trap_mdo->trap_count(idx);
    this_trap_count  = trap_mdo->inc_trap_count(idx);

    // If the runtime cannot find a place to store trap history,
    // it is estimated based on the general condition of the method.
    // If the method has ever been recompiled, or has ever incurred
    // a trap with the present reason , then this BCI is assumed
    // (pessimistically) to be the culprit.
    maybe_prior_trap      = (prior_trap_count != 0);
    maybe_prior_recompile = (trap_mdo->decompile_count() != 0);
  }
  ProfileData* pdata = NULL;


  // For reasons which are recorded per bytecode, we check per-BCI data.
  DeoptReason per_bc_reason = reason_recorded_per_bytecode_if_any(reason);
  assert(per_bc_reason != Reason_none || update_total_trap_count, "must be");
  if (per_bc_reason != Reason_none) {
    // Find the profile data for this BCI.  If there isn't one,
    // try to allocate one from the MDO's set of spares.
    // This will let us detect a repeated trap at this point.
    pdata = trap_mdo->allocate_bci_to_data(trap_bci, reason_is_speculate(reason) ? compiled_method : NULL);

    if (pdata != NULL) {
      if (reason_is_speculate(reason) && !pdata->is_SpeculativeTrapData()) {
        if (LogCompilation && xtty != NULL) {
          ttyLocker ttyl;
          // no more room for speculative traps in this MDO
          xtty->elem("speculative_traps_oom");
        }
      }
      // Query the trap state of this profile datum.
      int tstate0 = pdata->trap_state();
      if (!trap_state_has_reason(tstate0, per_bc_reason))
        maybe_prior_trap = false;
      if (!trap_state_is_recompiled(tstate0))
        maybe_prior_recompile = false;

      // Update the trap state of this profile datum.
      int tstate1 = tstate0;
      // Record the reason.
      tstate1 = trap_state_add_reason(tstate1, per_bc_reason);
      // Store the updated state on the MDO, for next time.
      if (tstate1 != tstate0)
        pdata->set_trap_state(tstate1);
    } else {
      if (LogCompilation && xtty != NULL) {
        ttyLocker ttyl;
        // Missing MDP?  Leave a small complaint in the log.
        xtty->elem("missing_mdp bci='%d'", trap_bci);
      }
    }
  }

  // Return results:
  ret_this_trap_count = this_trap_count;
  ret_maybe_prior_trap = maybe_prior_trap;
  ret_maybe_prior_recompile = maybe_prior_recompile;
  return pdata;
}

void
Deoptimization::update_method_data_from_interpreter(MethodData* trap_mdo, int trap_bci, int reason) {
  ResourceMark rm;
  // Ignored outputs:
  uint ignore_this_trap_count;
  bool ignore_maybe_prior_trap;
  bool ignore_maybe_prior_recompile;
  assert(!reason_is_speculate(reason), "reason speculate only used by compiler");
  // JVMCI uses the total counts to determine if deoptimizations are happening too frequently -> do not adjust total counts
  bool update_total_counts = true JVMCI_ONLY( && !UseJVMCICompiler);
  query_update_method_data(trap_mdo, trap_bci,
                           (DeoptReason)reason,
                           update_total_counts,
#if INCLUDE_JVMCI
                           false,
#endif
                           NULL,
                           ignore_this_trap_count,
                           ignore_maybe_prior_trap,
                           ignore_maybe_prior_recompile);
}

Deoptimization::UnrollBlock* Deoptimization::uncommon_trap(JavaThread* current, jint trap_request, jint exec_mode) {
  // Enable WXWrite: current function is called from methods compiled by C2 directly
  MACOS_AARCH64_ONLY(ThreadWXEnable wx(WXWrite, current));

  if (TraceDeoptimization) {
    tty->print("Uncommon trap ");
  }
  // Still in Java no safepoints
  {
    // This enters VM and may safepoint
    uncommon_trap_inner(current, trap_request);
  }
  HandleMark hm(current);
  return fetch_unroll_info_helper(current, exec_mode);
}

// Local derived constants.
// Further breakdown of DataLayout::trap_state, as promised by DataLayout.
const int DS_REASON_MASK   = ((uint)DataLayout::trap_mask) >> 1;
const int DS_RECOMPILE_BIT = DataLayout::trap_mask - DS_REASON_MASK;

//---------------------------trap_state_reason---------------------------------
Deoptimization::DeoptReason
Deoptimization::trap_state_reason(int trap_state) {
  // This assert provides the link between the width of DataLayout::trap_bits
  // and the encoding of "recorded" reasons.  It ensures there are enough
  // bits to store all needed reasons in the per-BCI MDO profile.
  assert(DS_REASON_MASK >= Reason_RECORDED_LIMIT, "enough bits");
  int recompile_bit = (trap_state & DS_RECOMPILE_BIT);
  trap_state -= recompile_bit;
  if (trap_state == DS_REASON_MASK) {
    return Reason_many;
  } else {
    assert((int)Reason_none == 0, "state=0 => Reason_none");
    return (DeoptReason)trap_state;
  }
}
//-------------------------trap_state_has_reason-------------------------------
int Deoptimization::trap_state_has_reason(int trap_state, int reason) {
  assert(reason_is_recorded_per_bytecode((DeoptReason)reason), "valid reason");
  assert(DS_REASON_MASK >= Reason_RECORDED_LIMIT, "enough bits");
  int recompile_bit = (trap_state & DS_RECOMPILE_BIT);
  trap_state -= recompile_bit;
  if (trap_state == DS_REASON_MASK) {
    return -1;  // true, unspecifically (bottom of state lattice)
  } else if (trap_state == reason) {
    return 1;   // true, definitely
  } else if (trap_state == 0) {
    return 0;   // false, definitely (top of state lattice)
  } else {
    return 0;   // false, definitely
  }
}
//-------------------------trap_state_add_reason-------------------------------
int Deoptimization::trap_state_add_reason(int trap_state, int reason) {
  assert(reason_is_recorded_per_bytecode((DeoptReason)reason) || reason == Reason_many, "valid reason");
  int recompile_bit = (trap_state & DS_RECOMPILE_BIT);
  trap_state -= recompile_bit;
  if (trap_state == DS_REASON_MASK) {
    return trap_state + recompile_bit;     // already at state lattice bottom
  } else if (trap_state == reason) {
    return trap_state + recompile_bit;     // the condition is already true
  } else if (trap_state == 0) {
    return reason + recompile_bit;          // no condition has yet been true
  } else {
    return DS_REASON_MASK + recompile_bit;  // fall to state lattice bottom
  }
}
//-----------------------trap_state_is_recompiled------------------------------
bool Deoptimization::trap_state_is_recompiled(int trap_state) {
  return (trap_state & DS_RECOMPILE_BIT) != 0;
}
//-----------------------trap_state_set_recompiled-----------------------------
int Deoptimization::trap_state_set_recompiled(int trap_state, bool z) {
  if (z)  return trap_state |  DS_RECOMPILE_BIT;
  else    return trap_state & ~DS_RECOMPILE_BIT;
}
//---------------------------format_trap_state---------------------------------
// This is used for debugging and diagnostics, including LogFile output.
const char* Deoptimization::format_trap_state(char* buf, size_t buflen,
                                              int trap_state) {
  assert(buflen > 0, "sanity");
  DeoptReason reason      = trap_state_reason(trap_state);
  bool        recomp_flag = trap_state_is_recompiled(trap_state);
  // Re-encode the state from its decoded components.
  int decoded_state = 0;
  if (reason_is_recorded_per_bytecode(reason) || reason == Reason_many)
    decoded_state = trap_state_add_reason(decoded_state, reason);
  if (recomp_flag)
    decoded_state = trap_state_set_recompiled(decoded_state, recomp_flag);
  // If the state re-encodes properly, format it symbolically.
  // Because this routine is used for debugging and diagnostics,
  // be robust even if the state is a strange value.
  size_t len;
  if (decoded_state != trap_state) {
    // Random buggy state that doesn't decode??
    len = jio_snprintf(buf, buflen, "#%d", trap_state);
  } else {
    len = jio_snprintf(buf, buflen, "%s%s",
                       trap_reason_name(reason),
                       recomp_flag ? " recompiled" : "");
  }
  return buf;
}


//--------------------------------statics--------------------------------------
const char* Deoptimization::_trap_reason_name[] = {
  // Note:  Keep this in sync. with enum DeoptReason.
  "none",
  "null_check",
  "null_assert" JVMCI_ONLY("_or_unreached0"),
  "range_check",
  "class_check",
  "array_check",
  "intrinsic" JVMCI_ONLY("_or_type_checked_inlining"),
  "bimorphic" JVMCI_ONLY("_or_optimized_type_check"),
  "profile_predicate",
  "unloaded",
  "uninitialized",
  "initialized",
  "unreached",
  "unhandled",
  "constraint",
  "div0_check",
  "age",
  "predicate",
  "loop_limit_check",
  "speculate_class_check",
  "speculate_null_check",
  "speculate_null_assert",
  "rtm_state_change",
  "unstable_if",
  "unstable_fused_if",
  "receiver_constraint",
#if INCLUDE_JVMCI
  "aliasing",
  "transfer_to_interpreter",
  "not_compiled_exception_handler",
  "unresolved",
  "jsr_mismatch",
#endif
  "tenured"
};
const char* Deoptimization::_trap_action_name[] = {
  // Note:  Keep this in sync. with enum DeoptAction.
  "none",
  "maybe_recompile",
  "reinterpret",
  "make_not_entrant",
  "make_not_compilable"
};

const char* Deoptimization::trap_reason_name(int reason) {
  // Check that every reason has a name
  STATIC_ASSERT(sizeof(_trap_reason_name)/sizeof(const char*) == Reason_LIMIT);

  if (reason == Reason_many)  return "many";
  if ((uint)reason < Reason_LIMIT)
    return _trap_reason_name[reason];
  static char buf[20];
  sprintf(buf, "reason%d", reason);
  return buf;
}
const char* Deoptimization::trap_action_name(int action) {
  // Check that every action has a name
  STATIC_ASSERT(sizeof(_trap_action_name)/sizeof(const char*) == Action_LIMIT);

  if ((uint)action < Action_LIMIT)
    return _trap_action_name[action];
  static char buf[20];
  sprintf(buf, "action%d", action);
  return buf;
}

// This is used for debugging and diagnostics, including LogFile output.
const char* Deoptimization::format_trap_request(char* buf, size_t buflen,
                                                int trap_request) {
  jint unloaded_class_index = trap_request_index(trap_request);
  const char* reason = trap_reason_name(trap_request_reason(trap_request));
  const char* action = trap_action_name(trap_request_action(trap_request));
#if INCLUDE_JVMCI
  int debug_id = trap_request_debug_id(trap_request);
#endif
  size_t len;
  if (unloaded_class_index < 0) {
    len = jio_snprintf(buf, buflen, "reason='%s' action='%s'" JVMCI_ONLY(" debug_id='%d'"),
                       reason, action
#if INCLUDE_JVMCI
                       ,debug_id
#endif
                       );
  } else {
    len = jio_snprintf(buf, buflen, "reason='%s' action='%s' index='%d'" JVMCI_ONLY(" debug_id='%d'"),
                       reason, action, unloaded_class_index
#if INCLUDE_JVMCI
                       ,debug_id
#endif
                       );
  }
  return buf;
}

juint Deoptimization::_deoptimization_hist
        [Deoptimization::Reason_LIMIT]
    [1 + Deoptimization::Action_LIMIT]
        [Deoptimization::BC_CASE_LIMIT]
  = {0};

enum {
  LSB_BITS = 8,
  LSB_MASK = right_n_bits(LSB_BITS)
};

void Deoptimization::gather_statistics(DeoptReason reason, DeoptAction action,
                                       Bytecodes::Code bc) {
  assert(reason >= 0 && reason < Reason_LIMIT, "oob");
  assert(action >= 0 && action < Action_LIMIT, "oob");
  _deoptimization_hist[Reason_none][0][0] += 1;  // total
  _deoptimization_hist[reason][0][0]      += 1;  // per-reason total
  juint* cases = _deoptimization_hist[reason][1+action];
  juint* bc_counter_addr = NULL;
  juint  bc_counter      = 0;
  // Look for an unused counter, or an exact match to this BC.
  if (bc != Bytecodes::_illegal) {
    for (int bc_case = 0; bc_case < BC_CASE_LIMIT; bc_case++) {
      juint* counter_addr = &cases[bc_case];
      juint  counter = *counter_addr;
      if ((counter == 0 && bc_counter_addr == NULL)
          || (Bytecodes::Code)(counter & LSB_MASK) == bc) {
        // this counter is either free or is already devoted to this BC
        bc_counter_addr = counter_addr;
        bc_counter = counter | bc;
      }
    }
  }
  if (bc_counter_addr == NULL) {
    // Overflow, or no given bytecode.
    bc_counter_addr = &cases[BC_CASE_LIMIT-1];
    bc_counter = (*bc_counter_addr & ~LSB_MASK);  // clear LSB
  }
  *bc_counter_addr = bc_counter + (1 << LSB_BITS);
}

jint Deoptimization::total_deoptimization_count() {
  return _deoptimization_hist[Reason_none][0][0];
}

void Deoptimization::print_statistics() {
  juint total = total_deoptimization_count();
  juint account = total;
  if (total != 0) {
    ttyLocker ttyl;
    if (xtty != NULL)  xtty->head("statistics type='deoptimization'");
    tty->print_cr("Deoptimization traps recorded:");
    #define PRINT_STAT_LINE(name, r) \
      tty->print_cr("  %4d (%4.1f%%) %s", (int)(r), ((r) * 100.0) / total, name);
    PRINT_STAT_LINE("total", total);
    // For each non-zero entry in the histogram, print the reason,
    // the action, and (if specifically known) the type of bytecode.
    for (int reason = 0; reason < Reason_LIMIT; reason++) {
      for (int action = 0; action < Action_LIMIT; action++) {
        juint* cases = _deoptimization_hist[reason][1+action];
        for (int bc_case = 0; bc_case < BC_CASE_LIMIT; bc_case++) {
          juint counter = cases[bc_case];
          if (counter != 0) {
            char name[1*K];
            Bytecodes::Code bc = (Bytecodes::Code)(counter & LSB_MASK);
            if (bc_case == BC_CASE_LIMIT && (int)bc == 0)
              bc = Bytecodes::_illegal;
            sprintf(name, "%s/%s/%s",
                    trap_reason_name(reason),
                    trap_action_name(action),
                    Bytecodes::is_defined(bc)? Bytecodes::name(bc): "other");
            juint r = counter >> LSB_BITS;
            tty->print_cr("  %40s: " UINT32_FORMAT " (%.1f%%)", name, r, (r * 100.0) / total);
            account -= r;
          }
        }
      }
    }
    if (account != 0) {
      PRINT_STAT_LINE("unaccounted", account);
    }
    #undef PRINT_STAT_LINE
    if (xtty != NULL)  xtty->tail("statistics");
  }
}

#else // COMPILER2_OR_JVMCI


// Stubs for C1 only system.
bool Deoptimization::trap_state_is_recompiled(int trap_state) {
  return false;
}

const char* Deoptimization::trap_reason_name(int reason) {
  return "unknown";
}

void Deoptimization::print_statistics() {
  // no output
}

void
Deoptimization::update_method_data_from_interpreter(MethodData* trap_mdo, int trap_bci, int reason) {
  // no udpate
}

int Deoptimization::trap_state_has_reason(int trap_state, int reason) {
  return 0;
}

void Deoptimization::gather_statistics(DeoptReason reason, DeoptAction action,
                                       Bytecodes::Code bc) {
  // no update
}

const char* Deoptimization::format_trap_state(char* buf, size_t buflen,
                                              int trap_state) {
  jio_snprintf(buf, buflen, "#%d", trap_state);
  return buf;
}

#endif // COMPILER2_OR_JVMCI
