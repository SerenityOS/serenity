/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Platform.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Heap/HeapBlock.h>
#include <stdio.h>
#include <sys/mman.h>

#ifdef HAS_ADDRESS_SANITIZER
#    include <sanitizer/asan_interface.h>
#endif

namespace JS {

NonnullOwnPtr<HeapBlock> HeapBlock::create_with_cell_size(Heap& heap, size_t cell_size)
{
#ifdef AK_OS_SERENITY
    char name[64];
    snprintf(name, sizeof(name), "LibJS: HeapBlock(%zu)", cell_size);
#else
    char const* name = nullptr;
#endif
    auto* block = static_cast<HeapBlock*>(heap.block_allocator().allocate_block(name));
    new (block) HeapBlock(heap, cell_size);
    return NonnullOwnPtr<HeapBlock>(NonnullOwnPtr<HeapBlock>::Adopt, *block);
}

HeapBlock::HeapBlock(Heap& heap, size_t cell_size)
    : m_heap(heap)
    , m_cell_size(cell_size)
{
    VERIFY(cell_size >= sizeof(FreelistEntry));
    ASAN_POISON_MEMORY_REGION(m_storage, block_size - sizeof(HeapBlock));
}

struct Tombstone final : public Cell {
    JS_CELL(Tombstone, Cell);
};

void HeapBlock::deallocate(Cell* cell)
{
    VERIFY(is_valid_cell_pointer(cell));
    VERIFY(!m_freelist || is_valid_cell_pointer(m_freelist));
    VERIFY(cell->state() == Cell::State::Live);
    VERIFY(!cell->is_marked());

    cell->~Cell();
    auto* tombstone = new (cell) Tombstone;
    tombstone->set_state(Cell::State::Dead);
}

}
