/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2007, 2008, 2009, 2010, 2011 Red Hat, Inc.
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
#include "asm/assembler.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "interpreter/zero/bytecodeInterpreter.hpp"
#include "interpreter/zero/zeroInterpreter.hpp"
#include "interpreter/zero/zeroInterpreterGenerator.hpp"
#include "oops/access.inline.hpp"
#include "oops/cpCache.inline.hpp"
#include "oops/klass.inline.hpp"
#include "oops/methodData.hpp"
#include "oops/method.hpp"
#include "oops/oop.inline.hpp"
#include "prims/jvmtiExport.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/timer.hpp"
#include "runtime/timerTrace.hpp"
#include "utilities/debug.hpp"
#include "utilities/macros.hpp"

#include "entry_zero.hpp"
#include "stack_zero.inline.hpp"

void ZeroInterpreter::initialize_stub() {
  if (_code != NULL) return;

  // generate interpreter
  int code_size = InterpreterCodeSize;
  NOT_PRODUCT(code_size *= 4;)  // debug uses extra interpreter code space
  _code = new StubQueue(new InterpreterCodeletInterface, code_size, NULL,
                         "Interpreter");
}

void ZeroInterpreter::initialize_code() {
  AbstractInterpreter::initialize();

  // generate interpreter
  { ResourceMark rm;
    TraceTime timer("Interpreter generation", TRACETIME_LOG(Info, startuptime));
    ZeroInterpreterGenerator g(_code);
    if (PrintInterpreter) print();
  }

  // Allow c++ interpreter to do one initialization now that switches are set, etc.
  BytecodeInterpreter start_msg(BytecodeInterpreter::initialize);
  if (JvmtiExport::can_post_interpreter_events()) {
    BytecodeInterpreter::run<true>(&start_msg);
  } else {
    BytecodeInterpreter::run<false>(&start_msg);
  }
}

void ZeroInterpreter::invoke_method(Method* method, address entry_point, TRAPS) {
  ((ZeroEntry *) entry_point)->invoke(method, THREAD);
}

void ZeroInterpreter::invoke_osr(Method* method,
                                address   entry_point,
                                address   osr_buf,
                                TRAPS) {
  ((ZeroEntry *) entry_point)->invoke_osr(method, osr_buf, THREAD);
}



InterpreterCodelet* ZeroInterpreter::codelet_containing(address pc) {
  // FIXME: I'm pretty sure _code is null and this is never called, which is why it's copied.
  return (InterpreterCodelet*)_code->stub_containing(pc);
}
#define fixup_after_potential_safepoint()       \
  method = istate->method()

#define CALL_VM_NOCHECK_NOFIX(func)             \
  thread->set_last_Java_frame();                \
  func;                                         \
  thread->reset_last_Java_frame();

#define CALL_VM_NOCHECK(func)                   \
  CALL_VM_NOCHECK_NOFIX(func)                   \
  fixup_after_potential_safepoint()

int ZeroInterpreter::normal_entry(Method* method, intptr_t UNUSED, TRAPS) {
  JavaThread *thread = THREAD;

  // Allocate and initialize our frame.
  InterpreterFrame *frame = InterpreterFrame::build(method, CHECK_0);
  thread->push_zero_frame(frame);

  // Execute those bytecodes!
  main_loop(0, THREAD);

  // No deoptimized frames on the stack
  return 0;
}

int ZeroInterpreter::Reference_get_entry(Method* method, intptr_t UNUSED, TRAPS) {
  JavaThread* thread = THREAD;
  ZeroStack* stack = thread->zero_stack();
  intptr_t* topOfStack = stack->sp();

  oop ref = STACK_OBJECT(0);

  // Shortcut if reference is known NULL
  if (ref == NULL) {
    return normal_entry(method, 0, THREAD);
  }

  // Read the referent with weaker semantics, and let GCs handle the rest.
  const int referent_offset = java_lang_ref_Reference::referent_offset();
  oop obj = HeapAccess<IN_HEAP | ON_WEAK_OOP_REF>::oop_load_at(ref, referent_offset);

  SET_STACK_OBJECT(obj, 0);

  // No deoptimized frames on the stack
  return 0;
}

intptr_t narrow(BasicType type, intptr_t result) {
  // mask integer result to narrower return type.
  switch (type) {
    case T_BOOLEAN:
      return result&1;
    case T_BYTE:
      return (intptr_t)(jbyte)result;
    case T_CHAR:
      return (intptr_t)(uintptr_t)(jchar)result;
    case T_SHORT:
      return (intptr_t)(jshort)result;
    case T_OBJECT:  // nothing to do fall through
    case T_ARRAY:
    case T_LONG:
    case T_INT:
    case T_FLOAT:
    case T_DOUBLE:
    case T_VOID:
      return result;
    default:
      ShouldNotReachHere();
      return result; // silence compiler warnings
  }
}


void ZeroInterpreter::main_loop(int recurse, TRAPS) {
  JavaThread *thread = THREAD;
  ZeroStack *stack = thread->zero_stack();

  // If we are entering from a deopt we may need to call
  // ourself a few times in order to get to our frame.
  if (recurse)
    main_loop(recurse - 1, THREAD);

  InterpreterFrame *frame = thread->top_zero_frame()->as_interpreter_frame();
  interpreterState istate = frame->interpreter_state();
  Method* method = istate->method();

  intptr_t *result = NULL;
  int result_slots = 0;

  while (true) {
    // We can set up the frame anchor with everything we want at
    // this point as we are thread_in_Java and no safepoints can
    // occur until we go to vm mode.  We do have to clear flags
    // on return from vm but that is it.
    thread->set_last_Java_frame();

    // Call the interpreter
    if (JvmtiExport::can_post_interpreter_events()) {
      BytecodeInterpreter::run<true>(istate);
    } else {
      BytecodeInterpreter::run<false>(istate);
    }
    fixup_after_potential_safepoint();

    // Clear the frame anchor
    thread->reset_last_Java_frame();

    // Examine the message from the interpreter to decide what to do
    if (istate->msg() == BytecodeInterpreter::call_method) {
      Method* callee = istate->callee();

      // Trim back the stack to put the parameters at the top
      stack->set_sp(istate->stack() + 1);

      // Make the call
      Interpreter::invoke_method(callee, istate->callee_entry_point(), THREAD);
      fixup_after_potential_safepoint();

      // Convert the result
      istate->set_stack(stack->sp() - 1);

      // Restore the stack
      stack->set_sp(istate->stack_limit() + 1);

      // Resume the interpreter
      istate->set_msg(BytecodeInterpreter::method_resume);
    }
    else if (istate->msg() == BytecodeInterpreter::more_monitors) {
      int monitor_words = frame::interpreter_frame_monitor_size();

      // Allocate the space
      stack->overflow_check(monitor_words, THREAD);
      if (HAS_PENDING_EXCEPTION)
        break;
      stack->alloc(monitor_words * wordSize);

      // Move the expression stack contents
      for (intptr_t *p = istate->stack() + 1; p < istate->stack_base(); p++)
        *(p - monitor_words) = *p;

      // Move the expression stack pointers
      istate->set_stack_limit(istate->stack_limit() - monitor_words);
      istate->set_stack(istate->stack() - monitor_words);
      istate->set_stack_base(istate->stack_base() - monitor_words);

      // Zero the new monitor so the interpreter can find it.
      ((BasicObjectLock *) istate->stack_base())->set_obj(NULL);

      // Resume the interpreter
      istate->set_msg(BytecodeInterpreter::got_monitors);
    }
    else if (istate->msg() == BytecodeInterpreter::return_from_method) {
      // Copy the result into the caller's frame
      result_slots = type2size[method->result_type()];
      assert(result_slots >= 0 && result_slots <= 2, "what?");
      result = istate->stack() + result_slots;
      break;
    }
    else if (istate->msg() == BytecodeInterpreter::throwing_exception) {
      assert(HAS_PENDING_EXCEPTION, "should do");
      break;
    }
    else if (istate->msg() == BytecodeInterpreter::do_osr) {
      // Unwind the current frame
      thread->pop_zero_frame();

      // Remove any extension of the previous frame
      int extra_locals = method->max_locals() - method->size_of_parameters();
      stack->set_sp(stack->sp() + extra_locals);

      // Jump into the OSR method
      Interpreter::invoke_osr(
        method, istate->osr_entry(), istate->osr_buf(), THREAD);
      return;
    }
    else {
      ShouldNotReachHere();
    }
  }

  // Unwind the current frame
  thread->pop_zero_frame();

  // Pop our local variables
  stack->set_sp(stack->sp() + method->max_locals());

  // Push our result
  for (int i = 0; i < result_slots; i++) {
    // Adjust result to smaller
    union {
      intptr_t res;
      jint res_jint;
    };
    res = result[-i];
    if (result_slots == 1) {
      BasicType t = method->result_type();
      if (is_subword_type(t)) {
        res_jint = (jint)narrow(t, res_jint);
      }
    }
    stack->push(res);
  }
}

int ZeroInterpreter::native_entry(Method* method, intptr_t UNUSED, TRAPS) {
  // Make sure method is native and not abstract
  assert(method->is_native() && !method->is_abstract(), "should be");

  JavaThread *thread = THREAD;
  ZeroStack *stack = thread->zero_stack();

  // Allocate and initialize our frame
  InterpreterFrame *frame = InterpreterFrame::build(method, CHECK_0);
  thread->push_zero_frame(frame);
  interpreterState istate = frame->interpreter_state();
  intptr_t *locals = istate->locals();

  // Lock if necessary
  BasicObjectLock *monitor;
  monitor = NULL;
  if (method->is_synchronized()) {
    monitor = (BasicObjectLock*) istate->stack_base();
    oop lockee = monitor->obj();
    markWord disp = lockee->mark().set_unlocked();

    monitor->lock()->set_displaced_header(disp);
    if (lockee->cas_set_mark(markWord::from_pointer(monitor), disp) != disp) {
      if (thread->is_lock_owned((address) disp.clear_lock_bits().to_pointer())) {
        monitor->lock()->set_displaced_header(markWord::from_pointer(NULL));
      }
      else {
        CALL_VM_NOCHECK(InterpreterRuntime::monitorenter(thread, monitor));
        if (HAS_PENDING_EXCEPTION)
          goto unwind_and_return;
      }
    }
  }

  // Get the signature handler
  InterpreterRuntime::SignatureHandler *handler; {
    address handlerAddr = method->signature_handler();
    if (handlerAddr == NULL) {
      CALL_VM_NOCHECK(InterpreterRuntime::prepare_native_call(thread, method));
      if (HAS_PENDING_EXCEPTION)
        goto unlock_unwind_and_return;

      handlerAddr = method->signature_handler();
      assert(handlerAddr != NULL, "eh?");
    }
    if (handlerAddr == (address) InterpreterRuntime::slow_signature_handler) {
      CALL_VM_NOCHECK(handlerAddr =
        InterpreterRuntime::slow_signature_handler(thread, method, NULL,NULL));
      if (HAS_PENDING_EXCEPTION)
        goto unlock_unwind_and_return;
    }
    handler = \
      InterpreterRuntime::SignatureHandler::from_handlerAddr(handlerAddr);
  }

  // Get the native function entry point
  address function;
  function = method->native_function();
  assert(function != NULL, "should be set if signature handler is");

  // Build the argument list
  stack->overflow_check(handler->argument_count() * 2, THREAD);
  if (HAS_PENDING_EXCEPTION)
    goto unlock_unwind_and_return;

  void **arguments;
  void *mirror; {
    arguments =
      (void **) stack->alloc(handler->argument_count() * sizeof(void **));
    void **dst = arguments;

    void *env = thread->jni_environment();
    *(dst++) = &env;

    if (method->is_static()) {
      istate->set_oop_temp(
        method->constants()->pool_holder()->java_mirror());
      mirror = istate->oop_temp_addr();
      *(dst++) = &mirror;
    }

    intptr_t *src = locals;
    for (int i = dst - arguments; i < handler->argument_count(); i++) {
      ffi_type *type = handler->argument_type(i);
      if (type == &ffi_type_pointer) {
        if (*src) {
          stack->push((intptr_t) src);
          *(dst++) = stack->sp();
        }
        else {
          *(dst++) = src;
        }
        src--;
      }
      else if (type->size == 4) {
        *(dst++) = src--;
      }
      else if (type->size == 8) {
        src--;
        *(dst++) = src--;
      }
      else {
        ShouldNotReachHere();
      }
    }
  }

  // Set up the Java frame anchor
  thread->set_last_Java_frame();

  // Change the thread state to _thread_in_native
  ThreadStateTransition::transition_from_java(thread, _thread_in_native);

  // Make the call
  intptr_t result[4 - LogBytesPerWord];
  ffi_call(handler->cif(), (void (*)()) function, result, arguments);

  // Change the thread state back to _thread_in_Java and ensure it
  // is seen by the GC thread.
  // ThreadStateTransition::transition_from_native() cannot be used
  // here because it does not check for asynchronous exceptions.
  // We have to manage the transition ourself.
  thread->set_thread_state_fence(_thread_in_native_trans);

  // Handle safepoint operations, pending suspend requests,
  // and pending asynchronous exceptions.
  if (SafepointMechanism::should_process(thread) ||
      thread->has_special_condition_for_native_trans()) {
    JavaThread::check_special_condition_for_native_trans(thread);
    CHECK_UNHANDLED_OOPS_ONLY(thread->clear_unhandled_oops());
  }

  // Finally we can change the thread state to _thread_in_Java.
  thread->set_thread_state(_thread_in_Java);
  fixup_after_potential_safepoint();

  // Clear the frame anchor
  thread->reset_last_Java_frame();

  // If the result was an oop then unbox it and store it in
  // oop_temp where the garbage collector can see it before
  // we release the handle it might be protected by.
  if (handler->result_type() == &ffi_type_pointer) {
    if (result[0] == 0) {
      istate->set_oop_temp(NULL);
    } else {
      jobject handle = reinterpret_cast<jobject>(result[0]);
      istate->set_oop_temp(JNIHandles::resolve(handle));
    }
  }

  // Reset handle block
  thread->active_handles()->clear();

 unlock_unwind_and_return:

  // Unlock if necessary
  if (monitor) {
    BasicLock *lock = monitor->lock();
    markWord header = lock->displaced_header();
    oop rcvr = monitor->obj();
    monitor->set_obj(NULL);

    if (header.to_pointer() != NULL) {
      markWord old_header = markWord::encode(lock);
      if (rcvr->cas_set_mark(header, old_header) != old_header) {
        monitor->set_obj(rcvr);
        InterpreterRuntime::monitorexit(monitor);
      }
    }
  }

 unwind_and_return:

  // Unwind the current activation
  thread->pop_zero_frame();

  // Pop our parameters
  stack->set_sp(stack->sp() + method->size_of_parameters());

  // Push our result
  if (!HAS_PENDING_EXCEPTION) {
    BasicType type = method->result_type();
    stack->set_sp(stack->sp() - type2size[type]);

    switch (type) {
    case T_VOID:
      break;

    case T_BOOLEAN:
#ifndef VM_LITTLE_ENDIAN
      result[0] <<= (BitsPerWord - BitsPerByte);
#endif
      SET_LOCALS_INT(*(jboolean *) result != 0, 0);
      break;

    case T_CHAR:
#ifndef VM_LITTLE_ENDIAN
      result[0] <<= (BitsPerWord - BitsPerShort);
#endif
      SET_LOCALS_INT(*(jchar *) result, 0);
      break;

    case T_BYTE:
#ifndef VM_LITTLE_ENDIAN
      result[0] <<= (BitsPerWord - BitsPerByte);
#endif
      SET_LOCALS_INT(*(jbyte *) result, 0);
      break;

    case T_SHORT:
#ifndef VM_LITTLE_ENDIAN
      result[0] <<= (BitsPerWord - BitsPerShort);
#endif
      SET_LOCALS_INT(*(jshort *) result, 0);
      break;

    case T_INT:
#ifndef VM_LITTLE_ENDIAN
      result[0] <<= (BitsPerWord - BitsPerInt);
#endif
      SET_LOCALS_INT(*(jint *) result, 0);
      break;

    case T_LONG:
      SET_LOCALS_LONG(*(jlong *) result, 0);
      break;

    case T_FLOAT:
      SET_LOCALS_FLOAT(*(jfloat *) result, 0);
      break;

    case T_DOUBLE:
      SET_LOCALS_DOUBLE(*(jdouble *) result, 0);
      break;

    case T_OBJECT:
    case T_ARRAY:
      SET_LOCALS_OBJECT(istate->oop_temp(), 0);
      break;

    default:
      ShouldNotReachHere();
    }
  }

  // No deoptimized frames on the stack
  return 0;
}

int ZeroInterpreter::getter_entry(Method* method, intptr_t UNUSED, TRAPS) {
  JavaThread* thread = THREAD;
  // Drop into the slow path if we need a safepoint check
  if (SafepointMechanism::should_process(thread)) {
    return normal_entry(method, 0, THREAD);
  }

  // Read the field index from the bytecode:
  //  0:  aload_0
  //  1:  getfield
  //  2:    index
  //  3:    index
  //  4:  return
  //
  // NB this is not raw bytecode: index is in machine order

  assert(method->is_getter(), "Expect the particular bytecode shape");
  u1* code = method->code_base();
  u2 index = Bytes::get_native_u2(&code[2]);

  // Get the entry from the constant pool cache, and drop into
  // the slow path if it has not been resolved
  ConstantPoolCache* cache = method->constants()->cache();
  ConstantPoolCacheEntry* entry = cache->entry_at(index);
  if (!entry->is_resolved(Bytecodes::_getfield)) {
    return normal_entry(method, 0, THREAD);
  }

  ZeroStack* stack = thread->zero_stack();
  intptr_t* topOfStack = stack->sp();

  // Load the object pointer and drop into the slow path
  // if we have a NullPointerException
  oop object = STACK_OBJECT(0);
  if (object == NULL) {
    return normal_entry(method, 0, THREAD);
  }

  // If needed, allocate additional slot on stack: we already have one
  // for receiver, and double/long need another one.
  switch (entry->flag_state()) {
    case ltos:
    case dtos:
      stack->overflow_check(1, CHECK_0);
      stack->alloc(wordSize);
      topOfStack = stack->sp();
      break;
  }

  // Read the field to stack(0)
  int offset = entry->f2_as_index();
  if (entry->is_volatile()) {
    if (support_IRIW_for_not_multiple_copy_atomic_cpu) {
      OrderAccess::fence();
    }
    switch (entry->flag_state()) {
      case btos:
      case ztos: SET_STACK_INT(object->byte_field_acquire(offset),      0); break;
      case ctos: SET_STACK_INT(object->char_field_acquire(offset),      0); break;
      case stos: SET_STACK_INT(object->short_field_acquire(offset),     0); break;
      case itos: SET_STACK_INT(object->int_field_acquire(offset),       0); break;
      case ltos: SET_STACK_LONG(object->long_field_acquire(offset),     0); break;
      case ftos: SET_STACK_FLOAT(object->float_field_acquire(offset),   0); break;
      case dtos: SET_STACK_DOUBLE(object->double_field_acquire(offset), 0); break;
      case atos: SET_STACK_OBJECT(object->obj_field_acquire(offset),    0); break;
      default:
        ShouldNotReachHere();
    }
  } else {
    switch (entry->flag_state()) {
      case btos:
      case ztos: SET_STACK_INT(object->byte_field(offset),      0); break;
      case ctos: SET_STACK_INT(object->char_field(offset),      0); break;
      case stos: SET_STACK_INT(object->short_field(offset),     0); break;
      case itos: SET_STACK_INT(object->int_field(offset),       0); break;
      case ltos: SET_STACK_LONG(object->long_field(offset),     0); break;
      case ftos: SET_STACK_FLOAT(object->float_field(offset),   0); break;
      case dtos: SET_STACK_DOUBLE(object->double_field(offset), 0); break;
      case atos: SET_STACK_OBJECT(object->obj_field(offset),    0); break;
      default:
        ShouldNotReachHere();
    }
  }

  // No deoptimized frames on the stack
  return 0;
}

int ZeroInterpreter::setter_entry(Method* method, intptr_t UNUSED, TRAPS) {
  JavaThread* thread = THREAD;
  // Drop into the slow path if we need a safepoint check
  if (SafepointMechanism::should_process(thread)) {
    return normal_entry(method, 0, THREAD);
  }

  // Read the field index from the bytecode:
  //  0:  aload_0
  //  1:  *load_1
  //  2:  putfield
  //  3:    index
  //  4:    index
  //  5:  return
  //
  // NB this is not raw bytecode: index is in machine order

  assert(method->is_setter(), "Expect the particular bytecode shape");
  u1* code = method->code_base();
  u2 index = Bytes::get_native_u2(&code[3]);

  // Get the entry from the constant pool cache, and drop into
  // the slow path if it has not been resolved
  ConstantPoolCache* cache = method->constants()->cache();
  ConstantPoolCacheEntry* entry = cache->entry_at(index);
  if (!entry->is_resolved(Bytecodes::_putfield)) {
    return normal_entry(method, 0, THREAD);
  }

  ZeroStack* stack = thread->zero_stack();
  intptr_t* topOfStack = stack->sp();

  // Figure out where the receiver is. If there is a long/double
  // operand on stack top, then receiver is two slots down.
  oop object = NULL;
  switch (entry->flag_state()) {
    case ltos:
    case dtos:
      object = STACK_OBJECT(-2);
      break;
    default:
      object = STACK_OBJECT(-1);
      break;
  }

  // Load the receiver pointer and drop into the slow path
  // if we have a NullPointerException
  if (object == NULL) {
    return normal_entry(method, 0, THREAD);
  }

  // Store the stack(0) to field
  int offset = entry->f2_as_index();
  if (entry->is_volatile()) {
    switch (entry->flag_state()) {
      case btos: object->release_byte_field_put(offset,   STACK_INT(0));     break;
      case ztos: object->release_byte_field_put(offset,   STACK_INT(0) & 1); break; // only store LSB
      case ctos: object->release_char_field_put(offset,   STACK_INT(0));     break;
      case stos: object->release_short_field_put(offset,  STACK_INT(0));     break;
      case itos: object->release_int_field_put(offset,    STACK_INT(0));     break;
      case ltos: object->release_long_field_put(offset,   STACK_LONG(0));    break;
      case ftos: object->release_float_field_put(offset,  STACK_FLOAT(0));   break;
      case dtos: object->release_double_field_put(offset, STACK_DOUBLE(0));  break;
      case atos: object->release_obj_field_put(offset,    STACK_OBJECT(0));  break;
      default:
        ShouldNotReachHere();
    }
    OrderAccess::storeload();
  } else {
    switch (entry->flag_state()) {
      case btos: object->byte_field_put(offset,   STACK_INT(0));     break;
      case ztos: object->byte_field_put(offset,   STACK_INT(0) & 1); break; // only store LSB
      case ctos: object->char_field_put(offset,   STACK_INT(0));     break;
      case stos: object->short_field_put(offset,  STACK_INT(0));     break;
      case itos: object->int_field_put(offset,    STACK_INT(0));     break;
      case ltos: object->long_field_put(offset,   STACK_LONG(0));    break;
      case ftos: object->float_field_put(offset,  STACK_FLOAT(0));   break;
      case dtos: object->double_field_put(offset, STACK_DOUBLE(0));  break;
      case atos: object->obj_field_put(offset,    STACK_OBJECT(0));  break;
      default:
        ShouldNotReachHere();
    }
  }

  // Nothing is returned, pop out parameters
  stack->set_sp(stack->sp() + method->size_of_parameters());

  // No deoptimized frames on the stack
  return 0;
}

int ZeroInterpreter::empty_entry(Method* method, intptr_t UNUSED, TRAPS) {
  JavaThread *thread = THREAD;
  ZeroStack *stack = thread->zero_stack();

  // Drop into the slow path if we need a safepoint check
  if (SafepointMechanism::should_process(thread)) {
    return normal_entry(method, 0, THREAD);
  }

  // Pop our parameters
  stack->set_sp(stack->sp() + method->size_of_parameters());

  // No deoptimized frames on the stack
  return 0;
}

InterpreterFrame *InterpreterFrame::build(Method* const method, TRAPS) {
  JavaThread *thread = THREAD;
  ZeroStack *stack = thread->zero_stack();

  // Calculate the size of the frame we'll build, including
  // any adjustments to the caller's frame that we'll make.
  int extra_locals  = 0;
  int monitor_words = 0;
  int stack_words   = 0;

  if (!method->is_native()) {
    extra_locals = method->max_locals() - method->size_of_parameters();
    stack_words  = method->max_stack();
  }
  if (method->is_synchronized()) {
    monitor_words = frame::interpreter_frame_monitor_size();
  }
  stack->overflow_check(
    extra_locals + header_words + monitor_words + stack_words, CHECK_NULL);

  // Adjust the caller's stack frame to accomodate any additional
  // local variables we have contiguously with our parameters.
  for (int i = 0; i < extra_locals; i++)
    stack->push(0);

  intptr_t *locals;
  if (method->is_native())
    locals = stack->sp() + (method->size_of_parameters() - 1);
  else
    locals = stack->sp() + (method->max_locals() - 1);

  stack->push(0); // next_frame, filled in later
  intptr_t *fp = stack->sp();
  assert(fp - stack->sp() == next_frame_off, "should be");

  stack->push(INTERPRETER_FRAME);
  assert(fp - stack->sp() == frame_type_off, "should be");

  interpreterState istate =
    (interpreterState) stack->alloc(sizeof(BytecodeInterpreter));
  assert(fp - stack->sp() == istate_off, "should be");

  istate->set_locals(locals);
  istate->set_method(method);
  istate->set_mirror(method->method_holder()->java_mirror());
  istate->set_self_link(istate);
  istate->set_prev_link(NULL);
  istate->set_thread(thread);
  istate->set_bcp(method->is_native() ? NULL : method->code_base());
  istate->set_constants(method->constants()->cache());
  istate->set_msg(BytecodeInterpreter::method_entry);
  istate->set_oop_temp(NULL);
  istate->set_callee(NULL);

  istate->set_monitor_base((BasicObjectLock *) stack->sp());
  if (method->is_synchronized()) {
    BasicObjectLock *monitor =
      (BasicObjectLock *) stack->alloc(monitor_words * wordSize);
    oop object;
    if (method->is_static())
      object = method->constants()->pool_holder()->java_mirror();
    else
      object = cast_to_oop((void*)locals[0]);
    monitor->set_obj(object);
  }

  istate->set_stack_base(stack->sp());
  istate->set_stack(stack->sp() - 1);
  if (stack_words)
    stack->alloc(stack_words * wordSize);
  istate->set_stack_limit(stack->sp() - 1);

  return (InterpreterFrame *) fp;
}

InterpreterFrame *InterpreterFrame::build(int size, TRAPS) {
  ZeroStack *stack = THREAD->zero_stack();

  int size_in_words = size >> LogBytesPerWord;
  assert(size_in_words * wordSize == size, "unaligned");
  assert(size_in_words >= header_words, "too small");
  stack->overflow_check(size_in_words, CHECK_NULL);

  stack->push(0); // next_frame, filled in later
  intptr_t *fp = stack->sp();
  assert(fp - stack->sp() == next_frame_off, "should be");

  stack->push(INTERPRETER_FRAME);
  assert(fp - stack->sp() == frame_type_off, "should be");

  interpreterState istate =
    (interpreterState) stack->alloc(sizeof(BytecodeInterpreter));
  assert(fp - stack->sp() == istate_off, "should be");
  istate->set_self_link(NULL); // mark invalid

  stack->alloc((size_in_words - header_words) * wordSize);

  return (InterpreterFrame *) fp;
}

address ZeroInterpreter::return_entry(TosState state, int length, Bytecodes::Code code) {
  ShouldNotCallThis();
  return NULL;
}

address ZeroInterpreter::deopt_entry(TosState state, int length) {
  return NULL;
}

address ZeroInterpreter::remove_activation_preserving_args_entry() {
  // Do an uncommon trap type entry. c++ interpreter will know
  // to pop frame and preserve the args
  return Interpreter::deopt_entry(vtos, 0);
}

address ZeroInterpreter::remove_activation_early_entry(TosState state) {
  return NULL;
}

// Helper for figuring out if frames are interpreter frames

bool ZeroInterpreter::contains(address pc) {
  return false; // make frame::print_value_on work
}
