/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/NonnullOwnPtr.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Heap/HeapBlock.h>
#include <stdio.h>
#include <sys/mman.h>

namespace JS {

NonnullOwnPtr<HeapBlock> HeapBlock::create_with_cell_size(Heap& heap, size_t cell_size)
{
    char name[64];
    snprintf(name, sizeof(name), "LibJS: HeapBlock(%zu)", cell_size);
    auto* block = static_cast<HeapBlock*>(heap.block_allocator().allocate_block(name));
    new (block) HeapBlock(heap, cell_size);
    return NonnullOwnPtr<HeapBlock>(NonnullOwnPtr<HeapBlock>::Adopt, *block);
}

void HeapBlock::operator delete(void* ptr)
{
#ifdef __serenity__
    int rc = munmap(ptr, block_size);
    VERIFY(rc == 0);
#else
    free(ptr);
#endif
}

HeapBlock::HeapBlock(Heap& heap, size_t cell_size)
    : m_heap(heap)
    , m_cell_size(cell_size)
{
    VERIFY(cell_size >= sizeof(FreelistEntry));
}

void HeapBlock::deallocate(Cell* cell)
{
    VERIFY(is_valid_cell_pointer(cell));
    VERIFY(!m_freelist || is_valid_cell_pointer(m_freelist));
    VERIFY(cell->state() == Cell::State::Live);
    VERIFY(!cell->is_marked());
    cell->~Cell();
    auto* freelist_entry = new (cell) FreelistEntry();
    freelist_entry->set_state(Cell::State::Dead);
    freelist_entry->next = m_freelist;
    m_freelist = freelist_entry;
}

}
