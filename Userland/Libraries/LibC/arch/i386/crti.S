/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

.section .init, "ax", @progbits
.align 4
.global _init
.type _init, @function
_init:
    pushl %ebp
    movl %esp, %ebp
    andl $-16, %esp

.section .fini, "ax", @progbits
.align 4
.global _fini
.type _fini, @function
_fini:
    pushl %ebp
    movl %esp, %ebp
    andl $-16, %esp
