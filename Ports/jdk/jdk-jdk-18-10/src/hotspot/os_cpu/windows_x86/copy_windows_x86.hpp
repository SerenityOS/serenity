/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef OS_CPU_WINDOWS_X86_COPY_WINDOWS_X86_HPP
#define OS_CPU_WINDOWS_X86_COPY_WINDOWS_X86_HPP

static void pd_conjoint_words(const HeapWord* from, HeapWord* to, size_t count) {
  (void)memmove(to, from, count * HeapWordSize);
}

static void pd_disjoint_words(const HeapWord* from, HeapWord* to, size_t count) {
#ifdef AMD64
  switch (count) {
  case 8:  to[7] = from[7];
  case 7:  to[6] = from[6];
  case 6:  to[5] = from[5];
  case 5:  to[4] = from[4];
  case 4:  to[3] = from[3];
  case 3:  to[2] = from[2];
  case 2:  to[1] = from[1];
  case 1:  to[0] = from[0];
  case 0:  break;
  default:
    (void)memcpy(to, from, count * HeapWordSize);
    break;
  }
#else
  (void)memcpy(to, from, count * HeapWordSize);
#endif // AMD64
}

static void pd_disjoint_words_atomic(const HeapWord* from, HeapWord* to, size_t count) {
  switch (count) {
  case 8:  to[7] = from[7];
  case 7:  to[6] = from[6];
  case 6:  to[5] = from[5];
  case 5:  to[4] = from[4];
  case 4:  to[3] = from[3];
  case 3:  to[2] = from[2];
  case 2:  to[1] = from[1];
  case 1:  to[0] = from[0];
  case 0:  break;
  default: while (count-- > 0) {
             *to++ = *from++;
           }
           break;
  }
}

static void pd_aligned_conjoint_words(const HeapWord* from, HeapWord* to, size_t count) {
  (void)memmove(to, from, count * HeapWordSize);
}

static void pd_aligned_disjoint_words(const HeapWord* from, HeapWord* to, size_t count) {
  pd_disjoint_words(from, to, count);
}

static void pd_conjoint_bytes(const void* from, void* to, size_t count) {
  (void)memmove(to, from, count);
}

static void pd_conjoint_bytes_atomic(const void* from, void* to, size_t count) {
  pd_conjoint_bytes(from, to, count);
}

static void pd_conjoint_jshorts_atomic(const jshort* from, jshort* to, size_t count) {
  if (from > to) {
    while (count-- > 0) {
      // Copy forwards
      *to++ = *from++;
    }
  } else {
    from += count - 1;
    to   += count - 1;
    while (count-- > 0) {
      // Copy backwards
      *to-- = *from--;
    }
  }
}

static void pd_conjoint_jints_atomic(const jint* from, jint* to, size_t count) {
  if (from > to) {
    while (count-- > 0) {
      // Copy forwards
      *to++ = *from++;
    }
  } else {
    from += count - 1;
    to   += count - 1;
    while (count-- > 0) {
      // Copy backwards
      *to-- = *from--;
    }
  }
}

static void pd_conjoint_jlongs_atomic(const jlong* from, jlong* to, size_t count) {
#ifdef AMD64
  assert(BytesPerLong == BytesPerOop, "jlongs and oops must be the same size");
  pd_conjoint_oops_atomic((const oop*)from, (oop*)to, count);
#else
  // Guarantee use of fild/fistp or xmm regs via some asm code, because compilers won't.
  __asm {
    mov    eax, from;
    mov    edx, to;
    mov    ecx, count;
    cmp    eax, edx;
    jbe    downtest;
    jmp    uptest;
  up:
    fild   qword ptr [eax];
    fistp  qword ptr [edx];
    add    eax, 8;
    add    edx, 8;
  uptest:
    sub    ecx, 1;
    jge    up;
    jmp    done;
  down:
    fild   qword ptr [eax][ecx*8];
    fistp  qword ptr [edx][ecx*8];
  downtest:
    sub    ecx, 1;
    jge    down;
  done:;
  }
#endif // AMD64
}

static void pd_conjoint_oops_atomic(const oop* from, oop* to, size_t count) {
  // Do better than this: inline memmove body  NEEDS CLEANUP
  if (from > to) {
    while (count-- > 0) {
      // Copy forwards
      *to++ = *from++;
    }
  } else {
    from += count - 1;
    to   += count - 1;
    while (count-- > 0) {
      // Copy backwards
      *to-- = *from--;
    }
  }
}

static void pd_arrayof_conjoint_bytes(const HeapWord* from, HeapWord* to, size_t count) {
#ifdef AMD64
  pd_conjoint_bytes_atomic(from, to, count);
#else
  pd_conjoint_bytes(from, to, count);
#endif // AMD64
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

#endif // OS_CPU_WINDOWS_X86_COPY_WINDOWS_X86_HPP
