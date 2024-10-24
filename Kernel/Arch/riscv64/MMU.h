/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <Kernel/Prekernel/Prekernel.h>

#include <AK/Platform.h>
VALIDATE_IS_RISCV64()

namespace Kernel::Memory {

[[noreturn]] void init_page_tables_and_jump_to_init(FlatPtr boot_hart_id, PhysicalPtr flattened_devicetree_paddr);

}
