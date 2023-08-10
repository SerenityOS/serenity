/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#if defined(__x86_64__)
#    include <Kernel/Arch/x86_64/mcontext.h>
#elif defined(__aarch64__)
#    include <Kernel/Arch/aarch64/mcontext.h>
#elif defined(__riscv) && __riscv_xlen == 64
#    include <Kernel/Arch/riscv64/mcontext.h>
#endif
