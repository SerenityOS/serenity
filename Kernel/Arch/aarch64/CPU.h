/*
 * Copyright (c) 2018-2022, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Types.h>
#include <Kernel/Memory/PhysicalAddress.h>

namespace Kernel {

static constexpr u32 safe_pstate_mask = 0xf0000000;

void initialize_exceptions();
void panic_without_mmu(StringView);
void dbgln_without_mmu(StringView);

namespace Memory {

[[noreturn]] void init_page_tables_and_jump_to_init(PhysicalPtr flattened_devicetree_paddr);

}

}
