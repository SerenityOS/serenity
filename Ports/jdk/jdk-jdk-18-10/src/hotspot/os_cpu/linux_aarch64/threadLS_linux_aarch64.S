// Copyright (c) 2015, Red Hat Inc. All rights reserved.
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

        // JavaThread::aarch64_get_thread_helper()
        //
        // Return the current thread pointer in x0.
        // Clobber x1, flags.
        // All other registers are preserved,

	.global	_ZN10JavaThread25aarch64_get_thread_helperEv
	.type	_ZN10JavaThread25aarch64_get_thread_helperEv, %function

_ZN10JavaThread25aarch64_get_thread_helperEv:
	stp x29, x30, [sp, -16]!
	adrp x0, :tlsdesc:_ZN6Thread12_thr_currentE
	ldr x1, [x0, #:tlsdesc_lo12:_ZN6Thread12_thr_currentE]
	add x0, x0, :tlsdesc_lo12:_ZN6Thread12_thr_currentE
	.tlsdesccall _ZN6Thread12_thr_currentE
	blr x1
	mrs x1, tpidr_el0
	add x0, x1, x0
	ldr x0, [x0]
	ldp x29, x30, [sp], 16
	ret

	.size _ZN10JavaThread25aarch64_get_thread_helperEv, .-_ZN10JavaThread25aarch64_get_thread_helperEv
