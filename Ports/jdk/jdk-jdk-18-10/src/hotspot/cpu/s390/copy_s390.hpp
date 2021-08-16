/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016, 2020 SAP SE. All rights reserved.
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

// Major contributions by LS

#ifndef CPU_S390_COPY_S390_HPP
#define CPU_S390_COPY_S390_HPP

// Inline functions for memory copy and fill.

// HeapWordSize (the size of class HeapWord) is 8 Bytes (the size of a
// pointer variable), since we always run the _LP64 model. As a consequence,
// HeapWord* memory ranges are always assumed to be doubleword-aligned,
// having a size which is an integer multiple of HeapWordSize.
//
// Dealing only with doubleword-aligned doubleword units has important
// positive performance and data access consequences. Many of the move
// instructions perform particularly well under these circumstances.
// Data access is "doubleword-concurrent", except for MVC and XC.
// Furthermore, data access can be forced to be sequential (MVCL and MVCLE)
// by use of the special padding byte 0xb1, where required. For copying,
// we use padding byte 0xb0 to prevent the D-cache from being polluted.
//
// On z/Architecture, gcc optimizes memcpy into a series of MVC instructions.
// This is optimal, even if just one HeapWord is copied. However, MVC
// copying is not atomic, i.e. not "doubleword concurrent" by definition.
//
// If the -mmvcle compiler option is specified, memcpy translates into
// code such that the entire memory range is copied or preset with just
// one MVCLE instruction.
//
// *to = *from is transformed into a MVC instruction already with -O1.
// Thus, for atomic copy operations, (inline) assembler code is required
// to guarantee atomic data accesses.
//
// For large (len >= MVCLEThreshold) chunks of memory, we exploit
// special H/W support of z/Architecture:
// 1) copy short piece of memory to page-align address(es)
// 2) copy largest part (all contained full pages) of memory using mvcle instruction.
//    z/Architecture processors have special H/W support for page-aligned storage
//    where len is an int multiple of page size. In that case, up to 4 cache lines are
//    processed in parallel and L1 cache is not polluted.
// 3) copy the remaining piece of memory.
//
//  Measurement classifications:
//  very rare - <=     10.000 calls AND <=     1.000 usec elapsed
//       rare - <=    100.000 calls AND <=    10.000 usec elapsed
//       some - <=  1.000.000 calls AND <=   100.000 usec elapsed
//       freq - <= 10.000.000 calls AND <= 1.000.000 usec elapsed
//  very freq - >  10.000.000 calls OR  >  1.000.000 usec elapsed

#undef USE_INLINE_ASM

static void copy_conjoint_jshorts_atomic(const jshort* from, jshort* to, size_t count) {
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

static void copy_conjoint_jints_atomic(const jint* from, jint* to, size_t count) {
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

static bool has_destructive_overlap(const char* from, char* to, size_t byte_count) {
  return (from < to) && ((to-from) < (ptrdiff_t)byte_count);
}

#ifdef USE_INLINE_ASM

  //--------------------------------------------------------------
  // Atomic copying. Atomicity is given by the minimum of source
  // and target alignment. Refer to mail comm with Tim Slegel/IBM.
  // Only usable for disjoint source and target.
  //--------------------------------------------------------------
  #define MOVE8_ATOMIC_4(_to,_from) {                            \
    unsigned long toaddr;                                        \
    unsigned long fromaddr;                                      \
    asm(                                                         \
      "LG      %[toaddr],%[to]     \n\t" /* address of to area   */ \
      "LG      %[fromaddr],%[from] \n\t" /* address of from area */ \
      "MVC     0(32,%[toaddr]),0(%[fromaddr]) \n\t" /* move data */ \
      : [to]       "+Q"  (_to)          /* outputs   */          \
      , [from]     "+Q"  (_from)                                 \
      , [toaddr]   "=a"  (toaddr)                                \
      , [fromaddr] "=a"  (fromaddr)                              \
      :                                                          \
      : "cc"                            /* clobbered */          \
    );                                                           \
  }
  #define MOVE8_ATOMIC_3(_to,_from) {                            \
    unsigned long toaddr;                                        \
    unsigned long fromaddr;                                      \
    asm(                                                         \
      "LG      %[toaddr],%[to]     \n\t" /* address of to area   */ \
      "LG      %[fromaddr],%[from] \n\t" /* address of from area */ \
      "MVC     0(24,%[toaddr]),0(%[fromaddr]) \n\t" /* move data */ \
      : [to]       "+Q"  (_to)          /* outputs   */          \
      , [from]     "+Q"  (_from)                                 \
      , [toaddr]   "=a"  (toaddr)                                \
      , [fromaddr] "=a"  (fromaddr)                              \
      :                                                          \
      : "cc"                            /* clobbered */          \
    );                                                           \
  }
  #define MOVE8_ATOMIC_2(_to,_from) {                            \
    unsigned long toaddr;                                        \
    unsigned long fromaddr;                                      \
    asm(                                                         \
      "LG      %[toaddr],%[to]     \n\t" /* address of to area   */ \
      "LG      %[fromaddr],%[from] \n\t" /* address of from area */ \
      "MVC     0(16,%[toaddr]),0(%[fromaddr]) \n\t" /* move data */ \
      : [to]       "+Q"  (_to)          /* outputs   */          \
      , [from]     "+Q"  (_from)                                 \
      , [toaddr]   "=a"  (toaddr)                                \
      , [fromaddr] "=a"  (fromaddr)                              \
      :                                                          \
      : "cc"                            /* clobbered */          \
    );                                                           \
  }
  #define MOVE8_ATOMIC_1(_to,_from) {                            \
    unsigned long toaddr;                                        \
    unsigned long fromaddr;                                      \
    asm(                                                         \
      "LG      %[toaddr],%[to]     \n\t" /* address of to area   */ \
      "LG      %[fromaddr],%[from] \n\t" /* address of from area */ \
      "MVC     0(8,%[toaddr]),0(%[fromaddr]) \n\t"  /* move data */ \
      : [to]       "+Q"  (_to)          /* outputs   */          \
      , [from]     "+Q"  (_from)                                 \
      , [toaddr]   "=a"  (toaddr)                                \
      , [fromaddr] "=a"  (fromaddr)                              \
      :                                                          \
      : "cc"                            /* clobbered */          \
    );                                                           \
  }

  //--------------------------------------------------------------
  // Atomic copying of 8-byte entities.
  // Conjoint/disjoint property does not matter. Entities are first
  // loaded and then stored.
  // _to and _from must be 8-byte aligned.
  //--------------------------------------------------------------
  #define COPY8_ATOMIC_4(_to,_from) {                            \
    unsigned long toaddr;                                        \
    asm(                                                         \
      "LG      3,%[from]        \n\t" /* address of from area */ \
      "LG      %[toaddr],%[to]  \n\t" /* address of to area   */ \
      "LMG     0,3,0(3)         \n\t" /* load data            */ \
      "STMG    0,3,0(%[toaddr]) \n\t" /* store data           */ \
      : [to]     "+Q"  (_to)          /* outputs   */            \
      , [from]   "+Q"  (_from)        /* outputs   */            \
      , [toaddr] "=a"  (toaddr)       /* inputs    */            \
      :                                                          \
      : "cc",  "r0", "r1", "r2", "r3" /* clobbered */            \
    );                                                           \
  }
  #define COPY8_ATOMIC_3(_to,_from) {                            \
    unsigned long toaddr;                                        \
    asm(                                                         \
      "LG      2,%[from]        \n\t" /* address of from area */ \
      "LG      %[toaddr],%[to]  \n\t" /* address of to area   */ \
      "LMG     0,2,0(2)         \n\t" /* load data            */ \
      "STMG    0,2,0(%[toaddr]) \n\t" /* store data           */ \
      : [to]     "+Q"  (_to)          /* outputs   */            \
      , [from]   "+Q"  (_from)        /* outputs   */            \
      , [toaddr] "=a"  (toaddr)       /* inputs    */            \
      :                                                          \
      : "cc",  "r0", "r1", "r2"       /* clobbered */            \
    );                                                           \
  }
  #define COPY8_ATOMIC_2(_to,_from) {                            \
    unsigned long toaddr;                                        \
    asm(                                                         \
      "LG      1,%[from]        \n\t" /* address of from area */ \
      "LG      %[toaddr],%[to]  \n\t" /* address of to area   */ \
      "LMG     0,1,0(1)         \n\t" /* load data            */ \
      "STMG    0,1,0(%[toaddr]) \n\t" /* store data           */ \
      : [to]     "+Q"  (_to)          /* outputs   */            \
      , [from]   "+Q"  (_from)        /* outputs   */            \
      , [toaddr] "=a"  (toaddr)       /* inputs    */            \
      :                                                          \
      : "cc",  "r0", "r1"             /* clobbered */            \
    );                                                           \
  }
  #define COPY8_ATOMIC_1(_to,_from) {                            \
    unsigned long addr;                                          \
    asm(                                                         \
      "LG      %[addr],%[from]  \n\t" /* address of from area */ \
      "LG      0,0(0,%[addr])   \n\t" /* load data            */ \
      "LG      %[addr],%[to]    \n\t" /* address of to area   */ \
      "STG     0,0(0,%[addr])   \n\t" /* store data           */ \
      : [to]     "+Q"  (_to)          /* outputs   */            \
      , [from]   "+Q"  (_from)        /* outputs   */            \
      , [addr]   "=a"  (addr)         /* inputs    */            \
      :                                                          \
      : "cc",  "r0"                   /* clobbered */            \
    );                                                           \
  }

  //--------------------------------------------------------------
  // Atomic copying of 4-byte entities.
  // Exactly 4 (four) entities are copied.
  // Conjoint/disjoint property does not matter. Entities are first
  // loaded and then stored.
  // _to and _from must be 4-byte aligned.
  //--------------------------------------------------------------
  #define COPY4_ATOMIC_4(_to,_from) {                            \
    unsigned long toaddr;                                        \
    asm(                                                         \
      "LG      3,%[from]        \n\t" /* address of from area */ \
      "LG      %[toaddr],%[to]  \n\t" /* address of to area   */ \
      "LM      0,3,0(3)         \n\t" /* load data            */ \
      "STM     0,3,0(%[toaddr]) \n\t" /* store data           */ \
      : [to]     "+Q"  (_to)          /* outputs   */            \
      , [from]   "+Q"  (_from)        /* outputs   */            \
      , [toaddr] "=a"  (toaddr)       /* inputs    */            \
      :                                                          \
      : "cc",  "r0", "r1", "r2", "r3" /* clobbered */            \
    );                                                           \
  }
  #define COPY4_ATOMIC_3(_to,_from) {                            \
    unsigned long toaddr;                                        \
    asm(                                                         \
      "LG      2,%[from]        \n\t" /* address of from area */ \
      "LG      %[toaddr],%[to]  \n\t" /* address of to area   */ \
      "LM      0,2,0(2)         \n\t" /* load data            */ \
      "STM     0,2,0(%[toaddr]) \n\t" /* store data           */ \
      : [to]     "+Q"  (_to)          /* outputs   */            \
      , [from]   "+Q"  (_from)        /* outputs   */            \
      , [toaddr] "=a"  (toaddr)       /* inputs    */            \
      :                                                          \
      : "cc",  "r0", "r1", "r2"       /* clobbered */            \
    );                                                           \
  }
  #define COPY4_ATOMIC_2(_to,_from) {                            \
    unsigned long toaddr;                                        \
    asm(                                                         \
      "LG      1,%[from]        \n\t" /* address of from area */ \
      "LG      %[toaddr],%[to]  \n\t" /* address of to area   */ \
      "LM      0,1,0(1)         \n\t" /* load data            */ \
      "STM     0,1,0(%[toaddr]) \n\t" /* store data           */ \
      : [to]     "+Q"  (_to)          /* outputs   */            \
      , [from]   "+Q"  (_from)        /* outputs   */            \
      , [toaddr] "=a"  (toaddr)       /* inputs    */            \
      :                                                          \
      : "cc",  "r0", "r1"             /* clobbered */            \
    );                                                           \
  }
  #define COPY4_ATOMIC_1(_to,_from) {                            \
    unsigned long addr;                                          \
    asm(                                                         \
      "LG      %[addr],%[from]  \n\t" /* address of from area */ \
      "L       0,0(0,%[addr])   \n\t" /* load data            */ \
      "LG      %[addr],%[to]    \n\t" /* address of to area   */ \
      "ST      0,0(0,%[addr])   \n\t" /* store data           */ \
      : [to]     "+Q"  (_to)          /* outputs   */            \
      , [from]   "+Q"  (_from)        /* outputs   */            \
      , [addr]   "=a"  (addr)         /* inputs    */            \
      :                                                          \
      : "cc",  "r0"                   /* clobbered */            \
    );                                                           \
  }

#if 0  // Waiting for gcc to support EXRL.
  #define MVC_MEMCOPY(_to,_from,_len)                                \
    if (VM_Version::has_ExecuteExtensions()) {                       \
      asm("\t"                                                       \
      "    LAY     1,-1(0,%[len])      \n\t" /* decr for MVC  */     \
      "    EXRL    1,1f                \n\t" /* execute MVC instr */ \
      "    BRC     15,2f               \n\t" /* skip template */     \
      "1:  MVC     0(%[len],%[to]),0(%[from]) \n\t"                  \
      "2:  BCR     0,0                 \n\t"                         \
      : [to]   "+Q"  (_to)             /* outputs   */               \
      , [from] "+Q"  (_from)           /* outputs   */               \
      : [len]  "r"   (_len)            /* inputs    */               \
      : "cc",  "r1"                    /* clobbered */               \
      );                                                             \
    } else {                                                         \
      asm("\t"                                                       \
      "    LARL    2,3f                \n\t"                         \
      "    LAY     1,-1(0,%[len])      \n\t" /* decr for MVC  */     \
      "    EX      1,0(2)              \n\t" /* execute MVC instr */ \
      "    BRC     15,4f               \n\t" /* skip template */     \
      "3:  MVC     0(%[len],%[to]),0(%[from])  \n\t"                 \
      "4:  BCR     0,0                 \n\t"                         \
      : [to]   "+Q"  (_to)             /* outputs   */               \
      , [from] "+Q"  (_from)           /* outputs   */               \
      : [len]  "r"   (_len)            /* inputs    */               \
      : "cc",  "r1", "r2"              /* clobbered */               \
      );                                                             \
    }
#else
  #define MVC_MEMCOPY(_to,_from,_len)                                \
  { unsigned long toaddr;   unsigned long tolen;                     \
    unsigned long fromaddr; unsigned long target;                    \
      asm("\t"                                                       \
      "    LTGR    %[tolen],%[len]     \n\t" /* decr for MVC  */     \
      "    BRC     8,2f                \n\t" /* do nothing for l=0*/ \
      "    AGHI    %[tolen],-1         \n\t"                         \
      "    LG      %[toaddr],%[to]     \n\t"                         \
      "    LG      %[fromaddr],%[from] \n\t"                         \
      "    LARL    %[target],1f        \n\t" /* addr of MVC instr */ \
      "    EX      %[tolen],0(%[target])         \n\t" /* execute MVC instr */ \
      "    BRC     15,2f                         \n\t" /* skip template */     \
      "1:  MVC     0(1,%[toaddr]),0(%[fromaddr]) \n\t"                         \
      "2:  BCR     0,0                 \n\t" /* nop a branch target*/\
      : [to]       "+Q"  (_to)         /* outputs   */               \
      , [from]     "+Q"  (_from)                                     \
      , [tolen]    "=a"  (tolen)                                     \
      , [toaddr]   "=a"  (toaddr)                                    \
      , [fromaddr] "=a"  (fromaddr)                                  \
      , [target]   "=a"  (target)                                    \
      : [len]       "r"  (_len)        /* inputs    */               \
      : "cc"                           /* clobbered */               \
      );                                                             \
  }
#endif

  #if 0  // code snippet to be used for debugging
      /* ASSERT code BEGIN */                                                \
      "    LARL    %[len],5f       \n\t"                                     \
      "    LARL    %[mta],4f       \n\t"                                     \
      "    SLGR    %[len],%[mta]   \n\t"                                     \
      "    CGHI    %[len],16       \n\t"                                     \
      "    BRC     7,9f            \n\t"      /* block size !=  16 */        \
                                                                             \
      "    LARL    %[len],1f       \n\t"                                     \
      "    SLGR    %[len],%[mta]   \n\t"                                     \
      "    CGHI    %[len],256      \n\t"                                     \
      "    BRC     7,9f            \n\t"      /* list len   != 256 */        \
                                                                             \
      "    LGR     0,0             \n\t"      /* artificial SIGILL */        \
      "9:  BRC     7,-2            \n\t"                                     \
      "    LARL    %[mta],1f       \n\t"      /* restore MVC table begin */  \
      /* ASSERT code END   */
  #endif

  // Optimized copying for data less than 4k
  // - no destructive overlap
  // - 0 <= _n_bytes <= 4096
  // This macro needs to be gcc-compiled with -march=z990. Otherwise, the
  // LAY instruction is not available.
  #define MVC_MULTI(_to,_from,_n_bytes)                                      \
  { unsigned long toaddr;                                                    \
    unsigned long fromaddr;                                                  \
    unsigned long movetable;                                                 \
    unsigned long len;                                                       \
      asm("\t"                                                               \
      "    LTGFR   %[len],%[nby]   \n\t"                                     \
      "    LG      %[ta],%[to]     \n\t"      /* address of to area   */     \
      "    BRC     8,1f            \n\t"      /* nothing to copy   */        \
                                                                             \
      "    NILL    %[nby],255      \n\t"      /* # bytes mod 256      */     \
      "    LG      %[fa],%[from]   \n\t"      /* address of from area */     \
      "    BRC     8,3f            \n\t"      /* no rest, skip copying */    \
                                                                             \
      "    LARL    %[mta],2f       \n\t"      /* MVC template addr */        \
      "    AHI     %[nby],-1       \n\t"      /* adjust for EX MVC  */       \
                                                                             \
      "    EX      %[nby],0(%[mta]) \n\t"     /* only rightmost */           \
                                              /* 8 bits of nby used */       \
      /* Since nby is <= 4096 on entry to this code, we do need */           \
      /* no zero extension before using it in addr calc.        */           \
      "    LA      %[fa],1(%[nby],%[fa]) \n\t"/* adjust from addr */         \
      "    LA      %[ta],1(%[nby],%[ta]) \n\t"/* adjust to   addr */         \
                                                                             \
      "3:  SRAG    %[nby],%[len],8 \n\t"      /* # cache lines     */        \
      "    LARL    %[mta],1f       \n\t"      /* MVC table begin   */        \
      "    BRC     8,1f            \n\t"      /* nothing to copy   */        \
                                                                             \
      /* Insert ASSERT code here if required. */                             \
                                                                             \
                                                                             \
      "    LNGFR   %[nby],%[nby]   \n\t"      /* negative offset into     */ \
      "    SLLG    %[nby],%[nby],4 \n\t"      /* MVC table 16-byte blocks */ \
      "    BC      15,0(%[nby],%[mta]) \n\t"  /* branch to block #ncl  */    \
                                                                             \
      "2:  MVC     0(1,%[ta]),0(%[fa]) \n\t"  /* MVC template */             \
                                                                             \
      "4:  MVC     0(256,%[ta]),0(%[fa])   \n\t" /* 4096 == l        */      \
      "    LAY     %[ta],256(0,%[ta])      \n\t"                             \
      "    LA      %[fa],256(0,%[fa])      \n\t"                             \
      "5:  MVC     0(256,%[ta]),0(%[fa])   \n\t" /* 3840 <= l < 4096 */      \
      "    LAY     %[ta],256(0,%[ta])      \n\t"                             \
      "    LA      %[fa],256(0,%[fa])      \n\t"                             \
      "    MVC     0(256,%[ta]),0(%[fa])   \n\t" /* 3548 <= l < 3328 */      \
      "    LAY     %[ta],256(0,%[ta])      \n\t"                             \
      "    LA      %[fa],256(0,%[fa])      \n\t"                             \
      "    MVC     0(256,%[ta]),0(%[fa])   \n\t" /* 3328 <= l < 3328 */      \
      "    LAY     %[ta],256(0,%[ta])      \n\t"                             \
      "    LA      %[fa],256(0,%[fa])      \n\t"                             \
      "    MVC     0(256,%[ta]),0(%[fa])   \n\t" /* 3072 <= l < 3328 */      \
      "    LAY     %[ta],256(0,%[ta])      \n\t"                             \
      "    LA      %[fa],256(0,%[fa])      \n\t"                             \
      "    MVC     0(256,%[ta]),0(%[fa])   \n\t" /* 2816 <= l < 3072 */      \
      "    LAY     %[ta],256(0,%[ta])      \n\t"                             \
      "    LA      %[fa],256(0,%[fa])      \n\t"                             \
      "    MVC     0(256,%[ta]),0(%[fa])   \n\t" /* 2560 <= l < 2816 */      \
      "    LAY     %[ta],256(0,%[ta])      \n\t"                             \
      "    LA      %[fa],256(0,%[fa])      \n\t"                             \
      "    MVC     0(256,%[ta]),0(%[fa])   \n\t" /* 2304 <= l < 2560 */      \
      "    LAY     %[ta],256(0,%[ta])      \n\t"                             \
      "    LA      %[fa],256(0,%[fa])      \n\t"                             \
      "    MVC     0(256,%[ta]),0(%[fa])   \n\t" /* 2048 <= l < 2304 */      \
      "    LAY     %[ta],256(0,%[ta])      \n\t"                             \
      "    LA      %[fa],256(0,%[fa])      \n\t"                             \
      "    MVC     0(256,%[ta]),0(%[fa])   \n\t" /* 1792 <= l < 2048 */      \
      "    LAY     %[ta],256(0,%[ta])      \n\t"                             \
      "    LA      %[fa],256(0,%[fa])      \n\t"                             \
      "    MVC     0(256,%[ta]),0(%[fa])   \n\t" /* 1536 <= l < 1792 */      \
      "    LAY     %[ta],256(0,%[ta])      \n\t"                             \
      "    LA      %[fa],256(0,%[fa])      \n\t"                             \
      "    MVC     0(256,%[ta]),0(%[fa])   \n\t" /* 1280 <= l < 1536 */      \
      "    LAY     %[ta],256(0,%[ta])      \n\t"                             \
      "    LA      %[fa],256(0,%[fa])      \n\t"                             \
      "    MVC     0(256,%[ta]),0(%[fa])   \n\t" /* 1024 <= l < 1280 */      \
      "    LAY     %[ta],256(0,%[ta])      \n\t"                             \
      "    LA      %[fa],256(0,%[fa])      \n\t"                             \
      "    MVC     0(256,%[ta]),0(%[fa])   \n\t" /*  768 <= l < 1024 */      \
      "    LAY     %[ta],256(0,%[ta])      \n\t"                             \
      "    LA      %[fa],256(0,%[fa])      \n\t"                             \
      "    MVC     0(256,%[ta]),0(%[fa])   \n\t" /*  512 <= l <  768 */      \
      "    LAY     %[ta],256(0,%[ta])      \n\t"                             \
      "    LA      %[fa],256(0,%[fa])      \n\t"                             \
      "    MVC     0(256,%[ta]),0(%[fa])   \n\t" /*  256 <= l <  512 */      \
      "    LAY     %[ta],256(0,%[ta])      \n\t"                             \
      "    LA      %[fa],256(0,%[fa])      \n\t"                             \
      "1:  BCR     0,0                     \n\t" /* nop as branch target */  \
      : [to]       "+Q"  (_to)          /* outputs   */          \
      , [from]     "+Q"  (_from)                                 \
      , [ta]       "=a"  (toaddr)                                \
      , [fa]       "=a"  (fromaddr)                              \
      , [mta]      "=a"  (movetable)                             \
      , [nby]      "+a"  (_n_bytes)                              \
      , [len]      "=a"  (len)                                   \
      :                                                          \
      : "cc"                            /* clobbered */          \
    );                                                           \
  }

  #define MVCLE_MEMCOPY(_to,_from,_len)                           \
    asm(                                                          \
      "    LG      0,%[to]     \n\t"   /* address of to area   */ \
      "    LG      2,%[from]   \n\t"   /* address of from area */ \
      "    LGR     1,%[len]    \n\t"   /* len of to area       */ \
      "    LGR     3,%[len]    \n\t"   /* len of from area     */ \
      "1:  MVCLE   0,2,176     \n\t"   /* copy storage, bypass cache (0xb0) */ \
      "    BRC     1,1b        \n\t"   /* retry if interrupted */ \
      : [to]   "+Q"  (_to)             /* outputs   */            \
      , [from] "+Q"  (_from)           /* outputs   */            \
      : [len]  "r"   (_len)            /* inputs    */            \
      : "cc",  "r0", "r1", "r2", "r3"  /* clobbered */            \
    );

  #define MVCLE_MEMINIT(_to,_val,_len)                            \
    asm(                                                          \
      "    LG      0,%[to]       \n\t" /* address of to area   */ \
      "    LGR     1,%[len]      \n\t" /* len of to area       */ \
      "    XGR     3,3           \n\t" /* from area len = 0    */ \
      "1:  MVCLE   0,2,0(%[val]) \n\t" /* init storage         */ \
      "    BRC     1,1b          \n\t" /* retry if interrupted */ \
      : [to]   "+Q"  (_to)             /* outputs   */            \
      : [len]  "r"   (_len)            /* inputs    */            \
      , [val]  "r"   (_val)            /* inputs    */            \
      : "cc",  "r0", "r1", "r3"        /* clobbered */            \
    );
  #define MVCLE_MEMZERO(_to,_len)                                 \
    asm(                                                          \
      "    LG      0,%[to]       \n\t" /* address of to area   */ \
      "    LGR     1,%[len]      \n\t" /* len of to area       */ \
      "    XGR     3,3           \n\t" /* from area len = 0    */ \
      "1:  MVCLE   0,2,0         \n\t" /* clear storage        */ \
      "    BRC     1,1b          \n\t" /* retry if interrupted */ \
      : [to]   "+Q"  (_to)             /* outputs   */            \
      : [len]  "r"   (_len)            /* inputs    */            \
      : "cc",  "r0", "r1", "r3"        /* clobbered */            \
    );

  // Clear a stretch of memory, 0 <= _len <= 256.
  // There is no alignment prereq.
  // There is no test for len out of range specified above.
  #define XC_MEMZERO_256(_to,_len)                                 \
{ unsigned long toaddr;   unsigned long tolen;                     \
  unsigned long target;                                            \
    asm("\t"                                                       \
    "    LTGR    %[tolen],%[len]     \n\t" /* decr for MVC  */     \
    "    BRC     8,2f                \n\t" /* do nothing for l=0*/ \
    "    AGHI    %[tolen],-1         \n\t" /* adjust for EX XC  */ \
    "    LARL    %[target],1f        \n\t" /* addr of XC instr  */ \
    "    LG      %[toaddr],%[to]     \n\t" /* addr of data area */ \
    "    EX      %[tolen],0(%[target])       \n\t" /* execute MVC instr */ \
    "    BRC     15,2f                       \n\t" /* skip template */     \
    "1:  XC      0(1,%[toaddr]),0(%[toaddr]) \n\t"                         \
    "2:  BCR     0,0                 \n\t" /* nop a branch target*/\
    : [to]       "+Q"  (_to)         /* outputs   */               \
    , [tolen]    "=a"  (tolen)                                     \
    , [toaddr]   "=a"  (toaddr)                                    \
    , [target]   "=a"  (target)                                    \
    : [len]       "r"  (_len)        /* inputs    */               \
    : "cc"                           /* clobbered */               \
    );                                                             \
}

  // Clear a stretch of memory, 256 < _len.
  // XC_MEMZERO_256 may be used to clear shorter areas.
  //
  // The code
  // - first zeroes a few bytes to align on a HeapWord.
  //   This step is currently inactive because all calls seem
  //   to have their data aligned on HeapWord boundaries.
  // - then zeroes a few HeapWords to align on a cache line.
  // - then zeroes entire cache lines in a loop.
  // - then zeroes the remaining (partial) cache line.
#if 1
  #define XC_MEMZERO_ANY(_to,_len)                                    \
{ unsigned long toaddr;   unsigned long tolen;                        \
  unsigned long len8;     unsigned long len256;                       \
  unsigned long target;   unsigned long lenx;                         \
    asm("\t"                                                          \
    "    LTGR    %[tolen],%[len]      \n\t" /*                   */   \
    "    BRC     8,2f                 \n\t" /* do nothing for l=0*/   \
    "    LG      %[toaddr],%[to]      \n\t" /* addr of data area */   \
    "    LARL    %[target],1f         \n\t" /* addr of XC instr  */   \
    " "                                                               \
    "    LCGR    %[len256],%[toaddr]  \n\t" /* cache line alignment */\
    "    NILL    %[len256],0xff       \n\t"                           \
    "    BRC     8,4f                 \n\t" /* already aligned     */ \
    "    NILH    %[len256],0x00       \n\t" /* zero extend         */ \
    "    LLGFR   %[len256],%[len256]  \n\t"                           \
    "    LAY     %[lenx],-1(,%[len256]) \n\t"                         \
    "    EX      %[lenx],0(%[target]) \n\t" /* execute MVC instr   */ \
    "    LA      %[toaddr],0(%[len256],%[toaddr]) \n\t"               \
    "    SGR     %[tolen],%[len256]   \n\t" /* adjust len          */ \
    " "                                                               \
    "4:  SRAG    %[lenx],%[tolen],8   \n\t" /* # cache lines       */ \
    "    BRC     8,6f                 \n\t" /* no full cache lines */ \
    "5:  XC      0(256,%[toaddr]),0(%[toaddr]) \n\t"                  \
    "    LA      %[toaddr],256(,%[toaddr]) \n\t"                      \
    "    BRCTG   %[lenx],5b           \n\t" /* iterate             */ \
    " "                                                               \
    "6:  NILL    %[tolen],0xff        \n\t" /* leftover bytes      */ \
    "    BRC     8,2f                 \n\t" /* done if none        */ \
    "    LAY     %[lenx],-1(,%[tolen]) \n\t"                          \
    "    EX      %[lenx],0(%[target]) \n\t" /* execute MVC instr   */ \
    "    BRC     15,2f                \n\t" /* skip template       */ \
    " "                                                               \
    "1:  XC      0(1,%[toaddr]),0(%[toaddr]) \n\t"                    \
    "2:  BCR     0,0                  \n\t" /* nop a branch target */ \
    : [to]       "+Q"  (_to)         /* outputs   */               \
    , [lenx]     "=a"  (lenx)                                      \
    , [len256]   "=a"  (len256)                                    \
    , [tolen]    "=a"  (tolen)                                     \
    , [toaddr]   "=a"  (toaddr)                                    \
    , [target]   "=a"  (target)                                    \
    : [len]       "r"  (_len)        /* inputs    */               \
    : "cc"                           /* clobbered */               \
    );                                                             \
}
#else
  #define XC_MEMZERO_ANY(_to,_len)                                    \
{ unsigned long toaddr;   unsigned long tolen;                        \
  unsigned long len8;     unsigned long len256;                       \
  unsigned long target;   unsigned long lenx;                         \
    asm("\t"                                                          \
    "    LTGR    %[tolen],%[len]      \n\t" /*                   */   \
    "    BRC     8,2f                 \n\t" /* do nothing for l=0*/   \
    "    LG      %[toaddr],%[to]      \n\t" /* addr of data area */   \
    "    LARL    %[target],1f         \n\t" /* addr of XC instr  */   \
    " "                                                               \
    "    LCGR    %[len8],%[toaddr]    \n\t" /* HeapWord alignment  */ \
    "    NILL    %[len8],0x07         \n\t"                           \
    "    BRC     8,3f                 \n\t" /* already aligned     */ \
    "    NILH    %[len8],0x00         \n\t" /* zero extend         */ \
    "    LLGFR   %[len8],%[len8]      \n\t"                           \
    "    LAY     %[lenx],-1(,%[len8]) \n\t"                           \
    "    EX      %[lenx],0(%[target]) \n\t" /* execute MVC instr */   \
    "    LA      %[toaddr],0(%[len8],%[toaddr]) \n\t"                 \
    "    SGR     %[tolen],%[len8]     \n\t" /* adjust len          */ \
    " "                                                               \
    "3:  LCGR    %[len256],%[toaddr]  \n\t" /* cache line alignment */\
    "    NILL    %[len256],0xff       \n\t"                           \
    "    BRC     8,4f                 \n\t" /* already aligned     */ \
    "    NILH    %[len256],0x00       \n\t" /* zero extend         */ \
    "    LLGFR   %[len256],%[len256]  \n\t"                           \
    "    LAY     %[lenx],-1(,%[len256]) \n\t"                         \
    "    EX      %[lenx],0(%[target]) \n\t" /* execute MVC instr   */ \
    "    LA      %[toaddr],0(%[len256],%[toaddr]) \n\t"               \
    "    SGR     %[tolen],%[len256]   \n\t" /* adjust len          */ \
    " "                                                               \
    "4:  SRAG    %[lenx],%[tolen],8   \n\t" /* # cache lines       */ \
    "    BRC     8,6f                 \n\t" /* no full cache lines */ \
    "5:  XC      0(256,%[toaddr]),0(%[toaddr]) \n\t"                  \
    "    LA      %[toaddr],256(,%[toaddr]) \n\t"                      \
    "    BRCTG   %[lenx],5b           \n\t" /* iterate             */ \
    " "                                                               \
    "6:  NILL    %[tolen],0xff        \n\t" /* leftover bytes      */ \
    "    BRC     8,2f                 \n\t" /* done if none        */ \
    "    LAY     %[lenx],-1(,%[tolen]) \n\t"                          \
    "    EX      %[lenx],0(%[target]) \n\t" /* execute MVC instr   */ \
    "    BRC     15,2f                \n\t" /* skip template       */ \
    " "                                                               \
    "1:  XC      0(1,%[toaddr]),0(%[toaddr]) \n\t"                    \
    "2:  BCR     0,0                  \n\t" /* nop a branch target */ \
    : [to]       "+Q"  (_to)         /* outputs   */               \
    , [lenx]     "=a"  (lenx)                                      \
    , [len8]     "=a"  (len8)                                      \
    , [len256]   "=a"  (len256)                                    \
    , [tolen]    "=a"  (tolen)                                     \
    , [toaddr]   "=a"  (toaddr)                                    \
    , [target]   "=a"  (target)                                    \
    : [len]       "r"  (_len)        /* inputs    */               \
    : "cc"                           /* clobbered */               \
    );                                                             \
}
#endif
#endif // USE_INLINE_ASM

//*************************************//
//   D I S J O I N T   C O P Y I N G   //
//*************************************//

static void pd_aligned_disjoint_words(const HeapWord* from, HeapWord* to, size_t count) {
  // JVM2008: very frequent, some tests frequent.

  // Copy HeapWord (=DW) aligned storage. Use MVCLE in inline-asm code.
  // MVCLE guarantees DW concurrent (i.e. atomic) accesses if both the addresses of the operands
  // are DW aligned and the length is an integer multiple of a DW. Should always be true here.
  //
  // No special exploit needed. H/W discovers suitable situations itself.
  //
  // For large chunks of memory, exploit special H/W support of z/Architecture:
  // 1) copy short piece of memory to page-align address(es)
  // 2) copy largest part (all contained full pages) of memory using mvcle instruction.
  //    z/Architecture processors have special H/W support for page-aligned storage
  //    where len is an int multiple of page size. In that case, up to 4 cache lines are
  //    processed in parallel and L1 cache is not polluted.
  // 3) copy the remaining piece of memory.
  //
#ifdef USE_INLINE_ASM
  jbyte* to_bytes   = (jbyte*)to;
  jbyte* from_bytes = (jbyte*)from;
  size_t len_bytes  = count*HeapWordSize;

  // Optimized copying for data less than 4k
  switch (count) {
    case 0: return;
    case 1: MOVE8_ATOMIC_1(to,from)
            return;
    case 2: MOVE8_ATOMIC_2(to,from)
            return;
//  case 3: MOVE8_ATOMIC_3(to,from)
//          return;
//  case 4: MOVE8_ATOMIC_4(to,from)
//          return;
    default:
      if (len_bytes <= 4096) {
        MVC_MULTI(to,from,len_bytes)
        return;
      }
      // else
      MVCLE_MEMCOPY(to_bytes, from_bytes, len_bytes)
      return;
  }
#else
  // Fallback code.
  switch (count) {
    case 0:
      return;

    case 1:
      *to = *from;
      return;

    case 2:
      *to++ = *from++;
      *to = *from;
      return;

    case 3:
      *to++ = *from++;
      *to++ = *from++;
      *to = *from;
      return;

    case 4:
      *to++ = *from++;
      *to++ = *from++;
      *to++ = *from++;
      *to = *from;
      return;

    default:
      while (count-- > 0)
        *(to++) = *(from++);
      return;
  }
#endif
}

static void pd_disjoint_words_atomic(const HeapWord* from, HeapWord* to, size_t count) {
  // JVM2008: < 4k calls.
  assert(((((size_t)from) & 0x07L) | (((size_t)to) & 0x07L)) == 0, "No atomic copy w/o aligned data");
  pd_aligned_disjoint_words(from, to, count); // Rare calls -> just delegate.
}

static void pd_disjoint_words(const HeapWord* from, HeapWord* to, size_t count) {
  // JVM2008: very rare.
  pd_aligned_disjoint_words(from, to, count); // Rare calls -> just delegate.
}


//*************************************//
//   C O N J O I N T   C O P Y I N G   //
//*************************************//

static void pd_aligned_conjoint_words(const HeapWord* from, HeapWord* to, size_t count) {
  // JVM2008: between some and lower end of frequent.

#ifdef USE_INLINE_ASM
  size_t  count_in = count;
  if (has_destructive_overlap((char*)from, (char*)to, count_in*BytesPerLong)) {
    switch (count_in) {
      case 4: COPY8_ATOMIC_4(to,from)
              return;
      case 3: COPY8_ATOMIC_3(to,from)
              return;
      case 2: COPY8_ATOMIC_2(to,from)
              return;
      case 1: COPY8_ATOMIC_1(to,from)
              return;
      case 0: return;
      default:
        from += count_in;
        to   += count_in;
        while (count_in-- > 0)
          *(--to) = *(--from); // Copy backwards, areas overlap destructively.
        return;
    }
  }
  // else
  jbyte* to_bytes   = (jbyte*)to;
  jbyte* from_bytes = (jbyte*)from;
  size_t len_bytes  = count_in*BytesPerLong;
  MVCLE_MEMCOPY(to_bytes, from_bytes, len_bytes)
  return;
#else
  // Fallback code.
  if (has_destructive_overlap((char*)from, (char*)to, count*BytesPerLong)) {
    HeapWord t1, t2, t3;
    switch (count) {
      case 0:
        return;

      case 1:
        *to = *from;
        return;

      case 2:
        t1 = *(from+1);
        *to = *from;
        *(to+1) = t1;
        return;

      case 3:
        t1 = *(from+1);
        t2 = *(from+2);
        *to = *from;
        *(to+1) = t1;
        *(to+2) = t2;
        return;

      case 4:
        t1 = *(from+1);
        t2 = *(from+2);
        t3 = *(from+3);
        *to = *from;
        *(to+1) = t1;
        *(to+2) = t2;
        *(to+3) = t3;
        return;

      default:
        from += count;
        to   += count;
        while (count-- > 0)
          *(--to) = *(--from); // Copy backwards, areas overlap destructively.
        return;
    }
  }
  // else
  // Just delegate. HeapWords are optimally aligned anyway.
  pd_aligned_disjoint_words(from, to, count);
#endif
}

static void pd_conjoint_words(const HeapWord* from, HeapWord* to, size_t count) {

  // Just delegate. HeapWords are optimally aligned anyway.
  pd_aligned_conjoint_words(from, to, count);
}

static void pd_conjoint_bytes(const void* from, void* to, size_t count) {

#ifdef USE_INLINE_ASM
  size_t count_in = count;
  if (has_destructive_overlap((char*)from, (char*)to, count_in))
    (void)memmove(to, from, count_in);
  else {
    jbyte*  to_bytes   = (jbyte*)to;
    jbyte*  from_bytes = (jbyte*)from;
    size_t  len_bytes  = count_in;
    MVCLE_MEMCOPY(to_bytes, from_bytes, len_bytes)
  }
#else
  if (has_destructive_overlap((char*)from, (char*)to, count))
    (void)memmove(to, from, count);
  else
    (void)memcpy(to, from, count);
#endif
}

//**************************************************//
//   C O N J O I N T  A T O M I C   C O P Y I N G   //
//**************************************************//

static void pd_conjoint_bytes_atomic(const void* from, void* to, size_t count) {
  // Call arraycopy stubs to do the job.
  pd_conjoint_bytes(from, to, count); // bytes are always accessed atomically.
}

static void pd_conjoint_jshorts_atomic(const jshort* from, jshort* to, size_t count) {

#ifdef USE_INLINE_ASM
  size_t count_in = count;
  if (has_destructive_overlap((const char*)from, (char*)to, count_in*BytesPerShort)) {
    // Use optimizations from shared code where no z-specific optimization exists.
    copy_conjoint_jshorts_atomic(from, to, count);
  } else {
    jbyte* to_bytes   = (jbyte*)to;
    jbyte* from_bytes = (jbyte*)from;
    size_t len_bytes  = count_in*BytesPerShort;
    MVCLE_MEMCOPY(to_bytes, from_bytes, len_bytes)
  }
#else
  // Use optimizations from shared code where no z-specific optimization exists.
  copy_conjoint_jshorts_atomic(from, to, count);
#endif
}

static void pd_conjoint_jints_atomic(const jint* from, jint* to, size_t count) {

#ifdef USE_INLINE_ASM
  size_t count_in = count;
  if (has_destructive_overlap((const char*)from, (char*)to, count_in*BytesPerInt)) {
    switch (count_in) {
      case 4: COPY4_ATOMIC_4(to,from)
              return;
      case 3: COPY4_ATOMIC_3(to,from)
              return;
      case 2: COPY4_ATOMIC_2(to,from)
              return;
      case 1: COPY4_ATOMIC_1(to,from)
              return;
      case 0: return;
      default:
        // Use optimizations from shared code where no z-specific optimization exists.
        copy_conjoint_jints_atomic(from, to, count_in);
        return;
    }
  }
  // else
  jbyte* to_bytes   = (jbyte*)to;
  jbyte* from_bytes = (jbyte*)from;
  size_t len_bytes  = count_in*BytesPerInt;
  MVCLE_MEMCOPY(to_bytes, from_bytes, len_bytes)
#else
  // Use optimizations from shared code where no z-specific optimization exists.
  copy_conjoint_jints_atomic(from, to, count);
#endif
}

static void pd_conjoint_jlongs_atomic(const jlong* from, jlong* to, size_t count) {

#ifdef USE_INLINE_ASM
  size_t count_in = count;
  if (has_destructive_overlap((char*)from, (char*)to, count_in*BytesPerLong)) {
    switch (count_in) {
      case 4: COPY8_ATOMIC_4(to,from) return;
      case 3: COPY8_ATOMIC_3(to,from) return;
      case 2: COPY8_ATOMIC_2(to,from) return;
      case 1: COPY8_ATOMIC_1(to,from) return;
      case 0: return;
      default:
        from += count_in;
        to   += count_in;
        while (count_in-- > 0) { *(--to) = *(--from); } // Copy backwards, areas overlap destructively.
        return;
    }
  }
  // else {
  jbyte* to_bytes   = (jbyte*)to;
  jbyte* from_bytes = (jbyte*)from;
  size_t len_bytes  = count_in*BytesPerLong;
  MVCLE_MEMCOPY(to_bytes, from_bytes, len_bytes)
#else
  size_t count_in = count;
  if (has_destructive_overlap((char*)from, (char*)to, count_in*BytesPerLong)) {
    if (count_in < 8) {
      from += count_in;
      to   += count_in;
      while (count_in-- > 0)
         *(--to) = *(--from); // Copy backwards, areas overlap destructively.
      return;
    }
    // else {
    from += count_in-1;
    to   += count_in-1;
    if (count_in&0x01) {
      *(to--) = *(from--);
      count_in--;
    }
    for (; count_in>0; count_in-=2) {
      *to     = *from;
      *(to-1) = *(from-1);
      to     -= 2;
      from   -= 2;
    }
  }
  else
    pd_aligned_disjoint_words((const HeapWord*)from, (HeapWord*)to, count_in); // rare calls -> just delegate.
#endif
}

static void pd_conjoint_oops_atomic(const oop* from, oop* to, size_t count) {

#ifdef USE_INLINE_ASM
  size_t count_in = count;
  if (has_destructive_overlap((char*)from, (char*)to, count_in*BytesPerOop)) {
    switch (count_in) {
      case 4: COPY8_ATOMIC_4(to,from) return;
      case 3: COPY8_ATOMIC_3(to,from) return;
      case 2: COPY8_ATOMIC_2(to,from) return;
      case 1: COPY8_ATOMIC_1(to,from) return;
      case 0: return;
      default:
        from += count_in;
        to   += count_in;
        while (count_in-- > 0) { *(--to) = *(--from); } // Copy backwards, areas overlap destructively.
        return;
    }
  }
  // else
  jbyte* to_bytes   = (jbyte*)to;
  jbyte* from_bytes = (jbyte*)from;
  size_t len_bytes  = count_in*BytesPerOop;
  MVCLE_MEMCOPY(to_bytes, from_bytes, len_bytes)
#else
  size_t count_in = count;
  if (has_destructive_overlap((char*)from, (char*)to, count_in*BytesPerOop)) {
    from += count_in;
    to   += count_in;
    while (count_in-- > 0) *(--to) = *(--from); // Copy backwards, areas overlap destructively.
    return;
  }
  // else
  pd_aligned_disjoint_words((HeapWord*)from, (HeapWord*)to, count_in); // rare calls -> just delegate.
  return;
#endif
}

static void pd_arrayof_conjoint_bytes(const HeapWord* from, HeapWord* to, size_t count) {
  pd_conjoint_bytes_atomic(from, to, count);
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

//**********************************************//
//  M E M O R Y   I N I T I A L I S A T I O N   //
//**********************************************//

static void pd_fill_to_bytes(void* to, size_t count, jubyte value) {
  // JVM2008: very rare, only in some tests.
#ifdef USE_INLINE_ASM
  // Initialize storage to a given value. Use memset instead of copy loop.
  // For large chunks of memory, exploit special H/W support of z/Architecture:
  // 1) init short piece of memory to page-align address
  // 2) init largest part (all contained full pages) of memory using mvcle instruction.
  //    z/Architecture processors have special H/W support for page-aligned storage
  //    where len is an int multiple of page size. In that case, up to 4 cache lines are
  //    processed in parallel and L1 cache is not polluted.
  // 3) init the remaining piece of memory.
  // Atomicity cannot really be an issue since gcc implements the loop body with XC anyway.
  // If atomicity is a problem, we have to prevent gcc optimization. Best workaround: inline asm.

  jbyte*  to_bytes  = (jbyte*)to;
  size_t  len_bytes = count;

  MVCLE_MEMINIT(to_bytes, value, len_bytes)

#else
  // Memset does the best job possible: loop over 256-byte MVCs, with
  // the last MVC EXecuted. With the -mmvcle option, initialization
  // is done using MVCLE -> slight advantage for large areas.
  (void)memset(to, value, count);
#endif
}

static void pd_fill_to_words(HeapWord* tohw, size_t count, juint value) {
  // Occurs in dbg builds only. Usually memory poisoning with BAADBABE, DEADBEEF, etc.
  // JVM2008: < 4k calls.
  if (value == 0) {
    pd_zero_to_words(tohw, count);
    return;
  }
  if (value == ~(juint)(0)) {
    pd_fill_to_bytes(tohw, count*HeapWordSize, (jubyte)(~(juint)(0)));
    return;
  }
  julong* to = (julong*) tohw;
  julong  v  = ((julong) value << 32) | value;
  while (count-- > 0) {
    *to++ = v;
  }
}

static void pd_fill_to_aligned_words(HeapWord* tohw, size_t count, juint value) {
  // JVM2008: very frequent, but virtually all calls are with value == 0.
  pd_fill_to_words(tohw, count, value);
}

//**********************************//
//  M E M O R Y   C L E A R I N G   //
//**********************************//

// Delegate to pd_zero_to_bytes. It also works HeapWord-atomic.
// Distinguish between simple and large zero_to_words.
static void pd_zero_to_words(HeapWord* tohw, size_t count) {
  pd_zero_to_bytes(tohw, count*HeapWordSize);
}

static void pd_zero_to_bytes(void* to, size_t count) {
  // JVM2008: some calls (generally), some tests frequent
#ifdef USE_INLINE_ASM
  // Even zero_to_bytes() requires HeapWord-atomic, or, at least, sequential
  // zeroing of the memory. MVCLE is not fit for that job:
  //   "As observed by other CPUs and by the channel subsystem,
  //    that portion of the first operand which is filled
  //    with the padding byte is not necessarily stored into in
  //    a left-to-right direction and may appear to be stored
  //    into more than once."
  // Therefore, implementation was changed to use (multiple) XC instructions.

  const long line_size = 256;
  jbyte* to_bytes  = (jbyte*)to;
  size_t len_bytes = count;

  if (len_bytes <= line_size) {
    XC_MEMZERO_256(to_bytes, len_bytes);
  } else {
    XC_MEMZERO_ANY(to_bytes, len_bytes);
  }

#else
  // Memset does the best job possible: loop over 256-byte MVCs, with
  // the last MVC EXecuted. With the -mmvcle option, initialization
  // is done using MVCLE -> slight advantage for large areas.
  (void)memset(to, 0, count);
#endif
}

#endif // CPU_S390_COPY_S390_HPP
