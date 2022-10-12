/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

.global _setjmp
.global setjmp
_setjmp:
setjmp:
    # FIXME: Possibly incomplete.
    ret

.global _longjmp
.global longjmp
_longjmp:
longjmp:
    # FIXME: Possibly incomplete.
    ret

.global _sigsetjmp
.global sigsetjmp
_sigsetjmp:
sigsetjmp:
    # FIXME: Possibly incomplete.
    ret
