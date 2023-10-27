/*
 * Copyright (c) 2018-2021, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/RegisterState.h>
#elif ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/RegisterState.h>
#elif ARCH(RISCV64)
#    include <Kernel/Arch/riscv64/RegisterState.h>
#else
#    error "Unknown architecture"
#endif
