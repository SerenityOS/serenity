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
#include <AK/NonnullOwnPtr.h>
#include <AK/kmalloc.h>
#include <LibJS/Heap/HeapBlock.h>
#include <stdio.h>
#include <sys/mman.h>

namespace JS {

NonnullOwnPtr<HeapBlock> HeapBlock::create_with_cell_size(Heap& heap, size_t cell_size)
{
    char name[64];
    snprintf(name, sizeof(name), "LibJS: HeapBlock(%zu)", cell_size);
    auto* block = (HeapBlock*)serenity_mmap(nullptr, block_size, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0, block_size, name);
    ASSERT(block != MAP_FAILED);
    new (block) HeapBlock(heap, cell_size);
    return NonnullOwnPtr<HeapBlock>(NonnullOwnPtr<HeapBlock>::Adopt, *block);
}

void HeapBlock::operator delete(void* ptr)
{
    int rc = munmap(ptr, block_size);
    ASSERT(rc == 0);
}

HeapBlock::HeapBlock(Heap& heap, size_t cell_size)
    : m_heap(heap)
    , m_cell_size(cell_size)
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
