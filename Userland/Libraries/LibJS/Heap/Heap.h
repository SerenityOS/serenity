/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/HashTable.h>
#include <AK/IntrusiveList.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/Forward.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/BlockAllocator.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Heap/CellAllocator.h>
#include <LibJS/Heap/Handle.h>
#include <LibJS/Heap/MarkedVector.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/WeakContainer.h>

namespace JS {

class Heap {
    AK_MAKE_NONCOPYABLE(Heap);
    AK_MAKE_NONMOVABLE(Heap);

public:
    explicit Heap(VM&);
    ~Heap();

    template<typename T, typename... Args>
    T* allocate_without_global_object(Args&&... args)
    {
        auto* memory = allocate_cell(sizeof(T));
        new (memory) T(forward<Args>(args)...);
        return static_cast<T*>(memory);
    }

    template<typename T, typename... Args>
    T* allocate(GlobalObject& global_object, Args&&... args)
    {
        auto* memory = allocate_cell(sizeof(T));
        new (memory) T(forward<Args>(args)...);
        auto* cell = static_cast<T*>(memory);
        cell->initialize(global_object);
        return cell;
    }

    enum class CollectionType {
        CollectGarbage,
        CollectEverything,
    };

    void collect_garbage(CollectionType = CollectionType::CollectGarbage, bool print_report = false);

    VM& vm() { return m_vm; }

    bool should_collect_on_every_allocation() const { return m_should_collect_on_every_allocation; }
    void set_should_collect_on_every_allocation(bool b) { m_should_collect_on_every_allocation = b; }

    void did_create_handle(Badge<HandleImpl>, HandleImpl&);
    void did_destroy_handle(Badge<HandleImpl>, HandleImpl&);

    void did_create_marked_value_list(Badge<MarkedValueList>, MarkedValueList&);
    void did_destroy_marked_value_list(Badge<MarkedValueList>, MarkedValueList&);

    void did_create_marked_vector(Badge<MarkedVectorBase>, MarkedVectorBase&);
    void did_destroy_marked_vector(Badge<MarkedVectorBase>, MarkedVectorBase&);

    void did_create_weak_container(Badge<WeakContainer>, WeakContainer&);
    void did_destroy_weak_container(Badge<WeakContainer>, WeakContainer&);

    void defer_gc(Badge<DeferGC>);
    void undefer_gc(Badge<DeferGC>);

    BlockAllocator& block_allocator() { return m_block_allocator; }

    void uproot_cell(Cell* cell);

private:
    Cell* allocate_cell(size_t);

    void gather_roots(HashTable<Cell*>&);
    void gather_conservative_roots(HashTable<Cell*>&);
    void mark_live_cells(const HashTable<Cell*>& live_cells);
    void sweep_dead_cells(bool print_report, const Core::ElapsedTimer&);

    CellAllocator& allocator_for_size(size_t);

    template<typename Callback>
    void for_each_block(Callback callback)
    {
        for (auto& allocator : m_allocators) {
            if (allocator->for_each_block(callback) == IterationDecision::Break)
                return;
        }
    }

    size_t m_max_allocations_between_gc { 100000 };
    size_t m_allocations_since_last_gc { 0 };

    bool m_should_collect_on_every_allocation { false };

    VM& m_vm;

    Vector<NonnullOwnPtr<CellAllocator>> m_allocators;

    HandleImpl::List m_handles;

    MarkedVectorBase::List m_marked_vectors;
    MarkedValueList::List m_marked_value_lists;

    WeakContainer::List m_weak_containers;

    Vector<Cell*> m_uprooted_cells;

    BlockAllocator m_block_allocator;

    size_t m_gc_deferrals { 0 };
    bool m_should_gc_when_deferral_ends { false };

    bool m_collecting_garbage { false };
};

}
