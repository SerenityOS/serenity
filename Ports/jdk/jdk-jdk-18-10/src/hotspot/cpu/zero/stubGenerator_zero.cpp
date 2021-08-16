/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2007, 2008, 2010, 2015 Red Hat, Inc.
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
#include "asm/assembler.inline.hpp"
#include "interpreter/interpreter.hpp"
#include "nativeInst_zero.hpp"
#include "oops/instanceOop.hpp"
#include "oops/method.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/oop.inline.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubCodeGenerator.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/thread.inline.hpp"
#include "stack_zero.inline.hpp"
#ifdef COMPILER2
#include "opto/runtime.hpp"
#endif

// For SafeFetch we need POSIX tls and setjmp
#include <setjmp.h>
#include <pthread.h>
static pthread_key_t g_jmpbuf_key;

// return the currently active jump buffer for this thread
//  - if there is any, NULL otherwise. Called from
//    zero signal handlers.
extern sigjmp_buf* get_jmp_buf_for_continuation() {
  return (sigjmp_buf*) pthread_getspecific(g_jmpbuf_key);
}

// Declaration and definition of StubGenerator (no .hpp file).
// For a more detailed description of the stub routine structure
// see the comment in stubRoutines.hpp

class StubGenerator: public StubCodeGenerator {
 private:
  // The call stub is used to call Java from C
  static void call_stub(
    JavaCallWrapper *call_wrapper,
    intptr_t*        result,
    BasicType        result_type,
    Method*          method,
    address          entry_point,
    intptr_t*        parameters,
    int              parameter_words,
    TRAPS) {
    JavaThread *thread = THREAD;
    ZeroStack *stack = thread->zero_stack();

    // Make sure we have no pending exceptions
    assert(!HAS_PENDING_EXCEPTION, "call_stub called with pending exception");

    // Set up the stack if necessary
    bool stack_needs_teardown = false;
    if (stack->needs_setup()) {
      size_t zero_stack_size = stack->suggest_size(thread);
      stack->setup(alloca(zero_stack_size), zero_stack_size);
      stack_needs_teardown = true;
    }

    // Allocate and initialize our frame
    EntryFrame *frame =
      EntryFrame::build(parameters, parameter_words, call_wrapper, THREAD);

    if (!HAS_PENDING_EXCEPTION) {
      // Push the frame
      thread->push_zero_frame(frame);

      // Make the call
      Interpreter::invoke_method(method, entry_point, THREAD);

      // Store the result
      if (!HAS_PENDING_EXCEPTION) {
        switch (result_type) {
        case T_INT:
          *(jint *) result = *(jint *) stack->sp();
          break;
        case T_LONG:
          *(jlong *) result = *(jlong *) stack->sp();
          break;
        case T_FLOAT:
          *(jfloat *) result = *(jfloat *) stack->sp();
          break;
        case T_DOUBLE:
          *(jdouble *) result = *(jdouble *) stack->sp();
          break;
        case T_OBJECT:
          *(oop *) result = *(oop *) stack->sp();
          break;
        default:
          ShouldNotReachHere();
        }
      }

      // Unwind the frame
      thread->pop_zero_frame();
    }

    // Tear down the stack if necessary
    if (stack_needs_teardown)
      stack->teardown();
  }

  // These stubs get called from some dumb test routine.
  // I'll write them properly when they're called from
  // something that's actually doing something.
  static void fake_arraycopy_stub(address src, address dst, int count) {
    assert(count == 0, "huh?");
  }

  void generate_arraycopy_stubs() {
    // Call the conjoint generation methods immediately after
    // the disjoint ones so that short branches from the former
    // to the latter can be generated.
    StubRoutines::_jbyte_disjoint_arraycopy  = (address) fake_arraycopy_stub;
    StubRoutines::_jbyte_arraycopy           = (address) fake_arraycopy_stub;

    StubRoutines::_jshort_disjoint_arraycopy = (address) fake_arraycopy_stub;
    StubRoutines::_jshort_arraycopy          = (address) fake_arraycopy_stub;

    StubRoutines::_jint_disjoint_arraycopy   = (address) fake_arraycopy_stub;
    StubRoutines::_jint_arraycopy            = (address) fake_arraycopy_stub;

    StubRoutines::_jlong_disjoint_arraycopy  = (address) fake_arraycopy_stub;
    StubRoutines::_jlong_arraycopy           = (address) fake_arraycopy_stub;

    StubRoutines::_oop_disjoint_arraycopy    = ShouldNotCallThisStub();
    StubRoutines::_oop_arraycopy             = ShouldNotCallThisStub();

    StubRoutines::_checkcast_arraycopy       = ShouldNotCallThisStub();
    StubRoutines::_generic_arraycopy         = ShouldNotCallThisStub();

    // Shared code tests for "NULL" to discover the stub is not generated.
    StubRoutines::_unsafe_arraycopy          = NULL;

    // We don't generate specialized code for HeapWord-aligned source
    // arrays, so just use the code we've already generated
    StubRoutines::_arrayof_jbyte_disjoint_arraycopy =
      StubRoutines::_jbyte_disjoint_arraycopy;
    StubRoutines::_arrayof_jbyte_arraycopy =
      StubRoutines::_jbyte_arraycopy;

    StubRoutines::_arrayof_jshort_disjoint_arraycopy =
      StubRoutines::_jshort_disjoint_arraycopy;
    StubRoutines::_arrayof_jshort_arraycopy =
      StubRoutines::_jshort_arraycopy;

    StubRoutines::_arrayof_jint_disjoint_arraycopy =
      StubRoutines::_jint_disjoint_arraycopy;
    StubRoutines::_arrayof_jint_arraycopy =
      StubRoutines::_jint_arraycopy;

    StubRoutines::_arrayof_jlong_disjoint_arraycopy =
      StubRoutines::_jlong_disjoint_arraycopy;
    StubRoutines::_arrayof_jlong_arraycopy =
      StubRoutines::_jlong_arraycopy;

    StubRoutines::_arrayof_oop_disjoint_arraycopy =
      StubRoutines::_oop_disjoint_arraycopy;
    StubRoutines::_arrayof_oop_arraycopy =
      StubRoutines::_oop_arraycopy;
  }

  static int SafeFetch32(int *adr, int errValue) {

    // set up a jump buffer; anchor the pointer to the jump buffer in tls; then
    // do the pointer access. If pointer is invalid, we crash; in signal
    // handler, we retrieve pointer to jmp buffer from tls, and jump back.
    //
    // Note: the jump buffer itself - which can get pretty large depending on
    // the architecture - lives on the stack and that is fine, because we will
    // not rewind the stack: either we crash, in which case signal handler
    // frame is below us, or we don't crash, in which case it does not matter.
    sigjmp_buf jb;
    if (sigsetjmp(jb, 1)) {
      // we crashed. clean up tls and return default value.
      pthread_setspecific(g_jmpbuf_key, NULL);
      return errValue;
    } else {
      // preparation phase
      pthread_setspecific(g_jmpbuf_key, &jb);
    }

    int value = errValue;
    value = *adr;

    // all went well. clean tls.
    pthread_setspecific(g_jmpbuf_key, NULL);

    return value;
  }

  static intptr_t SafeFetchN(intptr_t *adr, intptr_t errValue) {

    sigjmp_buf jb;
    if (sigsetjmp(jb, 1)) {
      // we crashed. clean up tls and return default value.
      pthread_setspecific(g_jmpbuf_key, NULL);
      return errValue;
    } else {
      // preparation phase
      pthread_setspecific(g_jmpbuf_key, &jb);
    }

    intptr_t value = errValue;
    value = *adr;

    // all went well. clean tls.
    pthread_setspecific(g_jmpbuf_key, NULL);

    return value;

  }

  void generate_initial() {
    // Generates all stubs and initializes the entry points

    // entry points that exist in all platforms Note: This is code
    // that could be shared among different platforms - however the
    // benefit seems to be smaller than the disadvantage of having a
    // much more complicated generator structure. See also comment in
    // stubRoutines.hpp.

    StubRoutines::_forward_exception_entry   = ShouldNotCallThisStub();
    StubRoutines::_call_stub_entry           = (address) call_stub;
    StubRoutines::_catch_exception_entry     = ShouldNotCallThisStub();

    // atomic calls
    StubRoutines::_atomic_xchg_entry         = ShouldNotCallThisStub();
    StubRoutines::_atomic_xchg_long_entry    = ShouldNotCallThisStub();
    StubRoutines::_atomic_cmpxchg_entry      = ShouldNotCallThisStub();
    StubRoutines::_atomic_cmpxchg_byte_entry = ShouldNotCallThisStub();
    StubRoutines::_atomic_cmpxchg_long_entry = ShouldNotCallThisStub();
    StubRoutines::_atomic_add_entry          = ShouldNotCallThisStub();
    StubRoutines::_atomic_add_long_entry     = ShouldNotCallThisStub();
    StubRoutines::_fence_entry               = ShouldNotCallThisStub();
  }

  void generate_all() {
    // Generates all stubs and initializes the entry points

    // These entry points require SharedInfo::stack0 to be set up in
    // non-core builds and need to be relocatable, so they each
    // fabricate a RuntimeStub internally.
    StubRoutines::_throw_AbstractMethodError_entry =
      ShouldNotCallThisStub();

    StubRoutines::_throw_NullPointerException_at_call_entry =
      ShouldNotCallThisStub();

    StubRoutines::_throw_StackOverflowError_entry =
      ShouldNotCallThisStub();

    // support for verify_oop (must happen after universe_init)
    StubRoutines::_verify_oop_subroutine_entry =
      ShouldNotCallThisStub();

    // arraycopy stubs used by compilers
    generate_arraycopy_stubs();

    // Safefetch stubs.
    pthread_key_create(&g_jmpbuf_key, NULL);
    StubRoutines::_safefetch32_entry = CAST_FROM_FN_PTR(address, StubGenerator::SafeFetch32);
    StubRoutines::_safefetch32_fault_pc = NULL;
    StubRoutines::_safefetch32_continuation_pc = NULL;

    StubRoutines::_safefetchN_entry = CAST_FROM_FN_PTR(address, StubGenerator::SafeFetchN);
    StubRoutines::_safefetchN_fault_pc = NULL;
    StubRoutines::_safefetchN_continuation_pc = NULL;
  }

 public:
  StubGenerator(CodeBuffer* code, bool all) : StubCodeGenerator(code) {
    if (all) {
      generate_all();
    } else {
      generate_initial();
    }
  }
};

void StubGenerator_generate(CodeBuffer* code, bool all) {
  StubGenerator g(code, all);
}

EntryFrame *EntryFrame::build(const intptr_t*  parameters,
                              int              parameter_words,
                              JavaCallWrapper* call_wrapper,
                              TRAPS) {

  ZeroStack *stack = THREAD->zero_stack();
  stack->overflow_check(header_words + parameter_words, CHECK_NULL);

  stack->push(0); // next_frame, filled in later
  intptr_t *fp = stack->sp();
  assert(fp - stack->sp() == next_frame_off, "should be");

  stack->push(ENTRY_FRAME);
  assert(fp - stack->sp() == frame_type_off, "should be");

  stack->push((intptr_t) call_wrapper);
  assert(fp - stack->sp() == call_wrapper_off, "should be");

  for (int i = 0; i < parameter_words; i++)
    stack->push(parameters[i]);

  return (EntryFrame *) fp;
}
