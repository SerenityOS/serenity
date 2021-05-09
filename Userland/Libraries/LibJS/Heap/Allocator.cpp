/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <LibJS/Heap/Allocator.h>
#include <LibJS/Heap/HeapBlock.h>

namespace JS {

Allocator::Allocator(size_t cell_size)
    : m_cell_size(cell_size)
{
}

Allocator::~Allocator()
{
}

Cell* Allocator::allocate_cell(Heap& heap)
{
    if (m_usable_blocks.is_empty()) {
        auto block = HeapBlock::create_with_cell_size(heap, m_cell_size);
        m_usable_blocks.append(*block.leak_ptr());
    }

    auto& block = *m_usable_blocks.last();
    auto* cell = block.allocate();
    VERIFY(cell);
    if (block.is_full())
        m_full_blocks.append(*m_usable_blocks.last());
    return cell;
}

void Allocator::block_did_become_empty(Badge<Heap>, HeapBlock& block)
{
    block.m_list_node.remove();
    delete &block;
}

void Allocator::block_did_become_usable(Badge<Heap>, HeapBlock& block)
{
    VERIFY(!block.is_full());
    m_usable_blocks.append(block);
}

}
