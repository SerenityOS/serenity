/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "accessBackend.inline.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/vm_version.hpp"
#include "utilities/copy.hpp"

namespace AccessInternal {
  // VM_Version::supports_cx8() is a surrogate for 'supports atomic long memory ops'.
  //
  // On platforms which do not support atomic compare-and-swap of jlong (8 byte)
  // values we have to use a lock-based scheme to enforce atomicity. This has to be
  // applied to all Unsafe operations that set the value of a jlong field. Even so
  // the compareAndSwapLong operation will not be atomic with respect to direct stores
  // to the field from Java code. It is important therefore that any Java code that
  // utilizes these Unsafe jlong operations does not perform direct stores. To permit
  // direct loads of the field from Java code we must also use Atomic::store within the
  // locked regions. And for good measure, in case there are direct stores, we also
  // employ Atomic::load within those regions. Note that the field in question must be
  // volatile and so must have atomic load/store accesses applied at the Java level.
  //
  // The locking scheme could utilize a range of strategies for controlling the locking
  // granularity: from a lock per-field through to a single global lock. The latter is
  // the simplest and is used for the current implementation. Note that the Java object
  // that contains the field, can not, in general, be used for locking. To do so can lead
  // to deadlocks as we may introduce locking into what appears to the Java code to be a
  // lock-free path.
  //
  // As all the locked-regions are very short and themselves non-blocking we can treat
  // them as leaf routines and elide safepoint checks (ie we don't perform any thread
  // state transitions even when blocking for the lock). Note that if we do choose to
  // add safepoint checks and thread state transitions, we must ensure that we calculate
  // the address of the field _after_ we have acquired the lock, else the object may have
  // been moved by the GC

#ifndef SUPPORTS_NATIVE_CX8

  // This is intentionally in the cpp file rather than the .inline.hpp file. It seems
  // desirable to trade faster JDK build times (not propagating vm_version.hpp)
  // for slightly worse runtime atomic jlong performance on 32 bit machines with
  // support for 64 bit atomics.
  bool wide_atomic_needs_locking() {
    return !VM_Version::supports_cx8();
  }

  AccessLocker::AccessLocker() {
    assert(!VM_Version::supports_cx8(), "why else?");
    UnsafeJlong_lock->lock_without_safepoint_check();
  }

  AccessLocker::~AccessLocker() {
    UnsafeJlong_lock->unlock();
  }

#endif

// These forward copying calls to Copy without exposing the Copy type in headers unnecessarily

  void arraycopy_arrayof_conjoint_oops(void* src, void* dst, size_t length) {
    Copy::arrayof_conjoint_oops(reinterpret_cast<HeapWord*>(src),
                                reinterpret_cast<HeapWord*>(dst), length);
  }

  void arraycopy_conjoint_oops(oop* src, oop* dst, size_t length) {
    Copy::conjoint_oops_atomic(src, dst, length);
  }

  void arraycopy_conjoint_oops(narrowOop* src, narrowOop* dst, size_t length) {
    Copy::conjoint_oops_atomic(src, dst, length);
  }

  void arraycopy_disjoint_words(void* src, void* dst, size_t length) {
    Copy::disjoint_words(reinterpret_cast<HeapWord*>(src),
                         reinterpret_cast<HeapWord*>(dst), length);
  }

  void arraycopy_disjoint_words_atomic(void* src, void* dst, size_t length) {
    Copy::disjoint_words_atomic(reinterpret_cast<HeapWord*>(src),
                                reinterpret_cast<HeapWord*>(dst), length);
  }

  template<>
  void arraycopy_conjoint<jboolean>(jboolean* src, jboolean* dst, size_t length) {
    Copy::conjoint_jbytes(reinterpret_cast<jbyte*>(src), reinterpret_cast<jbyte*>(dst), length);
  }

  template<>
  void arraycopy_conjoint<jbyte>(jbyte* src, jbyte* dst, size_t length) {
    Copy::conjoint_jbytes(src, dst, length);
  }

  template<>
  void arraycopy_conjoint<jchar>(jchar* src, jchar* dst, size_t length) {
    Copy::conjoint_jshorts_atomic(reinterpret_cast<jshort*>(src), reinterpret_cast<jshort*>(dst), length);
  }

  template<>
  void arraycopy_conjoint<jshort>(jshort* src, jshort* dst, size_t length) {
    Copy::conjoint_jshorts_atomic(src, dst, length);
  }

  template<>
  void arraycopy_conjoint<jint>(jint* src, jint* dst, size_t length) {
    Copy::conjoint_jints_atomic(src, dst, length);
  }

  template<>
  void arraycopy_conjoint<jfloat>(jfloat* src, jfloat* dst, size_t length) {
    Copy::conjoint_jints_atomic(reinterpret_cast<jint*>(src), reinterpret_cast<jint*>(dst), length);
  }

  template<>
  void arraycopy_conjoint<jlong>(jlong* src, jlong* dst, size_t length) {
    Copy::conjoint_jlongs_atomic(src, dst, length);
  }

  template<>
  void arraycopy_conjoint<jdouble>(jdouble* src, jdouble* dst, size_t length) {
    Copy::conjoint_jlongs_atomic(reinterpret_cast<jlong*>(src), reinterpret_cast<jlong*>(dst), length);
  }

  template<>
  void arraycopy_arrayof_conjoint<jbyte>(jbyte* src, jbyte* dst, size_t length) {
    Copy::arrayof_conjoint_jbytes(reinterpret_cast<HeapWord*>(src),
                                  reinterpret_cast<HeapWord*>(dst),
                                  length);
  }

  template<>
  void arraycopy_arrayof_conjoint<jshort>(jshort* src, jshort* dst, size_t length) {
    Copy::arrayof_conjoint_jshorts(reinterpret_cast<HeapWord*>(src),
                                   reinterpret_cast<HeapWord*>(dst),
                                   length);
  }

  template<>
  void arraycopy_arrayof_conjoint<jint>(jint* src, jint* dst, size_t length) {
    Copy::arrayof_conjoint_jints(reinterpret_cast<HeapWord*>(src),
                                 reinterpret_cast<HeapWord*>(dst),
                                 length);
  }

  template<>
  void arraycopy_arrayof_conjoint<jlong>(jlong* src, jlong* dst, size_t length) {
    Copy::arrayof_conjoint_jlongs(reinterpret_cast<HeapWord*>(src),
                                  reinterpret_cast<HeapWord*>(dst),
                                  length);
  }

  template<>
  void arraycopy_conjoint<void>(void* src, void* dst, size_t length) {
    Copy::conjoint_jbytes(reinterpret_cast<jbyte*>(src),
                          reinterpret_cast<jbyte*>(dst),
                          length);
  }

  template<>
  void arraycopy_conjoint_atomic<jbyte>(jbyte* src, jbyte* dst, size_t length) {
    Copy::conjoint_jbytes_atomic(src, dst, length);
  }

  template<>
  void arraycopy_conjoint_atomic<jshort>(jshort* src, jshort* dst, size_t length) {
    Copy::conjoint_jshorts_atomic(src, dst, length);
  }

  template<>
  void arraycopy_conjoint_atomic<jint>(jint* src, jint* dst, size_t length) {
    Copy::conjoint_jints_atomic(src, dst, length);
  }

  template<>
  void arraycopy_conjoint_atomic<jlong>(jlong* src, jlong* dst, size_t length) {
    Copy::conjoint_jlongs_atomic(src, dst, length);
  }

  template<>
  void arraycopy_conjoint_atomic<void>(void* src, void* dst, size_t length) {
    Copy::conjoint_memory_atomic(src, dst, length);
  }
}
