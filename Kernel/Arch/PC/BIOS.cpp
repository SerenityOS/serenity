/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    mapping.region = MM.allocate_kernel_region(mapping.paddr, PAGE_ROUND_UP(mapping.size), {}, Region::Access::Read);
    return mapping;
}

MappedROM map_ebda()
{
    auto ebda_segment_ptr = map_typed<u16>(PhysicalAddress(0x40e));
    auto ebda_length_ptr = map_typed<u16>(PhysicalAddress(0x413));

    PhysicalAddress ebda_paddr(*ebda_segment_ptr << 4);
    size_t ebda_size = *ebda_length_ptr;

    MappedROM mapping;
    mapping.region = MM.allocate_kernel_region(ebda_paddr.page_base(), PAGE_ROUND_UP(ebda_size), {}, Region::Access::Read);
    mapping.offset = ebda_paddr.offset_in_page();
    mapping.size = ebda_size;
    mapping.paddr = ebda_paddr;
    return mapping;
}

}
