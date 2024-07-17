/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/Types.h>

#include <Kernel/Firmware/EFI/EFI.h>
#include <Kernel/Memory/PhysicalAddress.h>

#include <Kernel/EFIPrekernel/Error.h>

namespace Kernel {

enum class Access {
    None = 0,
    Read = 1,
    Write = 2,
    Execute = 4,
};
AK_ENUM_BITWISE_OPERATORS(Access);

EFIErrorOr<void*> allocate_empty_root_page_table();
EFIErrorOr<void*> get_or_insert_page_table(void* root_page_table, FlatPtr vaddr, size_t level = 0, bool has_to_be_new = false);
EFIErrorOr<void> map_pages(void* root_page_table, FlatPtr start_vaddr, PhysicalPtr start_paddr, size_t page_count, Access access);

}
