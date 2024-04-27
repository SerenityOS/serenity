/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#if defined(__x86_64__)
#    include <arch/x86_64/fenv.h>
#elif defined(__aarch64__)
#    include <arch/aarch64/fenv.h>
#elif defined(__riscv) && __riscv_xlen == 64
#    include <arch/riscv64/fenv.h>
#else
#    error Unknown architecture
#endif
