/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

.section .init, "ax", @progbits
.p2align 2
.global _init
.type _init, @function
_init:
    # FIXME: Possibly incomplete.
    ret

.section .fini, "ax", @progbits
.p2align 4
.global _fini
.type _fini, @function
_fini:
    # FIXME: Possibly incomplete.
    ret
