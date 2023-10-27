/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

namespace Kernel {
struct FPUState;
}

#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/FPUState.h>
#elif ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/FPUState.h>
#elif ARCH(RISCV64)
#    include <Kernel/Arch/riscv64/FPUState.h>
#else
#    error "Unknown architecture"
#endif
