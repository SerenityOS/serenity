/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jni.h"
#include "jvm.h"
#include "classfile/classFileStream.hpp"
#include "classfile/classLoader.hpp"
#include "classfile/classLoadInfo.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmSymbols.hpp"
#include "jfr/jfrEvents.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "oops/access.inline.hpp"
#include "oops/fieldStreams.inline.hpp"
#include "oops/instanceKlass.inline.hpp"
#include "oops/klass.inline.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "prims/unsafe.hpp"
#include "runtime/globals.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/reflection.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/thread.hpp"
#include "runtime/threadSMR.hpp"
#include "runtime/vmOperations.hpp"
#include "runtime/vm_version.hpp"
#include "services/threadService.hpp"
#include "utilities/align.hpp"
#include "utilities/copy.hpp"
#include "utilities/dtrace.hpp"
#include "utilities/macros.hpp"

/**
 * Implementation of the jdk.internal.misc.Unsafe class
 */


#define MAX_OBJECT_SIZE \
  ( arrayOopDesc::header_size(T_DOUBLE) * HeapWordSize \
    + ((julong)max_jint * sizeof(double)) )


#define UNSAFE_ENTRY(result_type, header) \
  JVM_ENTRY(static result_type, header)

#define UNSAFE_LEAF(result_type, header) \
  JVM_LEAF(static result_type, header)

#define UNSAFE_END JVM_END


static inline void* addr_from_java(jlong addr) {
  // This assert fails in a variety of ways on 32-bit systems.
  // It is impossible to predict whether native code that converts
  // pointers to longs will sign-extend or zero-extend the addresses.
  //assert(addr == (uintptr_t)addr, "must not be odd high bits");
  return (void*)(uintptr_t)addr;
}

static inline jlong addr_to_java(void* p) {
  assert(p == (void*)(uintptr_t)p, "must not be odd high bits");
  return (uintptr_t)p;
}


// Note: The VM's obj_field and related accessors use byte-scaled
// ("unscaled") offsets, just as the unsafe methods do.

// However, the method Unsafe.fieldOffset explicitly declines to
// guarantee this.  The field offset values manipulated by the Java user
// through the Unsafe API are opaque cookies that just happen to be byte
// offsets.  We represent this state of affairs by passing the cookies
// through conversion functions when going between the VM and the Unsafe API.
// The conversion functions just happen to be no-ops at present.

static inline jlong field_offset_to_byte_offset(jlong field_offset) {
  return field_offset;
}

static inline jlong field_offset_from_byte_offset(jlong byte_offset) {
  return byte_offset;
}

static inline void assert_field_offset_sane(oop p, jlong field_offset) {
#ifdef ASSERT
  jlong byte_offset = field_offset_to_byte_offset(field_offset);

  if (p != NULL) {
    assert(byte_offset >= 0 && byte_offset <= (jlong)MAX_OBJECT_SIZE, "sane offset");
    if (byte_offset == (jint)byte_offset) {
      void* ptr_plus_disp = cast_from_oop<address>(p) + byte_offset;
      assert(p->field_addr((jint)byte_offset) == ptr_plus_disp,
             "raw [ptr+disp] must be consistent with oop::field_addr");
    }
    jlong p_size = HeapWordSize * (jlong)(p->size());
    assert(byte_offset < p_size, "Unsafe access: offset " INT64_FORMAT " > object's size " INT64_FORMAT, (int64_t)byte_offset, (int64_t)p_size);
  }
#endif
}

static inline void* index_oop_from_field_offset_long(oop p, jlong field_offset) {
  assert_field_offset_sane(p, field_offset);
  jlong byte_offset = field_offset_to_byte_offset(field_offset);

  if (sizeof(char*) == sizeof(jint)) {   // (this constant folds!)
    return cast_from_oop<address>(p) + (jint) byte_offset;
  } else {
    return cast_from_oop<address>(p) +        byte_offset;
  }
}

// Externally callable versions:
// (Use these in compiler intrinsics which emulate unsafe primitives.)
jlong Unsafe_field_offset_to_byte_offset(jlong field_offset) {
  return field_offset;
}
jlong Unsafe_field_offset_from_byte_offset(jlong byte_offset) {
  return byte_offset;
}


///// Data read/writes on the Java heap and in native (off-heap) memory

/**
 * Helper class to wrap memory accesses in JavaThread::doing_unsafe_access()
 */
class GuardUnsafeAccess {
  JavaThread* _thread;

public:
  GuardUnsafeAccess(JavaThread* thread) : _thread(thread) {
    // native/off-heap access which may raise SIGBUS if accessing
    // memory mapped file data in a region of the file which has
    // been truncated and is now invalid.
    _thread->set_doing_unsafe_access(true);
  }

  ~GuardUnsafeAccess() {
    _thread->set_doing_unsafe_access(false);
  }
};

/**
 * Helper class for accessing memory.
 *
 * Normalizes values and wraps accesses in
 * JavaThread::doing_unsafe_access() if needed.
 */
template <typename T>
class MemoryAccess : StackObj {
  JavaThread* _thread;
  oop _obj;
  ptrdiff_t _offset;

  // Resolves and returns the address of the memory access.
  // This raw memory access may fault, so we make sure it happens within the
  // guarded scope by making the access volatile at least. Since the store
  // of Thread::set_doing_unsafe_access() is also volatile, these accesses
  // can not be reordered by the compiler. Therefore, if the access triggers
  // a fault, we will know that Thread::doing_unsafe_access() returns true.
  volatile T* addr() {
    void* addr = index_oop_from_field_offset_long(_obj, _offset);
    return static_cast<volatile T*>(addr);
  }

  template <typename U>
  U normalize_for_write(U x) {
    return x;
  }

  jboolean normalize_for_write(jboolean x) {
    return x & 1;
  }

  template <typename U>
  U normalize_for_read(U x) {
    return x;
  }

  jboolean normalize_for_read(jboolean x) {
    return x != 0;
  }

public:
  MemoryAccess(JavaThread* thread, jobject obj, jlong offset)
    : _thread(thread), _obj(JNIHandles::resolve(obj)), _offset((ptrdiff_t)offset) {
    assert_field_offset_sane(_obj, offset);
  }

  T get() {
    if (_obj == NULL) {
      GuardUnsafeAccess guard(_thread);
      T ret = RawAccess<>::load(addr());
      return normalize_for_read(ret);
    } else {
      T ret = HeapAccess<>::load_at(_obj, _offset);
      return normalize_for_read(ret);
    }
  }

  void put(T x) {
    if (_obj == NULL) {
      GuardUnsafeAccess guard(_thread);
      RawAccess<>::store(addr(), normalize_for_write(x));
    } else {
      HeapAccess<>::store_at(_obj, _offset, normalize_for_write(x));
    }
  }


  T get_volatile() {
    if (_obj == NULL) {
      GuardUnsafeAccess guard(_thread);
      volatile T ret = RawAccess<MO_SEQ_CST>::load(addr());
      return normalize_for_read(ret);
    } else {
      T ret = HeapAccess<MO_SEQ_CST>::load_at(_obj, _offset);
      return normalize_for_read(ret);
    }
  }

  void put_volatile(T x) {
    if (_obj == NULL) {
      GuardUnsafeAccess guard(_thread);
      RawAccess<MO_SEQ_CST>::store(addr(), normalize_for_write(x));
    } else {
      HeapAccess<MO_SEQ_CST>::store_at(_obj, _offset, normalize_for_write(x));
    }
  }
};

// These functions allow a null base pointer with an arbitrary address.
// But if the base pointer is non-null, the offset should make some sense.
// That is, it should be in the range [0, MAX_OBJECT_SIZE].
UNSAFE_ENTRY(jobject, Unsafe_GetReference(JNIEnv *env, jobject unsafe, jobject obj, jlong offset)) {
  oop p = JNIHandles::resolve(obj);
  assert_field_offset_sane(p, offset);
  oop v = HeapAccess<ON_UNKNOWN_OOP_REF>::oop_load_at(p, offset);
  return JNIHandles::make_local(THREAD, v);
} UNSAFE_END

UNSAFE_ENTRY(void, Unsafe_PutReference(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jobject x_h)) {
  oop x = JNIHandles::resolve(x_h);
  oop p = JNIHandles::resolve(obj);
  assert_field_offset_sane(p, offset);
  HeapAccess<ON_UNKNOWN_OOP_REF>::oop_store_at(p, offset, x);
} UNSAFE_END

UNSAFE_ENTRY(jobject, Unsafe_GetReferenceVolatile(JNIEnv *env, jobject unsafe, jobject obj, jlong offset)) {
  oop p = JNIHandles::resolve(obj);
  assert_field_offset_sane(p, offset);
  oop v = HeapAccess<MO_SEQ_CST | ON_UNKNOWN_OOP_REF>::oop_load_at(p, offset);
  return JNIHandles::make_local(THREAD, v);
} UNSAFE_END

UNSAFE_ENTRY(void, Unsafe_PutReferenceVolatile(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jobject x_h)) {
  oop x = JNIHandles::resolve(x_h);
  oop p = JNIHandles::resolve(obj);
  assert_field_offset_sane(p, offset);
  HeapAccess<MO_SEQ_CST | ON_UNKNOWN_OOP_REF>::oop_store_at(p, offset, x);
} UNSAFE_END

UNSAFE_ENTRY(jobject, Unsafe_GetUncompressedObject(JNIEnv *env, jobject unsafe, jlong addr)) {
  oop v = *(oop*) (address) addr;
  return JNIHandles::make_local(THREAD, v);
} UNSAFE_END

#define DEFINE_GETSETOOP(java_type, Type) \
 \
UNSAFE_ENTRY(java_type, Unsafe_Get##Type(JNIEnv *env, jobject unsafe, jobject obj, jlong offset)) { \
  return MemoryAccess<java_type>(thread, obj, offset).get(); \
} UNSAFE_END \
 \
UNSAFE_ENTRY(void, Unsafe_Put##Type(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, java_type x)) { \
  MemoryAccess<java_type>(thread, obj, offset).put(x); \
} UNSAFE_END \
 \
// END DEFINE_GETSETOOP.

DEFINE_GETSETOOP(jboolean, Boolean)
DEFINE_GETSETOOP(jbyte, Byte)
DEFINE_GETSETOOP(jshort, Short);
DEFINE_GETSETOOP(jchar, Char);
DEFINE_GETSETOOP(jint, Int);
DEFINE_GETSETOOP(jlong, Long);
DEFINE_GETSETOOP(jfloat, Float);
DEFINE_GETSETOOP(jdouble, Double);

#undef DEFINE_GETSETOOP

#define DEFINE_GETSETOOP_VOLATILE(java_type, Type) \
 \
UNSAFE_ENTRY(java_type, Unsafe_Get##Type##Volatile(JNIEnv *env, jobject unsafe, jobject obj, jlong offset)) { \
  return MemoryAccess<java_type>(thread, obj, offset).get_volatile(); \
} UNSAFE_END \
 \
UNSAFE_ENTRY(void, Unsafe_Put##Type##Volatile(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, java_type x)) { \
  MemoryAccess<java_type>(thread, obj, offset).put_volatile(x); \
} UNSAFE_END \
 \
// END DEFINE_GETSETOOP_VOLATILE.

DEFINE_GETSETOOP_VOLATILE(jboolean, Boolean)
DEFINE_GETSETOOP_VOLATILE(jbyte, Byte)
DEFINE_GETSETOOP_VOLATILE(jshort, Short);
DEFINE_GETSETOOP_VOLATILE(jchar, Char);
DEFINE_GETSETOOP_VOLATILE(jint, Int);
DEFINE_GETSETOOP_VOLATILE(jlong, Long);
DEFINE_GETSETOOP_VOLATILE(jfloat, Float);
DEFINE_GETSETOOP_VOLATILE(jdouble, Double);

#undef DEFINE_GETSETOOP_VOLATILE

UNSAFE_LEAF(void, Unsafe_LoadFence(JNIEnv *env, jobject unsafe)) {
  OrderAccess::acquire();
} UNSAFE_END

UNSAFE_LEAF(void, Unsafe_StoreFence(JNIEnv *env, jobject unsafe)) {
  OrderAccess::release();
} UNSAFE_END

UNSAFE_LEAF(void, Unsafe_FullFence(JNIEnv *env, jobject unsafe)) {
  OrderAccess::fence();
} UNSAFE_END

////// Allocation requests

UNSAFE_ENTRY(jobject, Unsafe_AllocateInstance(JNIEnv *env, jobject unsafe, jclass cls)) {
  instanceOop i = InstanceKlass::allocate_instance(JNIHandles::resolve_non_null(cls), CHECK_NULL);
  return JNIHandles::make_local(THREAD, i);
} UNSAFE_END

UNSAFE_ENTRY(jlong, Unsafe_AllocateMemory0(JNIEnv *env, jobject unsafe, jlong size)) {
  size_t sz = (size_t)size;

  assert(is_aligned(sz, HeapWordSize), "sz not aligned");

  void* x = os::malloc(sz, mtOther);

  return addr_to_java(x);
} UNSAFE_END

UNSAFE_ENTRY(jlong, Unsafe_ReallocateMemory0(JNIEnv *env, jobject unsafe, jlong addr, jlong size)) {
  void* p = addr_from_java(addr);
  size_t sz = (size_t)size;

  assert(is_aligned(sz, HeapWordSize), "sz not aligned");

  void* x = os::realloc(p, sz, mtOther);

  return addr_to_java(x);
} UNSAFE_END

UNSAFE_ENTRY(void, Unsafe_FreeMemory0(JNIEnv *env, jobject unsafe, jlong addr)) {
  void* p = addr_from_java(addr);

  os::free(p);
} UNSAFE_END

UNSAFE_ENTRY(void, Unsafe_SetMemory0(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jlong size, jbyte value)) {
  size_t sz = (size_t)size;

  oop base = JNIHandles::resolve(obj);
  void* p = index_oop_from_field_offset_long(base, offset);

  Copy::fill_to_memory_atomic(p, sz, value);
} UNSAFE_END

UNSAFE_ENTRY(void, Unsafe_CopyMemory0(JNIEnv *env, jobject unsafe, jobject srcObj, jlong srcOffset, jobject dstObj, jlong dstOffset, jlong size)) {
  size_t sz = (size_t)size;

  oop srcp = JNIHandles::resolve(srcObj);
  oop dstp = JNIHandles::resolve(dstObj);

  void* src = index_oop_from_field_offset_long(srcp, srcOffset);
  void* dst = index_oop_from_field_offset_long(dstp, dstOffset);
  {
    GuardUnsafeAccess guard(thread);
    if (StubRoutines::unsafe_arraycopy() != NULL) {
      MACOS_AARCH64_ONLY(ThreadWXEnable wx(WXExec, thread));
      StubRoutines::UnsafeArrayCopy_stub()(src, dst, sz);
    } else {
      Copy::conjoint_memory_atomic(src, dst, sz);
    }
  }
} UNSAFE_END

// This function is a leaf since if the source and destination are both in native memory
// the copy may potentially be very large, and we don't want to disable GC if we can avoid it.
// If either source or destination (or both) are on the heap, the function will enter VM using
// JVM_ENTRY_FROM_LEAF
UNSAFE_LEAF(void, Unsafe_CopySwapMemory0(JNIEnv *env, jobject unsafe, jobject srcObj, jlong srcOffset, jobject dstObj, jlong dstOffset, jlong size, jlong elemSize)) {
  size_t sz = (size_t)size;
  size_t esz = (size_t)elemSize;

  if (srcObj == NULL && dstObj == NULL) {
    // Both src & dst are in native memory
    address src = (address)srcOffset;
    address dst = (address)dstOffset;

    {
      JavaThread* thread = JavaThread::thread_from_jni_environment(env);
      GuardUnsafeAccess guard(thread);
      Copy::conjoint_swap(src, dst, sz, esz);
    }
  } else {
    // At least one of src/dst are on heap, transition to VM to access raw pointers

    JVM_ENTRY_FROM_LEAF(env, void, Unsafe_CopySwapMemory0) {
      oop srcp = JNIHandles::resolve(srcObj);
      oop dstp = JNIHandles::resolve(dstObj);

      address src = (address)index_oop_from_field_offset_long(srcp, srcOffset);
      address dst = (address)index_oop_from_field_offset_long(dstp, dstOffset);

      {
        GuardUnsafeAccess guard(thread);
        Copy::conjoint_swap(src, dst, sz, esz);
      }
    } JVM_END
  }
} UNSAFE_END

UNSAFE_LEAF (void, Unsafe_WriteBack0(JNIEnv *env, jobject unsafe, jlong line)) {
  assert(VM_Version::supports_data_cache_line_flush(), "should not get here");
#ifdef ASSERT
  if (TraceMemoryWriteback) {
    tty->print_cr("Unsafe: writeback 0x%p", addr_from_java(line));
  }
#endif

  MACOS_AARCH64_ONLY(ThreadWXEnable wx(WXExec, Thread::current()));
  assert(StubRoutines::data_cache_writeback() != NULL, "sanity");
  (StubRoutines::DataCacheWriteback_stub())(addr_from_java(line));
} UNSAFE_END

static void doWriteBackSync0(bool is_pre)
{
  MACOS_AARCH64_ONLY(ThreadWXEnable wx(WXExec, Thread::current()));
  assert(StubRoutines::data_cache_writeback_sync() != NULL, "sanity");
  (StubRoutines::DataCacheWritebackSync_stub())(is_pre);
}

UNSAFE_LEAF (void, Unsafe_WriteBackPreSync0(JNIEnv *env, jobject unsafe)) {
  assert(VM_Version::supports_data_cache_line_flush(), "should not get here");
#ifdef ASSERT
  if (TraceMemoryWriteback) {
      tty->print_cr("Unsafe: writeback pre-sync");
  }
#endif

  doWriteBackSync0(true);
} UNSAFE_END

UNSAFE_LEAF (void, Unsafe_WriteBackPostSync0(JNIEnv *env, jobject unsafe)) {
  assert(VM_Version::supports_data_cache_line_flush(), "should not get here");
#ifdef ASSERT
  if (TraceMemoryWriteback) {
    tty->print_cr("Unsafe: writeback pre-sync");
  }
#endif

  doWriteBackSync0(false);
} UNSAFE_END

////// Random queries

static jlong find_field_offset(jclass clazz, jstring name, TRAPS) {
  assert(clazz != NULL, "clazz must not be NULL");
  assert(name != NULL, "name must not be NULL");

  ResourceMark rm(THREAD);
  char *utf_name = java_lang_String::as_utf8_string(JNIHandles::resolve_non_null(name));

  InstanceKlass* k = InstanceKlass::cast(java_lang_Class::as_Klass(JNIHandles::resolve_non_null(clazz)));

  jint offset = -1;
  for (JavaFieldStream fs(k); !fs.done(); fs.next()) {
    Symbol *name = fs.name();
    if (name->equals(utf_name)) {
      offset = fs.offset();
      break;
    }
  }
  if (offset < 0) {
    THROW_0(vmSymbols::java_lang_InternalError());
  }
  return field_offset_from_byte_offset(offset);
}

static jlong find_field_offset(jobject field, int must_be_static, TRAPS) {
  assert(field != NULL, "field must not be NULL");

  oop reflected   = JNIHandles::resolve_non_null(field);
  oop mirror      = java_lang_reflect_Field::clazz(reflected);
  Klass* k        = java_lang_Class::as_Klass(mirror);
  int slot        = java_lang_reflect_Field::slot(reflected);
  int modifiers   = java_lang_reflect_Field::modifiers(reflected);

  if (must_be_static >= 0) {
    int really_is_static = ((modifiers & JVM_ACC_STATIC) != 0);
    if (must_be_static != really_is_static) {
      THROW_0(vmSymbols::java_lang_IllegalArgumentException());
    }
  }

  int offset = InstanceKlass::cast(k)->field_offset(slot);
  return field_offset_from_byte_offset(offset);
}

UNSAFE_ENTRY(jlong, Unsafe_ObjectFieldOffset0(JNIEnv *env, jobject unsafe, jobject field)) {
  return find_field_offset(field, 0, THREAD);
} UNSAFE_END

UNSAFE_ENTRY(jlong, Unsafe_ObjectFieldOffset1(JNIEnv *env, jobject unsafe, jclass c, jstring name)) {
  return find_field_offset(c, name, THREAD);
} UNSAFE_END

UNSAFE_ENTRY(jlong, Unsafe_StaticFieldOffset0(JNIEnv *env, jobject unsafe, jobject field)) {
  return find_field_offset(field, 1, THREAD);
} UNSAFE_END

UNSAFE_ENTRY(jobject, Unsafe_StaticFieldBase0(JNIEnv *env, jobject unsafe, jobject field)) {
  assert(field != NULL, "field must not be NULL");

  // Note:  In this VM implementation, a field address is always a short
  // offset from the base of a a klass metaobject.  Thus, the full dynamic
  // range of the return type is never used.  However, some implementations
  // might put the static field inside an array shared by many classes,
  // or even at a fixed address, in which case the address could be quite
  // large.  In that last case, this function would return NULL, since
  // the address would operate alone, without any base pointer.

  oop reflected   = JNIHandles::resolve_non_null(field);
  oop mirror      = java_lang_reflect_Field::clazz(reflected);
  int modifiers   = java_lang_reflect_Field::modifiers(reflected);

  if ((modifiers & JVM_ACC_STATIC) == 0) {
    THROW_0(vmSymbols::java_lang_IllegalArgumentException());
  }

  return JNIHandles::make_local(THREAD, mirror);
} UNSAFE_END

UNSAFE_ENTRY(void, Unsafe_EnsureClassInitialized0(JNIEnv *env, jobject unsafe, jobject clazz)) {
  assert(clazz != NULL, "clazz must not be NULL");

  oop mirror = JNIHandles::resolve_non_null(clazz);

  Klass* klass = java_lang_Class::as_Klass(mirror);
  if (klass != NULL && klass->should_be_initialized()) {
    InstanceKlass* k = InstanceKlass::cast(klass);
    k->initialize(CHECK);
  }
}
UNSAFE_END

UNSAFE_ENTRY(jboolean, Unsafe_ShouldBeInitialized0(JNIEnv *env, jobject unsafe, jobject clazz)) {
  assert(clazz != NULL, "clazz must not be NULL");

  oop mirror = JNIHandles::resolve_non_null(clazz);
  Klass* klass = java_lang_Class::as_Klass(mirror);

  if (klass != NULL && klass->should_be_initialized()) {
    return true;
  }

  return false;
}
UNSAFE_END

static void getBaseAndScale(int& base, int& scale, jclass clazz, TRAPS) {
  assert(clazz != NULL, "clazz must not be NULL");

  oop mirror = JNIHandles::resolve_non_null(clazz);
  Klass* k = java_lang_Class::as_Klass(mirror);

  if (k == NULL || !k->is_array_klass()) {
    THROW(vmSymbols::java_lang_InvalidClassException());
  } else if (k->is_objArray_klass()) {
    base  = arrayOopDesc::base_offset_in_bytes(T_OBJECT);
    scale = heapOopSize;
  } else if (k->is_typeArray_klass()) {
    TypeArrayKlass* tak = TypeArrayKlass::cast(k);
    base  = tak->array_header_in_bytes();
    assert(base == arrayOopDesc::base_offset_in_bytes(tak->element_type()), "array_header_size semantics ok");
    scale = (1 << tak->log2_element_size());
  } else {
    ShouldNotReachHere();
  }
}

UNSAFE_ENTRY(jint, Unsafe_ArrayBaseOffset0(JNIEnv *env, jobject unsafe, jclass clazz)) {
  int base = 0, scale = 0;
  getBaseAndScale(base, scale, clazz, CHECK_0);

  return field_offset_from_byte_offset(base);
} UNSAFE_END


UNSAFE_ENTRY(jint, Unsafe_ArrayIndexScale0(JNIEnv *env, jobject unsafe, jclass clazz)) {
  int base = 0, scale = 0;
  getBaseAndScale(base, scale, clazz, CHECK_0);

  // This VM packs both fields and array elements down to the byte.
  // But watch out:  If this changes, so that array references for
  // a given primitive type (say, T_BOOLEAN) use different memory units
  // than fields, this method MUST return zero for such arrays.
  // For example, the VM used to store sub-word sized fields in full
  // words in the object layout, so that accessors like getByte(Object,int)
  // did not really do what one might expect for arrays.  Therefore,
  // this function used to report a zero scale factor, so that the user
  // would know not to attempt to access sub-word array elements.
  // // Code for unpacked fields:
  // if (scale < wordSize)  return 0;

  // The following allows for a pretty general fieldOffset cookie scheme,
  // but requires it to be linear in byte offset.
  return field_offset_from_byte_offset(scale) - field_offset_from_byte_offset(0);
} UNSAFE_END


static inline void throw_new(JNIEnv *env, const char *ename) {
  jclass cls = env->FindClass(ename);
  if (env->ExceptionCheck()) {
    env->ExceptionClear();
    tty->print_cr("Unsafe: cannot throw %s because FindClass has failed", ename);
    return;
  }

  env->ThrowNew(cls, NULL);
}

static jclass Unsafe_DefineClass_impl(JNIEnv *env, jstring name, jbyteArray data, int offset, int length, jobject loader, jobject pd) {
  // Code lifted from JDK 1.3 ClassLoader.c

  jbyte *body;
  char *utfName = NULL;
  jclass result = 0;
  char buf[128];

  assert(data != NULL, "Class bytes must not be NULL");
  assert(length >= 0, "length must not be negative: %d", length);

  if (UsePerfData) {
    ClassLoader::unsafe_defineClassCallCounter()->inc();
  }

  body = NEW_C_HEAP_ARRAY_RETURN_NULL(jbyte, length, mtInternal);
  if (body == NULL) {
    throw_new(env, "java/lang/OutOfMemoryError");
    return 0;
  }

  env->GetByteArrayRegion(data, offset, length, body);
  if (env->ExceptionOccurred()) {
    goto free_body;
  }

  if (name != NULL) {
    uint len = env->GetStringUTFLength(name);
    int unicode_len = env->GetStringLength(name);

    if (len >= sizeof(buf)) {
      utfName = NEW_C_HEAP_ARRAY_RETURN_NULL(char, len + 1, mtInternal);
      if (utfName == NULL) {
        throw_new(env, "java/lang/OutOfMemoryError");
        goto free_body;
      }
    } else {
      utfName = buf;
    }

    env->GetStringUTFRegion(name, 0, unicode_len, utfName);

    for (uint i = 0; i < len; i++) {
      if (utfName[i] == '.')   utfName[i] = '/';
    }
  }

  result = JVM_DefineClass(env, utfName, loader, body, length, pd);

  if (utfName && utfName != buf) {
    FREE_C_HEAP_ARRAY(char, utfName);
  }

 free_body:
  FREE_C_HEAP_ARRAY(jbyte, body);
  return result;
}


UNSAFE_ENTRY(jclass, Unsafe_DefineClass0(JNIEnv *env, jobject unsafe, jstring name, jbyteArray data, int offset, int length, jobject loader, jobject pd)) {
  ThreadToNativeFromVM ttnfv(thread);

  return Unsafe_DefineClass_impl(env, name, data, offset, length, loader, pd);
} UNSAFE_END


UNSAFE_ENTRY(void, Unsafe_ThrowException(JNIEnv *env, jobject unsafe, jthrowable thr)) {
  ThreadToNativeFromVM ttnfv(thread);
  env->Throw(thr);
} UNSAFE_END

// JSR166 ------------------------------------------------------------------

UNSAFE_ENTRY(jobject, Unsafe_CompareAndExchangeReference(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jobject e_h, jobject x_h)) {
  oop x = JNIHandles::resolve(x_h);
  oop e = JNIHandles::resolve(e_h);
  oop p = JNIHandles::resolve(obj);
  assert_field_offset_sane(p, offset);
  oop res = HeapAccess<ON_UNKNOWN_OOP_REF>::oop_atomic_cmpxchg_at(p, (ptrdiff_t)offset, e, x);
  return JNIHandles::make_local(THREAD, res);
} UNSAFE_END

UNSAFE_ENTRY(jint, Unsafe_CompareAndExchangeInt(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jint e, jint x)) {
  oop p = JNIHandles::resolve(obj);
  if (p == NULL) {
    volatile jint* addr = (volatile jint*)index_oop_from_field_offset_long(p, offset);
    return RawAccess<>::atomic_cmpxchg(addr, e, x);
  } else {
    assert_field_offset_sane(p, offset);
    return HeapAccess<>::atomic_cmpxchg_at(p, (ptrdiff_t)offset, e, x);
  }
} UNSAFE_END

UNSAFE_ENTRY(jlong, Unsafe_CompareAndExchangeLong(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jlong e, jlong x)) {
  oop p = JNIHandles::resolve(obj);
  if (p == NULL) {
    volatile jlong* addr = (volatile jlong*)index_oop_from_field_offset_long(p, offset);
    return RawAccess<>::atomic_cmpxchg(addr, e, x);
  } else {
    assert_field_offset_sane(p, offset);
    return HeapAccess<>::atomic_cmpxchg_at(p, (ptrdiff_t)offset, e, x);
  }
} UNSAFE_END

UNSAFE_ENTRY(jboolean, Unsafe_CompareAndSetReference(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jobject e_h, jobject x_h)) {
  oop x = JNIHandles::resolve(x_h);
  oop e = JNIHandles::resolve(e_h);
  oop p = JNIHandles::resolve(obj);
  assert_field_offset_sane(p, offset);
  oop ret = HeapAccess<ON_UNKNOWN_OOP_REF>::oop_atomic_cmpxchg_at(p, (ptrdiff_t)offset, e, x);
  return ret == e;
} UNSAFE_END

UNSAFE_ENTRY(jboolean, Unsafe_CompareAndSetInt(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jint e, jint x)) {
  oop p = JNIHandles::resolve(obj);
  if (p == NULL) {
    volatile jint* addr = (volatile jint*)index_oop_from_field_offset_long(p, offset);
    return RawAccess<>::atomic_cmpxchg(addr, e, x) == e;
  } else {
    assert_field_offset_sane(p, offset);
    return HeapAccess<>::atomic_cmpxchg_at(p, (ptrdiff_t)offset, e, x) == e;
  }
} UNSAFE_END

UNSAFE_ENTRY(jboolean, Unsafe_CompareAndSetLong(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jlong e, jlong x)) {
  oop p = JNIHandles::resolve(obj);
  if (p == NULL) {
    volatile jlong* addr = (volatile jlong*)index_oop_from_field_offset_long(p, offset);
    return RawAccess<>::atomic_cmpxchg(addr, e, x) == e;
  } else {
    assert_field_offset_sane(p, offset);
    return HeapAccess<>::atomic_cmpxchg_at(p, (ptrdiff_t)offset, e, x) == e;
  }
} UNSAFE_END

static void post_thread_park_event(EventThreadPark* event, const oop obj, jlong timeout_nanos, jlong until_epoch_millis) {
  assert(event != NULL, "invariant");
  assert(event->should_commit(), "invariant");
  event->set_parkedClass((obj != NULL) ? obj->klass() : NULL);
  event->set_timeout(timeout_nanos);
  event->set_until(until_epoch_millis);
  event->set_address((obj != NULL) ? (u8)cast_from_oop<uintptr_t>(obj) : 0);
  event->commit();
}

UNSAFE_ENTRY(void, Unsafe_Park(JNIEnv *env, jobject unsafe, jboolean isAbsolute, jlong time)) {
  HOTSPOT_THREAD_PARK_BEGIN((uintptr_t) thread->parker(), (int) isAbsolute, time);
  EventThreadPark event;

  JavaThreadParkedState jtps(thread, time != 0);
  thread->parker()->park(isAbsolute != 0, time);
  if (event.should_commit()) {
    const oop obj = thread->current_park_blocker();
    if (time == 0) {
      post_thread_park_event(&event, obj, min_jlong, min_jlong);
    } else {
      if (isAbsolute != 0) {
        post_thread_park_event(&event, obj, min_jlong, time);
      } else {
        post_thread_park_event(&event, obj, time, min_jlong);
      }
    }
  }
  HOTSPOT_THREAD_PARK_END((uintptr_t) thread->parker());
} UNSAFE_END

UNSAFE_ENTRY(void, Unsafe_Unpark(JNIEnv *env, jobject unsafe, jobject jthread)) {
  if (jthread != NULL) {
    ThreadsListHandle tlh;
    JavaThread* thr = NULL;
    oop java_thread = NULL;
    (void) tlh.cv_internal_thread_to_JavaThread(jthread, &thr, &java_thread);
    if (java_thread != NULL) {
      // This is a valid oop.
      if (thr != NULL) {
        // The JavaThread is alive.
        Parker* p = thr->parker();
        HOTSPOT_THREAD_UNPARK((uintptr_t) p);
        p->unpark();
      }
    }
  } // ThreadsListHandle is destroyed here.

} UNSAFE_END

UNSAFE_ENTRY(jint, Unsafe_GetLoadAverage0(JNIEnv *env, jobject unsafe, jdoubleArray loadavg, jint nelem)) {
  const int max_nelem = 3;
  double la[max_nelem];
  jint ret;

  typeArrayOop a = typeArrayOop(JNIHandles::resolve_non_null(loadavg));
  assert(a->is_typeArray(), "must be type array");

  ret = os::loadavg(la, nelem);
  if (ret == -1) {
    return -1;
  }

  // if successful, ret is the number of samples actually retrieved.
  assert(ret >= 0 && ret <= max_nelem, "Unexpected loadavg return value");
  switch(ret) {
    case 3: a->double_at_put(2, (jdouble)la[2]); // fall through
    case 2: a->double_at_put(1, (jdouble)la[1]); // fall through
    case 1: a->double_at_put(0, (jdouble)la[0]); break;
  }

  return ret;
} UNSAFE_END


/// JVM_RegisterUnsafeMethods

#define ADR "J"

#define LANG "Ljava/lang/"

#define OBJ LANG "Object;"
#define CLS LANG "Class;"
#define FLD LANG "reflect/Field;"
#define THR LANG "Throwable;"

#define DC_Args  LANG "String;[BII" LANG "ClassLoader;" "Ljava/security/ProtectionDomain;"
#define DAC_Args CLS "[B[" OBJ

#define CC (char*)  /*cast a literal from (const char*)*/
#define FN_PTR(f) CAST_FROM_FN_PTR(void*, &f)

#define DECLARE_GETPUTOOP(Type, Desc) \
    {CC "get" #Type,      CC "(" OBJ "J)" #Desc,       FN_PTR(Unsafe_Get##Type)}, \
    {CC "put" #Type,      CC "(" OBJ "J" #Desc ")V",   FN_PTR(Unsafe_Put##Type)}, \
    {CC "get" #Type "Volatile",      CC "(" OBJ "J)" #Desc,       FN_PTR(Unsafe_Get##Type##Volatile)}, \
    {CC "put" #Type "Volatile",      CC "(" OBJ "J" #Desc ")V",   FN_PTR(Unsafe_Put##Type##Volatile)}


static JNINativeMethod jdk_internal_misc_Unsafe_methods[] = {
    {CC "getReference",         CC "(" OBJ "J)" OBJ "",   FN_PTR(Unsafe_GetReference)},
    {CC "putReference",         CC "(" OBJ "J" OBJ ")V",  FN_PTR(Unsafe_PutReference)},
    {CC "getReferenceVolatile", CC "(" OBJ "J)" OBJ,      FN_PTR(Unsafe_GetReferenceVolatile)},
    {CC "putReferenceVolatile", CC "(" OBJ "J" OBJ ")V",  FN_PTR(Unsafe_PutReferenceVolatile)},

    {CC "getUncompressedObject", CC "(" ADR ")" OBJ,  FN_PTR(Unsafe_GetUncompressedObject)},

    DECLARE_GETPUTOOP(Boolean, Z),
    DECLARE_GETPUTOOP(Byte, B),
    DECLARE_GETPUTOOP(Short, S),
    DECLARE_GETPUTOOP(Char, C),
    DECLARE_GETPUTOOP(Int, I),
    DECLARE_GETPUTOOP(Long, J),
    DECLARE_GETPUTOOP(Float, F),
    DECLARE_GETPUTOOP(Double, D),

    {CC "allocateMemory0",    CC "(J)" ADR,              FN_PTR(Unsafe_AllocateMemory0)},
    {CC "reallocateMemory0",  CC "(" ADR "J)" ADR,       FN_PTR(Unsafe_ReallocateMemory0)},
    {CC "freeMemory0",        CC "(" ADR ")V",           FN_PTR(Unsafe_FreeMemory0)},

    {CC "objectFieldOffset0", CC "(" FLD ")J",           FN_PTR(Unsafe_ObjectFieldOffset0)},
    {CC "objectFieldOffset1", CC "(" CLS LANG "String;)J", FN_PTR(Unsafe_ObjectFieldOffset1)},
    {CC "staticFieldOffset0", CC "(" FLD ")J",           FN_PTR(Unsafe_StaticFieldOffset0)},
    {CC "staticFieldBase0",   CC "(" FLD ")" OBJ,        FN_PTR(Unsafe_StaticFieldBase0)},
    {CC "ensureClassInitialized0", CC "(" CLS ")V",      FN_PTR(Unsafe_EnsureClassInitialized0)},
    {CC "arrayBaseOffset0",   CC "(" CLS ")I",           FN_PTR(Unsafe_ArrayBaseOffset0)},
    {CC "arrayIndexScale0",   CC "(" CLS ")I",           FN_PTR(Unsafe_ArrayIndexScale0)},

    {CC "defineClass0",       CC "(" DC_Args ")" CLS,    FN_PTR(Unsafe_DefineClass0)},
    {CC "allocateInstance",   CC "(" CLS ")" OBJ,        FN_PTR(Unsafe_AllocateInstance)},
    {CC "throwException",     CC "(" THR ")V",           FN_PTR(Unsafe_ThrowException)},
    {CC "compareAndSetReference",CC "(" OBJ "J" OBJ "" OBJ ")Z", FN_PTR(Unsafe_CompareAndSetReference)},
    {CC "compareAndSetInt",   CC "(" OBJ "J""I""I"")Z",  FN_PTR(Unsafe_CompareAndSetInt)},
    {CC "compareAndSetLong",  CC "(" OBJ "J""J""J"")Z",  FN_PTR(Unsafe_CompareAndSetLong)},
    {CC "compareAndExchangeReference", CC "(" OBJ "J" OBJ "" OBJ ")" OBJ, FN_PTR(Unsafe_CompareAndExchangeReference)},
    {CC "compareAndExchangeInt",  CC "(" OBJ "J""I""I"")I", FN_PTR(Unsafe_CompareAndExchangeInt)},
    {CC "compareAndExchangeLong", CC "(" OBJ "J""J""J"")J", FN_PTR(Unsafe_CompareAndExchangeLong)},

    {CC "park",               CC "(ZJ)V",                FN_PTR(Unsafe_Park)},
    {CC "unpark",             CC "(" OBJ ")V",           FN_PTR(Unsafe_Unpark)},

    {CC "getLoadAverage0",    CC "([DI)I",               FN_PTR(Unsafe_GetLoadAverage0)},

    {CC "copyMemory0",        CC "(" OBJ "J" OBJ "JJ)V", FN_PTR(Unsafe_CopyMemory0)},
    {CC "copySwapMemory0",    CC "(" OBJ "J" OBJ "JJJ)V", FN_PTR(Unsafe_CopySwapMemory0)},
    {CC "writeback0",         CC "(" "J" ")V",           FN_PTR(Unsafe_WriteBack0)},
    {CC "writebackPreSync0",  CC "()V",                  FN_PTR(Unsafe_WriteBackPreSync0)},
    {CC "writebackPostSync0", CC "()V",                  FN_PTR(Unsafe_WriteBackPostSync0)},
    {CC "setMemory0",         CC "(" OBJ "JJB)V",        FN_PTR(Unsafe_SetMemory0)},

    {CC "shouldBeInitialized0", CC "(" CLS ")Z",         FN_PTR(Unsafe_ShouldBeInitialized0)},

    {CC "loadFence",          CC "()V",                  FN_PTR(Unsafe_LoadFence)},
    {CC "storeFence",         CC "()V",                  FN_PTR(Unsafe_StoreFence)},
    {CC "fullFence",          CC "()V",                  FN_PTR(Unsafe_FullFence)},
};

#undef CC
#undef FN_PTR

#undef ADR
#undef LANG
#undef OBJ
#undef CLS
#undef FLD
#undef THR
#undef DC_Args
#undef DAC_Args

#undef DECLARE_GETPUTOOP


// This function is exported, used by NativeLookup.
// The Unsafe_xxx functions above are called only from the interpreter.
// The optimizer looks at names and signatures to recognize
// individual functions.

JVM_ENTRY(void, JVM_RegisterJDKInternalMiscUnsafeMethods(JNIEnv *env, jclass unsafeclass)) {
  ThreadToNativeFromVM ttnfv(thread);

  int ok = env->RegisterNatives(unsafeclass, jdk_internal_misc_Unsafe_methods, sizeof(jdk_internal_misc_Unsafe_methods)/sizeof(JNINativeMethod));
  guarantee(ok == 0, "register jdk.internal.misc.Unsafe natives");
} JVM_END
