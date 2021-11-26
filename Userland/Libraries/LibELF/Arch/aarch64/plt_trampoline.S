/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

.p2align 4
.globl _plt_trampoline
.hidden _plt_trampoline
.type _plt_trampoline,@function
_plt_trampoline: # (object, relocation_index)
    # FIXME: Possibly incomplete.
    ret
