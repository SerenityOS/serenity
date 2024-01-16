/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/sys/types.h>

#ifdef __cplusplus
extern "C" {
#endif

struct __attribute__((packed)) __mcontext {
    uint64_t x[31];
    uint64_t pc;
    uint64_t padding; // See FIXME in sys$sigreturn, sizeof(__mcontext) % 16 has to be 8
};

#ifdef __cplusplus
}
#endif
