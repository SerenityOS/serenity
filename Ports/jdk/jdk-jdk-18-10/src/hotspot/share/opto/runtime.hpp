/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OPTO_RUNTIME_HPP
#define SHARE_OPTO_RUNTIME_HPP

#include "code/codeBlob.hpp"
#include "opto/machnode.hpp"
#include "opto/optoreg.hpp"
#include "opto/type.hpp"
#include "runtime/rtmLocking.hpp"
#include "runtime/deoptimization.hpp"
#include "runtime/vframe.hpp"

//------------------------------OptoRuntime------------------------------------
// Opto compiler runtime routines
//
// These are all generated from Ideal graphs.  They are called with the
// Java calling convention.  Internally they call C++.  They are made once at
// startup time and Opto compiles calls to them later.
// Things are broken up into quads: the signature they will be called with,
// the address of the generated code, the corresponding C++ code and an
// nmethod.

// The signature (returned by "xxx_Type()") is used at startup time by the
// Generator to make the generated code "xxx_Java".  Opto compiles calls
// to the generated code "xxx_Java".  When the compiled code gets executed,
// it calls the C++ code "xxx_C".  The generated nmethod is saved in the
// CodeCache.  Exception handlers use the nmethod to get the callee-save
// register OopMaps.
class CallInfo;

//
// NamedCounters are tagged counters which can be used for profiling
// code in various ways.  Currently they are used by the lock coarsening code
//

class NamedCounter : public CHeapObj<mtCompiler> {
public:
    enum CounterTag {
    NoTag,
    LockCounter,
    EliminatedLockCounter,
    RTMLockingCounter
  };

private:
  const char *  _name;
  int           _count;
  CounterTag    _tag;
  NamedCounter* _next;

 public:
  NamedCounter(const char *n, CounterTag tag = NoTag):
    _name(n == NULL ? NULL : os::strdup(n)),
    _count(0),
    _tag(tag),
    _next(NULL) {}

  ~NamedCounter() {
    if (_name != NULL) {
      os::free((void*)_name);
    }
  }

  const char * name() const     { return _name; }
  int count() const             { return _count; }
  address addr()                { return (address)&_count; }
  CounterTag tag() const        { return _tag; }
  void set_tag(CounterTag tag)  { _tag = tag; }

  NamedCounter* next() const    { return _next; }
  void set_next(NamedCounter* next) {
    assert(_next == NULL || next == NULL, "already set");
    _next = next;
  }

};

class RTMLockingNamedCounter : public NamedCounter {
 private:
 RTMLockingCounters _counters;

 public:
  RTMLockingNamedCounter(const char *n) :
    NamedCounter(n, RTMLockingCounter), _counters() {}

  RTMLockingCounters* counters() { return &_counters; }
};

typedef const TypeFunc*(*TypeFunc_generator)();

class OptoRuntime : public AllStatic {
  friend class Matcher;  // allow access to stub names

 private:
  // define stubs
  static address generate_stub(ciEnv* ci_env, TypeFunc_generator gen, address C_function, const char* name, int is_fancy_jump, bool pass_tls, bool return_pc);

  // References to generated stubs
  static address _new_instance_Java;
  static address _new_array_Java;
  static address _new_array_nozero_Java;
  static address _multianewarray2_Java;
  static address _multianewarray3_Java;
  static address _multianewarray4_Java;
  static address _multianewarray5_Java;
  static address _multianewarrayN_Java;
  static address _vtable_must_compile_Java;
  static address _complete_monitor_locking_Java;
  static address _rethrow_Java;
  static address _monitor_notify_Java;
  static address _monitor_notifyAll_Java;

  static address _slow_arraycopy_Java;
  static address _register_finalizer_Java;

  //
  // Implementation of runtime methods
  // =================================

  // Allocate storage for a Java instance.
  static void new_instance_C(Klass* instance_klass, JavaThread* current);

  // Allocate storage for a objArray or typeArray
  static void new_array_C(Klass* array_klass, int len, JavaThread* current);
  static void new_array_nozero_C(Klass* array_klass, int len, JavaThread* current);

  // Allocate storage for a multi-dimensional arrays
  // Note: needs to be fixed for arbitrary number of dimensions
  static void multianewarray2_C(Klass* klass, int len1, int len2, JavaThread* current);
  static void multianewarray3_C(Klass* klass, int len1, int len2, int len3, JavaThread* current);
  static void multianewarray4_C(Klass* klass, int len1, int len2, int len3, int len4, JavaThread* current);
  static void multianewarray5_C(Klass* klass, int len1, int len2, int len3, int len4, int len5, JavaThread* current);
  static void multianewarrayN_C(Klass* klass, arrayOopDesc* dims, JavaThread* current);

public:
  static void monitor_notify_C(oopDesc* obj, JavaThread* current);
  static void monitor_notifyAll_C(oopDesc* obj, JavaThread* current);

private:

  // Implicit exception support
  static void throw_null_exception_C(JavaThread* thread);

  // Exception handling
  static address handle_exception_C       (JavaThread* current);
  static address handle_exception_C_helper(JavaThread* current, nmethod*& nm);
  static address rethrow_C                (oopDesc* exception, JavaThread *thread, address return_pc );
  static void deoptimize_caller_frame     (JavaThread *thread);
  static void deoptimize_caller_frame     (JavaThread *thread, bool doit);
  static bool is_deoptimized_caller_frame (JavaThread *thread);

  // CodeBlob support
  // ===================================================================

  static ExceptionBlob*       _exception_blob;
  static void generate_exception_blob();

  static void register_finalizer(oopDesc* obj, JavaThread* current);

 public:

  static bool is_callee_saved_register(MachRegisterNumbers reg);

  // One time only generate runtime code stubs. Returns true
  // when runtime stubs have been generated successfully and
  // false otherwise.
  static bool generate(ciEnv* env);

  // Returns the name of a stub
  static const char* stub_name(address entry);

  // access to runtime stubs entry points for java code
  static address new_instance_Java()                     { return _new_instance_Java; }
  static address new_array_Java()                        { return _new_array_Java; }
  static address new_array_nozero_Java()                 { return _new_array_nozero_Java; }
  static address multianewarray2_Java()                  { return _multianewarray2_Java; }
  static address multianewarray3_Java()                  { return _multianewarray3_Java; }
  static address multianewarray4_Java()                  { return _multianewarray4_Java; }
  static address multianewarray5_Java()                  { return _multianewarray5_Java; }
  static address multianewarrayN_Java()                  { return _multianewarrayN_Java; }
  static address vtable_must_compile_stub()              { return _vtable_must_compile_Java; }
  static address complete_monitor_locking_Java()         { return _complete_monitor_locking_Java; }
  static address monitor_notify_Java()                   { return _monitor_notify_Java; }
  static address monitor_notifyAll_Java()                { return _monitor_notifyAll_Java; }

  static address slow_arraycopy_Java()                   { return _slow_arraycopy_Java; }
  static address register_finalizer_Java()               { return _register_finalizer_Java; }

  static ExceptionBlob*    exception_blob()                      { return _exception_blob; }

  // Implicit exception support
  static void throw_div0_exception_C      (JavaThread* thread);
  static void throw_stack_overflow_error_C(JavaThread* thread);

  // Exception handling
  static address rethrow_stub()             { return _rethrow_Java; }


  // Type functions
  // ======================================================

  static const TypeFunc* new_instance_Type(); // object allocation (slow case)
  static const TypeFunc* new_array_Type ();   // [a]newarray (slow case)
  static const TypeFunc* multianewarray_Type(int ndim); // multianewarray
  static const TypeFunc* multianewarray2_Type(); // multianewarray
  static const TypeFunc* multianewarray3_Type(); // multianewarray
  static const TypeFunc* multianewarray4_Type(); // multianewarray
  static const TypeFunc* multianewarray5_Type(); // multianewarray
  static const TypeFunc* multianewarrayN_Type(); // multianewarray
  static const TypeFunc* complete_monitor_enter_Type();
  static const TypeFunc* complete_monitor_exit_Type();
  static const TypeFunc* monitor_notify_Type();
  static const TypeFunc* uncommon_trap_Type();
  static const TypeFunc* athrow_Type();
  static const TypeFunc* rethrow_Type();
  static const TypeFunc* Math_D_D_Type();  // sin,cos & friends
  static const TypeFunc* Math_DD_D_Type(); // mod,pow & friends
  static const TypeFunc* Math_Vector_Vector_Type(uint num_arg, const TypeVect* in_type, const TypeVect* out_type);
  static const TypeFunc* modf_Type();
  static const TypeFunc* l2f_Type();
  static const TypeFunc* void_long_Type();

  static const TypeFunc* flush_windows_Type();

  // arraycopy routine types
  static const TypeFunc* fast_arraycopy_Type(); // bit-blasters
  static const TypeFunc* checkcast_arraycopy_Type();
  static const TypeFunc* generic_arraycopy_Type();
  static const TypeFunc* slow_arraycopy_Type();   // the full routine

  static const TypeFunc* array_fill_Type();

  static const TypeFunc* aescrypt_block_Type();
  static const TypeFunc* cipherBlockChaining_aescrypt_Type();
  static const TypeFunc* electronicCodeBook_aescrypt_Type();
  static const TypeFunc* counterMode_aescrypt_Type();

  static const TypeFunc* digestBase_implCompress_Type(bool is_sha3);
  static const TypeFunc* digestBase_implCompressMB_Type(bool is_sha3);

  static const TypeFunc* multiplyToLen_Type();
  static const TypeFunc* montgomeryMultiply_Type();
  static const TypeFunc* montgomerySquare_Type();

  static const TypeFunc* squareToLen_Type();

  static const TypeFunc* mulAdd_Type();

  static const TypeFunc* bigIntegerShift_Type();

  static const TypeFunc* vectorizedMismatch_Type();

  static const TypeFunc* ghash_processBlocks_Type();
  static const TypeFunc* base64_encodeBlock_Type();
  static const TypeFunc* base64_decodeBlock_Type();

  static const TypeFunc* updateBytesCRC32_Type();
  static const TypeFunc* updateBytesCRC32C_Type();

  static const TypeFunc* updateBytesAdler32_Type();

  // leaf on stack replacement interpreter accessor types
  static const TypeFunc* osr_end_Type();

  static const TypeFunc* register_finalizer_Type();

  JFR_ONLY(static const TypeFunc* get_class_id_intrinsic_Type();)

  // Dtrace support
  static const TypeFunc* dtrace_method_entry_exit_Type();
  static const TypeFunc* dtrace_object_alloc_Type();

 private:
 static NamedCounter * volatile _named_counters;

 public:
 // helper function which creates a named counter labeled with the
 // if they are available
 static NamedCounter* new_named_counter(JVMState* jvms, NamedCounter::CounterTag tag);

 // dumps all the named counters
 static void          print_named_counters();

};

#endif // SHARE_OPTO_RUNTIME_HPP
