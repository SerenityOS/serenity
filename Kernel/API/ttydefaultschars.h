/*
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/termios.h>
#include <Kernel/API/ttydefaults.h>

#ifdef __clang__
#    pragma clang diagnostic push
#    pragma clang diagnostic ignored "-Wc99-designator"
#endif

#ifdef __cplusplus
extern "C" {
#endif

static cc_t const ttydefchars[NCCS] = {
    [VINTR] = CINTR,
    [VQUIT] = CQUIT,
    [VERASE] = CERASE,
    [VKILL] = CKILL,
    [VEOF] = CEOF,
    [VTIME] = CTIME,
    [VMIN] = CMIN,
    [VSWTC] = CSWTC,
    [VSTART] = CSTART,
    [VSTOP] = CSTOP,
    [VSUSP] = CSUSP,
    [VEOL] = CEOL,
    [VREPRINT] = CREPRINT,
    [VDISCARD] = CDISCARD,
    [VWERASE] = CWERASE,
    [VLNEXT] = CLNEXT,
    [VEOL2] = CEOL2
};

#ifdef __clang__
#    pragma clang diagnostic pop
#endif

#ifdef __cplusplus
}
#endif
