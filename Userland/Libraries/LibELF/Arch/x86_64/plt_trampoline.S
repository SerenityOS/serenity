/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

    .align 4
    .globl _plt_trampoline
    .hidden _plt_trampoline
    .type _plt_trampoline,@function
_plt_trampoline:
    int3
