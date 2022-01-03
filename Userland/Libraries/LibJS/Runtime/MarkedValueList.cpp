/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/MarkedValueList.h>

namespace JS {

MarkedValueList::MarkedValueList(Heap& heap)
    : m_heap(&heap)
{
    m_heap->did_create_marked_value_list({}, *this);
}

MarkedValueList::MarkedValueList(MarkedValueList const& other)
    : Vector<Value, 32>(other)
    , m_heap(other.m_heap)
{
    m_heap->did_create_marked_value_list({}, *this);
}

MarkedValueList::MarkedValueList(MarkedValueList&& other)
    : Vector<Value, 32>(move(static_cast<Vector<Value, 32>&>(other)))
    , m_heap(other.m_heap)
{
    m_heap->did_create_marked_value_list({}, *this);
}

MarkedValueList::~MarkedValueList()
{
    m_heap->did_destroy_marked_value_list({}, *this);
}

MarkedValueList& MarkedValueList::operator=(JS::MarkedValueList const& other)
{
    Vector<Value, 32>::operator=(other);

    if (m_heap != other.m_heap) {
        m_heap = other.m_heap;

        // NOTE: IntrusiveList will remove this MarkedValueList from the old heap it was part of.
        m_heap->did_create_marked_value_list({}, *this);
    }

    return *this;
}

}
