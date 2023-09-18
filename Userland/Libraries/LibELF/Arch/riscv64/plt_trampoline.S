/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

.p2align 4
.globl _plt_trampoline
.hidden _plt_trampoline
.type _plt_trampoline,@function

_plt_trampoline:
    // FIXME: Implement the PLT trampoline.
    unimp
