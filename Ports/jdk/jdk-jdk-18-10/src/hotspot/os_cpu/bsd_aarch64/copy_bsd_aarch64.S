/*
 * Copyright (c) 2016, Linaro Ltd. All rights reserved.
 * Copyright (c) 2021, Azul Systems, Inc. All rights reserved.
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

#define CFUNC(x) _##x

        .global CFUNC(_Copy_conjoint_words)
        .global CFUNC(_Copy_disjoint_words)

s       .req    x0
d       .req    x1
count   .req    x2
t0      .req    x3
t1      .req    x4
t2      .req    x5
t3      .req    x6
t4      .req    x7
t5      .req    x8
t6      .req    x9
t7      .req    x10

        .align  6
CFUNC(_Copy_disjoint_words):
        // Ensure 2 word aligned
        tbz     s, #3, fwd_copy_aligned
        ldr     t0, [s], #8
        str     t0, [d], #8
        sub     count, count, #1

fwd_copy_aligned:
        // Bias s & d so we only pre index on the last copy
        sub     s, s, #16
        sub     d, d, #16

        ldp     t0, t1, [s, #16]
        ldp     t2, t3, [s, #32]
        ldp     t4, t5, [s, #48]
        ldp     t6, t7, [s, #64]!

        subs    count, count, #16
        blo     fwd_copy_drain

fwd_copy_again:
        prfm    pldl1keep, [s, #256]
        stp     t0, t1, [d, #16]
        ldp     t0, t1, [s, #16]
        stp     t2, t3, [d, #32]
        ldp     t2, t3, [s, #32]
        stp     t4, t5, [d, #48]
        ldp     t4, t5, [s, #48]
        stp     t6, t7, [d, #64]!
        ldp     t6, t7, [s, #64]!
        subs    count, count, #8
        bhs     fwd_copy_again

fwd_copy_drain:
        stp     t0, t1, [d, #16]
        stp     t2, t3, [d, #32]
        stp     t4, t5, [d, #48]
        stp     t6, t7, [d, #64]!

        // count is now -8..-1 for 0..7 words to copy
        adr     t0, 0f
        add     t0, t0, count, lsl #5
        br      t0

        .align  5
        ret                             // -8 == 0 words
        .align  5
        ldr     t0, [s, #16]            // -7 == 1 word
        str     t0, [d, #16]
        ret
        .align  5
        ldp     t0, t1, [s, #16]        // -6 = 2 words
        stp     t0, t1, [d, #16]
        ret
        .align  5
        ldp     t0, t1, [s, #16]        // -5 = 3 words
        ldr     t2, [s, #32]
        stp     t0, t1, [d, #16]
        str     t2, [d, #32]
        ret
        .align  5
        ldp     t0, t1, [s, #16]        // -4 = 4 words
        ldp     t2, t3, [s, #32]
        stp     t0, t1, [d, #16]
        stp     t2, t3, [d, #32]
        ret
        .align  5
        ldp     t0, t1, [s, #16]        // -3 = 5 words
        ldp     t2, t3, [s, #32]
        ldr     t4, [s, #48]
        stp     t0, t1, [d, #16]
        stp     t2, t3, [d, #32]
        str     t4, [d, #48]
        ret
        .align  5
        ldp     t0, t1, [s, #16]        // -2 = 6 words
        ldp     t2, t3, [s, #32]
        ldp     t4, t5, [s, #48]
        stp     t0, t1, [d, #16]
        stp     t2, t3, [d, #32]
        stp     t4, t5, [d, #48]
        ret
        .align  5
        ldp     t0, t1, [s, #16]        // -1 = 7 words
        ldp     t2, t3, [s, #32]
        ldp     t4, t5, [s, #48]
        ldr     t6, [s, #64]
        stp     t0, t1, [d, #16]
        stp     t2, t3, [d, #32]
        stp     t4, t5, [d, #48]
        str     t6, [d, #64]
        // Is always aligned here, code for 7 words is one instruction
        // too large so it just falls through.
        .align  5
0:
        ret

        .align  6
CFUNC(_Copy_conjoint_words):
        sub     t0, d, s
        cmp     t0, count, lsl #3
        bhs     CFUNC(_Copy_disjoint_words)

        add     s, s, count, lsl #3
        add     d, d, count, lsl #3

        // Ensure 2 word aligned
        tbz     s, #3, bwd_copy_aligned
        ldr     t0, [s, #-8]!
        str     t0, [d, #-8]!
        sub     count, count, #1

bwd_copy_aligned:
        ldp     t0, t1, [s, #-16]
        ldp     t2, t3, [s, #-32]
        ldp     t4, t5, [s, #-48]
        ldp     t6, t7, [s, #-64]!

        subs    count, count, #16
        blo     bwd_copy_drain

bwd_copy_again:
        prfum   pldl1keep, [s, #-256]
        stp     t0, t1, [d, #-16]
        ldp     t0, t1, [s, #-16]
        stp     t2, t3, [d, #-32]
        ldp     t2, t3, [s, #-32]
        stp     t4, t5, [d, #-48]
        ldp     t4, t5, [s, #-48]
        stp     t6, t7, [d, #-64]!
        ldp     t6, t7, [s, #-64]!
        subs    count, count, #8
        bhs     bwd_copy_again

bwd_copy_drain:
        stp     t0, t1, [d, #-16]
        stp     t2, t3, [d, #-32]
        stp     t4, t5, [d, #-48]
        stp     t6, t7, [d, #-64]!

        // count is now -8..-1 for 0..7 words to copy
        adr     t0, 0f
        add     t0, t0, count, lsl #5
        br      t0

        .align  5
        ret                             // -8 == 0 words
        .align  5
        ldr     t0, [s, #-8]            // -7 == 1 word
        str     t0, [d, #-8]
        ret
        .align  5
        ldp     t0, t1, [s, #-16]       // -6 = 2 words
        stp     t0, t1, [d, #-16]
        ret
        .align  5
        ldp     t0, t1, [s, #-16]       // -5 = 3 words
        ldr     t2, [s, #-24]
        stp     t0, t1, [d, #-16]
        str     t2, [d, #-24]
        ret
        .align  5
        ldp     t0, t1, [s, #-16]       // -4 = 4 words
        ldp     t2, t3, [s, #-32]
        stp     t0, t1, [d, #-16]
        stp     t2, t3, [d, #-32]
        ret
        .align  5
        ldp     t0, t1, [s, #-16]       // -3 = 5 words
        ldp     t2, t3, [s, #-32]
        ldr     t4, [s, #-40]
        stp     t0, t1, [d, #-16]
        stp     t2, t3, [d, #-32]
        str     t4, [d, #-40]
        ret
        .align  5
        ldp     t0, t1, [s, #-16]       // -2 = 6 words
        ldp     t2, t3, [s, #-32]
        ldp     t4, t5, [s, #-48]
        stp     t0, t1, [d, #-16]
        stp     t2, t3, [d, #-32]
        stp     t4, t5, [d, #-48]
        ret
        .align  5
        ldp     t0, t1, [s, #-16]       // -1 = 7 words
        ldp     t2, t3, [s, #-32]
        ldp     t4, t5, [s, #-48]
        ldr     t6, [s, #-56]
        stp     t0, t1, [d, #-16]
        stp     t2, t3, [d, #-32]
        stp     t4, t5, [d, #-48]
        str     t6, [d, #-56]
        // Is always aligned here, code for 7 words is one instruction
        // too large so it just falls through.
        .align  5
0:
        ret
