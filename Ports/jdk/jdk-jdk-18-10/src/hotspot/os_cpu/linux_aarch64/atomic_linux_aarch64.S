// Copyright (c) 2021, Red Hat Inc. All rights reserved.
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//
// This code is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License version 2 only, as
// published by the Free Software Foundation.
//
// This code is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
// version 2 for more details (a copy is included in the LICENSE file that
// accompanied this code).
//
// You should have received a copy of the GNU General Public License version
// 2 along with this work; if not, write to the Free Software Foundation,
// Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
//
// Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
// or visit www.oracle.com if you need additional information or have any
// questions.



        .text

        .globl aarch64_atomic_fetch_add_8_default_impl
        .align 5
aarch64_atomic_fetch_add_8_default_impl:
        prfm    pstl1strm, [x0]
0:      ldaxr   x2, [x0]
        add     x8, x2, x1
        stlxr   w9, x8, [x0]
        cbnz    w9, 0b
        dmb     ish
        mov     x0, x2
        ret

        .globl aarch64_atomic_fetch_add_4_default_impl
        .align 5
aarch64_atomic_fetch_add_4_default_impl:
        prfm    pstl1strm, [x0]
0:      ldaxr   w2, [x0]
        add     w8, w2, w1
        stlxr   w9, w8, [x0]
        cbnz    w9, 0b
        dmb     ish
        mov     w0, w2
        ret

        .globl aarch64_atomic_xchg_4_default_impl
        .align 5
aarch64_atomic_xchg_4_default_impl:
        prfm    pstl1strm, [x0]
0:      ldaxr   w2, [x0]
        stlxr   w8, w1, [x0]
        cbnz    w8, 0b
        dmb     ish
        mov     w0, w2
        ret

        .globl aarch64_atomic_xchg_8_default_impl
        .align 5
aarch64_atomic_xchg_8_default_impl:
        prfm    pstl1strm, [x0]
0:      ldaxr   x2, [x0]
        stlxr   w8, x1, [x0]
        cbnz    w8, 0b
        dmb     ish
        mov     x0, x2
        ret

        .globl aarch64_atomic_cmpxchg_1_default_impl
        .align 5
aarch64_atomic_cmpxchg_1_default_impl:
        dmb     ish
        prfm    pstl1strm, [x0]
0:      ldxrb   w3, [x0]
        eor     w8, w3, w1
        tst     x8, #0xff
        b.ne    1f
        stxrb   w8, w2, [x0]
        cbnz    w8, 0b
1:      mov     w0, w3
        dmb     ish
        ret

        .globl aarch64_atomic_cmpxchg_4_default_impl
        .align 5
aarch64_atomic_cmpxchg_4_default_impl:
        dmb     ish
        prfm    pstl1strm, [x0]
0:      ldxr    w3, [x0]
        cmp     w3, w1
        b.ne    1f
        stxr    w8, w2, [x0]
        cbnz    w8, 0b
1:      mov     w0, w3
        dmb     ish
        ret

        .globl aarch64_atomic_cmpxchg_8_default_impl
        .align 5
aarch64_atomic_cmpxchg_8_default_impl:
        dmb     ish
        prfm    pstl1strm, [x0]
0:      ldxr    x3, [x0]
        cmp     x3, x1
        b.ne    1f
        stxr    w8, x2, [x0]
        cbnz    w8, 0b
1:      mov     x0, x3
        dmb     ish
        ret

        .globl aarch64_atomic_cmpxchg_4_release_default_impl
        .align 5
aarch64_atomic_cmpxchg_4_release_default_impl:
        prfm    pstl1strm, [x0]
0:      ldxr    w3, [x0]
        cmp     w3, w1
        b.ne    1f
        stlxr   w8, w2, [x0]
        cbnz    w8, 0b
1:      mov     w0, w3
        ret

        .globl aarch64_atomic_cmpxchg_8_release_default_impl
        .align 5
aarch64_atomic_cmpxchg_8_release_default_impl:
        prfm    pstl1strm, [x0]
0:      ldxr    x3, [x0]
        cmp     x3, x1
        b.ne    1f
        stlxr   w8, x2, [x0]
        cbnz    w8, 0b
1:      mov     x0, x3
        ret

        .globl aarch64_atomic_cmpxchg_4_seq_cst_default_impl
        .align 5
aarch64_atomic_cmpxchg_4_seq_cst_default_impl:
        prfm    pstl1strm, [x0]
0:      ldaxr   w3, [x0]
        cmp     w3, w1
        b.ne    1f
        stlxr   w8, w2, [x0]
        cbnz    w8, 0b
1:      mov     w0, w3
        ret

        .globl aarch64_atomic_cmpxchg_8_seq_cst_default_impl
        .align 5
aarch64_atomic_cmpxchg_8_seq_cst_default_impl:
        prfm    pstl1strm, [x0]
0:      ldaxr   x3, [x0]
        cmp     x3, x1
        b.ne    1f
        stlxr   w8, x2, [x0]
        cbnz    w8, 0b
1:      mov     x0, x3
        ret

.globl aarch64_atomic_cmpxchg_1_relaxed_default_impl
        .align 5
aarch64_atomic_cmpxchg_1_relaxed_default_impl:
        prfm    pstl1strm, [x0]
0:      ldxrb   w3, [x0]
        eor     w8, w3, w1
        tst     x8, #0xff
        b.ne    1f
        stxrb   w8, w2, [x0]
        cbnz    w8, 0b
1:      mov     w0, w3
        ret

        .globl aarch64_atomic_cmpxchg_4_relaxed_default_impl
        .align 5
aarch64_atomic_cmpxchg_4_relaxed_default_impl:
        prfm    pstl1strm, [x0]
0:      ldxr    w3, [x0]
        cmp     w3, w1
        b.ne    1f
        stxr    w8, w2, [x0]
        cbnz    w8, 0b
1:      mov     w0, w3
        ret

        .globl aarch64_atomic_cmpxchg_8_relaxed_default_impl
        .align 5
aarch64_atomic_cmpxchg_8_relaxed_default_impl:
        prfm    pstl1strm, [x0]
0:      ldxr    x3, [x0]
        cmp     x3, x1
        b.ne    1f
        stxr    w8, x2, [x0]
        cbnz    w8, 0b
1:      mov     x0, x3
        ret
