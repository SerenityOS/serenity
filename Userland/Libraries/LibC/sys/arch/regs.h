/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

#if ARCH(X86_64)
#    include "x86_64/regs.h"
#elif ARCH(AARCH64)
#    include "aarch64/regs.h"
#elif ARCH(RISCV64)
#    include "riscv64/regs.h"
#else
#    error Unknown architecture
#endif
