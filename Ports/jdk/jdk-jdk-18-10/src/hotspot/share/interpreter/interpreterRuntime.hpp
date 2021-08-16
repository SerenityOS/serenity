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

#ifndef SHARE_INTERPRETER_INTERPRETERRUNTIME_HPP
#define SHARE_INTERPRETER_INTERPRETERRUNTIME_HPP

#include "interpreter/bytecode.hpp"
#include "interpreter/linkResolver.hpp"
#include "oops/method.hpp"
#include "runtime/frame.hpp"
#include "runtime/signature.hpp"
#include "runtime/thread.hpp"
#include "utilities/macros.hpp"

class BufferBlob;
class CodeBuffer;

// The InterpreterRuntime is called by the interpreter for everything
// that cannot/should not be dealt with in assembly and needs C support.

class InterpreterRuntime: AllStatic {
  friend class BytecodeClosure; // for method and bcp
  friend class PrintingClosure; // for method and bcp

 private:

  static void      set_bcp_and_mdp(address bcp, JavaThread* current);
  static void      note_trap_inner(JavaThread* current, int reason,
                                   const methodHandle& trap_method, int trap_bci);
  static void      note_trap(JavaThread* current, int reason);

  // Inner work method for Interpreter's frequency counter overflow.
  static nmethod* frequency_counter_overflow_inner(JavaThread* current, address branch_bcp);

 public:
  // Constants
  static void    ldc           (JavaThread* current, bool wide);
  static void    resolve_ldc   (JavaThread* current, Bytecodes::Code bytecode);

  // Allocation
  static void    _new          (JavaThread* current, ConstantPool* pool, int index);
  static void    newarray      (JavaThread* current, BasicType type, jint size);
  static void    anewarray     (JavaThread* current, ConstantPool* pool, int index, jint size);
  static void    multianewarray(JavaThread* current, jint* first_size_address);
  static void    register_finalizer(JavaThread* current, oopDesc* obj);

  // Quicken instance-of and check-cast bytecodes
  static void    quicken_io_cc(JavaThread* current);

  // Exceptions thrown by the interpreter
  static void    throw_AbstractMethodError(JavaThread* current);
  static void    throw_AbstractMethodErrorWithMethod(JavaThread* current, Method* oop);
  static void    throw_AbstractMethodErrorVerbose(JavaThread* current,
                                                  Klass* recvKlass,
                                                  Method* missingMethod);

  static void    throw_IncompatibleClassChangeError(JavaThread* current);
  static void    throw_IncompatibleClassChangeErrorVerbose(JavaThread* current,
                                                           Klass* resc,
                                                           Klass* interfaceKlass);
  static void    throw_StackOverflowError(JavaThread* current);
  static void    throw_delayed_StackOverflowError(JavaThread* current);
  static void    throw_ArrayIndexOutOfBoundsException(JavaThread* current, arrayOopDesc* a, jint index);
  static void    throw_ClassCastException(JavaThread* current, oopDesc* obj);
  static void    throw_NullPointerException(JavaThread* current);

  static void    create_exception(JavaThread* current, char* name, char* message);
  static void    create_klass_exception(JavaThread* current, char* name, oopDesc* obj);
  static address exception_handler_for_exception(JavaThread* current, oopDesc* exception);
#if INCLUDE_JVMTI
  static void    member_name_arg_or_null(JavaThread* current, address dmh, Method* m, address bcp);
#endif
  static void    throw_pending_exception(JavaThread* current);

  static void resolve_from_cache(JavaThread* current, Bytecodes::Code bytecode);
 private:
  // Statics & fields
  static void resolve_get_put(JavaThread* current, Bytecodes::Code bytecode);

  // Calls
  static void resolve_invoke(JavaThread* current, Bytecodes::Code bytecode);
  static void resolve_invokehandle (JavaThread* current);
  static void resolve_invokedynamic(JavaThread* current);

 public:
  // Synchronization
  static void    monitorenter(JavaThread* current, BasicObjectLock* elem);
  static void    monitorexit (BasicObjectLock* elem);

  static void    throw_illegal_monitor_state_exception(JavaThread* current);
  static void    new_illegal_monitor_state_exception(JavaThread* current);

  // Breakpoints
  static void _breakpoint(JavaThread* current, Method* method, address bcp);
  static Bytecodes::Code get_original_bytecode_at(JavaThread* current, Method* method, address bcp);
  static void            set_original_bytecode_at(JavaThread* current, Method* method, address bcp, Bytecodes::Code new_code);

  // Safepoints
  static void    at_safepoint(JavaThread* current);
  static void    at_unwind(JavaThread* current);

  // Debugger support
  static void post_field_access(JavaThread* current, oopDesc* obj,
    ConstantPoolCacheEntry *cp_entry);
  static void post_field_modification(JavaThread* current, oopDesc* obj,
    ConstantPoolCacheEntry *cp_entry, jvalue *value);
  static void post_method_entry(JavaThread* current);
  static void post_method_exit (JavaThread* current);
  static int  interpreter_contains(address pc);

  // Native signature handlers
  static void prepare_native_call(JavaThread* current, Method* method);
  static address slow_signature_handler(JavaThread* current,
                                        Method* method,
                                        intptr_t* from, intptr_t* to);

#if defined(IA32) || defined(AMD64) || defined(ARM)
  // Popframe support (only needed on x86, AMD64 and ARM)
  static void popframe_move_outgoing_args(JavaThread* current, void* src_address, void* dest_address);
#endif

  // bytecode tracing is only used by the TraceBytecodes
  static intptr_t trace_bytecode(JavaThread* current, intptr_t preserve_this_value, intptr_t tos, intptr_t tos2) PRODUCT_RETURN0;

  // Platform dependent stuff
#include CPU_HEADER(interpreterRT)

  // optional normalization of fingerprints to reduce the number of adapters
  static uint64_t normalize_fast_native_fingerprint(uint64_t fingerprint);

  // Interpreter's frequency counter overflow
  static nmethod* frequency_counter_overflow(JavaThread* current, address branch_bcp);

  // Interpreter profiling support
  static jint    bcp_to_di(Method* method, address cur_bcp);
  static void    update_mdp_for_ret(JavaThread* current, int bci);
#ifdef ASSERT
  static void    verify_mdp(Method* method, address bcp, address mdp);
#endif // ASSERT
  static MethodCounters* build_method_counters(JavaThread* current, Method* m);
};


class SignatureHandlerLibrary: public AllStatic {
 public:
  enum { buffer_size =  1*K }; // the size of the temporary code buffer
  enum { blob_size   = 32*K }; // the size of a handler code blob.

 private:
  static BufferBlob*              _handler_blob; // the current buffer blob containing the generated handlers
  static address                  _handler;      // next available address within _handler_blob;
  static GrowableArray<uint64_t>* _fingerprints; // the fingerprint collection
  static GrowableArray<address>*  _handlers;     // the corresponding handlers
  static address                  _buffer;       // the temporary code buffer

  static address set_handler_blob();
  static void initialize();
  static address set_handler(CodeBuffer* buffer);
  static void pd_set_handler(address handler);

 public:
  static void add(const methodHandle& method);
  static void add(uint64_t fingerprint, address handler);
};

#endif // SHARE_INTERPRETER_INTERPRETERRUNTIME_HPP
