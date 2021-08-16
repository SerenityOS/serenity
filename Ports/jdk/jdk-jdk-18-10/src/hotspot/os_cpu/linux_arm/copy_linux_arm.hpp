/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef OS_CPU_LINUX_ARM_COPY_LINUX_ARM_HPP
#define OS_CPU_LINUX_ARM_COPY_LINUX_ARM_HPP

static void pd_conjoint_words(const HeapWord* from, HeapWord* to, size_t count) {
  _Copy_conjoint_words(from, to, count * HeapWordSize);
}

static void pd_disjoint_words(const HeapWord* from, HeapWord* to, size_t count) {
  _Copy_disjoint_words(from, to, count * HeapWordSize);
}

static void pd_disjoint_words_atomic(const HeapWord* from, HeapWord* to, size_t count) {
  pd_disjoint_words(from, to, count);
}

static void pd_aligned_conjoint_words(const HeapWord* from, HeapWord* to, size_t count) {
  pd_conjoint_words(from, to, count);
}

static void pd_aligned_disjoint_words(const HeapWord* from, HeapWord* to, size_t count) {
  pd_disjoint_words(from, to, count);
}

static void pd_conjoint_bytes(const void* from, void* to, size_t count) {
  memmove(to, from, count);
}

static void pd_conjoint_bytes_atomic(const void* from, void* to, size_t count) {
  pd_conjoint_bytes(from, to, count);
}

static void pd_conjoint_jshorts_atomic(const jshort* from, jshort* to, size_t count) {
  _Copy_conjoint_jshorts_atomic(from, to, count * BytesPerShort);
}

static void pd_conjoint_jints_atomic(const jint* from, jint* to, size_t count) {
  assert(HeapWordSize == BytesPerInt, "heapwords and jints must be the same size");
  // pd_conjoint_words is word-atomic in this implementation.
  pd_conjoint_words((const HeapWord*)from, (HeapWord*)to, count);
}

static void pd_conjoint_jlongs_atomic(const jlong* from, jlong* to, size_t count) {
  _Copy_conjoint_jlongs_atomic(from, to, count * BytesPerLong);
}

static void pd_conjoint_oops_atomic(const oop* from, oop* to, size_t count) {
  assert(BytesPerHeapOop == BytesPerInt, "32-bit architecture");
  pd_conjoint_jints_atomic((const jint*)from, (jint*)to, count);
}

static void pd_arrayof_conjoint_bytes(const HeapWord* from, HeapWord* to, size_t count) {
  pd_conjoint_bytes_atomic((const void*)from, (void*)to, count);
}

static void pd_arrayof_conjoint_jshorts(const HeapWord* from, HeapWord* to, size_t count) {
  pd_conjoint_jshorts_atomic((const jshort*)from, (jshort*)to, count);
}

static void pd_arrayof_conjoint_jints(const HeapWord* from, HeapWord* to, size_t count) {
  pd_conjoint_jints_atomic((const jint*)from, (jint*)to, count);
}

static void pd_arrayof_conjoint_jlongs(const HeapWord* from, HeapWord* to, size_t count) {
  pd_conjoint_jlongs_atomic((const jlong*)from, (jlong*)to, count);
}

static void pd_arrayof_conjoint_oops(const HeapWord* from, HeapWord* to, size_t count) {
  pd_conjoint_oops_atomic((const oop*)from, (oop*)to, count);
}

#endif // OS_CPU_LINUX_ARM_COPY_LINUX_ARM_HPP
