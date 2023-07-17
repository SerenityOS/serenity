/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PieceHeap.h"
#include "Peer.h"
#include "PeerSession.h"
#include "Torrent.h"

namespace BitTorrent {

PieceStatus::PieceStatus(u64 index_in_torrent)
    : index_in_torrent(index_in_torrent)
{
}

size_t PieceStatus::key() const
{
    return havers.size();
}

void PieceHeap::insert(PieceHeap::Value value)
{
    auto index = m_size++;
    if (m_elements.size() < m_size)
        m_elements.empend(value);
    else
        m_elements[m_size - 1] = value;

    value->index_in_heap = index;
    heapify_up(index);
}

PieceHeap::Value PieceHeap::pop_min()
{
    VERIFY(!is_empty());
    auto index = --m_size;
    swap(m_elements[0], m_elements[index]);
    heapify_down(0);
    m_elements[index]->index_in_heap.clear();
    return m_elements[index];
}

PieceHeap::Value PieceHeap::peek_min() const
{
    VERIFY(!is_empty());
    return m_elements[0];
}
void PieceHeap::update(PieceHeap::Value value)
{
    auto index = value->index_in_heap.value();
    auto& element = m_elements[index];
    VERIFY(value == element);

    auto parent = (index - 1) / 2;
    if (index > 0 && value->key() < m_elements[parent]->key())
        heapify_up(index);
    else
        heapify_down(index);
}
void PieceHeap::swap(PieceHeap::Value& a, PieceHeap::Value& b)
{
    AK::swap(a, b);
    AK::swap(a->index_in_heap, b->index_in_heap);
}
void PieceHeap::heapify_down(size_t index)
{
    while (index * 2 + 1 < m_size) {
        auto left_child = index * 2 + 1;
        auto right_child = index * 2 + 2;

        auto min_child = left_child;
        if (right_child < m_size && m_elements[right_child]->key() < m_elements[min_child]->key())
            min_child = right_child;

        if (m_elements[index]->key() <= m_elements[min_child]->key())
            break;
        swap(m_elements[index], m_elements[min_child]);
        index = min_child;
    }
}
void PieceHeap::heapify_up(size_t index)
{
    while (index != 0) {
        auto parent = (index - 1) / 2;

        if (m_elements[index]->key() >= m_elements[parent]->key())
            break;
        swap(m_elements[index], m_elements[parent]);
        index = parent;
    }
}

}
