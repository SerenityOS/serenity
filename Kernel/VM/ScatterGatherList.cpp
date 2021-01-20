/*
 * Copyright (c) 2021, the SerenityOS developers.
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

#include <Kernel/VM/ScatterGatherList.h>

namespace Kernel {

ScatterGatherList ScatterGatherList::create_from_buffer(const u8* buffer, size_t size)
{
    VERIFY(buffer && size);
    ScatterGatherList new_list;
    auto* region = MM.find_region_from_vaddr(VirtualAddress(buffer));
    VERIFY(region);
    while (size > 0) {
        size_t offset_in_page = (VirtualAddress(buffer) - region->vaddr()).get() % PAGE_SIZE;
        size_t size_in_page = min(PAGE_SIZE - offset_in_page, size);
        VERIFY(offset_in_page + size_in_page - 1 <= PAGE_SIZE);
        new_list.add_entry(region->physical_page(region->page_index_from_address(VirtualAddress(buffer)))->paddr().get(), offset_in_page, size_in_page);
        size -= size_in_page;
        buffer += size_in_page;
    }
    return new_list;
}

ScatterGatherList ScatterGatherList::create_from_physical(PhysicalAddress paddr, size_t size)
{
    VERIFY(!paddr.is_null() && size);
    ScatterGatherList new_list;
    new_list.add_entry(paddr.page_base().get(), paddr.offset_in_page(), size);
    return new_list;
}

void ScatterGatherList::add_entry(FlatPtr addr, size_t offset, size_t size)
{
    m_entries.append({ addr, offset, size });
}

void ScatterGatherList::for_each_entry(Function<void(const FlatPtr, const size_t)> callback) const
{
    for (auto& entry : m_entries)
        callback(entry.page_base + entry.offset, entry.length);
}

}
