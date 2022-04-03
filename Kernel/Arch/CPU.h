/*
 * Copyright (c) 2018-2022, James Mintram <me@jamesrm.com>
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

#if ARCH(X86_64) || ARCH(I386)
#    include <Kernel/Arch/x86/CPU.h>
#elif ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/CPU.h>
#else
#    error "Unknown architecture"
#endif

namespace Kernel {

struct RegisterState;

void dump_registers(RegisterState const& regs);
void handle_crash(RegisterState const&, char const* description, int signal, bool out_of_memory = false);

}
