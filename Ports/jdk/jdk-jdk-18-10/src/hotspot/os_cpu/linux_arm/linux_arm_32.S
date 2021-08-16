#
# Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#


        # NOTE WELL!  The _Copy functions are called directly
	# from server-compiler-generated code via CallLeafNoFP,
	# which means that they *must* either not use floating
	# point or use it in the same manner as does the server
	# compiler.

        .globl _Copy_conjoint_bytes
	.type _Copy_conjoint_bytes, %function
        .globl _Copy_arrayof_conjoint_bytes
	.type _Copy_arrayof_conjoint_bytes, %function
	.globl _Copy_disjoint_words
	.type _Copy_disjoint_words, %function
	.globl _Copy_conjoint_words
	.type _Copy_conjoint_words, %function
        .globl _Copy_conjoint_jshorts_atomic
	.type _Copy_conjoint_jshorts_atomic, %function
	.globl _Copy_arrayof_conjoint_jshorts
	.type _Copy_arrayof_conjoint_jshorts, %function
        .globl _Copy_conjoint_jints_atomic
	.type _Copy_conjoint_jints_atomic, %function
        .globl _Copy_arrayof_conjoint_jints
	.type _Copy_arrayof_conjoint_jints, %function
	.globl _Copy_conjoint_jlongs_atomic
	.type _Copy_conjoint_jlongs_atomic, %function
	.globl _Copy_arrayof_conjoint_jlongs
	.type _Copy_arrayof_conjoint_jlongs, %function

from	.req	r0
to	.req	r1

	.text
        .globl  SpinPause
        .type SpinPause, %function
SpinPause:
        bx      LR

        # Support for void Copy::conjoint_bytes(void* from,
        #                                       void* to,
        #                                       size_t count)
_Copy_conjoint_bytes:
        swi     0x9f0001

        # Support for void Copy::arrayof_conjoint_bytes(void* from,
        #                                               void* to,
        #                                               size_t count)
_Copy_arrayof_conjoint_bytes:
        swi     0x9f0001


        # Support for void Copy::disjoint_words(void* from,
        #                                       void* to,
        #                                       size_t count)
_Copy_disjoint_words:
        stmdb    sp!, {r3 - r9, ip}
 
        cmp     r2, #0
        beq     disjoint_words_finish

        pld     [from, #0]
        cmp     r2, #12
        ble disjoint_words_small

        .align 3
dw_f2b_loop_32:
        subs    r2, #32
	blt	dw_f2b_loop_32_finish
        ldmia from!, {r3 - r9, ip}
        nop
	pld     [from]
        stmia to!, {r3 - r9, ip}
        bgt     dw_f2b_loop_32
dw_f2b_loop_32_finish:
        addlts  r2, #32
        beq     disjoint_words_finish
        cmp     r2, #16
	blt	disjoint_words_small
        ldmia from!, {r3 - r6}
        subge   r2, r2, #16
        stmia to!, {r3 - r6}
        beq     disjoint_words_finish
disjoint_words_small:
        cmp     r2, #8
        ldr     r7, [from], #4
        ldrge   r8, [from], #4
        ldrgt   r9, [from], #4
        str     r7, [to], #4
        strge   r8, [to], #4
        strgt   r9, [to], #4

disjoint_words_finish:
        ldmia   sp!, {r3 - r9, ip}
        bx      lr


        # Support for void Copy::conjoint_words(void* from,
        #                                       void* to,
        #                                       size_t count)
_Copy_conjoint_words:
        stmdb    sp!, {r3 - r9, ip}

	cmp	r2, #0
	beq	conjoint_words_finish

        pld     [from, #0]
        cmp     r2, #12
        ble conjoint_words_small

        subs    r3, to, from
        cmphi   r2, r3
        bhi     cw_b2f_copy
        .align 3
cw_f2b_loop_32:
        subs    r2, #32
	blt	cw_f2b_loop_32_finish
        ldmia from!, {r3 - r9, ip}
        nop
	pld     [from]
        stmia to!, {r3 - r9, ip}
        bgt     cw_f2b_loop_32
cw_f2b_loop_32_finish:
        addlts  r2, #32
        beq     conjoint_words_finish
        cmp     r2, #16
	blt	conjoint_words_small
        ldmia from!, {r3 - r6}
        subge   r2, r2, #16
        stmia to!, {r3 - r6}
        beq     conjoint_words_finish
conjoint_words_small:
        cmp     r2, #8
        ldr     r7, [from], #4
        ldrge   r8, [from], #4
        ldrgt   r9, [from], #4
        str     r7, [to], #4
        strge   r8, [to], #4
        strgt   r9, [to], #4
        b       conjoint_words_finish

	# Src and dest overlap, copy in a descending order
cw_b2f_copy:
        add     from, r2
        pld     [from, #-32]
        add     to, r2
        .align 3
cw_b2f_loop_32:
        subs    r2, #32
	blt	cw_b2f_loop_32_finish
        ldmdb from!, {r3-r9,ip}
        nop
	pld     [from, #-32]
        stmdb to!, {r3-r9,ip}
        bgt     cw_b2f_loop_32
cw_b2f_loop_32_finish:
        addlts  r2, #32
        beq     conjoint_words_finish
        cmp     r2, #16
	blt	cw_b2f_copy_small
        ldmdb from!, {r3 - r6}
        subge   r2, r2, #16
        stmdb to!, {r3 - r6}
        beq     conjoint_words_finish
cw_b2f_copy_small:
        cmp     r2, #8
        ldr     r7, [from, #-4]!
        ldrge   r8, [from, #-4]!
        ldrgt   r9, [from, #-4]!
        str     r7, [to, #-4]!
        strge   r8, [to, #-4]!
        strgt   r9, [to, #-4]!

conjoint_words_finish:
        ldmia   sp!, {r3 - r9, ip}
        bx      lr

        # Support for void Copy::conjoint_jshorts_atomic(void* from,
        #                                                void* to,
        #                                                size_t count)
_Copy_conjoint_jshorts_atomic:
        stmdb   sp!, {r3 - r9, ip}

	cmp	r2, #0
	beq	conjoint_shorts_finish	

        subs    r3, to, from
        cmphi   r2, r3
        bhi     cs_b2f_copy

        pld     [from]

        ands    r3, to, #3
        bne     cs_f2b_dest_u
        ands    r3, from, #3
        bne     cs_f2b_src_u

	# Aligned source address
        .align 3
cs_f2b_loop_32:
        subs    r2, #32
	blt	cs_f2b_loop_32_finish
        ldmia from!, {r3 - r9, ip}
        nop
        pld     [from]
        stmia to!, {r3 - r9, ip}
        bgt     cs_f2b_loop_32
cs_f2b_loop_32_finish:
        addlts  r2, #32
        beq     conjoint_shorts_finish
        movs    r6, r2, lsr #3
        .align 3
cs_f2b_8_loop:
        beq     cs_f2b_4
        ldmia   from!, {r4-r5}
        subs    r6, #1
        stmia   to!, {r4-r5}
        bgt     cs_f2b_8_loop

cs_f2b_4:
        ands    r2, #7
        beq     conjoint_shorts_finish
        cmp     r2, #4
        ldrh    r3, [from], #2
        ldrgeh  r4, [from], #2
        ldrgth  r5, [from], #2
        strh    r3, [to], #2
        strgeh  r4, [to], #2
        strgth  r5, [to], #2
        b       conjoint_shorts_finish

	# Destination not aligned
cs_f2b_dest_u:
        ldrh    r3, [from], #2
        subs    r2, #2
        strh    r3, [to], #2
        beq     conjoint_shorts_finish

	# Check to see if source is not aligned ether
        ands    r3, from, #3
        beq     cs_f2b_loop_32

cs_f2b_src_u:
        cmp     r2, #16
        blt     cs_f2b_8_u

	# Load 2 first bytes to r7 and make src ptr word aligned
        bic     from, #3
        ldr     r7, [from], #4

	# Destination aligned, source not
        mov     r8, r2, lsr #4
        .align 3
cs_f2b_16_u_loop:
        mov     r3, r7, lsr #16
        ldmia   from!, {r4 - r7}
        orr     r3, r3, r4, lsl #16
        mov     r4, r4, lsr #16
        pld     [from]
        orr     r4, r4, r5, lsl #16
        mov     r5, r5, lsr #16
        orr     r5, r5, r6, lsl #16
        mov     r6, r6, lsr #16
        orr     r6, r6, r7, lsl #16
        stmia   to!, {r3 - r6}
        subs    r8, #1
        bgt     cs_f2b_16_u_loop
        ands    r2, #0xf
        beq     conjoint_shorts_finish
        sub     from, #2

cs_f2b_8_u:
        cmp     r2, #8
        blt     cs_f2b_4_u
        ldrh    r4, [from], #2
        ldr     r5, [from], #4
        ldrh    r6, [from], #2
        orr     r4, r4, r5, lsl #16
        mov     r5, r5, lsr #16
        orr     r5, r5, r6, lsl #16
        subs    r2, #8
        stmia	to!, {r4 - r5}
cs_f2b_4_u:
        beq     conjoint_shorts_finish
        cmp     r2, #4
        ldrh    r3, [from], #2
        ldrgeh  r4, [from], #2
        ldrgth  r5, [from], #2
        strh    r3, [to], #2
        strgeh  r4, [to], #2
        strgth  r5, [to], #2
        b       conjoint_shorts_finish

	# Src and dest overlap, copy in a descending order
cs_b2f_copy:
        add     from, r2
        pld     [from, #-32]
        add     to, r2

        ands    r3, to, #3
        bne     cs_b2f_dest_u
        ands    r3, from, #3
        bne     cs_b2f_src_u
        .align 3
cs_b2f_loop_32:
        subs    r2, #32
	blt	cs_b2f_loop_32_finish
        ldmdb from!, {r3-r9,ip}
        nop
        pld     [from, #-32]
        stmdb to!, {r3-r9,ip}
        bgt     cs_b2f_loop_32
cs_b2f_loop_32_finish:
        addlts  r2, #32
        beq     conjoint_shorts_finish
        cmp     r2, #24
        blt     cs_b2f_16
        ldmdb   from!, {r3-r8}
        sub     r2, #24
        stmdb   to!, {r3-r8}
        beq     conjoint_shorts_finish
cs_b2f_16:
        cmp     r2, #16
        blt     cs_b2f_8
        ldmdb   from!, {r3-r6}
        sub     r2, #16
        stmdb   to!, {r3-r6}
        beq     conjoint_shorts_finish
cs_b2f_8:
        cmp     r2, #8
        blt     cs_b2f_all_copy
        ldmdb   from!, {r3-r4}
        sub     r2, #8
        stmdb   to!, {r3-r4}
        beq     conjoint_shorts_finish

cs_b2f_all_copy:
        cmp     r2, #4
        ldrh    r3, [from, #-2]!
        ldrgeh  r4, [from, #-2]!
        ldrgth  r5, [from, #-2]!
        strh    r3, [to, #-2]!
        strgeh  r4, [to, #-2]!
        strgth  r5, [to, #-2]!
        b       conjoint_shorts_finish

	# Destination not aligned
cs_b2f_dest_u:
        ldrh    r3, [from, #-2]!
        strh    r3, [to, #-2]!
        sub     r2, #2
	# Check source alignment as well
        ands    r3, from, #3
        beq     cs_b2f_loop_32

	# Source not aligned
cs_b2f_src_u:
        bic     from, #3
        .align 3
cs_b2f_16_loop_u:
        subs    r2, #16
        blt     cs_b2f_16_loop_u_finished
        ldr     r7, [from]
        mov     r3, r7
        ldmdb   from!, {r4 - r7}
        mov     r4, r4, lsr #16
        orr     r4, r4, r5, lsl #16
        pld     [from, #-32]
        mov     r5, r5, lsr #16
        orr     r5, r5, r6, lsl #16
        mov     r6, r6, lsr #16
        orr     r6, r6, r7, lsl #16
        mov     r7, r7, lsr #16
        orr     r7, r7, r3, lsl #16
        stmdb   to!, {r4 - r7}
        bgt     cs_b2f_16_loop_u
        beq     conjoint_shorts_finish
cs_b2f_16_loop_u_finished:
        addlts  r2, #16
        ldr     r3, [from]
	cmp     r2, #10
        blt     cs_b2f_2_u_loop
        ldmdb   from!, {r4 - r5}
        mov     r6, r4, lsr #16
        orr     r6, r6, r5, lsl #16
        mov     r7, r5, lsr #16
        orr     r7, r7, r3, lsl #16
        stmdb   to!, {r6-r7}
        sub     r2, #8
	.align 3
cs_b2f_2_u_loop:
        subs    r2, #2
        ldrh    r3, [from], #-2
        strh    r3, [to, #-2]!
        bgt     cs_b2f_2_u_loop

conjoint_shorts_finish:
        ldmia   sp!, {r3 - r9, ip}
        bx      lr


        # Support for void Copy::arrayof_conjoint_jshorts(void* from,
        #                                                 void* to,
        #                                                 size_t count)
_Copy_arrayof_conjoint_jshorts:
        swi     0x9f0001

        # Support for void Copy::conjoint_jints_atomic(void* from,
        #                                              void* to,
        #                                              size_t count)
_Copy_conjoint_jints_atomic:
_Copy_arrayof_conjoint_jints:
        swi     0x9f0001
	
        # Support for void Copy::conjoint_jlongs_atomic(jlong* from,
        #                                               jlong* to,
        #                                               size_t count)
_Copy_conjoint_jlongs_atomic:
_Copy_arrayof_conjoint_jlongs:
        stmdb    sp!, {r3 - r9, ip}

	cmp	r2, #0
	beq	conjoint_longs_finish

        pld     [from, #0]
        cmp     r2, #24
        ble conjoint_longs_small

        subs    r3, to, from
        cmphi   r2, r3
        bhi     cl_b2f_copy
        .align 3
cl_f2b_loop_32:
        subs    r2, #32
	blt	cl_f2b_loop_32_finish
        ldmia from!, {r3 - r9, ip}
        nop
	pld     [from]
        stmia to!, {r3 - r9, ip}
        bgt     cl_f2b_loop_32
cl_f2b_loop_32_finish:
        addlts  r2, #32
        beq     conjoint_longs_finish
conjoint_longs_small:
        cmp     r2, #16
	blt	cl_f2b_copy_8
	bgt	cl_f2b_copy_24
        ldmia 	from!, {r3 - r6}
        stmia 	to!, {r3 - r6}
	b	conjoint_longs_finish
cl_f2b_copy_8:
        ldmia   from!, {r3 - r4}
        stmia   to!, {r3 - r4}
        b       conjoint_longs_finish
cl_f2b_copy_24:
	ldmia   from!, {r3 - r8}
        stmia   to!, {r3 - r8}
        b       conjoint_longs_finish

	# Src and dest overlap, copy in a descending order
cl_b2f_copy:
        add     from, r2
        pld     [from, #-32]
        add     to, r2
        .align 3
cl_b2f_loop_32:
        subs    r2, #32
	blt	cl_b2f_loop_32_finish
        ldmdb 	from!, {r3 - r9, ip}
        nop
	pld     [from]
        stmdb 	to!, {r3 - r9, ip}
        bgt     cl_b2f_loop_32
cl_b2f_loop_32_finish:
        addlts  r2, #32
        beq     conjoint_longs_finish
        cmp     r2, #16
	blt	cl_b2f_copy_8
	bgt	cl_b2f_copy_24
        ldmdb   from!, {r3 - r6}
        stmdb   to!, {r3 - r6}
        b       conjoint_longs_finish
cl_b2f_copy_8:
	ldmdb   from!, {r3 - r4}
        stmdb   to!, {r3 - r4}
        b       conjoint_longs_finish
cl_b2f_copy_24:
	ldmdb   from!, {r3 - r8}
        stmdb   to!, {r3 - r8}

conjoint_longs_finish:
        ldmia   sp!, {r3 - r9, ip}
        bx      lr


