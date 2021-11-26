/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

.section .init, "ax", @progbits
    movl %ebp, %esp
    popl %ebp
    retl

.section .fini, "ax", @progbits
    movl %ebp, %esp
    popl %ebp
    retl
