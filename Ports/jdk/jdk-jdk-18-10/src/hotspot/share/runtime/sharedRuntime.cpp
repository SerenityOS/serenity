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
#include "classfile/javaClasses.hpp"
#include "jvm.h"
#include "classfile/stringTable.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "code/codeCache.hpp"
#include "code/compiledIC.hpp"
#include "code/icBuffer.hpp"
#include "code/compiledMethod.inline.hpp"
#include "code/scopeDesc.hpp"
#include "code/vtableStubs.hpp"
#include "compiler/abstractCompiler.hpp"
#include "compiler/compileBroker.hpp"
#include "compiler/disassembler.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/gcLocker.inline.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "jfr/jfrEvents.hpp"
#include "logging/log.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/compiledICHolder.inline.hpp"
#include "oops/klass.hpp"
#include "oops/method.inline.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/oop.inline.hpp"
#include "prims/forte.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/methodHandles.hpp"
#include "prims/nativeLookup.hpp"
#include "runtime/atomic.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/init.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/java.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stackWatermarkSet.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/synchronizer.hpp"
#include "runtime/vframe.inline.hpp"
#include "runtime/vframeArray.hpp"
#include "runtime/vm_version.hpp"
#include "utilities/copy.hpp"
#include "utilities/dtrace.hpp"
#include "utilities/events.hpp"
#include "utilities/hashtable.inline.hpp"
#include "utilities/macros.hpp"
#include "utilities/xmlstream.hpp"
#ifdef COMPILER1
#include "c1/c1_Runtime1.hpp"
#endif

// Shared stub locations
RuntimeStub*        SharedRuntime::_wrong_method_blob;
RuntimeStub*        SharedRuntime::_wrong_method_abstract_blob;
RuntimeStub*        SharedRuntime::_ic_miss_blob;
RuntimeStub*        SharedRuntime::_resolve_opt_virtual_call_blob;
RuntimeStub*        SharedRuntime::_resolve_virtual_call_blob;
RuntimeStub*        SharedRuntime::_resolve_static_call_blob;
address             SharedRuntime::_resolve_static_call_entry;

DeoptimizationBlob* SharedRuntime::_deopt_blob;
SafepointBlob*      SharedRuntime::_polling_page_vectors_safepoint_handler_blob;
SafepointBlob*      SharedRuntime::_polling_page_safepoint_handler_blob;
SafepointBlob*      SharedRuntime::_polling_page_return_handler_blob;

#ifdef COMPILER2
UncommonTrapBlob*   SharedRuntime::_uncommon_trap_blob;
#endif // COMPILER2


//----------------------------generate_stubs-----------------------------------
void SharedRuntime::generate_stubs() {
  _wrong_method_blob                   = generate_resolve_blob(CAST_FROM_FN_PTR(address, SharedRuntime::handle_wrong_method),          "wrong_method_stub");
  _wrong_method_abstract_blob          = generate_resolve_blob(CAST_FROM_FN_PTR(address, SharedRuntime::handle_wrong_method_abstract), "wrong_method_abstract_stub");
  _ic_miss_blob                        = generate_resolve_blob(CAST_FROM_FN_PTR(address, SharedRuntime::handle_wrong_method_ic_miss),  "ic_miss_stub");
  _resolve_opt_virtual_call_blob       = generate_resolve_blob(CAST_FROM_FN_PTR(address, SharedRuntime::resolve_opt_virtual_call_C),   "resolve_opt_virtual_call");
  _resolve_virtual_call_blob           = generate_resolve_blob(CAST_FROM_FN_PTR(address, SharedRuntime::resolve_virtual_call_C),       "resolve_virtual_call");
  _resolve_static_call_blob            = generate_resolve_blob(CAST_FROM_FN_PTR(address, SharedRuntime::resolve_static_call_C),        "resolve_static_call");
  _resolve_static_call_entry           = _resolve_static_call_blob->entry_point();

  AdapterHandlerLibrary::initialize();

#if COMPILER2_OR_JVMCI
  // Vectors are generated only by C2 and JVMCI.
  bool support_wide = is_wide_vector(MaxVectorSize);
  if (support_wide) {
    _polling_page_vectors_safepoint_handler_blob = generate_handler_blob(CAST_FROM_FN_PTR(address, SafepointSynchronize::handle_polling_page_exception), POLL_AT_VECTOR_LOOP);
  }
#endif // COMPILER2_OR_JVMCI
  _polling_page_safepoint_handler_blob = generate_handler_blob(CAST_FROM_FN_PTR(address, SafepointSynchronize::handle_polling_page_exception), POLL_AT_LOOP);
  _polling_page_return_handler_blob    = generate_handler_blob(CAST_FROM_FN_PTR(address, SafepointSynchronize::handle_polling_page_exception), POLL_AT_RETURN);

  generate_deopt_blob();

#ifdef COMPILER2
  generate_uncommon_trap_blob();
#endif // COMPILER2
}

#include <math.h>

// Implementation of SharedRuntime

#ifndef PRODUCT
// For statistics
int SharedRuntime::_ic_miss_ctr = 0;
int SharedRuntime::_wrong_method_ctr = 0;
int SharedRuntime::_resolve_static_ctr = 0;
int SharedRuntime::_resolve_virtual_ctr = 0;
int SharedRuntime::_resolve_opt_virtual_ctr = 0;
int SharedRuntime::_implicit_null_throws = 0;
int SharedRuntime::_implicit_div0_throws = 0;

int64_t SharedRuntime::_nof_normal_calls = 0;
int64_t SharedRuntime::_nof_optimized_calls = 0;
int64_t SharedRuntime::_nof_inlined_calls = 0;
int64_t SharedRuntime::_nof_megamorphic_calls = 0;
int64_t SharedRuntime::_nof_static_calls = 0;
int64_t SharedRuntime::_nof_inlined_static_calls = 0;
int64_t SharedRuntime::_nof_interface_calls = 0;
int64_t SharedRuntime::_nof_optimized_interface_calls = 0;
int64_t SharedRuntime::_nof_inlined_interface_calls = 0;
int64_t SharedRuntime::_nof_megamorphic_interface_calls = 0;

int SharedRuntime::_new_instance_ctr=0;
int SharedRuntime::_new_array_ctr=0;
int SharedRuntime::_multi2_ctr=0;
int SharedRuntime::_multi3_ctr=0;
int SharedRuntime::_multi4_ctr=0;
int SharedRuntime::_multi5_ctr=0;
int SharedRuntime::_mon_enter_stub_ctr=0;
int SharedRuntime::_mon_exit_stub_ctr=0;
int SharedRuntime::_mon_enter_ctr=0;
int SharedRuntime::_mon_exit_ctr=0;
int SharedRuntime::_partial_subtype_ctr=0;
int SharedRuntime::_jbyte_array_copy_ctr=0;
int SharedRuntime::_jshort_array_copy_ctr=0;
int SharedRuntime::_jint_array_copy_ctr=0;
int SharedRuntime::_jlong_array_copy_ctr=0;
int SharedRuntime::_oop_array_copy_ctr=0;
int SharedRuntime::_checkcast_array_copy_ctr=0;
int SharedRuntime::_unsafe_array_copy_ctr=0;
int SharedRuntime::_generic_array_copy_ctr=0;
int SharedRuntime::_slow_array_copy_ctr=0;
int SharedRuntime::_find_handler_ctr=0;
int SharedRuntime::_rethrow_ctr=0;

int     SharedRuntime::_ICmiss_index                    = 0;
int     SharedRuntime::_ICmiss_count[SharedRuntime::maxICmiss_count];
address SharedRuntime::_ICmiss_at[SharedRuntime::maxICmiss_count];


void SharedRuntime::trace_ic_miss(address at) {
  for (int i = 0; i < _ICmiss_index; i++) {
    if (_ICmiss_at[i] == at) {
      _ICmiss_count[i]++;
      return;
    }
  }
  int index = _ICmiss_index++;
  if (_ICmiss_index >= maxICmiss_count) _ICmiss_index = maxICmiss_count - 1;
  _ICmiss_at[index] = at;
  _ICmiss_count[index] = 1;
}

void SharedRuntime::print_ic_miss_histogram() {
  if (ICMissHistogram) {
    tty->print_cr("IC Miss Histogram:");
    int tot_misses = 0;
    for (int i = 0; i < _ICmiss_index; i++) {
      tty->print_cr("  at: " INTPTR_FORMAT "  nof: %d", p2i(_ICmiss_at[i]), _ICmiss_count[i]);
      tot_misses += _ICmiss_count[i];
    }
    tty->print_cr("Total IC misses: %7d", tot_misses);
  }
}
#endif // PRODUCT


JRT_LEAF(jlong, SharedRuntime::lmul(jlong y, jlong x))
  return x * y;
JRT_END


JRT_LEAF(jlong, SharedRuntime::ldiv(jlong y, jlong x))
  if (x == min_jlong && y == CONST64(-1)) {
    return x;
  } else {
    return x / y;
  }
JRT_END


JRT_LEAF(jlong, SharedRuntime::lrem(jlong y, jlong x))
  if (x == min_jlong && y == CONST64(-1)) {
    return 0;
  } else {
    return x % y;
  }
JRT_END


const juint  float_sign_mask  = 0x7FFFFFFF;
const juint  float_infinity   = 0x7F800000;
const julong double_sign_mask = CONST64(0x7FFFFFFFFFFFFFFF);
const julong double_infinity  = CONST64(0x7FF0000000000000);

JRT_LEAF(jfloat, SharedRuntime::frem(jfloat  x, jfloat  y))
#ifdef _WIN64
  // 64-bit Windows on amd64 returns the wrong values for
  // infinity operands.
  union { jfloat f; juint i; } xbits, ybits;
  xbits.f = x;
  ybits.f = y;
  // x Mod Infinity == x unless x is infinity
  if (((xbits.i & float_sign_mask) != float_infinity) &&
       ((ybits.i & float_sign_mask) == float_infinity) ) {
    return x;
  }
  return ((jfloat)fmod_winx64((double)x, (double)y));
#else
  return ((jfloat)fmod((double)x,(double)y));
#endif
JRT_END


JRT_LEAF(jdouble, SharedRuntime::drem(jdouble x, jdouble y))
#ifdef _WIN64
  union { jdouble d; julong l; } xbits, ybits;
  xbits.d = x;
  ybits.d = y;
  // x Mod Infinity == x unless x is infinity
  if (((xbits.l & double_sign_mask) != double_infinity) &&
       ((ybits.l & double_sign_mask) == double_infinity) ) {
    return x;
  }
  return ((jdouble)fmod_winx64((double)x, (double)y));
#else
  return ((jdouble)fmod((double)x,(double)y));
#endif
JRT_END

#ifdef __SOFTFP__
JRT_LEAF(jfloat, SharedRuntime::fadd(jfloat x, jfloat y))
  return x + y;
JRT_END

JRT_LEAF(jfloat, SharedRuntime::fsub(jfloat x, jfloat y))
  return x - y;
JRT_END

JRT_LEAF(jfloat, SharedRuntime::fmul(jfloat x, jfloat y))
  return x * y;
JRT_END

JRT_LEAF(jfloat, SharedRuntime::fdiv(jfloat x, jfloat y))
  return x / y;
JRT_END

JRT_LEAF(jdouble, SharedRuntime::dadd(jdouble x, jdouble y))
  return x + y;
JRT_END

JRT_LEAF(jdouble, SharedRuntime::dsub(jdouble x, jdouble y))
  return x - y;
JRT_END

JRT_LEAF(jdouble, SharedRuntime::dmul(jdouble x, jdouble y))
  return x * y;
JRT_END

JRT_LEAF(jdouble, SharedRuntime::ddiv(jdouble x, jdouble y))
  return x / y;
JRT_END

JRT_LEAF(jfloat, SharedRuntime::i2f(jint x))
  return (jfloat)x;
JRT_END

JRT_LEAF(jdouble, SharedRuntime::i2d(jint x))
  return (jdouble)x;
JRT_END

JRT_LEAF(jdouble, SharedRuntime::f2d(jfloat x))
  return (jdouble)x;
JRT_END

JRT_LEAF(int,  SharedRuntime::fcmpl(float x, float y))
  return x>y ? 1 : (x==y ? 0 : -1);  /* x<y or is_nan*/
JRT_END

JRT_LEAF(int,  SharedRuntime::fcmpg(float x, float y))
  return x<y ? -1 : (x==y ? 0 : 1);  /* x>y or is_nan */
JRT_END

JRT_LEAF(int,  SharedRuntime::dcmpl(double x, double y))
  return x>y ? 1 : (x==y ? 0 : -1); /* x<y or is_nan */
JRT_END

JRT_LEAF(int,  SharedRuntime::dcmpg(double x, double y))
  return x<y ? -1 : (x==y ? 0 : 1);  /* x>y or is_nan */
JRT_END

// Functions to return the opposite of the aeabi functions for nan.
JRT_LEAF(int, SharedRuntime::unordered_fcmplt(float x, float y))
  return (x < y) ? 1 : ((g_isnan(x) || g_isnan(y)) ? 1 : 0);
JRT_END

JRT_LEAF(int, SharedRuntime::unordered_dcmplt(double x, double y))
  return (x < y) ? 1 : ((g_isnan(x) || g_isnan(y)) ? 1 : 0);
JRT_END

JRT_LEAF(int, SharedRuntime::unordered_fcmple(float x, float y))
  return (x <= y) ? 1 : ((g_isnan(x) || g_isnan(y)) ? 1 : 0);
JRT_END

JRT_LEAF(int, SharedRuntime::unordered_dcmple(double x, double y))
  return (x <= y) ? 1 : ((g_isnan(x) || g_isnan(y)) ? 1 : 0);
JRT_END

JRT_LEAF(int, SharedRuntime::unordered_fcmpge(float x, float y))
  return (x >= y) ? 1 : ((g_isnan(x) || g_isnan(y)) ? 1 : 0);
JRT_END

JRT_LEAF(int, SharedRuntime::unordered_dcmpge(double x, double y))
  return (x >= y) ? 1 : ((g_isnan(x) || g_isnan(y)) ? 1 : 0);
JRT_END

JRT_LEAF(int, SharedRuntime::unordered_fcmpgt(float x, float y))
  return (x > y) ? 1 : ((g_isnan(x) || g_isnan(y)) ? 1 : 0);
JRT_END

JRT_LEAF(int, SharedRuntime::unordered_dcmpgt(double x, double y))
  return (x > y) ? 1 : ((g_isnan(x) || g_isnan(y)) ? 1 : 0);
JRT_END

// Intrinsics make gcc generate code for these.
float  SharedRuntime::fneg(float f)   {
  return -f;
}

double SharedRuntime::dneg(double f)  {
  return -f;
}

#endif // __SOFTFP__

#if defined(__SOFTFP__) || defined(E500V2)
// Intrinsics make gcc generate code for these.
double SharedRuntime::dabs(double f)  {
  return (f <= (double)0.0) ? (double)0.0 - f : f;
}

#endif

#if defined(__SOFTFP__) || defined(PPC)
double SharedRuntime::dsqrt(double f) {
  return sqrt(f);
}
#endif

JRT_LEAF(jint, SharedRuntime::f2i(jfloat  x))
  if (g_isnan(x))
    return 0;
  if (x >= (jfloat) max_jint)
    return max_jint;
  if (x <= (jfloat) min_jint)
    return min_jint;
  return (jint) x;
JRT_END


JRT_LEAF(jlong, SharedRuntime::f2l(jfloat  x))
  if (g_isnan(x))
    return 0;
  if (x >= (jfloat) max_jlong)
    return max_jlong;
  if (x <= (jfloat) min_jlong)
    return min_jlong;
  return (jlong) x;
JRT_END


JRT_LEAF(jint, SharedRuntime::d2i(jdouble x))
  if (g_isnan(x))
    return 0;
  if (x >= (jdouble) max_jint)
    return max_jint;
  if (x <= (jdouble) min_jint)
    return min_jint;
  return (jint) x;
JRT_END


JRT_LEAF(jlong, SharedRuntime::d2l(jdouble x))
  if (g_isnan(x))
    return 0;
  if (x >= (jdouble) max_jlong)
    return max_jlong;
  if (x <= (jdouble) min_jlong)
    return min_jlong;
  return (jlong) x;
JRT_END


JRT_LEAF(jfloat, SharedRuntime::d2f(jdouble x))
  return (jfloat)x;
JRT_END


JRT_LEAF(jfloat, SharedRuntime::l2f(jlong x))
  return (jfloat)x;
JRT_END


JRT_LEAF(jdouble, SharedRuntime::l2d(jlong x))
  return (jdouble)x;
JRT_END

// Exception handling across interpreter/compiler boundaries
//
// exception_handler_for_return_address(...) returns the continuation address.
// The continuation address is the entry point of the exception handler of the
// previous frame depending on the return address.

address SharedRuntime::raw_exception_handler_for_return_address(JavaThread* current, address return_address) {
  // Note: This is called when we have unwound the frame of the callee that did
  // throw an exception. So far, no check has been performed by the StackWatermarkSet.
  // Notably, the stack is not walkable at this point, and hence the check must
  // be deferred until later. Specifically, any of the handlers returned here in
  // this function, will get dispatched to, and call deferred checks to
  // StackWatermarkSet::after_unwind at a point where the stack is walkable.
  assert(frame::verify_return_pc(return_address), "must be a return address: " INTPTR_FORMAT, p2i(return_address));
  assert(current->frames_to_pop_failed_realloc() == 0 || Interpreter::contains(return_address), "missed frames to pop?");

  // Reset method handle flag.
  current->set_is_method_handle_return(false);

#if INCLUDE_JVMCI
  // JVMCI's ExceptionHandlerStub expects the thread local exception PC to be clear
  // and other exception handler continuations do not read it
  current->set_exception_pc(NULL);
#endif // INCLUDE_JVMCI

  // The fastest case first
  CodeBlob* blob = CodeCache::find_blob(return_address);
  CompiledMethod* nm = (blob != NULL) ? blob->as_compiled_method_or_null() : NULL;
  if (nm != NULL) {
    // Set flag if return address is a method handle call site.
    current->set_is_method_handle_return(nm->is_method_handle_return(return_address));
    // native nmethods don't have exception handlers
    assert(!nm->is_native_method(), "no exception handler");
    assert(nm->header_begin() != nm->exception_begin(), "no exception handler");
    if (nm->is_deopt_pc(return_address)) {
      // If we come here because of a stack overflow, the stack may be
      // unguarded. Reguard the stack otherwise if we return to the
      // deopt blob and the stack bang causes a stack overflow we
      // crash.
      StackOverflow* overflow_state = current->stack_overflow_state();
      bool guard_pages_enabled = overflow_state->reguard_stack_if_needed();
      if (overflow_state->reserved_stack_activation() != current->stack_base()) {
        overflow_state->set_reserved_stack_activation(current->stack_base());
      }
      assert(guard_pages_enabled, "stack banging in deopt blob may cause crash");
      // The deferred StackWatermarkSet::after_unwind check will be performed in
      // Deoptimization::fetch_unroll_info (with exec_mode == Unpack_exception)
      return SharedRuntime::deopt_blob()->unpack_with_exception();
    } else {
      // The deferred StackWatermarkSet::after_unwind check will be performed in
      // * OptoRuntime::rethrow_C for C2 code
      // * exception_handler_for_pc_helper via Runtime1::handle_exception_from_callee_id for C1 code
      return nm->exception_begin();
    }
  }

  // Entry code
  if (StubRoutines::returns_to_call_stub(return_address)) {
    // The deferred StackWatermarkSet::after_unwind check will be performed in
    // JavaCallWrapper::~JavaCallWrapper
    return StubRoutines::catch_exception_entry();
  }
  if (blob != NULL && blob->is_optimized_entry_blob()) {
    return ((OptimizedEntryBlob*)blob)->exception_handler();
  }
  // Interpreted code
  if (Interpreter::contains(return_address)) {
    // The deferred StackWatermarkSet::after_unwind check will be performed in
    // InterpreterRuntime::exception_handler_for_exception
    return Interpreter::rethrow_exception_entry();
  }

  guarantee(blob == NULL || !blob->is_runtime_stub(), "caller should have skipped stub");
  guarantee(!VtableStubs::contains(return_address), "NULL exceptions in vtables should have been handled already!");

#ifndef PRODUCT
  { ResourceMark rm;
    tty->print_cr("No exception handler found for exception at " INTPTR_FORMAT " - potential problems:", p2i(return_address));
    tty->print_cr("a) exception happened in (new?) code stubs/buffers that is not handled here");
    tty->print_cr("b) other problem");
  }
#endif // PRODUCT

  ShouldNotReachHere();
  return NULL;
}


JRT_LEAF(address, SharedRuntime::exception_handler_for_return_address(JavaThread* current, address return_address))
  return raw_exception_handler_for_return_address(current, return_address);
JRT_END


address SharedRuntime::get_poll_stub(address pc) {
  address stub;
  // Look up the code blob
  CodeBlob *cb = CodeCache::find_blob(pc);

  // Should be an nmethod
  guarantee(cb != NULL && cb->is_compiled(), "safepoint polling: pc must refer to an nmethod");

  // Look up the relocation information
  assert(((CompiledMethod*)cb)->is_at_poll_or_poll_return(pc),
    "safepoint polling: type must be poll");

#ifdef ASSERT
  if (!((NativeInstruction*)pc)->is_safepoint_poll()) {
    tty->print_cr("bad pc: " PTR_FORMAT, p2i(pc));
    Disassembler::decode(cb);
    fatal("Only polling locations are used for safepoint");
  }
#endif

  bool at_poll_return = ((CompiledMethod*)cb)->is_at_poll_return(pc);
  bool has_wide_vectors = ((CompiledMethod*)cb)->has_wide_vectors();
  if (at_poll_return) {
    assert(SharedRuntime::polling_page_return_handler_blob() != NULL,
           "polling page return stub not created yet");
    stub = SharedRuntime::polling_page_return_handler_blob()->entry_point();
  } else if (has_wide_vectors) {
    assert(SharedRuntime::polling_page_vectors_safepoint_handler_blob() != NULL,
           "polling page vectors safepoint stub not created yet");
    stub = SharedRuntime::polling_page_vectors_safepoint_handler_blob()->entry_point();
  } else {
    assert(SharedRuntime::polling_page_safepoint_handler_blob() != NULL,
           "polling page safepoint stub not created yet");
    stub = SharedRuntime::polling_page_safepoint_handler_blob()->entry_point();
  }
  log_debug(safepoint)("... found polling page %s exception at pc = "
                       INTPTR_FORMAT ", stub =" INTPTR_FORMAT,
                       at_poll_return ? "return" : "loop",
                       (intptr_t)pc, (intptr_t)stub);
  return stub;
}


oop SharedRuntime::retrieve_receiver( Symbol* sig, frame caller ) {
  assert(caller.is_interpreted_frame(), "");
  int args_size = ArgumentSizeComputer(sig).size() + 1;
  assert(args_size <= caller.interpreter_frame_expression_stack_size(), "receiver must be on interpreter stack");
  oop result = cast_to_oop(*caller.interpreter_frame_tos_at(args_size - 1));
  assert(Universe::heap()->is_in(result) && oopDesc::is_oop(result), "receiver must be an oop");
  return result;
}


void SharedRuntime::throw_and_post_jvmti_exception(JavaThread* current, Handle h_exception) {
  if (JvmtiExport::can_post_on_exceptions()) {
    vframeStream vfst(current, true);
    methodHandle method = methodHandle(current, vfst.method());
    address bcp = method()->bcp_from(vfst.bci());
    JvmtiExport::post_exception_throw(current, method(), bcp, h_exception());
  }

#if INCLUDE_JVMCI
  if (EnableJVMCI && UseJVMCICompiler) {
    vframeStream vfst(current, true);
    methodHandle method = methodHandle(current, vfst.method());
    int bci = vfst.bci();
    MethodData* trap_mdo = method->method_data();
    if (trap_mdo != NULL) {
      // Set exception_seen if the exceptional bytecode is an invoke
      Bytecode_invoke call = Bytecode_invoke_check(method, bci);
      if (call.is_valid()) {
        ResourceMark rm(current);
        ProfileData* pdata = trap_mdo->allocate_bci_to_data(bci, NULL);
        if (pdata != NULL && pdata->is_BitData()) {
          BitData* bit_data = (BitData*) pdata;
          bit_data->set_exception_seen();
        }
      }
    }
  }
#endif

  Exceptions::_throw(current, __FILE__, __LINE__, h_exception);
}

void SharedRuntime::throw_and_post_jvmti_exception(JavaThread* current, Symbol* name, const char *message) {
  Handle h_exception = Exceptions::new_exception(current, name, message);
  throw_and_post_jvmti_exception(current, h_exception);
}

// The interpreter code to call this tracing function is only
// called/generated when UL is on for redefine, class and has the right level
// and tags. Since obsolete methods are never compiled, we don't have
// to modify the compilers to generate calls to this function.
//
JRT_LEAF(int, SharedRuntime::rc_trace_method_entry(
    JavaThread* thread, Method* method))
  if (method->is_obsolete()) {
    // We are calling an obsolete method, but this is not necessarily
    // an error. Our method could have been redefined just after we
    // fetched the Method* from the constant pool.
    ResourceMark rm;
    log_trace(redefine, class, obsolete)("calling obsolete method '%s'", method->name_and_sig_as_C_string());
  }
  return 0;
JRT_END

// ret_pc points into caller; we are returning caller's exception handler
// for given exception
address SharedRuntime::compute_compiled_exc_handler(CompiledMethod* cm, address ret_pc, Handle& exception,
                                                    bool force_unwind, bool top_frame_only, bool& recursive_exception_occurred) {
  assert(cm != NULL, "must exist");
  ResourceMark rm;

#if INCLUDE_JVMCI
  if (cm->is_compiled_by_jvmci()) {
    // lookup exception handler for this pc
    int catch_pco = ret_pc - cm->code_begin();
    ExceptionHandlerTable table(cm);
    HandlerTableEntry *t = table.entry_for(catch_pco, -1, 0);
    if (t != NULL) {
      return cm->code_begin() + t->pco();
    } else {
      return Deoptimization::deoptimize_for_missing_exception_handler(cm);
    }
  }
#endif // INCLUDE_JVMCI

  nmethod* nm = cm->as_nmethod();
  ScopeDesc* sd = nm->scope_desc_at(ret_pc);
  // determine handler bci, if any
  EXCEPTION_MARK;

  int handler_bci = -1;
  int scope_depth = 0;
  if (!force_unwind) {
    int bci = sd->bci();
    bool recursive_exception = false;
    do {
      bool skip_scope_increment = false;
      // exception handler lookup
      Klass* ek = exception->klass();
      methodHandle mh(THREAD, sd->method());
      handler_bci = Method::fast_exception_handler_bci_for(mh, ek, bci, THREAD);
      if (HAS_PENDING_EXCEPTION) {
        recursive_exception = true;
        // We threw an exception while trying to find the exception handler.
        // Transfer the new exception to the exception handle which will
        // be set into thread local storage, and do another lookup for an
        // exception handler for this exception, this time starting at the
        // BCI of the exception handler which caused the exception to be
        // thrown (bugs 4307310 and 4546590). Set "exception" reference
        // argument to ensure that the correct exception is thrown (4870175).
        recursive_exception_occurred = true;
        exception = Handle(THREAD, PENDING_EXCEPTION);
        CLEAR_PENDING_EXCEPTION;
        if (handler_bci >= 0) {
          bci = handler_bci;
          handler_bci = -1;
          skip_scope_increment = true;
        }
      }
      else {
        recursive_exception = false;
      }
      if (!top_frame_only && handler_bci < 0 && !skip_scope_increment) {
        sd = sd->sender();
        if (sd != NULL) {
          bci = sd->bci();
        }
        ++scope_depth;
      }
    } while (recursive_exception || (!top_frame_only && handler_bci < 0 && sd != NULL));
  }

  // found handling method => lookup exception handler
  int catch_pco = ret_pc - nm->code_begin();

  ExceptionHandlerTable table(nm);
  HandlerTableEntry *t = table.entry_for(catch_pco, handler_bci, scope_depth);
  if (t == NULL && (nm->is_compiled_by_c1() || handler_bci != -1)) {
    // Allow abbreviated catch tables.  The idea is to allow a method
    // to materialize its exceptions without committing to the exact
    // routing of exceptions.  In particular this is needed for adding
    // a synthetic handler to unlock monitors when inlining
    // synchronized methods since the unlock path isn't represented in
    // the bytecodes.
    t = table.entry_for(catch_pco, -1, 0);
  }

#ifdef COMPILER1
  if (t == NULL && nm->is_compiled_by_c1()) {
    assert(nm->unwind_handler_begin() != NULL, "");
    return nm->unwind_handler_begin();
  }
#endif

  if (t == NULL) {
    ttyLocker ttyl;
    tty->print_cr("MISSING EXCEPTION HANDLER for pc " INTPTR_FORMAT " and handler bci %d", p2i(ret_pc), handler_bci);
    tty->print_cr("   Exception:");
    exception->print();
    tty->cr();
    tty->print_cr(" Compiled exception table :");
    table.print();
    nm->print_code();
    guarantee(false, "missing exception handler");
    return NULL;
  }

  return nm->code_begin() + t->pco();
}

JRT_ENTRY(void, SharedRuntime::throw_AbstractMethodError(JavaThread* current))
  // These errors occur only at call sites
  throw_and_post_jvmti_exception(current, vmSymbols::java_lang_AbstractMethodError());
JRT_END

JRT_ENTRY(void, SharedRuntime::throw_IncompatibleClassChangeError(JavaThread* current))
  // These errors occur only at call sites
  throw_and_post_jvmti_exception(current, vmSymbols::java_lang_IncompatibleClassChangeError(), "vtable stub");
JRT_END

JRT_ENTRY(void, SharedRuntime::throw_ArithmeticException(JavaThread* current))
  throw_and_post_jvmti_exception(current, vmSymbols::java_lang_ArithmeticException(), "/ by zero");
JRT_END

JRT_ENTRY(void, SharedRuntime::throw_NullPointerException(JavaThread* current))
  throw_and_post_jvmti_exception(current, vmSymbols::java_lang_NullPointerException(), NULL);
JRT_END

JRT_ENTRY(void, SharedRuntime::throw_NullPointerException_at_call(JavaThread* current))
  // This entry point is effectively only used for NullPointerExceptions which occur at inline
  // cache sites (when the callee activation is not yet set up) so we are at a call site
  throw_and_post_jvmti_exception(current, vmSymbols::java_lang_NullPointerException(), NULL);
JRT_END

JRT_ENTRY(void, SharedRuntime::throw_StackOverflowError(JavaThread* current))
  throw_StackOverflowError_common(current, false);
JRT_END

JRT_ENTRY(void, SharedRuntime::throw_delayed_StackOverflowError(JavaThread* current))
  throw_StackOverflowError_common(current, true);
JRT_END

void SharedRuntime::throw_StackOverflowError_common(JavaThread* current, bool delayed) {
  // We avoid using the normal exception construction in this case because
  // it performs an upcall to Java, and we're already out of stack space.
  JavaThread* THREAD = current; // For exception macros.
  Klass* k = vmClasses::StackOverflowError_klass();
  oop exception_oop = InstanceKlass::cast(k)->allocate_instance(CHECK);
  if (delayed) {
    java_lang_Throwable::set_message(exception_oop,
                                     Universe::delayed_stack_overflow_error_message());
  }
  Handle exception (current, exception_oop);
  if (StackTraceInThrowable) {
    java_lang_Throwable::fill_in_stack_trace(exception);
  }
  // Increment counter for hs_err file reporting
  Atomic::inc(&Exceptions::_stack_overflow_errors);
  throw_and_post_jvmti_exception(current, exception);
}

address SharedRuntime::continuation_for_implicit_exception(JavaThread* current,
                                                           address pc,
                                                           ImplicitExceptionKind exception_kind)
{
  address target_pc = NULL;

  if (Interpreter::contains(pc)) {
    switch (exception_kind) {
      case IMPLICIT_NULL:           return Interpreter::throw_NullPointerException_entry();
      case IMPLICIT_DIVIDE_BY_ZERO: return Interpreter::throw_ArithmeticException_entry();
      case STACK_OVERFLOW:          return Interpreter::throw_StackOverflowError_entry();
      default:                      ShouldNotReachHere();
    }
  } else {
    switch (exception_kind) {
      case STACK_OVERFLOW: {
        // Stack overflow only occurs upon frame setup; the callee is
        // going to be unwound. Dispatch to a shared runtime stub
        // which will cause the StackOverflowError to be fabricated
        // and processed.
        // Stack overflow should never occur during deoptimization:
        // the compiled method bangs the stack by as much as the
        // interpreter would need in case of a deoptimization. The
        // deoptimization blob and uncommon trap blob bang the stack
        // in a debug VM to verify the correctness of the compiled
        // method stack banging.
        assert(current->deopt_mark() == NULL, "no stack overflow from deopt blob/uncommon trap");
        Events::log_exception(current, "StackOverflowError at " INTPTR_FORMAT, p2i(pc));
        return StubRoutines::throw_StackOverflowError_entry();
      }

      case IMPLICIT_NULL: {
        if (VtableStubs::contains(pc)) {
          // We haven't yet entered the callee frame. Fabricate an
          // exception and begin dispatching it in the caller. Since
          // the caller was at a call site, it's safe to destroy all
          // caller-saved registers, as these entry points do.
          VtableStub* vt_stub = VtableStubs::stub_containing(pc);

          // If vt_stub is NULL, then return NULL to signal handler to report the SEGV error.
          if (vt_stub == NULL) return NULL;

          if (vt_stub->is_abstract_method_error(pc)) {
            assert(!vt_stub->is_vtable_stub(), "should never see AbstractMethodErrors from vtable-type VtableStubs");
            Events::log_exception(current, "AbstractMethodError at " INTPTR_FORMAT, p2i(pc));
            // Instead of throwing the abstract method error here directly, we re-resolve
            // and will throw the AbstractMethodError during resolve. As a result, we'll
            // get a more detailed error message.
            return SharedRuntime::get_handle_wrong_method_stub();
          } else {
            Events::log_exception(current, "NullPointerException at vtable entry " INTPTR_FORMAT, p2i(pc));
            // Assert that the signal comes from the expected location in stub code.
            assert(vt_stub->is_null_pointer_exception(pc),
                   "obtained signal from unexpected location in stub code");
            return StubRoutines::throw_NullPointerException_at_call_entry();
          }
        } else {
          CodeBlob* cb = CodeCache::find_blob(pc);

          // If code blob is NULL, then return NULL to signal handler to report the SEGV error.
          if (cb == NULL) return NULL;

          // Exception happened in CodeCache. Must be either:
          // 1. Inline-cache check in C2I handler blob,
          // 2. Inline-cache check in nmethod, or
          // 3. Implicit null exception in nmethod

          if (!cb->is_compiled()) {
            bool is_in_blob = cb->is_adapter_blob() || cb->is_method_handles_adapter_blob();
            if (!is_in_blob) {
              // Allow normal crash reporting to handle this
              return NULL;
            }
            Events::log_exception(current, "NullPointerException in code blob at " INTPTR_FORMAT, p2i(pc));
            // There is no handler here, so we will simply unwind.
            return StubRoutines::throw_NullPointerException_at_call_entry();
          }

          // Otherwise, it's a compiled method.  Consult its exception handlers.
          CompiledMethod* cm = (CompiledMethod*)cb;
          if (cm->inlinecache_check_contains(pc)) {
            // exception happened inside inline-cache check code
            // => the nmethod is not yet active (i.e., the frame
            // is not set up yet) => use return address pushed by
            // caller => don't push another return address
            Events::log_exception(current, "NullPointerException in IC check " INTPTR_FORMAT, p2i(pc));
            return StubRoutines::throw_NullPointerException_at_call_entry();
          }

          if (cm->method()->is_method_handle_intrinsic()) {
            // exception happened inside MH dispatch code, similar to a vtable stub
            Events::log_exception(current, "NullPointerException in MH adapter " INTPTR_FORMAT, p2i(pc));
            return StubRoutines::throw_NullPointerException_at_call_entry();
          }

#ifndef PRODUCT
          _implicit_null_throws++;
#endif
          target_pc = cm->continuation_for_implicit_null_exception(pc);
          // If there's an unexpected fault, target_pc might be NULL,
          // in which case we want to fall through into the normal
          // error handling code.
        }

        break; // fall through
      }


      case IMPLICIT_DIVIDE_BY_ZERO: {
        CompiledMethod* cm = CodeCache::find_compiled(pc);
        guarantee(cm != NULL, "must have containing compiled method for implicit division-by-zero exceptions");
#ifndef PRODUCT
        _implicit_div0_throws++;
#endif
        target_pc = cm->continuation_for_implicit_div0_exception(pc);
        // If there's an unexpected fault, target_pc might be NULL,
        // in which case we want to fall through into the normal
        // error handling code.
        break; // fall through
      }

      default: ShouldNotReachHere();
    }

    assert(exception_kind == IMPLICIT_NULL || exception_kind == IMPLICIT_DIVIDE_BY_ZERO, "wrong implicit exception kind");

    if (exception_kind == IMPLICIT_NULL) {
#ifndef PRODUCT
      // for AbortVMOnException flag
      Exceptions::debug_check_abort("java.lang.NullPointerException");
#endif //PRODUCT
      Events::log_exception(current, "Implicit null exception at " INTPTR_FORMAT " to " INTPTR_FORMAT, p2i(pc), p2i(target_pc));
    } else {
#ifndef PRODUCT
      // for AbortVMOnException flag
      Exceptions::debug_check_abort("java.lang.ArithmeticException");
#endif //PRODUCT
      Events::log_exception(current, "Implicit division by zero exception at " INTPTR_FORMAT " to " INTPTR_FORMAT, p2i(pc), p2i(target_pc));
    }
    return target_pc;
  }

  ShouldNotReachHere();
  return NULL;
}


/**
 * Throws an java/lang/UnsatisfiedLinkError.  The address of this method is
 * installed in the native function entry of all native Java methods before
 * they get linked to their actual native methods.
 *
 * \note
 * This method actually never gets called!  The reason is because
 * the interpreter's native entries call NativeLookup::lookup() which
 * throws the exception when the lookup fails.  The exception is then
 * caught and forwarded on the return from NativeLookup::lookup() call
 * before the call to the native function.  This might change in the future.
 */
JNI_ENTRY(void*, throw_unsatisfied_link_error(JNIEnv* env, ...))
{
  // We return a bad value here to make sure that the exception is
  // forwarded before we look at the return value.
  THROW_(vmSymbols::java_lang_UnsatisfiedLinkError(), (void*)badAddress);
}
JNI_END

address SharedRuntime::native_method_throw_unsatisfied_link_error_entry() {
  return CAST_FROM_FN_PTR(address, &throw_unsatisfied_link_error);
}

JRT_ENTRY_NO_ASYNC(void, SharedRuntime::register_finalizer(JavaThread* current, oopDesc* obj))
#if INCLUDE_JVMCI
  if (!obj->klass()->has_finalizer()) {
    return;
  }
#endif // INCLUDE_JVMCI
  assert(oopDesc::is_oop(obj), "must be a valid oop");
  assert(obj->klass()->has_finalizer(), "shouldn't be here otherwise");
  InstanceKlass::register_finalizer(instanceOop(obj), CHECK);
JRT_END

jlong SharedRuntime::get_java_tid(Thread* thread) {
  if (thread != NULL) {
    if (thread->is_Java_thread()) {
      oop obj = JavaThread::cast(thread)->threadObj();
      return (obj == NULL) ? 0 : java_lang_Thread::thread_id(obj);
    }
  }
  return 0;
}

/**
 * This function ought to be a void function, but cannot be because
 * it gets turned into a tail-call on sparc, which runs into dtrace bug
 * 6254741.  Once that is fixed we can remove the dummy return value.
 */
int SharedRuntime::dtrace_object_alloc(oopDesc* o, int size) {
  return dtrace_object_alloc_base(Thread::current(), o, size);
}

int SharedRuntime::dtrace_object_alloc_base(Thread* thread, oopDesc* o, int size) {
  assert(DTraceAllocProbes, "wrong call");
  Klass* klass = o->klass();
  Symbol* name = klass->name();
  HOTSPOT_OBJECT_ALLOC(
                   get_java_tid(thread),
                   (char *) name->bytes(), name->utf8_length(), size * HeapWordSize);
  return 0;
}

JRT_LEAF(int, SharedRuntime::dtrace_method_entry(
    JavaThread* current, Method* method))
  assert(DTraceMethodProbes, "wrong call");
  Symbol* kname = method->klass_name();
  Symbol* name = method->name();
  Symbol* sig = method->signature();
  HOTSPOT_METHOD_ENTRY(
      get_java_tid(current),
      (char *) kname->bytes(), kname->utf8_length(),
      (char *) name->bytes(), name->utf8_length(),
      (char *) sig->bytes(), sig->utf8_length());
  return 0;
JRT_END

JRT_LEAF(int, SharedRuntime::dtrace_method_exit(
    JavaThread* current, Method* method))
  assert(DTraceMethodProbes, "wrong call");
  Symbol* kname = method->klass_name();
  Symbol* name = method->name();
  Symbol* sig = method->signature();
  HOTSPOT_METHOD_RETURN(
      get_java_tid(current),
      (char *) kname->bytes(), kname->utf8_length(),
      (char *) name->bytes(), name->utf8_length(),
      (char *) sig->bytes(), sig->utf8_length());
  return 0;
JRT_END


// Finds receiver, CallInfo (i.e. receiver method), and calling bytecode)
// for a call current in progress, i.e., arguments has been pushed on stack
// put callee has not been invoked yet.  Used by: resolve virtual/static,
// vtable updates, etc.  Caller frame must be compiled.
Handle SharedRuntime::find_callee_info(Bytecodes::Code& bc, CallInfo& callinfo, TRAPS) {
  JavaThread* current = THREAD;
  ResourceMark rm(current);

  // last java frame on stack (which includes native call frames)
  vframeStream vfst(current, true);  // Do not skip and javaCalls

  return find_callee_info_helper(vfst, bc, callinfo, THREAD);
}

Method* SharedRuntime::extract_attached_method(vframeStream& vfst) {
  CompiledMethod* caller = vfst.nm();

  nmethodLocker caller_lock(caller);

  address pc = vfst.frame_pc();
  { // Get call instruction under lock because another thread may be busy patching it.
    CompiledICLocker ic_locker(caller);
    return caller->attached_method_before_pc(pc);
  }
  return NULL;
}

// Finds receiver, CallInfo (i.e. receiver method), and calling bytecode
// for a call current in progress, i.e., arguments has been pushed on stack
// but callee has not been invoked yet.  Caller frame must be compiled.
Handle SharedRuntime::find_callee_info_helper(vframeStream& vfst, Bytecodes::Code& bc,
                                              CallInfo& callinfo, TRAPS) {
  Handle receiver;
  Handle nullHandle;  // create a handy null handle for exception returns
  JavaThread* current = THREAD;

  assert(!vfst.at_end(), "Java frame must exist");

  // Find caller and bci from vframe
  methodHandle caller(current, vfst.method());
  int          bci   = vfst.bci();

  Bytecode_invoke bytecode(caller, bci);
  int bytecode_index = bytecode.index();
  bc = bytecode.invoke_code();

  methodHandle attached_method(current, extract_attached_method(vfst));
  if (attached_method.not_null()) {
    Method* callee = bytecode.static_target(CHECK_NH);
    vmIntrinsics::ID id = callee->intrinsic_id();
    // When VM replaces MH.invokeBasic/linkTo* call with a direct/virtual call,
    // it attaches statically resolved method to the call site.
    if (MethodHandles::is_signature_polymorphic(id) &&
        MethodHandles::is_signature_polymorphic_intrinsic(id)) {
      bc = MethodHandles::signature_polymorphic_intrinsic_bytecode(id);

      // Adjust invocation mode according to the attached method.
      switch (bc) {
        case Bytecodes::_invokevirtual:
          if (attached_method->method_holder()->is_interface()) {
            bc = Bytecodes::_invokeinterface;
          }
          break;
        case Bytecodes::_invokeinterface:
          if (!attached_method->method_holder()->is_interface()) {
            bc = Bytecodes::_invokevirtual;
          }
          break;
        case Bytecodes::_invokehandle:
          if (!MethodHandles::is_signature_polymorphic_method(attached_method())) {
            bc = attached_method->is_static() ? Bytecodes::_invokestatic
                                              : Bytecodes::_invokevirtual;
          }
          break;
        default:
          break;
      }
    }
  }

  assert(bc != Bytecodes::_illegal, "not initialized");

  bool has_receiver = bc != Bytecodes::_invokestatic &&
                      bc != Bytecodes::_invokedynamic &&
                      bc != Bytecodes::_invokehandle;

  // Find receiver for non-static call
  if (has_receiver) {
    // This register map must be update since we need to find the receiver for
    // compiled frames. The receiver might be in a register.
    RegisterMap reg_map2(current);
    frame stubFrame   = current->last_frame();
    // Caller-frame is a compiled frame
    frame callerFrame = stubFrame.sender(&reg_map2);

    if (attached_method.is_null()) {
      Method* callee = bytecode.static_target(CHECK_NH);
      if (callee == NULL) {
        THROW_(vmSymbols::java_lang_NoSuchMethodException(), nullHandle);
      }
    }

    // Retrieve from a compiled argument list
    receiver = Handle(current, callerFrame.retrieve_receiver(&reg_map2));

    if (receiver.is_null()) {
      THROW_(vmSymbols::java_lang_NullPointerException(), nullHandle);
    }
  }

  // Resolve method
  if (attached_method.not_null()) {
    // Parameterized by attached method.
    LinkResolver::resolve_invoke(callinfo, receiver, attached_method, bc, CHECK_NH);
  } else {
    // Parameterized by bytecode.
    constantPoolHandle constants(current, caller->constants());
    LinkResolver::resolve_invoke(callinfo, receiver, constants, bytecode_index, bc, CHECK_NH);
  }

#ifdef ASSERT
  // Check that the receiver klass is of the right subtype and that it is initialized for virtual calls
  if (has_receiver) {
    assert(receiver.not_null(), "should have thrown exception");
    Klass* receiver_klass = receiver->klass();
    Klass* rk = NULL;
    if (attached_method.not_null()) {
      // In case there's resolved method attached, use its holder during the check.
      rk = attached_method->method_holder();
    } else {
      // Klass is already loaded.
      constantPoolHandle constants(current, caller->constants());
      rk = constants->klass_ref_at(bytecode_index, CHECK_NH);
    }
    Klass* static_receiver_klass = rk;
    assert(receiver_klass->is_subtype_of(static_receiver_klass),
           "actual receiver must be subclass of static receiver klass");
    if (receiver_klass->is_instance_klass()) {
      if (InstanceKlass::cast(receiver_klass)->is_not_initialized()) {
        tty->print_cr("ERROR: Klass not yet initialized!!");
        receiver_klass->print();
      }
      assert(!InstanceKlass::cast(receiver_klass)->is_not_initialized(), "receiver_klass must be initialized");
    }
  }
#endif

  return receiver;
}

methodHandle SharedRuntime::find_callee_method(TRAPS) {
  JavaThread* current = THREAD;
  ResourceMark rm(current);
  // We need first to check if any Java activations (compiled, interpreted)
  // exist on the stack since last JavaCall.  If not, we need
  // to get the target method from the JavaCall wrapper.
  vframeStream vfst(current, true);  // Do not skip any javaCalls
  methodHandle callee_method;
  if (vfst.at_end()) {
    // No Java frames were found on stack since we did the JavaCall.
    // Hence the stack can only contain an entry_frame.  We need to
    // find the target method from the stub frame.
    RegisterMap reg_map(current, false);
    frame fr = current->last_frame();
    assert(fr.is_runtime_frame(), "must be a runtimeStub");
    fr = fr.sender(&reg_map);
    assert(fr.is_entry_frame(), "must be");
    // fr is now pointing to the entry frame.
    callee_method = methodHandle(current, fr.entry_frame_call_wrapper()->callee_method());
  } else {
    Bytecodes::Code bc;
    CallInfo callinfo;
    find_callee_info_helper(vfst, bc, callinfo, CHECK_(methodHandle()));
    callee_method = methodHandle(current, callinfo.selected_method());
  }
  assert(callee_method()->is_method(), "must be");
  return callee_method;
}

// Resolves a call.
methodHandle SharedRuntime::resolve_helper(bool is_virtual, bool is_optimized, TRAPS) {
  methodHandle callee_method;
  callee_method = resolve_sub_helper(is_virtual, is_optimized, THREAD);
  if (JvmtiExport::can_hotswap_or_post_breakpoint()) {
    int retry_count = 0;
    while (!HAS_PENDING_EXCEPTION && callee_method->is_old() &&
           callee_method->method_holder() != vmClasses::Object_klass()) {
      // If has a pending exception then there is no need to re-try to
      // resolve this method.
      // If the method has been redefined, we need to try again.
      // Hack: we have no way to update the vtables of arrays, so don't
      // require that java.lang.Object has been updated.

      // It is very unlikely that method is redefined more than 100 times
      // in the middle of resolve. If it is looping here more than 100 times
      // means then there could be a bug here.
      guarantee((retry_count++ < 100),
                "Could not resolve to latest version of redefined method");
      // method is redefined in the middle of resolve so re-try.
      callee_method = resolve_sub_helper(is_virtual, is_optimized, THREAD);
    }
  }
  return callee_method;
}

// This fails if resolution required refilling of IC stubs
bool SharedRuntime::resolve_sub_helper_internal(methodHandle callee_method, const frame& caller_frame,
                                                CompiledMethod* caller_nm, bool is_virtual, bool is_optimized,
                                                Handle receiver, CallInfo& call_info, Bytecodes::Code invoke_code, TRAPS) {
  StaticCallInfo static_call_info;
  CompiledICInfo virtual_call_info;

  // Make sure the callee nmethod does not get deoptimized and removed before
  // we are done patching the code.
  CompiledMethod* callee = callee_method->code();

  if (callee != NULL) {
    assert(callee->is_compiled(), "must be nmethod for patching");
  }

  if (callee != NULL && !callee->is_in_use()) {
    // Patch call site to C2I adapter if callee nmethod is deoptimized or unloaded.
    callee = NULL;
  }
  nmethodLocker nl_callee(callee);
#ifdef ASSERT
  address dest_entry_point = callee == NULL ? 0 : callee->entry_point(); // used below
#endif

  bool is_nmethod = caller_nm->is_nmethod();

  if (is_virtual) {
    assert(receiver.not_null() || invoke_code == Bytecodes::_invokehandle, "sanity check");
    bool static_bound = call_info.resolved_method()->can_be_statically_bound();
    Klass* klass = invoke_code == Bytecodes::_invokehandle ? NULL : receiver->klass();
    CompiledIC::compute_monomorphic_entry(callee_method, klass,
                     is_optimized, static_bound, is_nmethod, virtual_call_info,
                     CHECK_false);
  } else {
    // static call
    CompiledStaticCall::compute_entry(callee_method, is_nmethod, static_call_info);
  }

  // grab lock, check for deoptimization and potentially patch caller
  {
    CompiledICLocker ml(caller_nm);

    // Lock blocks for safepoint during which both nmethods can change state.

    // Now that we are ready to patch if the Method* was redefined then
    // don't update call site and let the caller retry.
    // Don't update call site if callee nmethod was unloaded or deoptimized.
    // Don't update call site if callee nmethod was replaced by an other nmethod
    // which may happen when multiply alive nmethod (tiered compilation)
    // will be supported.
    if (!callee_method->is_old() &&
        (callee == NULL || (callee->is_in_use() && callee_method->code() == callee))) {
      NoSafepointVerifier nsv;
#ifdef ASSERT
      // We must not try to patch to jump to an already unloaded method.
      if (dest_entry_point != 0) {
        CodeBlob* cb = CodeCache::find_blob(dest_entry_point);
        assert((cb != NULL) && cb->is_compiled() && (((CompiledMethod*)cb) == callee),
               "should not call unloaded nmethod");
      }
#endif
      if (is_virtual) {
        CompiledIC* inline_cache = CompiledIC_before(caller_nm, caller_frame.pc());
        if (inline_cache->is_clean()) {
          if (!inline_cache->set_to_monomorphic(virtual_call_info)) {
            return false;
          }
        }
      } else {
        if (VM_Version::supports_fast_class_init_checks() &&
            invoke_code == Bytecodes::_invokestatic &&
            callee_method->needs_clinit_barrier() &&
            callee != NULL && callee->is_compiled_by_jvmci()) {
          return true; // skip patching for JVMCI
        }
        CompiledStaticCall* ssc = caller_nm->compiledStaticCall_before(caller_frame.pc());
        if (ssc->is_clean()) ssc->set(static_call_info);
      }
    }
  } // unlock CompiledICLocker
  return true;
}

// Resolves a call.  The compilers generate code for calls that go here
// and are patched with the real destination of the call.
methodHandle SharedRuntime::resolve_sub_helper(bool is_virtual, bool is_optimized, TRAPS) {
  JavaThread* current = THREAD;
  ResourceMark rm(current);
  RegisterMap cbl_map(current, false);
  frame caller_frame = current->last_frame().sender(&cbl_map);

  CodeBlob* caller_cb = caller_frame.cb();
  guarantee(caller_cb != NULL && caller_cb->is_compiled(), "must be called from compiled method");
  CompiledMethod* caller_nm = caller_cb->as_compiled_method_or_null();

  // make sure caller is not getting deoptimized
  // and removed before we are done with it.
  // CLEANUP - with lazy deopt shouldn't need this lock
  nmethodLocker caller_lock(caller_nm);

  // determine call info & receiver
  // note: a) receiver is NULL for static calls
  //       b) an exception is thrown if receiver is NULL for non-static calls
  CallInfo call_info;
  Bytecodes::Code invoke_code = Bytecodes::_illegal;
  Handle receiver = find_callee_info(invoke_code, call_info, CHECK_(methodHandle()));
  methodHandle callee_method(current, call_info.selected_method());

  assert((!is_virtual && invoke_code == Bytecodes::_invokestatic ) ||
         (!is_virtual && invoke_code == Bytecodes::_invokespecial) ||
         (!is_virtual && invoke_code == Bytecodes::_invokehandle ) ||
         (!is_virtual && invoke_code == Bytecodes::_invokedynamic) ||
         ( is_virtual && invoke_code != Bytecodes::_invokestatic ), "inconsistent bytecode");

  assert(caller_nm->is_alive() && !caller_nm->is_unloading(), "It should be alive");

#ifndef PRODUCT
  // tracing/debugging/statistics
  int *addr = (is_optimized) ? (&_resolve_opt_virtual_ctr) :
                (is_virtual) ? (&_resolve_virtual_ctr) :
                               (&_resolve_static_ctr);
  Atomic::inc(addr);

  if (TraceCallFixup) {
    ResourceMark rm(current);
    tty->print("resolving %s%s (%s) call to",
               (is_optimized) ? "optimized " : "", (is_virtual) ? "virtual" : "static",
               Bytecodes::name(invoke_code));
    callee_method->print_short_name(tty);
    tty->print_cr(" at pc: " INTPTR_FORMAT " to code: " INTPTR_FORMAT,
                  p2i(caller_frame.pc()), p2i(callee_method->code()));
  }
#endif

  if (invoke_code == Bytecodes::_invokestatic) {
    assert(callee_method->method_holder()->is_initialized() ||
           callee_method->method_holder()->is_reentrant_initialization(current),
           "invalid class initialization state for invoke_static");
    if (!VM_Version::supports_fast_class_init_checks() && callee_method->needs_clinit_barrier()) {
      // In order to keep class initialization check, do not patch call
      // site for static call when the class is not fully initialized.
      // Proper check is enforced by call site re-resolution on every invocation.
      //
      // When fast class initialization checks are supported (VM_Version::supports_fast_class_init_checks() == true),
      // explicit class initialization check is put in nmethod entry (VEP).
      assert(callee_method->method_holder()->is_linked(), "must be");
      return callee_method;
    }
  }

  // JSR 292 key invariant:
  // If the resolved method is a MethodHandle invoke target, the call
  // site must be a MethodHandle call site, because the lambda form might tail-call
  // leaving the stack in a state unknown to either caller or callee
  // TODO detune for now but we might need it again
//  assert(!callee_method->is_compiled_lambda_form() ||
//         caller_nm->is_method_handle_return(caller_frame.pc()), "must be MH call site");

  // Compute entry points. This might require generation of C2I converter
  // frames, so we cannot be holding any locks here. Furthermore, the
  // computation of the entry points is independent of patching the call.  We
  // always return the entry-point, but we only patch the stub if the call has
  // not been deoptimized.  Return values: For a virtual call this is an
  // (cached_oop, destination address) pair. For a static call/optimized
  // virtual this is just a destination address.

  // Patching IC caches may fail if we run out if transition stubs.
  // We refill the ic stubs then and try again.
  for (;;) {
    ICRefillVerifier ic_refill_verifier;
    bool successful = resolve_sub_helper_internal(callee_method, caller_frame, caller_nm,
                                                  is_virtual, is_optimized, receiver,
                                                  call_info, invoke_code, CHECK_(methodHandle()));
    if (successful) {
      return callee_method;
    } else {
      InlineCacheBuffer::refill_ic_stubs();
    }
  }

}


// Inline caches exist only in compiled code
JRT_BLOCK_ENTRY(address, SharedRuntime::handle_wrong_method_ic_miss(JavaThread* current))
#ifdef ASSERT
  RegisterMap reg_map(current, false);
  frame stub_frame = current->last_frame();
  assert(stub_frame.is_runtime_frame(), "sanity check");
  frame caller_frame = stub_frame.sender(&reg_map);
  assert(!caller_frame.is_interpreted_frame() && !caller_frame.is_entry_frame() && !caller_frame.is_optimized_entry_frame(), "unexpected frame");
#endif /* ASSERT */

  methodHandle callee_method;
  JRT_BLOCK
    callee_method = SharedRuntime::handle_ic_miss_helper(CHECK_NULL);
    // Return Method* through TLS
    current->set_vm_result_2(callee_method());
  JRT_BLOCK_END
  // return compiled code entry point after potential safepoints
  assert(callee_method->verified_code_entry() != NULL, " Jump to zero!");
  return callee_method->verified_code_entry();
JRT_END


// Handle call site that has been made non-entrant
JRT_BLOCK_ENTRY(address, SharedRuntime::handle_wrong_method(JavaThread* current))
  // 6243940 We might end up in here if the callee is deoptimized
  // as we race to call it.  We don't want to take a safepoint if
  // the caller was interpreted because the caller frame will look
  // interpreted to the stack walkers and arguments are now
  // "compiled" so it is much better to make this transition
  // invisible to the stack walking code. The i2c path will
  // place the callee method in the callee_target. It is stashed
  // there because if we try and find the callee by normal means a
  // safepoint is possible and have trouble gc'ing the compiled args.
  RegisterMap reg_map(current, false);
  frame stub_frame = current->last_frame();
  assert(stub_frame.is_runtime_frame(), "sanity check");
  frame caller_frame = stub_frame.sender(&reg_map);

  if (caller_frame.is_interpreted_frame() ||
      caller_frame.is_entry_frame() ||
      caller_frame.is_optimized_entry_frame()) {
    Method* callee = current->callee_target();
    guarantee(callee != NULL && callee->is_method(), "bad handshake");
    current->set_vm_result_2(callee);
    current->set_callee_target(NULL);
    if (caller_frame.is_entry_frame() && VM_Version::supports_fast_class_init_checks()) {
      // Bypass class initialization checks in c2i when caller is in native.
      // JNI calls to static methods don't have class initialization checks.
      // Fast class initialization checks are present in c2i adapters and call into
      // SharedRuntime::handle_wrong_method() on the slow path.
      //
      // JVM upcalls may land here as well, but there's a proper check present in
      // LinkResolver::resolve_static_call (called from JavaCalls::call_static),
      // so bypassing it in c2i adapter is benign.
      return callee->get_c2i_no_clinit_check_entry();
    } else {
      return callee->get_c2i_entry();
    }
  }

  // Must be compiled to compiled path which is safe to stackwalk
  methodHandle callee_method;
  JRT_BLOCK
    // Force resolving of caller (if we called from compiled frame)
    callee_method = SharedRuntime::reresolve_call_site(CHECK_NULL);
    current->set_vm_result_2(callee_method());
  JRT_BLOCK_END
  // return compiled code entry point after potential safepoints
  assert(callee_method->verified_code_entry() != NULL, " Jump to zero!");
  return callee_method->verified_code_entry();
JRT_END

// Handle abstract method call
JRT_BLOCK_ENTRY(address, SharedRuntime::handle_wrong_method_abstract(JavaThread* current))
  // Verbose error message for AbstractMethodError.
  // Get the called method from the invoke bytecode.
  vframeStream vfst(current, true);
  assert(!vfst.at_end(), "Java frame must exist");
  methodHandle caller(current, vfst.method());
  Bytecode_invoke invoke(caller, vfst.bci());
  DEBUG_ONLY( invoke.verify(); )

  // Find the compiled caller frame.
  RegisterMap reg_map(current);
  frame stubFrame = current->last_frame();
  assert(stubFrame.is_runtime_frame(), "must be");
  frame callerFrame = stubFrame.sender(&reg_map);
  assert(callerFrame.is_compiled_frame(), "must be");

  // Install exception and return forward entry.
  address res = StubRoutines::throw_AbstractMethodError_entry();
  JRT_BLOCK
    methodHandle callee(current, invoke.static_target(current));
    if (!callee.is_null()) {
      oop recv = callerFrame.retrieve_receiver(&reg_map);
      Klass *recv_klass = (recv != NULL) ? recv->klass() : NULL;
      res = StubRoutines::forward_exception_entry();
      LinkResolver::throw_abstract_method_error(callee, recv_klass, CHECK_(res));
    }
  JRT_BLOCK_END
  return res;
JRT_END


// resolve a static call and patch code
JRT_BLOCK_ENTRY(address, SharedRuntime::resolve_static_call_C(JavaThread* current ))
  methodHandle callee_method;
  JRT_BLOCK
    callee_method = SharedRuntime::resolve_helper(false, false, CHECK_NULL);
    current->set_vm_result_2(callee_method());
  JRT_BLOCK_END
  // return compiled code entry point after potential safepoints
  assert(callee_method->verified_code_entry() != NULL, " Jump to zero!");
  return callee_method->verified_code_entry();
JRT_END


// resolve virtual call and update inline cache to monomorphic
JRT_BLOCK_ENTRY(address, SharedRuntime::resolve_virtual_call_C(JavaThread* current))
  methodHandle callee_method;
  JRT_BLOCK
    callee_method = SharedRuntime::resolve_helper(true, false, CHECK_NULL);
    current->set_vm_result_2(callee_method());
  JRT_BLOCK_END
  // return compiled code entry point after potential safepoints
  assert(callee_method->verified_code_entry() != NULL, " Jump to zero!");
  return callee_method->verified_code_entry();
JRT_END


// Resolve a virtual call that can be statically bound (e.g., always
// monomorphic, so it has no inline cache).  Patch code to resolved target.
JRT_BLOCK_ENTRY(address, SharedRuntime::resolve_opt_virtual_call_C(JavaThread* current))
  methodHandle callee_method;
  JRT_BLOCK
    callee_method = SharedRuntime::resolve_helper(true, true, CHECK_NULL);
    current->set_vm_result_2(callee_method());
  JRT_BLOCK_END
  // return compiled code entry point after potential safepoints
  assert(callee_method->verified_code_entry() != NULL, " Jump to zero!");
  return callee_method->verified_code_entry();
JRT_END

// The handle_ic_miss_helper_internal function returns false if it failed due
// to either running out of vtable stubs or ic stubs due to IC transitions
// to transitional states. The needs_ic_stub_refill value will be set if
// the failure was due to running out of IC stubs, in which case handle_ic_miss_helper
// refills the IC stubs and tries again.
bool SharedRuntime::handle_ic_miss_helper_internal(Handle receiver, CompiledMethod* caller_nm,
                                                   const frame& caller_frame, methodHandle callee_method,
                                                   Bytecodes::Code bc, CallInfo& call_info,
                                                   bool& needs_ic_stub_refill, TRAPS) {
  CompiledICLocker ml(caller_nm);
  CompiledIC* inline_cache = CompiledIC_before(caller_nm, caller_frame.pc());
  bool should_be_mono = false;
  if (inline_cache->is_optimized()) {
    if (TraceCallFixup) {
      ResourceMark rm(THREAD);
      tty->print("OPTIMIZED IC miss (%s) call to", Bytecodes::name(bc));
      callee_method->print_short_name(tty);
      tty->print_cr(" code: " INTPTR_FORMAT, p2i(callee_method->code()));
    }
    should_be_mono = true;
  } else if (inline_cache->is_icholder_call()) {
    CompiledICHolder* ic_oop = inline_cache->cached_icholder();
    if (ic_oop != NULL) {
      if (!ic_oop->is_loader_alive()) {
        // Deferred IC cleaning due to concurrent class unloading
        if (!inline_cache->set_to_clean()) {
          needs_ic_stub_refill = true;
          return false;
        }
      } else if (receiver()->klass() == ic_oop->holder_klass()) {
        // This isn't a real miss. We must have seen that compiled code
        // is now available and we want the call site converted to a
        // monomorphic compiled call site.
        // We can't assert for callee_method->code() != NULL because it
        // could have been deoptimized in the meantime
        if (TraceCallFixup) {
          ResourceMark rm(THREAD);
          tty->print("FALSE IC miss (%s) converting to compiled call to", Bytecodes::name(bc));
          callee_method->print_short_name(tty);
          tty->print_cr(" code: " INTPTR_FORMAT, p2i(callee_method->code()));
        }
        should_be_mono = true;
      }
    }
  }

  if (should_be_mono) {
    // We have a path that was monomorphic but was going interpreted
    // and now we have (or had) a compiled entry. We correct the IC
    // by using a new icBuffer.
    CompiledICInfo info;
    Klass* receiver_klass = receiver()->klass();
    inline_cache->compute_monomorphic_entry(callee_method,
                                            receiver_klass,
                                            inline_cache->is_optimized(),
                                            false, caller_nm->is_nmethod(),
                                            info, CHECK_false);
    if (!inline_cache->set_to_monomorphic(info)) {
      needs_ic_stub_refill = true;
      return false;
    }
  } else if (!inline_cache->is_megamorphic() && !inline_cache->is_clean()) {
    // Potential change to megamorphic

    bool successful = inline_cache->set_to_megamorphic(&call_info, bc, needs_ic_stub_refill, CHECK_false);
    if (needs_ic_stub_refill) {
      return false;
    }
    if (!successful) {
      if (!inline_cache->set_to_clean()) {
        needs_ic_stub_refill = true;
        return false;
      }
    }
  } else {
    // Either clean or megamorphic
  }
  return true;
}

methodHandle SharedRuntime::handle_ic_miss_helper(TRAPS) {
  JavaThread* current = THREAD;
  ResourceMark rm(current);
  CallInfo call_info;
  Bytecodes::Code bc;

  // receiver is NULL for static calls. An exception is thrown for NULL
  // receivers for non-static calls
  Handle receiver = find_callee_info(bc, call_info, CHECK_(methodHandle()));
  // Compiler1 can produce virtual call sites that can actually be statically bound
  // If we fell thru to below we would think that the site was going megamorphic
  // when in fact the site can never miss. Worse because we'd think it was megamorphic
  // we'd try and do a vtable dispatch however methods that can be statically bound
  // don't have vtable entries (vtable_index < 0) and we'd blow up. So we force a
  // reresolution of the  call site (as if we did a handle_wrong_method and not an
  // plain ic_miss) and the site will be converted to an optimized virtual call site
  // never to miss again. I don't believe C2 will produce code like this but if it
  // did this would still be the correct thing to do for it too, hence no ifdef.
  //
  if (call_info.resolved_method()->can_be_statically_bound()) {
    methodHandle callee_method = SharedRuntime::reresolve_call_site(CHECK_(methodHandle()));
    if (TraceCallFixup) {
      RegisterMap reg_map(current, false);
      frame caller_frame = current->last_frame().sender(&reg_map);
      ResourceMark rm(current);
      tty->print("converting IC miss to reresolve (%s) call to", Bytecodes::name(bc));
      callee_method->print_short_name(tty);
      tty->print_cr(" from pc: " INTPTR_FORMAT, p2i(caller_frame.pc()));
      tty->print_cr(" code: " INTPTR_FORMAT, p2i(callee_method->code()));
    }
    return callee_method;
  }

  methodHandle callee_method(current, call_info.selected_method());

#ifndef PRODUCT
  Atomic::inc(&_ic_miss_ctr);

  // Statistics & Tracing
  if (TraceCallFixup) {
    ResourceMark rm(current);
    tty->print("IC miss (%s) call to", Bytecodes::name(bc));
    callee_method->print_short_name(tty);
    tty->print_cr(" code: " INTPTR_FORMAT, p2i(callee_method->code()));
  }

  if (ICMissHistogram) {
    MutexLocker m(VMStatistic_lock);
    RegisterMap reg_map(current, false);
    frame f = current->last_frame().real_sender(&reg_map);// skip runtime stub
    // produce statistics under the lock
    trace_ic_miss(f.pc());
  }
#endif

  // install an event collector so that when a vtable stub is created the
  // profiler can be notified via a DYNAMIC_CODE_GENERATED event. The
  // event can't be posted when the stub is created as locks are held
  // - instead the event will be deferred until the event collector goes
  // out of scope.
  JvmtiDynamicCodeEventCollector event_collector;

  // Update inline cache to megamorphic. Skip update if we are called from interpreted.
  // Transitioning IC caches may require transition stubs. If we run out
  // of transition stubs, we have to drop locks and perform a safepoint
  // that refills them.
  RegisterMap reg_map(current, false);
  frame caller_frame = current->last_frame().sender(&reg_map);
  CodeBlob* cb = caller_frame.cb();
  CompiledMethod* caller_nm = cb->as_compiled_method();

  for (;;) {
    ICRefillVerifier ic_refill_verifier;
    bool needs_ic_stub_refill = false;
    bool successful = handle_ic_miss_helper_internal(receiver, caller_nm, caller_frame, callee_method,
                                                     bc, call_info, needs_ic_stub_refill, CHECK_(methodHandle()));
    if (successful || !needs_ic_stub_refill) {
      return callee_method;
    } else {
      InlineCacheBuffer::refill_ic_stubs();
    }
  }
}

static bool clear_ic_at_addr(CompiledMethod* caller_nm, address call_addr, bool is_static_call) {
  CompiledICLocker ml(caller_nm);
  if (is_static_call) {
    CompiledStaticCall* ssc = caller_nm->compiledStaticCall_at(call_addr);
    if (!ssc->is_clean()) {
      return ssc->set_to_clean();
    }
  } else {
    // compiled, dispatched call (which used to call an interpreted method)
    CompiledIC* inline_cache = CompiledIC_at(caller_nm, call_addr);
    if (!inline_cache->is_clean()) {
      return inline_cache->set_to_clean();
    }
  }
  return true;
}

//
// Resets a call-site in compiled code so it will get resolved again.
// This routines handles both virtual call sites, optimized virtual call
// sites, and static call sites. Typically used to change a call sites
// destination from compiled to interpreted.
//
methodHandle SharedRuntime::reresolve_call_site(TRAPS) {
  JavaThread* current = THREAD;
  ResourceMark rm(current);
  RegisterMap reg_map(current, false);
  frame stub_frame = current->last_frame();
  assert(stub_frame.is_runtime_frame(), "must be a runtimeStub");
  frame caller = stub_frame.sender(&reg_map);

  // Do nothing if the frame isn't a live compiled frame.
  // nmethod could be deoptimized by the time we get here
  // so no update to the caller is needed.

  if (caller.is_compiled_frame() && !caller.is_deoptimized_frame()) {

    address pc = caller.pc();

    // Check for static or virtual call
    bool is_static_call = false;
    CompiledMethod* caller_nm = CodeCache::find_compiled(pc);

    // Default call_addr is the location of the "basic" call.
    // Determine the address of the call we a reresolving. With
    // Inline Caches we will always find a recognizable call.
    // With Inline Caches disabled we may or may not find a
    // recognizable call. We will always find a call for static
    // calls and for optimized virtual calls. For vanilla virtual
    // calls it depends on the state of the UseInlineCaches switch.
    //
    // With Inline Caches disabled we can get here for a virtual call
    // for two reasons:
    //   1 - calling an abstract method. The vtable for abstract methods
    //       will run us thru handle_wrong_method and we will eventually
    //       end up in the interpreter to throw the ame.
    //   2 - a racing deoptimization. We could be doing a vanilla vtable
    //       call and between the time we fetch the entry address and
    //       we jump to it the target gets deoptimized. Similar to 1
    //       we will wind up in the interprter (thru a c2i with c2).
    //
    address call_addr = NULL;
    {
      // Get call instruction under lock because another thread may be
      // busy patching it.
      CompiledICLocker ml(caller_nm);
      // Location of call instruction
      call_addr = caller_nm->call_instruction_address(pc);
    }
    // Make sure nmethod doesn't get deoptimized and removed until
    // this is done with it.
    // CLEANUP - with lazy deopt shouldn't need this lock
    nmethodLocker nmlock(caller_nm);

    if (call_addr != NULL) {
      RelocIterator iter(caller_nm, call_addr, call_addr+1);
      int ret = iter.next(); // Get item
      if (ret) {
        assert(iter.addr() == call_addr, "must find call");
        if (iter.type() == relocInfo::static_call_type) {
          is_static_call = true;
        } else {
          assert(iter.type() == relocInfo::virtual_call_type ||
                 iter.type() == relocInfo::opt_virtual_call_type
                , "unexpected relocInfo. type");
        }
      } else {
        assert(!UseInlineCaches, "relocation info. must exist for this address");
      }

      // Cleaning the inline cache will force a new resolve. This is more robust
      // than directly setting it to the new destination, since resolving of calls
      // is always done through the same code path. (experience shows that it
      // leads to very hard to track down bugs, if an inline cache gets updated
      // to a wrong method). It should not be performance critical, since the
      // resolve is only done once.

      for (;;) {
        ICRefillVerifier ic_refill_verifier;
        if (!clear_ic_at_addr(caller_nm, call_addr, is_static_call)) {
          InlineCacheBuffer::refill_ic_stubs();
        } else {
          break;
        }
      }
    }
  }

  methodHandle callee_method = find_callee_method(CHECK_(methodHandle()));


#ifndef PRODUCT
  Atomic::inc(&_wrong_method_ctr);

  if (TraceCallFixup) {
    ResourceMark rm(current);
    tty->print("handle_wrong_method reresolving call to");
    callee_method->print_short_name(tty);
    tty->print_cr(" code: " INTPTR_FORMAT, p2i(callee_method->code()));
  }
#endif

  return callee_method;
}

address SharedRuntime::handle_unsafe_access(JavaThread* thread, address next_pc) {
  // The faulting unsafe accesses should be changed to throw the error
  // synchronously instead. Meanwhile the faulting instruction will be
  // skipped over (effectively turning it into a no-op) and an
  // asynchronous exception will be raised which the thread will
  // handle at a later point. If the instruction is a load it will
  // return garbage.

  // Request an async exception.
  thread->set_pending_unsafe_access_error();

  // Return address of next instruction to execute.
  return next_pc;
}

#ifdef ASSERT
void SharedRuntime::check_member_name_argument_is_last_argument(const methodHandle& method,
                                                                const BasicType* sig_bt,
                                                                const VMRegPair* regs) {
  ResourceMark rm;
  const int total_args_passed = method->size_of_parameters();
  const VMRegPair*    regs_with_member_name = regs;
        VMRegPair* regs_without_member_name = NEW_RESOURCE_ARRAY(VMRegPair, total_args_passed - 1);

  const int member_arg_pos = total_args_passed - 1;
  assert(member_arg_pos >= 0 && member_arg_pos < total_args_passed, "oob");
  assert(sig_bt[member_arg_pos] == T_OBJECT, "dispatch argument must be an object");

  int comp_args_on_stack = java_calling_convention(sig_bt, regs_without_member_name, total_args_passed - 1);

  for (int i = 0; i < member_arg_pos; i++) {
    VMReg a =    regs_with_member_name[i].first();
    VMReg b = regs_without_member_name[i].first();
    assert(a->value() == b->value(), "register allocation mismatch: a=" INTX_FORMAT ", b=" INTX_FORMAT, a->value(), b->value());
  }
  assert(regs_with_member_name[member_arg_pos].first()->is_valid(), "bad member arg");
}
#endif

bool SharedRuntime::should_fixup_call_destination(address destination, address entry_point, address caller_pc, Method* moop, CodeBlob* cb) {
  if (destination != entry_point) {
    CodeBlob* callee = CodeCache::find_blob(destination);
    // callee == cb seems weird. It means calling interpreter thru stub.
    if (callee != NULL && (callee == cb || callee->is_adapter_blob())) {
      // static call or optimized virtual
      if (TraceCallFixup) {
        tty->print("fixup callsite           at " INTPTR_FORMAT " to compiled code for", p2i(caller_pc));
        moop->print_short_name(tty);
        tty->print_cr(" to " INTPTR_FORMAT, p2i(entry_point));
      }
      return true;
    } else {
      if (TraceCallFixup) {
        tty->print("failed to fixup callsite at " INTPTR_FORMAT " to compiled code for", p2i(caller_pc));
        moop->print_short_name(tty);
        tty->print_cr(" to " INTPTR_FORMAT, p2i(entry_point));
      }
      // assert is too strong could also be resolve destinations.
      // assert(InlineCacheBuffer::contains(destination) || VtableStubs::contains(destination), "must be");
    }
  } else {
    if (TraceCallFixup) {
      tty->print("already patched callsite at " INTPTR_FORMAT " to compiled code for", p2i(caller_pc));
      moop->print_short_name(tty);
      tty->print_cr(" to " INTPTR_FORMAT, p2i(entry_point));
    }
  }
  return false;
}

// ---------------------------------------------------------------------------
// We are calling the interpreter via a c2i. Normally this would mean that
// we were called by a compiled method. However we could have lost a race
// where we went int -> i2c -> c2i and so the caller could in fact be
// interpreted. If the caller is compiled we attempt to patch the caller
// so he no longer calls into the interpreter.
JRT_LEAF(void, SharedRuntime::fixup_callers_callsite(Method* method, address caller_pc))
  Method* moop(method);

  address entry_point = moop->from_compiled_entry_no_trampoline();

  // It's possible that deoptimization can occur at a call site which hasn't
  // been resolved yet, in which case this function will be called from
  // an nmethod that has been patched for deopt and we can ignore the
  // request for a fixup.
  // Also it is possible that we lost a race in that from_compiled_entry
  // is now back to the i2c in that case we don't need to patch and if
  // we did we'd leap into space because the callsite needs to use
  // "to interpreter" stub in order to load up the Method*. Don't
  // ask me how I know this...

  CodeBlob* cb = CodeCache::find_blob(caller_pc);
  if (cb == NULL || !cb->is_compiled() || entry_point == moop->get_c2i_entry()) {
    return;
  }

  // The check above makes sure this is a nmethod.
  CompiledMethod* nm = cb->as_compiled_method_or_null();
  assert(nm, "must be");

  // Get the return PC for the passed caller PC.
  address return_pc = caller_pc + frame::pc_return_offset;

  // There is a benign race here. We could be attempting to patch to a compiled
  // entry point at the same time the callee is being deoptimized. If that is
  // the case then entry_point may in fact point to a c2i and we'd patch the
  // call site with the same old data. clear_code will set code() to NULL
  // at the end of it. If we happen to see that NULL then we can skip trying
  // to patch. If we hit the window where the callee has a c2i in the
  // from_compiled_entry and the NULL isn't present yet then we lose the race
  // and patch the code with the same old data. Asi es la vida.

  if (moop->code() == NULL) return;

  if (nm->is_in_use()) {
    // Expect to find a native call there (unless it was no-inline cache vtable dispatch)
    CompiledICLocker ic_locker(nm);
    if (NativeCall::is_call_before(return_pc)) {
      ResourceMark mark;
      NativeCallWrapper* call = nm->call_wrapper_before(return_pc);
      //
      // bug 6281185. We might get here after resolving a call site to a vanilla
      // virtual call. Because the resolvee uses the verified entry it may then
      // see compiled code and attempt to patch the site by calling us. This would
      // then incorrectly convert the call site to optimized and its downhill from
      // there. If you're lucky you'll get the assert in the bugid, if not you've
      // just made a call site that could be megamorphic into a monomorphic site
      // for the rest of its life! Just another racing bug in the life of
      // fixup_callers_callsite ...
      //
      RelocIterator iter(nm, call->instruction_address(), call->next_instruction_address());
      iter.next();
      assert(iter.has_current(), "must have a reloc at java call site");
      relocInfo::relocType typ = iter.reloc()->type();
      if (typ != relocInfo::static_call_type &&
           typ != relocInfo::opt_virtual_call_type &&
           typ != relocInfo::static_stub_type) {
        return;
      }
      address destination = call->destination();
      if (should_fixup_call_destination(destination, entry_point, caller_pc, moop, cb)) {
        call->set_destination_mt_safe(entry_point);
      }
    }
  }
JRT_END


// same as JVM_Arraycopy, but called directly from compiled code
JRT_ENTRY(void, SharedRuntime::slow_arraycopy_C(oopDesc* src,  jint src_pos,
                                                oopDesc* dest, jint dest_pos,
                                                jint length,
                                                JavaThread* current)) {
#ifndef PRODUCT
  _slow_array_copy_ctr++;
#endif
  // Check if we have null pointers
  if (src == NULL || dest == NULL) {
    THROW(vmSymbols::java_lang_NullPointerException());
  }
  // Do the copy.  The casts to arrayOop are necessary to the copy_array API,
  // even though the copy_array API also performs dynamic checks to ensure
  // that src and dest are truly arrays (and are conformable).
  // The copy_array mechanism is awkward and could be removed, but
  // the compilers don't call this function except as a last resort,
  // so it probably doesn't matter.
  src->klass()->copy_array((arrayOopDesc*)src, src_pos,
                                        (arrayOopDesc*)dest, dest_pos,
                                        length, current);
}
JRT_END

// The caller of generate_class_cast_message() (or one of its callers)
// must use a ResourceMark in order to correctly free the result.
char* SharedRuntime::generate_class_cast_message(
    JavaThread* thread, Klass* caster_klass) {

  // Get target class name from the checkcast instruction
  vframeStream vfst(thread, true);
  assert(!vfst.at_end(), "Java frame must exist");
  Bytecode_checkcast cc(vfst.method(), vfst.method()->bcp_from(vfst.bci()));
  constantPoolHandle cpool(thread, vfst.method()->constants());
  Klass* target_klass = ConstantPool::klass_at_if_loaded(cpool, cc.index());
  Symbol* target_klass_name = NULL;
  if (target_klass == NULL) {
    // This klass should be resolved, but just in case, get the name in the klass slot.
    target_klass_name = cpool->klass_name_at(cc.index());
  }
  return generate_class_cast_message(caster_klass, target_klass, target_klass_name);
}


// The caller of generate_class_cast_message() (or one of its callers)
// must use a ResourceMark in order to correctly free the result.
char* SharedRuntime::generate_class_cast_message(
    Klass* caster_klass, Klass* target_klass, Symbol* target_klass_name) {
  const char* caster_name = caster_klass->external_name();

  assert(target_klass != NULL || target_klass_name != NULL, "one must be provided");
  const char* target_name = target_klass == NULL ? target_klass_name->as_klass_external_name() :
                                                   target_klass->external_name();

  size_t msglen = strlen(caster_name) + strlen("class ") + strlen(" cannot be cast to class ") + strlen(target_name) + 1;

  const char* caster_klass_description = "";
  const char* target_klass_description = "";
  const char* klass_separator = "";
  if (target_klass != NULL && caster_klass->module() == target_klass->module()) {
    caster_klass_description = caster_klass->joint_in_module_of_loader(target_klass);
  } else {
    caster_klass_description = caster_klass->class_in_module_of_loader();
    target_klass_description = (target_klass != NULL) ? target_klass->class_in_module_of_loader() : "";
    klass_separator = (target_klass != NULL) ? "; " : "";
  }

  // add 3 for parenthesis and preceeding space
  msglen += strlen(caster_klass_description) + strlen(target_klass_description) + strlen(klass_separator) + 3;

  char* message = NEW_RESOURCE_ARRAY_RETURN_NULL(char, msglen);
  if (message == NULL) {
    // Shouldn't happen, but don't cause even more problems if it does
    message = const_cast<char*>(caster_klass->external_name());
  } else {
    jio_snprintf(message,
                 msglen,
                 "class %s cannot be cast to class %s (%s%s%s)",
                 caster_name,
                 target_name,
                 caster_klass_description,
                 klass_separator,
                 target_klass_description
                 );
  }
  return message;
}

JRT_LEAF(void, SharedRuntime::reguard_yellow_pages())
  (void) JavaThread::current()->stack_overflow_state()->reguard_stack();
JRT_END

void SharedRuntime::monitor_enter_helper(oopDesc* obj, BasicLock* lock, JavaThread* current) {
  if (!SafepointSynchronize::is_synchronizing()) {
    // Only try quick_enter() if we're not trying to reach a safepoint
    // so that the calling thread reaches the safepoint more quickly.
    if (ObjectSynchronizer::quick_enter(obj, current, lock)) return;
  }
  // NO_ASYNC required because an async exception on the state transition destructor
  // would leave you with the lock held and it would never be released.
  // The normal monitorenter NullPointerException is thrown without acquiring a lock
  // and the model is that an exception implies the method failed.
  JRT_BLOCK_NO_ASYNC
  Handle h_obj(THREAD, obj);
  ObjectSynchronizer::enter(h_obj, lock, current);
  assert(!HAS_PENDING_EXCEPTION, "Should have no exception here");
  JRT_BLOCK_END
}

// Handles the uncommon case in locking, i.e., contention or an inflated lock.
JRT_BLOCK_ENTRY(void, SharedRuntime::complete_monitor_locking_C(oopDesc* obj, BasicLock* lock, JavaThread* current))
  SharedRuntime::monitor_enter_helper(obj, lock, current);
JRT_END

void SharedRuntime::monitor_exit_helper(oopDesc* obj, BasicLock* lock, JavaThread* current) {
  assert(JavaThread::current() == current, "invariant");
  // Exit must be non-blocking, and therefore no exceptions can be thrown.
  ExceptionMark em(current);
  // The object could become unlocked through a JNI call, which we have no other checks for.
  // Give a fatal message if CheckJNICalls. Otherwise we ignore it.
  if (obj->is_unlocked()) {
    if (CheckJNICalls) {
      fatal("Object has been unlocked by JNI");
    }
    return;
  }
  ObjectSynchronizer::exit(obj, lock, current);
}

// Handles the uncommon cases of monitor unlocking in compiled code
JRT_LEAF(void, SharedRuntime::complete_monitor_unlocking_C(oopDesc* obj, BasicLock* lock, JavaThread* current))
  SharedRuntime::monitor_exit_helper(obj, lock, current);
JRT_END

#ifndef PRODUCT

void SharedRuntime::print_statistics() {
  ttyLocker ttyl;
  if (xtty != NULL)  xtty->head("statistics type='SharedRuntime'");

  SharedRuntime::print_ic_miss_histogram();

  // Dump the JRT_ENTRY counters
  if (_new_instance_ctr) tty->print_cr("%5d new instance requires GC", _new_instance_ctr);
  if (_new_array_ctr) tty->print_cr("%5d new array requires GC", _new_array_ctr);
  if (_multi2_ctr) tty->print_cr("%5d multianewarray 2 dim", _multi2_ctr);
  if (_multi3_ctr) tty->print_cr("%5d multianewarray 3 dim", _multi3_ctr);
  if (_multi4_ctr) tty->print_cr("%5d multianewarray 4 dim", _multi4_ctr);
  if (_multi5_ctr) tty->print_cr("%5d multianewarray 5 dim", _multi5_ctr);

  tty->print_cr("%5d inline cache miss in compiled", _ic_miss_ctr);
  tty->print_cr("%5d wrong method", _wrong_method_ctr);
  tty->print_cr("%5d unresolved static call site", _resolve_static_ctr);
  tty->print_cr("%5d unresolved virtual call site", _resolve_virtual_ctr);
  tty->print_cr("%5d unresolved opt virtual call site", _resolve_opt_virtual_ctr);

  if (_mon_enter_stub_ctr) tty->print_cr("%5d monitor enter stub", _mon_enter_stub_ctr);
  if (_mon_exit_stub_ctr) tty->print_cr("%5d monitor exit stub", _mon_exit_stub_ctr);
  if (_mon_enter_ctr) tty->print_cr("%5d monitor enter slow", _mon_enter_ctr);
  if (_mon_exit_ctr) tty->print_cr("%5d monitor exit slow", _mon_exit_ctr);
  if (_partial_subtype_ctr) tty->print_cr("%5d slow partial subtype", _partial_subtype_ctr);
  if (_jbyte_array_copy_ctr) tty->print_cr("%5d byte array copies", _jbyte_array_copy_ctr);
  if (_jshort_array_copy_ctr) tty->print_cr("%5d short array copies", _jshort_array_copy_ctr);
  if (_jint_array_copy_ctr) tty->print_cr("%5d int array copies", _jint_array_copy_ctr);
  if (_jlong_array_copy_ctr) tty->print_cr("%5d long array copies", _jlong_array_copy_ctr);
  if (_oop_array_copy_ctr) tty->print_cr("%5d oop array copies", _oop_array_copy_ctr);
  if (_checkcast_array_copy_ctr) tty->print_cr("%5d checkcast array copies", _checkcast_array_copy_ctr);
  if (_unsafe_array_copy_ctr) tty->print_cr("%5d unsafe array copies", _unsafe_array_copy_ctr);
  if (_generic_array_copy_ctr) tty->print_cr("%5d generic array copies", _generic_array_copy_ctr);
  if (_slow_array_copy_ctr) tty->print_cr("%5d slow array copies", _slow_array_copy_ctr);
  if (_find_handler_ctr) tty->print_cr("%5d find exception handler", _find_handler_ctr);
  if (_rethrow_ctr) tty->print_cr("%5d rethrow handler", _rethrow_ctr);

  AdapterHandlerLibrary::print_statistics();

  if (xtty != NULL)  xtty->tail("statistics");
}

inline double percent(int x, int y) {
  return 100.0 * x / MAX2(y, 1);
}

inline double percent(int64_t x, int64_t y) {
  return 100.0 * x / MAX2(y, (int64_t)1);
}

class MethodArityHistogram {
 public:
  enum { MAX_ARITY = 256 };
 private:
  static uint64_t _arity_histogram[MAX_ARITY]; // histogram of #args
  static uint64_t _size_histogram[MAX_ARITY];  // histogram of arg size in words
  static uint64_t _total_compiled_calls;
  static uint64_t _max_compiled_calls_per_method;
  static int _max_arity;                       // max. arity seen
  static int _max_size;                        // max. arg size seen

  static void add_method_to_histogram(nmethod* nm) {
    Method* method = (nm == NULL) ? NULL : nm->method();
    if ((method != NULL) && nm->is_alive()) {
      ArgumentCount args(method->signature());
      int arity   = args.size() + (method->is_static() ? 0 : 1);
      int argsize = method->size_of_parameters();
      arity   = MIN2(arity, MAX_ARITY-1);
      argsize = MIN2(argsize, MAX_ARITY-1);
      uint64_t count = (uint64_t)method->compiled_invocation_count();
      _max_compiled_calls_per_method = count > _max_compiled_calls_per_method ? count : _max_compiled_calls_per_method;
      _total_compiled_calls    += count;
      _arity_histogram[arity]  += count;
      _size_histogram[argsize] += count;
      _max_arity = MAX2(_max_arity, arity);
      _max_size  = MAX2(_max_size, argsize);
    }
  }

  void print_histogram_helper(int n, uint64_t* histo, const char* name) {
    const int N = MIN2(9, n);
    double sum = 0;
    double weighted_sum = 0;
    for (int i = 0; i <= n; i++) { sum += histo[i]; weighted_sum += i*histo[i]; }
    if (sum >= 1.0) { // prevent divide by zero or divide overflow
      double rest = sum;
      double percent = sum / 100;
      for (int i = 0; i <= N; i++) {
        rest -= histo[i];
        tty->print_cr("%4d: " UINT64_FORMAT_W(12) " (%5.1f%%)", i, histo[i], histo[i] / percent);
      }
      tty->print_cr("rest: " INT64_FORMAT_W(12) " (%5.1f%%)", (int64_t)rest, rest / percent);
      tty->print_cr("(avg. %s = %3.1f, max = %d)", name, weighted_sum / sum, n);
      tty->print_cr("(total # of compiled calls = " INT64_FORMAT_W(14) ")", _total_compiled_calls);
      tty->print_cr("(max # of compiled calls   = " INT64_FORMAT_W(14) ")", _max_compiled_calls_per_method);
    } else {
      tty->print_cr("Histogram generation failed for %s. n = %d, sum = %7.5f", name, n, sum);
    }
  }

  void print_histogram() {
    tty->print_cr("\nHistogram of call arity (incl. rcvr, calls to compiled methods only):");
    print_histogram_helper(_max_arity, _arity_histogram, "arity");
    tty->print_cr("\nHistogram of parameter block size (in words, incl. rcvr):");
    print_histogram_helper(_max_size, _size_histogram, "size");
    tty->cr();
  }

 public:
  MethodArityHistogram() {
    // Take the Compile_lock to protect against changes in the CodeBlob structures
    MutexLocker mu1(Compile_lock, Mutex::_safepoint_check_flag);
    // Take the CodeCache_lock to protect against changes in the CodeHeap structure
    MutexLocker mu2(CodeCache_lock, Mutex::_no_safepoint_check_flag);
    _max_arity = _max_size = 0;
    _total_compiled_calls = 0;
    _max_compiled_calls_per_method = 0;
    for (int i = 0; i < MAX_ARITY; i++) _arity_histogram[i] = _size_histogram[i] = 0;
    CodeCache::nmethods_do(add_method_to_histogram);
    print_histogram();
  }
};

uint64_t MethodArityHistogram::_arity_histogram[MethodArityHistogram::MAX_ARITY];
uint64_t MethodArityHistogram::_size_histogram[MethodArityHistogram::MAX_ARITY];
uint64_t MethodArityHistogram::_total_compiled_calls;
uint64_t MethodArityHistogram::_max_compiled_calls_per_method;
int MethodArityHistogram::_max_arity;
int MethodArityHistogram::_max_size;

void SharedRuntime::print_call_statistics(uint64_t comp_total) {
  tty->print_cr("Calls from compiled code:");
  int64_t total  = _nof_normal_calls + _nof_interface_calls + _nof_static_calls;
  int64_t mono_c = _nof_normal_calls - _nof_optimized_calls - _nof_megamorphic_calls;
  int64_t mono_i = _nof_interface_calls - _nof_optimized_interface_calls - _nof_megamorphic_interface_calls;
  tty->print_cr("\t" INT64_FORMAT_W(12) " (100%%)  total non-inlined   ", total);
  tty->print_cr("\t" INT64_FORMAT_W(12) " (%4.1f%%) |- virtual calls       ", _nof_normal_calls, percent(_nof_normal_calls, total));
  tty->print_cr("\t" INT64_FORMAT_W(12) " (%4.0f%%) |  |- inlined          ", _nof_inlined_calls, percent(_nof_inlined_calls, _nof_normal_calls));
  tty->print_cr("\t" INT64_FORMAT_W(12) " (%4.0f%%) |  |- optimized        ", _nof_optimized_calls, percent(_nof_optimized_calls, _nof_normal_calls));
  tty->print_cr("\t" INT64_FORMAT_W(12) " (%4.0f%%) |  |- monomorphic      ", mono_c, percent(mono_c, _nof_normal_calls));
  tty->print_cr("\t" INT64_FORMAT_W(12) " (%4.0f%%) |  |- megamorphic      ", _nof_megamorphic_calls, percent(_nof_megamorphic_calls, _nof_normal_calls));
  tty->print_cr("\t" INT64_FORMAT_W(12) " (%4.1f%%) |- interface calls     ", _nof_interface_calls, percent(_nof_interface_calls, total));
  tty->print_cr("\t" INT64_FORMAT_W(12) " (%4.0f%%) |  |- inlined          ", _nof_inlined_interface_calls, percent(_nof_inlined_interface_calls, _nof_interface_calls));
  tty->print_cr("\t" INT64_FORMAT_W(12) " (%4.0f%%) |  |- optimized        ", _nof_optimized_interface_calls, percent(_nof_optimized_interface_calls, _nof_interface_calls));
  tty->print_cr("\t" INT64_FORMAT_W(12) " (%4.0f%%) |  |- monomorphic      ", mono_i, percent(mono_i, _nof_interface_calls));
  tty->print_cr("\t" INT64_FORMAT_W(12) " (%4.0f%%) |  |- megamorphic      ", _nof_megamorphic_interface_calls, percent(_nof_megamorphic_interface_calls, _nof_interface_calls));
  tty->print_cr("\t" INT64_FORMAT_W(12) " (%4.1f%%) |- static/special calls", _nof_static_calls, percent(_nof_static_calls, total));
  tty->print_cr("\t" INT64_FORMAT_W(12) " (%4.0f%%) |  |- inlined          ", _nof_inlined_static_calls, percent(_nof_inlined_static_calls, _nof_static_calls));
  tty->cr();
  tty->print_cr("Note 1: counter updates are not MT-safe.");
  tty->print_cr("Note 2: %% in major categories are relative to total non-inlined calls;");
  tty->print_cr("        %% in nested categories are relative to their category");
  tty->print_cr("        (and thus add up to more than 100%% with inlining)");
  tty->cr();

  MethodArityHistogram h;
}
#endif


// A simple wrapper class around the calling convention information
// that allows sharing of adapters for the same calling convention.
class AdapterFingerPrint : public CHeapObj<mtCode> {
 private:
  enum {
    _basic_type_bits = 4,
    _basic_type_mask = right_n_bits(_basic_type_bits),
    _basic_types_per_int = BitsPerInt / _basic_type_bits,
    _compact_int_count = 3
  };
  // TO DO:  Consider integrating this with a more global scheme for compressing signatures.
  // For now, 4 bits per components (plus T_VOID gaps after double/long) is not excessive.

  union {
    int  _compact[_compact_int_count];
    int* _fingerprint;
  } _value;
  int _length; // A negative length indicates the fingerprint is in the compact form,
               // Otherwise _value._fingerprint is the array.

  // Remap BasicTypes that are handled equivalently by the adapters.
  // These are correct for the current system but someday it might be
  // necessary to make this mapping platform dependent.
  static int adapter_encoding(BasicType in) {
    switch (in) {
      case T_BOOLEAN:
      case T_BYTE:
      case T_SHORT:
      case T_CHAR:
        // There are all promoted to T_INT in the calling convention
        return T_INT;

      case T_OBJECT:
      case T_ARRAY:
        // In other words, we assume that any register good enough for
        // an int or long is good enough for a managed pointer.
#ifdef _LP64
        return T_LONG;
#else
        return T_INT;
#endif

      case T_INT:
      case T_LONG:
      case T_FLOAT:
      case T_DOUBLE:
      case T_VOID:
        return in;

      default:
        ShouldNotReachHere();
        return T_CONFLICT;
    }
  }

 public:
  AdapterFingerPrint(int total_args_passed, BasicType* sig_bt) {
    // The fingerprint is based on the BasicType signature encoded
    // into an array of ints with eight entries per int.
    int* ptr;
    int len = (total_args_passed + (_basic_types_per_int-1)) / _basic_types_per_int;
    if (len <= _compact_int_count) {
      assert(_compact_int_count == 3, "else change next line");
      _value._compact[0] = _value._compact[1] = _value._compact[2] = 0;
      // Storing the signature encoded as signed chars hits about 98%
      // of the time.
      _length = -len;
      ptr = _value._compact;
    } else {
      _length = len;
      _value._fingerprint = NEW_C_HEAP_ARRAY(int, _length, mtCode);
      ptr = _value._fingerprint;
    }

    // Now pack the BasicTypes with 8 per int
    int sig_index = 0;
    for (int index = 0; index < len; index++) {
      int value = 0;
      for (int byte = 0; sig_index < total_args_passed && byte < _basic_types_per_int; byte++) {
        int bt = adapter_encoding(sig_bt[sig_index++]);
        assert((bt & _basic_type_mask) == bt, "must fit in 4 bits");
        value = (value << _basic_type_bits) | bt;
      }
      ptr[index] = value;
    }
  }

  ~AdapterFingerPrint() {
    if (_length > 0) {
      FREE_C_HEAP_ARRAY(int, _value._fingerprint);
    }
  }

  int value(int index) {
    if (_length < 0) {
      return _value._compact[index];
    }
    return _value._fingerprint[index];
  }
  int length() {
    if (_length < 0) return -_length;
    return _length;
  }

  bool is_compact() {
    return _length <= 0;
  }

  unsigned int compute_hash() {
    int hash = 0;
    for (int i = 0; i < length(); i++) {
      int v = value(i);
      hash = (hash << 8) ^ v ^ (hash >> 5);
    }
    return (unsigned int)hash;
  }

  const char* as_string() {
    stringStream st;
    st.print("0x");
    for (int i = 0; i < length(); i++) {
      st.print("%x", value(i));
    }
    return st.as_string();
  }

#ifndef PRODUCT
  // Reconstitutes the basic type arguments from the fingerprint,
  // producing strings like LIJDF
  const char* as_basic_args_string() {
    stringStream st;
    bool long_prev = false;
    for (int i = 0; i < length(); i++) {
      unsigned val = (unsigned)value(i);
      // args are packed so that first/lower arguments are in the highest
      // bits of each int value, so iterate from highest to the lowest
      for (int j = 32 - _basic_type_bits; j >= 0; j -= _basic_type_bits) {
        unsigned v = (val >> j) & _basic_type_mask;
        if (v == 0) {
          assert(i == length() - 1, "Only expect zeroes in the last word");
          continue;
        }
        if (long_prev) {
          long_prev = false;
          if (v == T_VOID) {
            st.print("J");
          } else {
            st.print("L");
          }
        }
        switch (v) {
          case T_INT:    st.print("I");    break;
          case T_LONG:   long_prev = true; break;
          case T_FLOAT:  st.print("F");    break;
          case T_DOUBLE: st.print("D");    break;
          case T_VOID:   break;
          default: ShouldNotReachHere();
        }
      }
    }
    if (long_prev) {
      st.print("L");
    }
    return st.as_string();
  }
#endif // !product

  bool equals(AdapterFingerPrint* other) {
    if (other->_length != _length) {
      return false;
    }
    if (_length < 0) {
      assert(_compact_int_count == 3, "else change next line");
      return _value._compact[0] == other->_value._compact[0] &&
             _value._compact[1] == other->_value._compact[1] &&
             _value._compact[2] == other->_value._compact[2];
    } else {
      for (int i = 0; i < _length; i++) {
        if (_value._fingerprint[i] != other->_value._fingerprint[i]) {
          return false;
        }
      }
    }
    return true;
  }
};


// A hashtable mapping from AdapterFingerPrints to AdapterHandlerEntries
class AdapterHandlerTable : public BasicHashtable<mtCode> {
  friend class AdapterHandlerTableIterator;

 private:

#ifndef PRODUCT
  static int _lookups; // number of calls to lookup
  static int _buckets; // number of buckets checked
  static int _equals;  // number of buckets checked with matching hash
  static int _hits;    // number of successful lookups
  static int _compact; // number of equals calls with compact signature
#endif

  AdapterHandlerEntry* bucket(int i) {
    return (AdapterHandlerEntry*)BasicHashtable<mtCode>::bucket(i);
  }

 public:
  AdapterHandlerTable()
    : BasicHashtable<mtCode>(293, (sizeof(AdapterHandlerEntry))) { }

  // Create a new entry suitable for insertion in the table
  AdapterHandlerEntry* new_entry(AdapterFingerPrint* fingerprint, address i2c_entry, address c2i_entry, address c2i_unverified_entry, address c2i_no_clinit_check_entry) {
    AdapterHandlerEntry* entry = (AdapterHandlerEntry*)BasicHashtable<mtCode>::new_entry(fingerprint->compute_hash());
    entry->init(fingerprint, i2c_entry, c2i_entry, c2i_unverified_entry, c2i_no_clinit_check_entry);
    return entry;
  }

  // Insert an entry into the table
  void add(AdapterHandlerEntry* entry) {
    int index = hash_to_index(entry->hash());
    add_entry(index, entry);
  }

  void free_entry(AdapterHandlerEntry* entry) {
    entry->deallocate();
    BasicHashtable<mtCode>::free_entry(entry);
  }

  // Find a entry with the same fingerprint if it exists
  AdapterHandlerEntry* lookup(int total_args_passed, BasicType* sig_bt) {
    NOT_PRODUCT(_lookups++);
    AdapterFingerPrint fp(total_args_passed, sig_bt);
    unsigned int hash = fp.compute_hash();
    int index = hash_to_index(hash);
    for (AdapterHandlerEntry* e = bucket(index); e != NULL; e = e->next()) {
      NOT_PRODUCT(_buckets++);
      if (e->hash() == hash) {
        NOT_PRODUCT(_equals++);
        if (fp.equals(e->fingerprint())) {
#ifndef PRODUCT
          if (fp.is_compact()) _compact++;
          _hits++;
#endif
          return e;
        }
      }
    }
    return NULL;
  }

#ifndef PRODUCT
  void print_statistics() {
    ResourceMark rm;
    int longest = 0;
    int empty = 0;
    int total = 0;
    int nonempty = 0;
    for (int index = 0; index < table_size(); index++) {
      int count = 0;
      for (AdapterHandlerEntry* e = bucket(index); e != NULL; e = e->next()) {
        count++;
      }
      if (count != 0) nonempty++;
      if (count == 0) empty++;
      if (count > longest) longest = count;
      total += count;
    }
    tty->print_cr("AdapterHandlerTable: empty %d longest %d total %d average %f",
                  empty, longest, total, total / (double)nonempty);
    tty->print_cr("AdapterHandlerTable: lookups %d buckets %d equals %d hits %d compact %d",
                  _lookups, _buckets, _equals, _hits, _compact);
  }
#endif
};


#ifndef PRODUCT

int AdapterHandlerTable::_lookups;
int AdapterHandlerTable::_buckets;
int AdapterHandlerTable::_equals;
int AdapterHandlerTable::_hits;
int AdapterHandlerTable::_compact;

#endif

class AdapterHandlerTableIterator : public StackObj {
 private:
  AdapterHandlerTable* _table;
  int _index;
  AdapterHandlerEntry* _current;

  void scan() {
    while (_index < _table->table_size()) {
      AdapterHandlerEntry* a = _table->bucket(_index);
      _index++;
      if (a != NULL) {
        _current = a;
        return;
      }
    }
  }

 public:
  AdapterHandlerTableIterator(AdapterHandlerTable* table): _table(table), _index(0), _current(NULL) {
    scan();
  }
  bool has_next() {
    return _current != NULL;
  }
  AdapterHandlerEntry* next() {
    if (_current != NULL) {
      AdapterHandlerEntry* result = _current;
      _current = _current->next();
      if (_current == NULL) scan();
      return result;
    } else {
      return NULL;
    }
  }
};


// ---------------------------------------------------------------------------
// Implementation of AdapterHandlerLibrary
AdapterHandlerTable* AdapterHandlerLibrary::_adapters = NULL;
AdapterHandlerEntry* AdapterHandlerLibrary::_abstract_method_handler = NULL;
AdapterHandlerEntry* AdapterHandlerLibrary::_no_arg_handler = NULL;
AdapterHandlerEntry* AdapterHandlerLibrary::_int_arg_handler = NULL;
AdapterHandlerEntry* AdapterHandlerLibrary::_obj_arg_handler = NULL;
AdapterHandlerEntry* AdapterHandlerLibrary::_obj_int_arg_handler = NULL;
AdapterHandlerEntry* AdapterHandlerLibrary::_obj_obj_arg_handler = NULL;
const int AdapterHandlerLibrary_size = 16*K;
BufferBlob* AdapterHandlerLibrary::_buffer = NULL;

BufferBlob* AdapterHandlerLibrary::buffer_blob() {
  return _buffer;
}

extern "C" void unexpected_adapter_call() {
  ShouldNotCallThis();
}

static void post_adapter_creation(const AdapterBlob* new_adapter, const AdapterHandlerEntry* entry) {
  char blob_id[256];
  jio_snprintf(blob_id,
                sizeof(blob_id),
                "%s(%s)",
                new_adapter->name(),
                entry->fingerprint()->as_string());
  Forte::register_stub(blob_id, new_adapter->content_begin(), new_adapter->content_end());

  if (JvmtiExport::should_post_dynamic_code_generated()) {
    JvmtiExport::post_dynamic_code_generated(blob_id, new_adapter->content_begin(), new_adapter->content_end());
  }
}

void AdapterHandlerLibrary::initialize() {
  ResourceMark rm;
  AdapterBlob* no_arg_blob = NULL;
  AdapterBlob* int_arg_blob = NULL;
  AdapterBlob* obj_arg_blob = NULL;
  AdapterBlob* obj_int_arg_blob = NULL;
  AdapterBlob* obj_obj_arg_blob = NULL;
  {
    MutexLocker mu(AdapterHandlerLibrary_lock);
    assert(_adapters == NULL, "Initializing more than once");

    _adapters = new AdapterHandlerTable();

    // Create a special handler for abstract methods.  Abstract methods
    // are never compiled so an i2c entry is somewhat meaningless, but
    // throw AbstractMethodError just in case.
    // Pass wrong_method_abstract for the c2i transitions to return
    // AbstractMethodError for invalid invocations.
    address wrong_method_abstract = SharedRuntime::get_handle_wrong_method_abstract_stub();
    _abstract_method_handler = AdapterHandlerLibrary::new_entry(new AdapterFingerPrint(0, NULL),
                                                                StubRoutines::throw_AbstractMethodError_entry(),
                                                                wrong_method_abstract, wrong_method_abstract);

    _buffer = BufferBlob::create("adapters", AdapterHandlerLibrary_size);

    _no_arg_handler = create_adapter(no_arg_blob, 0, NULL, true);

    BasicType obj_args[] = { T_OBJECT };
    _obj_arg_handler = create_adapter(obj_arg_blob, 1, obj_args, true);

    BasicType int_args[] = { T_INT };
    _int_arg_handler = create_adapter(int_arg_blob, 1, int_args, true);

    BasicType obj_int_args[] = { T_OBJECT, T_INT };
    _obj_int_arg_handler = create_adapter(obj_int_arg_blob, 2, obj_int_args, true);

    BasicType obj_obj_args[] = { T_OBJECT, T_OBJECT };
    _obj_obj_arg_handler = create_adapter(obj_obj_arg_blob, 2, obj_obj_args, true);

    assert(no_arg_blob != NULL &&
          obj_arg_blob != NULL &&
          int_arg_blob != NULL &&
          obj_int_arg_blob != NULL &&
          obj_obj_arg_blob != NULL, "Initial adapters must be properly created");
  }

  // Outside of the lock
  post_adapter_creation(no_arg_blob, _no_arg_handler);
  post_adapter_creation(obj_arg_blob, _obj_arg_handler);
  post_adapter_creation(int_arg_blob, _int_arg_handler);
  post_adapter_creation(obj_int_arg_blob, _obj_int_arg_handler);
  post_adapter_creation(obj_obj_arg_blob, _obj_obj_arg_handler);
}

AdapterHandlerEntry* AdapterHandlerLibrary::new_entry(AdapterFingerPrint* fingerprint,
                                                      address i2c_entry,
                                                      address c2i_entry,
                                                      address c2i_unverified_entry,
                                                      address c2i_no_clinit_check_entry) {
  return _adapters->new_entry(fingerprint, i2c_entry, c2i_entry, c2i_unverified_entry, c2i_no_clinit_check_entry);
}

AdapterHandlerEntry* AdapterHandlerLibrary::get_simple_adapter(const methodHandle& method) {
  if (method->is_abstract()) {
    return _abstract_method_handler;
  }
  int total_args_passed = method->size_of_parameters(); // All args on stack
  if (total_args_passed == 0) {
    return _no_arg_handler;
  } else if (total_args_passed == 1) {
    if (!method->is_static()) {
      return _obj_arg_handler;
    }
    switch (method->signature()->char_at(1)) {
      case JVM_SIGNATURE_CLASS:
      case JVM_SIGNATURE_ARRAY:
        return _obj_arg_handler;
      case JVM_SIGNATURE_INT:
      case JVM_SIGNATURE_BOOLEAN:
      case JVM_SIGNATURE_CHAR:
      case JVM_SIGNATURE_BYTE:
      case JVM_SIGNATURE_SHORT:
        return _int_arg_handler;
    }
  } else if (total_args_passed == 2 &&
             !method->is_static()) {
    switch (method->signature()->char_at(1)) {
      case JVM_SIGNATURE_CLASS:
      case JVM_SIGNATURE_ARRAY:
        return _obj_obj_arg_handler;
      case JVM_SIGNATURE_INT:
      case JVM_SIGNATURE_BOOLEAN:
      case JVM_SIGNATURE_CHAR:
      case JVM_SIGNATURE_BYTE:
      case JVM_SIGNATURE_SHORT:
        return _obj_int_arg_handler;
    }
  }
  return NULL;
}

class AdapterSignatureIterator : public SignatureIterator {
 private:
  BasicType stack_sig_bt[16];
  BasicType* sig_bt;
  int index;

 public:
  AdapterSignatureIterator(Symbol* signature,
                           fingerprint_t fingerprint,
                           bool is_static,
                           int total_args_passed) :
    SignatureIterator(signature, fingerprint),
    index(0)
  {
    sig_bt = (total_args_passed <= 16) ? stack_sig_bt : NEW_RESOURCE_ARRAY(BasicType, total_args_passed);
    if (!is_static) { // Pass in receiver first
      sig_bt[index++] = T_OBJECT;
    }
    do_parameters_on(this);
  }

  BasicType* basic_types() {
    return sig_bt;
  }

#ifdef ASSERT
  int slots() {
    return index;
  }
#endif

 private:

  friend class SignatureIterator;  // so do_parameters_on can call do_type
  void do_type(BasicType type) {
    sig_bt[index++] = type;
    if (type == T_LONG || type == T_DOUBLE) {
      sig_bt[index++] = T_VOID; // Longs & doubles take 2 Java slots
    }
  }
};

AdapterHandlerEntry* AdapterHandlerLibrary::get_adapter(const methodHandle& method) {
  // Use customized signature handler.  Need to lock around updates to
  // the AdapterHandlerTable (it is not safe for concurrent readers
  // and a single writer: this could be fixed if it becomes a
  // problem).
  assert(_adapters != NULL, "Uninitialized");

  // Fast-path for trivial adapters
  AdapterHandlerEntry* entry = get_simple_adapter(method);
  if (entry != NULL) {
    return entry;
  }

  ResourceMark rm;
  AdapterBlob* new_adapter = NULL;

  // Fill in the signature array, for the calling-convention call.
  int total_args_passed = method->size_of_parameters(); // All args on stack

  AdapterSignatureIterator si(method->signature(), method->constMethod()->fingerprint(),
                              method->is_static(), total_args_passed);
  assert(si.slots() == total_args_passed, "");
  BasicType* sig_bt = si.basic_types();
  {
    MutexLocker mu(AdapterHandlerLibrary_lock);

    // Lookup method signature's fingerprint
    entry = _adapters->lookup(total_args_passed, sig_bt);

    if (entry != NULL) {
#ifdef ASSERT
      if (VerifyAdapterSharing) {
        AdapterBlob* comparison_blob = NULL;
        AdapterHandlerEntry* comparison_entry = create_adapter(comparison_blob, total_args_passed, sig_bt, false);
        assert(comparison_blob == NULL, "no blob should be created when creating an adapter for comparison");
        assert(comparison_entry->compare_code(entry), "code must match");
        // Release the one just created and return the original
        _adapters->free_entry(comparison_entry);
      }
#endif
      return entry;
    }

    entry = create_adapter(new_adapter, total_args_passed, sig_bt, /* allocate_code_blob */ true);
  }

  // Outside of the lock
  if (new_adapter != NULL) {
    post_adapter_creation(new_adapter, entry);
  }
  return entry;
}

AdapterHandlerEntry* AdapterHandlerLibrary::create_adapter(AdapterBlob*& new_adapter,
                                                           int total_args_passed,
                                                           BasicType* sig_bt,
                                                           bool allocate_code_blob) {

  // StubRoutines::code2() is initialized after this function can be called. As a result,
  // VerifyAdapterCalls and VerifyAdapterSharing can fail if we re-use code that generated
  // prior to StubRoutines::code2() being set. Checks refer to checks generated in an I2C
  // stub that ensure that an I2C stub is called from an interpreter frame.
  bool contains_all_checks = StubRoutines::code2() != NULL;

  VMRegPair stack_regs[16];
  VMRegPair* regs = (total_args_passed <= 16) ? stack_regs : NEW_RESOURCE_ARRAY(VMRegPair, total_args_passed);

  // Get a description of the compiled java calling convention and the largest used (VMReg) stack slot usage
  int comp_args_on_stack = SharedRuntime::java_calling_convention(sig_bt, regs, total_args_passed);
  BufferBlob* buf = buffer_blob(); // the temporary code buffer in CodeCache
  CodeBuffer buffer(buf);
  short buffer_locs[20];
  buffer.insts()->initialize_shared_locs((relocInfo*)buffer_locs,
                                          sizeof(buffer_locs)/sizeof(relocInfo));

  // Make a C heap allocated version of the fingerprint to store in the adapter
  AdapterFingerPrint* fingerprint = new AdapterFingerPrint(total_args_passed, sig_bt);
  MacroAssembler _masm(&buffer);
  AdapterHandlerEntry* entry = SharedRuntime::generate_i2c2i_adapters(&_masm,
                                                total_args_passed,
                                                comp_args_on_stack,
                                                sig_bt,
                                                regs,
                                                fingerprint);

#ifdef ASSERT
  if (VerifyAdapterSharing) {
    entry->save_code(buf->code_begin(), buffer.insts_size());
    if (!allocate_code_blob) {
      return entry;
    }
  }
#endif

  new_adapter = AdapterBlob::create(&buffer);
  NOT_PRODUCT(int insts_size = buffer.insts_size());
  if (new_adapter == NULL) {
    // CodeCache is full, disable compilation
    // Ought to log this but compile log is only per compile thread
    // and we're some non descript Java thread.
    return NULL;
  }
  entry->relocate(new_adapter->content_begin());
#ifndef PRODUCT
  // debugging suppport
  if (PrintAdapterHandlers || PrintStubCode) {
    ttyLocker ttyl;
    entry->print_adapter_on(tty);
    tty->print_cr("i2c argument handler #%d for: %s %s (%d bytes generated)",
                  _adapters->number_of_entries(), fingerprint->as_basic_args_string(),
                  fingerprint->as_string(), insts_size);
    tty->print_cr("c2i argument handler starts at %p", entry->get_c2i_entry());
    if (Verbose || PrintStubCode) {
      address first_pc = entry->base_address();
      if (first_pc != NULL) {
        Disassembler::decode(first_pc, first_pc + insts_size);
        tty->cr();
      }
    }
  }
#endif

  // Add the entry only if the entry contains all required checks (see sharedRuntime_xxx.cpp)
  // The checks are inserted only if -XX:+VerifyAdapterCalls is specified.
  if (contains_all_checks || !VerifyAdapterCalls) {
    _adapters->add(entry);
  }
  return entry;
}

address AdapterHandlerEntry::base_address() {
  address base = _i2c_entry;
  if (base == NULL)  base = _c2i_entry;
  assert(base <= _c2i_entry || _c2i_entry == NULL, "");
  assert(base <= _c2i_unverified_entry || _c2i_unverified_entry == NULL, "");
  assert(base <= _c2i_no_clinit_check_entry || _c2i_no_clinit_check_entry == NULL, "");
  return base;
}

void AdapterHandlerEntry::relocate(address new_base) {
  address old_base = base_address();
  assert(old_base != NULL, "");
  ptrdiff_t delta = new_base - old_base;
  if (_i2c_entry != NULL)
    _i2c_entry += delta;
  if (_c2i_entry != NULL)
    _c2i_entry += delta;
  if (_c2i_unverified_entry != NULL)
    _c2i_unverified_entry += delta;
  if (_c2i_no_clinit_check_entry != NULL)
    _c2i_no_clinit_check_entry += delta;
  assert(base_address() == new_base, "");
}


void AdapterHandlerEntry::deallocate() {
  delete _fingerprint;
#ifdef ASSERT
  FREE_C_HEAP_ARRAY(unsigned char, _saved_code);
#endif
}


#ifdef ASSERT
// Capture the code before relocation so that it can be compared
// against other versions.  If the code is captured after relocation
// then relative instructions won't be equivalent.
void AdapterHandlerEntry::save_code(unsigned char* buffer, int length) {
  _saved_code = NEW_C_HEAP_ARRAY(unsigned char, length, mtCode);
  _saved_code_length = length;
  memcpy(_saved_code, buffer, length);
}


bool AdapterHandlerEntry::compare_code(AdapterHandlerEntry* other) {
  assert(_saved_code != NULL && other->_saved_code != NULL, "code not saved");

  if (other->_saved_code_length != _saved_code_length) {
    return false;
  }

  return memcmp(other->_saved_code, _saved_code, _saved_code_length) == 0;
}
#endif


/**
 * Create a native wrapper for this native method.  The wrapper converts the
 * Java-compiled calling convention to the native convention, handles
 * arguments, and transitions to native.  On return from the native we transition
 * back to java blocking if a safepoint is in progress.
 */
void AdapterHandlerLibrary::create_native_wrapper(const methodHandle& method) {
  ResourceMark rm;
  nmethod* nm = NULL;
  address critical_entry = NULL;

  assert(method->is_native(), "must be native");
  assert(method->is_method_handle_intrinsic() ||
         method->has_native_function(), "must have something valid to call!");

  if (CriticalJNINatives && !method->is_method_handle_intrinsic()) {
    // We perform the I/O with transition to native before acquiring AdapterHandlerLibrary_lock.
    critical_entry = NativeLookup::lookup_critical_entry(method);
  }

  {
    // Perform the work while holding the lock, but perform any printing outside the lock
    MutexLocker mu(AdapterHandlerLibrary_lock);
    // See if somebody beat us to it
    if (method->code() != NULL) {
      return;
    }

    const int compile_id = CompileBroker::assign_compile_id(method, CompileBroker::standard_entry_bci);
    assert(compile_id > 0, "Must generate native wrapper");


    ResourceMark rm;
    BufferBlob*  buf = buffer_blob(); // the temporary code buffer in CodeCache
    if (buf != NULL) {
      CodeBuffer buffer(buf);
      struct { double data[20]; } locs_buf;
      buffer.insts()->initialize_shared_locs((relocInfo*)&locs_buf, sizeof(locs_buf) / sizeof(relocInfo));
#if defined(AARCH64)
      // On AArch64 with ZGC and nmethod entry barriers, we need all oops to be
      // in the constant pool to ensure ordering between the barrier and oops
      // accesses. For native_wrappers we need a constant.
      buffer.initialize_consts_size(8);
#endif
      MacroAssembler _masm(&buffer);

      // Fill in the signature array, for the calling-convention call.
      const int total_args_passed = method->size_of_parameters();

      VMRegPair stack_regs[16];
      VMRegPair* regs = (total_args_passed <= 16) ? stack_regs : NEW_RESOURCE_ARRAY(VMRegPair, total_args_passed);

      AdapterSignatureIterator si(method->signature(), method->constMethod()->fingerprint(),
                              method->is_static(), total_args_passed);
      BasicType* sig_bt = si.basic_types();
      assert(si.slots() == total_args_passed, "");
      BasicType ret_type = si.return_type();

      // Now get the compiled-Java arguments layout.
      int comp_args_on_stack = SharedRuntime::java_calling_convention(sig_bt, regs, total_args_passed);

      // Generate the compiled-to-native wrapper code
      nm = SharedRuntime::generate_native_wrapper(&_masm, method, compile_id, sig_bt, regs, ret_type, critical_entry);

      if (nm != NULL) {
        {
          MutexLocker pl(CompiledMethod_lock, Mutex::_no_safepoint_check_flag);
          if (nm->make_in_use()) {
            method->set_code(method, nm);
          }
        }

        DirectiveSet* directive = DirectivesStack::getDefaultDirective(CompileBroker::compiler(CompLevel_simple));
        if (directive->PrintAssemblyOption) {
          nm->print_code();
        }
        DirectivesStack::release(directive);
      }
    }
  } // Unlock AdapterHandlerLibrary_lock


  // Install the generated code.
  if (nm != NULL) {
    const char *msg = method->is_static() ? "(static)" : "";
    CompileTask::print_ul(nm, msg);
    if (PrintCompilation) {
      ttyLocker ttyl;
      CompileTask::print(tty, nm, msg);
    }
    nm->post_compiled_method_load_event();
  }
}

// -------------------------------------------------------------------------
// Java-Java calling convention
// (what you use when Java calls Java)

//------------------------------name_for_receiver----------------------------------
// For a given signature, return the VMReg for parameter 0.
VMReg SharedRuntime::name_for_receiver() {
  VMRegPair regs;
  BasicType sig_bt = T_OBJECT;
  (void) java_calling_convention(&sig_bt, &regs, 1);
  // Return argument 0 register.  In the LP64 build pointers
  // take 2 registers, but the VM wants only the 'main' name.
  return regs.first();
}

VMRegPair *SharedRuntime::find_callee_arguments(Symbol* sig, bool has_receiver, bool has_appendix, int* arg_size) {
  // This method is returning a data structure allocating as a
  // ResourceObject, so do not put any ResourceMarks in here.

  BasicType *sig_bt = NEW_RESOURCE_ARRAY(BasicType, 256);
  VMRegPair *regs = NEW_RESOURCE_ARRAY(VMRegPair, 256);
  int cnt = 0;
  if (has_receiver) {
    sig_bt[cnt++] = T_OBJECT; // Receiver is argument 0; not in signature
  }

  for (SignatureStream ss(sig); !ss.at_return_type(); ss.next()) {
    BasicType type = ss.type();
    sig_bt[cnt++] = type;
    if (is_double_word_type(type))
      sig_bt[cnt++] = T_VOID;
  }

  if (has_appendix) {
    sig_bt[cnt++] = T_OBJECT;
  }

  assert(cnt < 256, "grow table size");

  int comp_args_on_stack;
  comp_args_on_stack = java_calling_convention(sig_bt, regs, cnt);

  // the calling convention doesn't count out_preserve_stack_slots so
  // we must add that in to get "true" stack offsets.

  if (comp_args_on_stack) {
    for (int i = 0; i < cnt; i++) {
      VMReg reg1 = regs[i].first();
      if (reg1->is_stack()) {
        // Yuck
        reg1 = reg1->bias(out_preserve_stack_slots());
      }
      VMReg reg2 = regs[i].second();
      if (reg2->is_stack()) {
        // Yuck
        reg2 = reg2->bias(out_preserve_stack_slots());
      }
      regs[i].set_pair(reg2, reg1);
    }
  }

  // results
  *arg_size = cnt;
  return regs;
}

// OSR Migration Code
//
// This code is used convert interpreter frames into compiled frames.  It is
// called from very start of a compiled OSR nmethod.  A temp array is
// allocated to hold the interesting bits of the interpreter frame.  All
// active locks are inflated to allow them to move.  The displaced headers and
// active interpreter locals are copied into the temp buffer.  Then we return
// back to the compiled code.  The compiled code then pops the current
// interpreter frame off the stack and pushes a new compiled frame.  Then it
// copies the interpreter locals and displaced headers where it wants.
// Finally it calls back to free the temp buffer.
//
// All of this is done NOT at any Safepoint, nor is any safepoint or GC allowed.

JRT_LEAF(intptr_t*, SharedRuntime::OSR_migration_begin( JavaThread *current) )
  // During OSR migration, we unwind the interpreted frame and replace it with a compiled
  // frame. The stack watermark code below ensures that the interpreted frame is processed
  // before it gets unwound. This is helpful as the size of the compiled frame could be
  // larger than the interpreted frame, which could result in the new frame not being
  // processed correctly.
  StackWatermarkSet::before_unwind(current);

  //
  // This code is dependent on the memory layout of the interpreter local
  // array and the monitors. On all of our platforms the layout is identical
  // so this code is shared. If some platform lays the their arrays out
  // differently then this code could move to platform specific code or
  // the code here could be modified to copy items one at a time using
  // frame accessor methods and be platform independent.

  frame fr = current->last_frame();
  assert(fr.is_interpreted_frame(), "");
  assert(fr.interpreter_frame_expression_stack_size()==0, "only handle empty stacks");

  // Figure out how many monitors are active.
  int active_monitor_count = 0;
  for (BasicObjectLock *kptr = fr.interpreter_frame_monitor_end();
       kptr < fr.interpreter_frame_monitor_begin();
       kptr = fr.next_monitor_in_interpreter_frame(kptr) ) {
    if (kptr->obj() != NULL) active_monitor_count++;
  }

  // QQQ we could place number of active monitors in the array so that compiled code
  // could double check it.

  Method* moop = fr.interpreter_frame_method();
  int max_locals = moop->max_locals();
  // Allocate temp buffer, 1 word per local & 2 per active monitor
  int buf_size_words = max_locals + active_monitor_count * BasicObjectLock::size();
  intptr_t *buf = NEW_C_HEAP_ARRAY(intptr_t,buf_size_words, mtCode);

  // Copy the locals.  Order is preserved so that loading of longs works.
  // Since there's no GC I can copy the oops blindly.
  assert(sizeof(HeapWord)==sizeof(intptr_t), "fix this code");
  Copy::disjoint_words((HeapWord*)fr.interpreter_frame_local_at(max_locals-1),
                       (HeapWord*)&buf[0],
                       max_locals);

  // Inflate locks.  Copy the displaced headers.  Be careful, there can be holes.
  int i = max_locals;
  for (BasicObjectLock *kptr2 = fr.interpreter_frame_monitor_end();
       kptr2 < fr.interpreter_frame_monitor_begin();
       kptr2 = fr.next_monitor_in_interpreter_frame(kptr2) ) {
    if (kptr2->obj() != NULL) {         // Avoid 'holes' in the monitor array
      BasicLock *lock = kptr2->lock();
      // Inflate so the object's header no longer refers to the BasicLock.
      if (lock->displaced_header().is_unlocked()) {
        // The object is locked and the resulting ObjectMonitor* will also be
        // locked so it can't be async deflated until ownership is dropped.
        // See the big comment in basicLock.cpp: BasicLock::move_to().
        ObjectSynchronizer::inflate_helper(kptr2->obj());
      }
      // Now the displaced header is free to move because the
      // object's header no longer refers to it.
      buf[i++] = (intptr_t)lock->displaced_header().value();
      buf[i++] = cast_from_oop<intptr_t>(kptr2->obj());
    }
  }
  assert(i - max_locals == active_monitor_count*2, "found the expected number of monitors");

  return buf;
JRT_END

JRT_LEAF(void, SharedRuntime::OSR_migration_end( intptr_t* buf) )
  FREE_C_HEAP_ARRAY(intptr_t, buf);
JRT_END

bool AdapterHandlerLibrary::contains(const CodeBlob* b) {
  AdapterHandlerTableIterator iter(_adapters);
  while (iter.has_next()) {
    AdapterHandlerEntry* a = iter.next();
    if (b == CodeCache::find_blob(a->get_i2c_entry())) return true;
  }
  return false;
}

void AdapterHandlerLibrary::print_handler_on(outputStream* st, const CodeBlob* b) {
  AdapterHandlerTableIterator iter(_adapters);
  while (iter.has_next()) {
    AdapterHandlerEntry* a = iter.next();
    if (b == CodeCache::find_blob(a->get_i2c_entry())) {
      st->print("Adapter for signature: ");
      a->print_adapter_on(tty);
      return;
    }
  }
  assert(false, "Should have found handler");
}

void AdapterHandlerEntry::print_adapter_on(outputStream* st) const {
  st->print("AHE@" INTPTR_FORMAT ": %s", p2i(this), fingerprint()->as_string());
  if (get_i2c_entry() != NULL) {
    st->print(" i2c: " INTPTR_FORMAT, p2i(get_i2c_entry()));
  }
  if (get_c2i_entry() != NULL) {
    st->print(" c2i: " INTPTR_FORMAT, p2i(get_c2i_entry()));
  }
  if (get_c2i_unverified_entry() != NULL) {
    st->print(" c2iUV: " INTPTR_FORMAT, p2i(get_c2i_unverified_entry()));
  }
  if (get_c2i_no_clinit_check_entry() != NULL) {
    st->print(" c2iNCI: " INTPTR_FORMAT, p2i(get_c2i_no_clinit_check_entry()));
  }
  st->cr();
}

#ifndef PRODUCT

void AdapterHandlerLibrary::print_statistics() {
  _adapters->print_statistics();
}

#endif /* PRODUCT */

JRT_LEAF(void, SharedRuntime::enable_stack_reserved_zone(JavaThread* current))
  StackOverflow* overflow_state = current->stack_overflow_state();
  overflow_state->enable_stack_reserved_zone(/*check_if_disabled*/true);
  overflow_state->set_reserved_stack_activation(current->stack_base());
JRT_END

frame SharedRuntime::look_for_reserved_stack_annotated_method(JavaThread* current, frame fr) {
  ResourceMark rm(current);
  frame activation;
  CompiledMethod* nm = NULL;
  int count = 1;

  assert(fr.is_java_frame(), "Must start on Java frame");

  while (true) {
    Method* method = NULL;
    bool found = false;
    if (fr.is_interpreted_frame()) {
      method = fr.interpreter_frame_method();
      if (method != NULL && method->has_reserved_stack_access()) {
        found = true;
      }
    } else {
      CodeBlob* cb = fr.cb();
      if (cb != NULL && cb->is_compiled()) {
        nm = cb->as_compiled_method();
        method = nm->method();
        // scope_desc_near() must be used, instead of scope_desc_at() because on
        // SPARC, the pcDesc can be on the delay slot after the call instruction.
        for (ScopeDesc *sd = nm->scope_desc_near(fr.pc()); sd != NULL; sd = sd->sender()) {
          method = sd->method();
          if (method != NULL && method->has_reserved_stack_access()) {
            found = true;
      }
    }
      }
    }
    if (found) {
      activation = fr;
      warning("Potentially dangerous stack overflow in "
              "ReservedStackAccess annotated method %s [%d]",
              method->name_and_sig_as_C_string(), count++);
      EventReservedStackActivation event;
      if (event.should_commit()) {
        event.set_method(method);
        event.commit();
      }
    }
    if (fr.is_first_java_frame()) {
      break;
    } else {
      fr = fr.java_sender();
    }
  }
  return activation;
}

void SharedRuntime::on_slowpath_allocation_exit(JavaThread* current) {
  // After any safepoint, just before going back to compiled code,
  // we inform the GC that we will be doing initializing writes to
  // this object in the future without emitting card-marks, so
  // GC may take any compensating steps.

  oop new_obj = current->vm_result();
  if (new_obj == NULL) return;

  BarrierSet *bs = BarrierSet::barrier_set();
  bs->on_slowpath_allocation_exit(current, new_obj);
}
