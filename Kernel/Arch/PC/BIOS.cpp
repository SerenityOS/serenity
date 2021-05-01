/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Arch/PC/BIOS.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/TypedMapping.h>

namespace Kernel {

MappedROM map_bios()
{
    MappedROM mapping;
    mapping.size = 128 * KiB;
    mapping.paddr = PhysicalAddress(0xe0000);
    mapping.region = MM.allocate_kernel_region(mapping.paddr, page_round_up(mapping.size), {}, Region::Access::Read);
    return mapping;
}

MappedROM map_ebda()
{
    auto ebda_segment_ptr = map_typed<u16>(PhysicalAddress(0x40e));
    auto ebda_length_ptr_b0 = map_typed<u8>(PhysicalAddress(0x413));
    auto ebda_length_ptr_b1 = map_typed<u8>(PhysicalAddress(0x414));

    PhysicalAddress ebda_paddr(*ebda_segment_ptr << 4);
    size_t ebda_size = (*ebda_length_ptr_b1 << 8) | *ebda_length_ptr_b0;

    MappedROM mapping;
    mapping.region = MM.allocate_kernel_region(ebda_paddr.page_base(), page_round_up(ebda_size), {}, Region::Access::Read);
    mapping.offset = ebda_paddr.offset_in_page();
    mapping.size = ebda_size;
    mapping.paddr = ebda_paddr;
    return mapping;
}

}
