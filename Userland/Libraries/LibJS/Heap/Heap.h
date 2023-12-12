/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
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
#include <LibJS/Heap/HeapRoot.h>
#include <LibJS/Heap/Internals.h>
#include <LibJS/Heap/MarkedVector.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/ExecutionContext.h>
#include <LibJS/Runtime/WeakContainer.h>

namespace JS {

class Heap : public HeapBase {
    AK_MAKE_NONCOPYABLE(Heap);
    AK_MAKE_NONMOVABLE(Heap);

public:
    explicit Heap(VM&);
    ~Heap();

    template<typename T, typename... Args>
    NonnullGCPtr<T> allocate_without_realm(Args&&... args)
    {
        auto* memory = allocate_cell<T>();
        defer_gc();
        new (memory) T(forward<Args>(args)...);
        undefer_gc();
        return *static_cast<T*>(memory);
    }

    template<typename T, typename... Args>
    NonnullGCPtr<T> allocate(Realm& realm, Args&&... args)
    {
        auto* memory = allocate_cell<T>();
        defer_gc();
        new (memory) T(forward<Args>(args)...);
        undefer_gc();
        auto* cell = static_cast<T*>(memory);
        memory->initialize(realm);
        return *cell;
    }

    enum class CollectionType {
        CollectGarbage,
        CollectEverything,
    };

    void collect_garbage(CollectionType = CollectionType::CollectGarbage, bool print_report = false);
    AK::JsonObject dump_graph();

    bool should_collect_on_every_allocation() const { return m_should_collect_on_every_allocation; }
    void set_should_collect_on_every_allocation(bool b) { m_should_collect_on_every_allocation = b; }

    void did_create_handle(Badge<HandleImpl>, HandleImpl&);
    void did_destroy_handle(Badge<HandleImpl>, HandleImpl&);

    void did_create_marked_vector(Badge<MarkedVectorBase>, MarkedVectorBase&);
    void did_destroy_marked_vector(Badge<MarkedVectorBase>, MarkedVectorBase&);

    void did_create_weak_container(Badge<WeakContainer>, WeakContainer&);
    void did_destroy_weak_container(Badge<WeakContainer>, WeakContainer&);

    void did_create_execution_context(Badge<ExecutionContext>, ExecutionContext&);
    void did_destroy_execution_context(Badge<ExecutionContext>, ExecutionContext&);

    BlockAllocator& block_allocator() { return m_block_allocator; }

    void uproot_cell(Cell* cell);

private:
    friend class MarkingVisitor;
    friend class GraphConstructorVisitor;
    friend class DeferGC;

    void defer_gc();
    void undefer_gc();

    static bool cell_must_survive_garbage_collection(Cell const&);

    template<typename T>
    Cell* allocate_cell()
    {
        will_allocate(sizeof(T));
        if constexpr (requires { T::cell_allocator.allocate_cell(*this); }) {
            if constexpr (IsSame<T, typename decltype(T::cell_allocator)::CellType>) {
                return T::cell_allocator.allocate_cell(*this);
            }
        }
        return allocator_for_size(sizeof(T)).allocate_cell(*this);
    }

    void will_allocate(size_t);

    void find_min_and_max_block_addresses(FlatPtr& min_address, FlatPtr& max_address);
    void gather_roots(HashMap<Cell*, HeapRoot>&);
    void gather_conservative_roots(HashMap<Cell*, HeapRoot>&);
    void gather_asan_fake_stack_roots(HashMap<FlatPtr, HeapRoot>&, FlatPtr, FlatPtr min_block_address, FlatPtr max_block_address);
    void mark_live_cells(HashMap<Cell*, HeapRoot> const& live_cells);
    void finalize_unmarked_cells();
    void sweep_dead_cells(bool print_report, Core::ElapsedTimer const&);

    ALWAYS_INLINE CellAllocator& allocator_for_size(size_t cell_size)
    {
        // FIXME: Use binary search?
        for (auto& allocator : m_allocators) {
            if (allocator->cell_size() >= cell_size)
                return *allocator;
        }
        dbgln("Cannot get CellAllocator for cell size {}, largest available is {}!", cell_size, m_allocators.last()->cell_size());
        VERIFY_NOT_REACHED();
    }

    template<typename Callback>
    void for_each_block(Callback callback)
    {
        for (auto& allocator : m_allocators) {
            if (allocator->for_each_block(callback) == IterationDecision::Break)
                return;
        }
    }

    static constexpr size_t GC_MIN_BYTES_THRESHOLD { 4 * 1024 * 1024 };
    size_t m_gc_bytes_threshold { GC_MIN_BYTES_THRESHOLD };
    size_t m_allocated_bytes_since_last_gc { 0 };

    bool m_should_collect_on_every_allocation { false };

    Vector<NonnullOwnPtr<CellAllocator>> m_allocators;

    HandleImpl::List m_handles;
    MarkedVectorBase::List m_marked_vectors;
    WeakContainer::List m_weak_containers;
    ExecutionContext::List m_execution_contexts;

    Vector<GCPtr<Cell>> m_uprooted_cells;

    BlockAllocator m_block_allocator;

    size_t m_gc_deferrals { 0 };
    bool m_should_gc_when_deferral_ends { false };

    bool m_collecting_garbage { false };
};

inline void Heap::did_create_handle(Badge<HandleImpl>, HandleImpl& impl)
{
    VERIFY(!m_handles.contains(impl));
    m_handles.append(impl);
}

inline void Heap::did_destroy_handle(Badge<HandleImpl>, HandleImpl& impl)
{
    VERIFY(m_handles.contains(impl));
    m_handles.remove(impl);
}

inline void Heap::did_create_marked_vector(Badge<MarkedVectorBase>, MarkedVectorBase& vector)
{
    VERIFY(!m_marked_vectors.contains(vector));
    m_marked_vectors.append(vector);
}

inline void Heap::did_destroy_marked_vector(Badge<MarkedVectorBase>, MarkedVectorBase& vector)
{
    VERIFY(m_marked_vectors.contains(vector));
    m_marked_vectors.remove(vector);
}

inline void Heap::did_create_weak_container(Badge<WeakContainer>, WeakContainer& set)
{
    VERIFY(!m_weak_containers.contains(set));
    m_weak_containers.append(set);
}

inline void Heap::did_destroy_weak_container(Badge<WeakContainer>, WeakContainer& set)
{
    VERIFY(m_weak_containers.contains(set));
    m_weak_containers.remove(set);
}

inline void Heap::did_create_execution_context(Badge<ExecutionContext>, ExecutionContext& set)
{
    VERIFY(!m_execution_contexts.contains(set));
    m_execution_contexts.append(set);
}

inline void Heap::did_destroy_execution_context(Badge<ExecutionContext>, ExecutionContext& set)
{
    VERIFY(m_execution_contexts.contains(set));
    m_execution_contexts.remove(set);
}

}
