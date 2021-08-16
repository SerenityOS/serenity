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

#ifndef OS_CPU_LINUX_X86_COPY_LINUX_X86_HPP
#define OS_CPU_LINUX_X86_COPY_LINUX_X86_HPP

static void pd_conjoint_words(const HeapWord* from, HeapWord* to, size_t count) {
#ifdef AMD64
  (void)memmove(to, from, count * HeapWordSize);
#else
  // Includes a zero-count check.
  intx temp = 0;
  __asm__ volatile("        testl   %6,%6         ;"
                   "        jz      7f            ;"
                   "        cmpl    %4,%5         ;"
                   "        leal    -4(%4,%6,4),%3;"
                   "        jbe     1f            ;"
                   "        cmpl    %7,%5         ;"
                   "        jbe     4f            ;"
                   "1:      cmpl    $32,%6        ;"
                   "        ja      3f            ;"
                   "        subl    %4,%1         ;"
                   "2:      movl    (%4),%3       ;"
                   "        movl    %7,(%5,%4,1)  ;"
                   "        addl    $4,%0         ;"
                   "        subl    $1,%2          ;"
                   "        jnz     2b            ;"
                   "        jmp     7f            ;"
                   "3:      rep;    smovl         ;"
                   "        jmp     7f            ;"
                   "4:      cmpl    $32,%2        ;"
                   "        movl    %7,%0         ;"
                   "        leal    -4(%5,%6,4),%1;"
                   "        ja      6f            ;"
                   "        subl    %4,%1         ;"
                   "5:      movl    (%4),%3       ;"
                   "        movl    %7,(%5,%4,1)  ;"
                   "        subl    $4,%0         ;"
                   "        subl    $1,%2          ;"
                   "        jnz     5b            ;"
                   "        jmp     7f            ;"
                   "6:      std                   ;"
                   "        rep;    smovl         ;"
                   "        cld                   ;"
                   "7:      nop                    "
                   : "=S" (from), "=D" (to), "=c" (count), "=r" (temp)
                   : "0"  (from), "1"  (to), "2"  (count), "3"  (temp)
                   : "memory", "flags");
#endif // AMD64
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
  // Includes a zero-count check.
  intx temp = 0;
  __asm__ volatile("        testl   %6,%6       ;"
                   "        jz      3f          ;"
                   "        cmpl    $32,%6      ;"
                   "        ja      2f          ;"
                   "        subl    %4,%1       ;"
                   "1:      movl    (%4),%3     ;"
                   "        movl    %7,(%5,%4,1);"
                   "        addl    $4,%0       ;"
                   "        subl    $1,%2        ;"
                   "        jnz     1b          ;"
                   "        jmp     3f          ;"
                   "2:      rep;    smovl       ;"
                   "3:      nop                  "
                   : "=S" (from), "=D" (to), "=c" (count), "=r" (temp)
                   : "0"  (from), "1"  (to), "2"  (count), "3"  (temp)
                   : "memory", "cc");
#endif // AMD64
}

static void pd_disjoint_words_atomic(const HeapWord* from, HeapWord* to, size_t count) {
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
    while (count-- > 0) {
      *to++ = *from++;
    }
    break;
  }
#else
  // pd_disjoint_words is word-atomic in this implementation.
  pd_disjoint_words(from, to, count);
#endif // AMD64
}

static void pd_aligned_conjoint_words(const HeapWord* from, HeapWord* to, size_t count) {
  pd_conjoint_words(from, to, count);
}

static void pd_aligned_disjoint_words(const HeapWord* from, HeapWord* to, size_t count) {
  pd_disjoint_words(from, to, count);
}

static void pd_conjoint_bytes(const void* from, void* to, size_t count) {
#ifdef AMD64
  (void)memmove(to, from, count);
#else
  // Includes a zero-count check.
  intx temp = 0;
  __asm__ volatile("        testl   %6,%6          ;"
                   "        jz      13f            ;"
                   "        cmpl    %4,%5          ;"
                   "        leal    -1(%4,%6),%3   ;"
                   "        jbe     1f             ;"
                   "        cmpl    %7,%5          ;"
                   "        jbe     8f             ;"
                   "1:      cmpl    $3,%6          ;"
                   "        jbe     6f             ;"
                   "        movl    %6,%3          ;"
                   "        movl    $4,%2          ;"
                   "        subl    %4,%2          ;"
                   "        andl    $3,%2          ;"
                   "        jz      2f             ;"
                   "        subl    %6,%3          ;"
                   "        rep;    smovb          ;"
                   "2:      movl    %7,%2          ;"
                   "        shrl    $2,%2          ;"
                   "        jz      5f             ;"
                   "        cmpl    $32,%2         ;"
                   "        ja      4f             ;"
                   "        subl    %4,%1          ;"
                   "3:      movl    (%4),%%edx     ;"
                   "        movl    %%edx,(%5,%4,1);"
                   "        addl    $4,%0          ;"
                   "        subl    $1,%2           ;"
                   "        jnz     3b             ;"
                   "        addl    %4,%1          ;"
                   "        jmp     5f             ;"
                   "4:      rep;    smovl          ;"
                   "5:      movl    %7,%2          ;"
                   "        andl    $3,%2          ;"
                   "        jz      13f            ;"
                   "6:      xorl    %7,%3          ;"
                   "7:      movb    (%4,%7,1),%%dl ;"
                   "        movb    %%dl,(%5,%7,1) ;"
                   "        addl    $1,%3          ;"
                   "        subl    $1,%2           ;"
                   "        jnz     7b             ;"
                   "        jmp     13f            ;"
                   "8:      std                    ;"
                   "        cmpl    $12,%2         ;"
                   "        ja      9f             ;"
                   "        movl    %7,%0          ;"
                   "        leal    -1(%6,%5),%1   ;"
                   "        jmp     11f            ;"
                   "9:      xchgl   %3,%2          ;"
                   "        movl    %6,%0          ;"
                   "        addl    $1,%2          ;"
                   "        leal    -1(%7,%5),%1   ;"
                   "        andl    $3,%2          ;"
                   "        jz      10f            ;"
                   "        subl    %6,%3          ;"
                   "        rep;    smovb          ;"
                   "10:     movl    %7,%2          ;"
                   "        subl    $3,%0          ;"
                   "        shrl    $2,%2          ;"
                   "        subl    $3,%1          ;"
                   "        rep;    smovl          ;"
                   "        andl    $3,%3          ;"
                   "        jz      12f            ;"
                   "        movl    %7,%2          ;"
                   "        addl    $3,%0          ;"
                   "        addl    $3,%1          ;"
                   "11:     rep;    smovb          ;"
                   "12:     cld                    ;"
                   "13:     nop                    ;"
                   : "=S" (from), "=D" (to), "=c" (count), "=r" (temp)
                   : "0"  (from), "1"  (to), "2"  (count), "3"  (temp)
                   : "memory", "flags", "%edx");
#endif // AMD64
}

static void pd_conjoint_bytes_atomic(const void* from, void* to, size_t count) {
  pd_conjoint_bytes(from, to, count);
}

static void pd_conjoint_jshorts_atomic(const jshort* from, jshort* to, size_t count) {
  _Copy_conjoint_jshorts_atomic(from, to, count);
}

static void pd_conjoint_jints_atomic(const jint* from, jint* to, size_t count) {
#ifdef AMD64
  _Copy_conjoint_jints_atomic(from, to, count);
#else
  assert(HeapWordSize == BytesPerInt, "heapwords and jints must be the same size");
  // pd_conjoint_words is word-atomic in this implementation.
  pd_conjoint_words((const HeapWord*)from, (HeapWord*)to, count);
#endif // AMD64
}

static void pd_conjoint_jlongs_atomic(const jlong* from, jlong* to, size_t count) {
#ifdef AMD64
  _Copy_conjoint_jlongs_atomic(from, to, count);
#else
  // Guarantee use of fild/fistp or xmm regs via some asm code, because compilers won't.
  if (from > to) {
    while (count-- > 0) {
      __asm__ volatile("fildll (%0); fistpll (%1)"
                       :
                       : "r" (from), "r" (to)
                       : "memory" );
      ++from;
      ++to;
    }
  } else {
    while (count-- > 0) {
      __asm__ volatile("fildll (%0,%2,8); fistpll (%1,%2,8)"
                       :
                       : "r" (from), "r" (to), "r" (count)
                       : "memory" );
    }
  }
#endif // AMD64
}

static void pd_conjoint_oops_atomic(const oop* from, oop* to, size_t count) {
#ifdef AMD64
  assert(BytesPerLong == BytesPerOop, "jlongs and oops must be the same size");
  _Copy_conjoint_jlongs_atomic((const jlong*)from, (jlong*)to, count);
#else
  assert(HeapWordSize == BytesPerOop, "heapwords and oops must be the same size");
  // pd_conjoint_words is word-atomic in this implementation.
  pd_conjoint_words((const HeapWord*)from, (HeapWord*)to, count);
#endif // AMD64
}

static void pd_arrayof_conjoint_bytes(const HeapWord* from, HeapWord* to, size_t count) {
  _Copy_arrayof_conjoint_bytes(from, to, count);
}

static void pd_arrayof_conjoint_jshorts(const HeapWord* from, HeapWord* to, size_t count) {
  _Copy_arrayof_conjoint_jshorts(from, to, count);
}

static void pd_arrayof_conjoint_jints(const HeapWord* from, HeapWord* to, size_t count) {
#ifdef AMD64
   _Copy_arrayof_conjoint_jints(from, to, count);
#else
  pd_conjoint_jints_atomic((const jint*)from, (jint*)to, count);
#endif // AMD64
}

static void pd_arrayof_conjoint_jlongs(const HeapWord* from, HeapWord* to, size_t count) {
#ifdef AMD64
  _Copy_arrayof_conjoint_jlongs(from, to, count);
#else
  pd_conjoint_jlongs_atomic((const jlong*)from, (jlong*)to, count);
#endif // AMD64
}

static void pd_arrayof_conjoint_oops(const HeapWord* from, HeapWord* to, size_t count) {
#ifdef AMD64
  assert(BytesPerLong == BytesPerOop, "jlongs and oops must be the same size");
  _Copy_arrayof_conjoint_jlongs(from, to, count);
#else
  pd_conjoint_oops_atomic((const oop*)from, (oop*)to, count);
#endif // AMD64
}

#endif // OS_CPU_LINUX_X86_COPY_LINUX_X86_HPP
