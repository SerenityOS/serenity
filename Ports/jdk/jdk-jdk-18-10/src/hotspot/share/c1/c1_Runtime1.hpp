/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_C1_C1_RUNTIME1_HPP
#define SHARE_C1_C1_RUNTIME1_HPP

#include "c1/c1_FrameMap.hpp"
#include "code/stubs.hpp"
#include "interpreter/interpreter.hpp"
#include "memory/allocation.hpp"
#include "runtime/deoptimization.hpp"

class StubAssembler;

// The Runtime1 holds all assembly stubs and VM
// runtime routines needed by code code generated
// by the Compiler1.

#define RUNTIME1_STUBS(stub, last_entry) \
  stub(dtrace_object_alloc)          \
  stub(unwind_exception)             \
  stub(forward_exception)            \
  stub(throw_range_check_failed)       /* throws ArrayIndexOutOfBoundsException */ \
  stub(throw_index_exception)          /* throws IndexOutOfBoundsException */ \
  stub(throw_div0_exception)         \
  stub(throw_null_pointer_exception) \
  stub(register_finalizer)           \
  stub(new_instance)                 \
  stub(fast_new_instance)            \
  stub(fast_new_instance_init_check) \
  stub(new_type_array)               \
  stub(new_object_array)             \
  stub(new_multi_array)              \
  stub(handle_exception_nofpu)         /* optimized version that does not preserve fpu registers */ \
  stub(handle_exception)             \
  stub(handle_exception_from_callee) \
  stub(throw_array_store_exception)  \
  stub(throw_class_cast_exception)   \
  stub(throw_incompatible_class_change_error)   \
  stub(slow_subtype_check)           \
  stub(monitorenter)                 \
  stub(monitorenter_nofpu)             /* optimized version that does not preserve fpu registers */ \
  stub(monitorexit)                  \
  stub(monitorexit_nofpu)              /* optimized version that does not preserve fpu registers */ \
  stub(deoptimize)                   \
  stub(access_field_patching)        \
  stub(load_klass_patching)          \
  stub(load_mirror_patching)         \
  stub(load_appendix_patching)       \
  stub(fpu2long_stub)                \
  stub(counter_overflow)             \
  stub(predicate_failed_trap)        \
  last_entry(number_of_ids)

#define DECLARE_STUB_ID(x)       x ## _id ,
#define DECLARE_LAST_STUB_ID(x)  x
#define STUB_NAME(x)             #x " Runtime1 stub",
#define LAST_STUB_NAME(x)        #x " Runtime1 stub"

class StubAssemblerCodeGenClosure: public Closure {
 public:
  virtual OopMapSet* generate_code(StubAssembler* sasm) = 0;
};

class Runtime1: public AllStatic {
  friend class VMStructs;
  friend class ArrayCopyStub;

 public:
  enum StubID {
    RUNTIME1_STUBS(DECLARE_STUB_ID, DECLARE_LAST_STUB_ID)
  };

  // statistics
#ifndef PRODUCT
  static int _generic_arraycopystub_cnt;
  static int _arraycopy_slowcase_cnt;
  static int _arraycopy_checkcast_cnt;
  static int _arraycopy_checkcast_attempt_cnt;
  static int _new_type_array_slowcase_cnt;
  static int _new_object_array_slowcase_cnt;
  static int _new_instance_slowcase_cnt;
  static int _new_multi_array_slowcase_cnt;
  static int _monitorenter_slowcase_cnt;
  static int _monitorexit_slowcase_cnt;
  static int _patch_code_slowcase_cnt;
  static int _throw_range_check_exception_count;
  static int _throw_index_exception_count;
  static int _throw_div0_exception_count;
  static int _throw_null_pointer_exception_count;
  static int _throw_class_cast_exception_count;
  static int _throw_incompatible_class_change_error_count;
  static int _throw_count;
#endif

 private:
  static CodeBlob* _blobs[number_of_ids];
  static const char* _blob_names[];

  // stub generation
 public:
  static CodeBlob*  generate_blob(BufferBlob* buffer_blob, int stub_id, const char* name, bool expect_oop_map, StubAssemblerCodeGenClosure *cl);
  static void       generate_blob_for(BufferBlob* blob, StubID id);
  static OopMapSet* generate_code_for(StubID id, StubAssembler* sasm);
 private:
  static OopMapSet* generate_exception_throw(StubAssembler* sasm, address target, bool has_argument);
  static OopMapSet* generate_handle_exception(StubID id, StubAssembler* sasm);
  static void       generate_unwind_exception(StubAssembler *sasm);
  static OopMapSet* generate_patching(StubAssembler* sasm, address target);

  static OopMapSet* generate_stub_call(StubAssembler* sasm, Register result, address entry,
                                       Register arg1 = noreg, Register arg2 = noreg, Register arg3 = noreg);

  // runtime entry points
  static void new_instance    (JavaThread* current, Klass* klass);
  static void new_type_array  (JavaThread* current, Klass* klass, jint length);
  static void new_object_array(JavaThread* current, Klass* klass, jint length);
  static void new_multi_array (JavaThread* current, Klass* klass, int rank, jint* dims);

  static address counter_overflow(JavaThread* current, int bci, Method* method);

  static void unimplemented_entry(JavaThread* current, StubID id);

  static address exception_handler_for_pc(JavaThread* current);

  static void throw_range_check_exception(JavaThread* current, int index, arrayOopDesc* a);
  static void throw_index_exception(JavaThread* current, int index);
  static void throw_div0_exception(JavaThread* current);
  static void throw_null_pointer_exception(JavaThread* current);
  static void throw_class_cast_exception(JavaThread* current, oopDesc* object);
  static void throw_incompatible_class_change_error(JavaThread* current);
  static void throw_array_store_exception(JavaThread* current, oopDesc* object);

  static void monitorenter(JavaThread* current, oopDesc* obj, BasicObjectLock* lock);
  static void monitorexit (JavaThread* current, BasicObjectLock* lock);

  static void deoptimize(JavaThread* current, jint trap_request);

  static int access_field_patching(JavaThread* current);
  static int move_klass_patching(JavaThread* current);
  static int move_mirror_patching(JavaThread* current);
  static int move_appendix_patching(JavaThread* current);

  static void patch_code(JavaThread* current, StubID stub_id);

 public:
  // initialization
  static void initialize(BufferBlob* blob);
  static void initialize_pd();

  // stubs
  static CodeBlob* blob_for (StubID id);
  static address   entry_for(StubID id)          { return blob_for(id)->code_begin(); }
  static const char* name_for (StubID id);
  static const char* name_for_address(address entry);

  // platform might add runtime names.
  static const char* pd_name_for_address(address entry);

  // method tracing
  static void trace_block_entry(jint block_id);

#ifndef PRODUCT
  static address throw_count_address()               { return (address)&_throw_count;             }
  static address arraycopy_count_address(BasicType type);
#endif

  // directly accessible leaf routine
  static int  is_instance_of(oopDesc* mirror, oopDesc* obj);

  static void predicate_failed_trap(JavaThread* current);

  static void print_statistics()                 PRODUCT_RETURN;
};

#endif // SHARE_C1_C1_RUNTIME1_HPP
