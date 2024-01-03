/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <Kernel/Memory/PhysicalAddress.h>

#include <AK/Platform.h>
VALIDATE_IS_RISCV64()

namespace Kernel {

void dbgln_without_mmu(StringView);
[[noreturn]] void panic_without_mmu(StringView);

extern "C" [[noreturn]] void pre_init(FlatPtr mhartid, PhysicalPtr fdt_phys_addr);

}
