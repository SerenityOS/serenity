/*
 * Copyright (c) 2018-2022, James Mintram <me@jamesrm.com>
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

#define PAGE_MASK (~(FlatPtr)0xfffu)

#define LSW(x) ((u32)(x) & 0xFFFF)
#define MSW(x) (((u32)(x) >> 16) & 0xFFFF)
#define LSB(x) ((x) & 0xFF)
#define MSB(x) (((x) >> 8) & 0xFF)

#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/CPU.h>
#elif ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/CPU.h>
#elif ARCH(RISCV64)
#    include <Kernel/Arch/riscv64/CPU.h>
#else
#    error "Unknown architecture"
#endif

namespace Kernel {

struct RegisterState;

void dump_registers(RegisterState const& regs);
void handle_crash(RegisterState const&, char const* description, int signal, bool out_of_memory = false);

}
