/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/Types.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/Cell.h>

namespace JS {

class HeapBlock {
    AK_MAKE_NONCOPYABLE(HeapBlock);
    AK_MAKE_NONMOVABLE(HeapBlock);

public:
    static constexpr size_t block_size = 16 * KiB;
    static NonnullOwnPtr<HeapBlock> create_with_cell_size(Heap&, size_t);

    void operator delete(void*);

    size_t cell_size() const { return m_cell_size; }
    size_t cell_count() const { return (block_size - sizeof(HeapBlock)) / m_cell_size; }
    bool is_full() const { return !m_freelist; }

    ALWAYS_INLINE Cell* allocate()
    {
        if (!m_freelist)
            return nullptr;
        VERIFY(is_valid_cell_pointer(m_freelist));
        return exchange(m_freelist, m_freelist->next);
    }

    void deallocate(Cell*);

    template<typename Callback>
    void for_each_cell(Callback callback)
    {
        for (size_t i = 0; i < cell_count(); ++i)
            callback(cell(i));
    }

    Heap& heap() { return m_heap; }

    static HeapBlock* from_cell(const Cell* cell)
    {
        return reinterpret_cast<HeapBlock*>((FlatPtr)cell & ~(block_size - 1));
    }

    Cell* cell_from_possible_pointer(FlatPtr pointer)
    {
        if (pointer < reinterpret_cast<FlatPtr>(m_storage))
            return nullptr;
        size_t cell_index = (pointer - reinterpret_cast<FlatPtr>(m_storage)) / m_cell_size;
        if (cell_index >= cell_count())
            return nullptr;
        return cell(cell_index);
    }

    bool is_valid_cell_pointer(const Cell* cell)
    {
        return cell_from_possible_pointer((FlatPtr)cell);
    }

    IntrusiveListNode<HeapBlock> m_list_node;

private:
    HeapBlock(Heap&, size_t cell_size);

    struct FreelistEntry final : public Cell {
        FreelistEntry* next { nullptr };

        virtual const char* class_name() const override { return "FreelistEntry"; }
    };

    Cell* cell(size_t index)
    {
        return reinterpret_cast<Cell*>(&m_storage[index * cell_size()]);
    }

    FreelistEntry* init_freelist_entry(size_t index)
    {
        return new (&m_storage[index * cell_size()]) FreelistEntry();
    }

    Heap& m_heap;
    size_t m_cell_size { 0 };
    FreelistEntry* m_freelist { nullptr };
    alignas(Cell) u8 m_storage[];
};

}
