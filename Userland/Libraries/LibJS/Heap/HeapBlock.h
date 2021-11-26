/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/Platform.h>
#include <AK/Types.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/Cell.h>

#ifdef HAS_ADDRESS_SANITIZER
#    include <sanitizer/asan_interface.h>
#endif

namespace JS {

class HeapBlock {
    AK_MAKE_NONCOPYABLE(HeapBlock);
    AK_MAKE_NONMOVABLE(HeapBlock);

public:
    static constexpr size_t block_size = 16 * KiB;
    static NonnullOwnPtr<HeapBlock> create_with_cell_size(Heap&, size_t);

    size_t cell_size() const { return m_cell_size; }
    size_t cell_count() const { return (block_size - sizeof(HeapBlock)) / m_cell_size; }
    bool is_full() const { return !has_lazy_freelist() && !m_freelist; }

    ALWAYS_INLINE Cell* allocate()
    {
        Cell* allocated_cell = nullptr;
        if (m_freelist) {
            VERIFY(is_valid_cell_pointer(m_freelist));
            allocated_cell = exchange(m_freelist, m_freelist->next);
        } else if (has_lazy_freelist()) {
            allocated_cell = cell(m_next_lazy_freelist_index++);
        }

        if (allocated_cell) {
            ASAN_UNPOISON_MEMORY_REGION(allocated_cell, m_cell_size);
        }
        return allocated_cell;
    }

    void deallocate(Cell*);

    template<typename Callback>
    void for_each_cell(Callback callback)
    {
        auto end = has_lazy_freelist() ? m_next_lazy_freelist_index : cell_count();
        for (size_t i = 0; i < end; ++i)
            callback(cell(i));
    }

    template<Cell::State state, typename Callback>
    void for_each_cell_in_state(Callback callback)
    {
        for_each_cell([&](auto* cell) {
            if (cell->state() == state)
                callback(cell);
        });
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
        auto end = has_lazy_freelist() ? m_next_lazy_freelist_index : cell_count();
        if (cell_index >= end)
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

    bool has_lazy_freelist() const { return m_next_lazy_freelist_index < cell_count(); }

    struct FreelistEntry final : public Cell {
        FreelistEntry* next { nullptr };

        virtual const char* class_name() const override { return "FreelistEntry"; }
    };

    Cell* cell(size_t index)
    {
        return reinterpret_cast<Cell*>(&m_storage[index * cell_size()]);
    }

    Heap& m_heap;
    size_t m_cell_size { 0 };
    size_t m_next_lazy_freelist_index { 0 };
    FreelistEntry* m_freelist { nullptr };
    alignas(Cell) u8 m_storage[];

public:
    static constexpr size_t min_possible_cell_size = sizeof(FreelistEntry);
};

}
