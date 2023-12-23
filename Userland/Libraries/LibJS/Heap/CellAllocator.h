/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/NonnullOwnPtr.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/HeapBlock.h>

#define JS_DECLARE_ALLOCATOR(ClassName) \
    static JS::TypeIsolatingCellAllocator<ClassName> cell_allocator;

#define JS_DEFINE_ALLOCATOR(ClassName) \
    JS::TypeIsolatingCellAllocator<ClassName> ClassName::cell_allocator;

namespace JS {

class CellAllocator {
public:
    explicit CellAllocator(size_t cell_size);
    ~CellAllocator() = default;

    size_t cell_size() const { return m_cell_size; }

    Cell* allocate_cell(Heap&);

    template<typename Callback>
    IterationDecision for_each_block(Callback callback)
    {
        for (auto& block : m_full_blocks) {
            if (callback(block) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        for (auto& block : m_usable_blocks) {
            if (callback(block) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    void block_did_become_empty(Badge<Heap>, HeapBlock&);
    void block_did_become_usable(Badge<Heap>, HeapBlock&);

    IntrusiveListNode<CellAllocator> m_list_node;
    using List = IntrusiveList<&CellAllocator::m_list_node>;

private:
    size_t const m_cell_size;

    using BlockList = IntrusiveList<&HeapBlock::m_list_node>;
    BlockList m_full_blocks;
    BlockList m_usable_blocks;
};

template<typename T>
class TypeIsolatingCellAllocator {
public:
    using CellType = T;

    CellAllocator allocator { sizeof(T) };
};

}
