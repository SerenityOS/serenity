/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/Noncopyable.h>
#include <AK/Vector.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/Cell.h>

namespace JS {

class MarkedVectorBase {
    AK_MAKE_NONCOPYABLE(MarkedVectorBase);

public:
    void append(Cell* cell) { m_cells.append(cell); }
    void prepend(Cell* cell) { m_cells.prepend(cell); }
    size_t size() const { return m_cells.size(); }

    Span<Cell*> cells() { return m_cells.span(); }

protected:
    explicit MarkedVectorBase(Heap&);

    MarkedVectorBase(MarkedVectorBase&&);
    MarkedVectorBase& operator=(MarkedVectorBase&& other)
    {
        m_cells = move(other.m_cells);
        return *this;
    }

    ~MarkedVectorBase();

    Heap& m_heap;

    IntrusiveListNode<MarkedVectorBase> m_list_node;
    Vector<Cell*> m_cells;

public:
    using List = IntrusiveList<&MarkedVectorBase::m_list_node>;
};

template<typename T>
class MarkedVector : public MarkedVectorBase {
public:
    explicit MarkedVector(Heap& heap)
        : MarkedVectorBase(heap)
    {
    }

    ~MarkedVector() = default;

    MarkedVector(MarkedVector&&) = default;
    MarkedVector& operator=(MarkedVector&&) = default;

    Span<T*> span() { return Span<T*> { bit_cast<T**>(m_cells.data()), m_cells.size() }; }

    auto begin() { return span().begin(); }
    auto begin() const { return span().begin(); }

    auto end() { return span().end(); }
    auto end() const { return span().end(); }
};

}
