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

#include <AK/Assertions.h>
#include <LibJS/HeapBlock.h>

namespace JS {

HeapBlock::HeapBlock(size_t cell_size)
    : m_cell_size(cell_size)
{
    for (size_t i = 0; i < cell_count(); ++i) {
        auto* freelist_entry = static_cast<FreelistEntry*>(cell(i));
        freelist_entry->set_live(false);
        if (i == cell_count() - 1)
            freelist_entry->next = nullptr;
        else
            freelist_entry->next = static_cast<FreelistEntry*>(cell(i + 1));
    }
    m_freelist = static_cast<FreelistEntry*>(cell(0));
}

Cell* HeapBlock::allocate()
{
    if (!m_freelist)
        return nullptr;
    return exchange(m_freelist, m_freelist->next);
}

void HeapBlock::deallocate(Cell* cell)
{
    ASSERT(cell->is_live());
    ASSERT(!cell->is_marked());
    cell->~Cell();
    auto* freelist_entry = static_cast<FreelistEntry*>(cell);
    freelist_entry->set_live(false);
    freelist_entry->next = m_freelist;
    m_freelist = freelist_entry;
}

}
