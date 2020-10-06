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
        m_usable_blocks.append(HeapBlock::create_with_cell_size(heap, m_cell_size));
    }

    auto& block = *m_usable_blocks.last();
    auto* cell = block.allocate();
    ASSERT(cell);
    if (block.is_full()) {
        m_full_blocks.append(m_usable_blocks.take_last());
    }
    return cell;
}

void Allocator::block_did_become_empty(Badge<Heap>, HeapBlock& block)
{
    bool removed_something = false;
    removed_something |= m_full_blocks.remove_first_matching([&block](auto& entry) { return entry == &block; });
    removed_something |= m_usable_blocks.remove_first_matching([&block](auto& entry) { return entry == &block; });
    ASSERT(removed_something);
}

void Allocator::block_did_become_usable(Badge<Heap>, HeapBlock& block)
{
    ASSERT(!block.is_full());
    auto it = m_full_blocks.find([&](auto& entry) {
        return entry == &block;
    });
    ASSERT(it != m_full_blocks.end());
    auto owned_block = m_full_blocks.take(it.index());
    m_usable_blocks.append(move(owned_block));
}

}
