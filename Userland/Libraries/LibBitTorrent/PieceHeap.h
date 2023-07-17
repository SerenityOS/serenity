/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashTable.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/RefCounted.h>
#include <AK/Types.h>
#include <AK/Vector.h>

namespace BitTorrent {

struct PeerSession;

struct PieceStatus : public RefCounted<PieceStatus> {
    PieceStatus(u64 index_in_torrent);
    Optional<size_t> index_in_heap = {};
    u64 index_in_torrent;
    size_t key() const;
    HashTable<NonnullRefPtr<BitTorrent::PeerSession>> havers;
    bool currently_downloading { false };
};

// Based on AK::BinaryHeap
// Poor man's version of an intrusive binary heap/priority queue
class PieceHeap {
    using Value = NonnullRefPtr<PieceStatus>;

public:
    [[nodiscard]] size_t size() const { return m_size; }
    [[nodiscard]] bool is_empty() const { return m_size == 0; }
    void insert(Value value);
    Value pop_min();
    [[nodiscard]] Value peek_min() const;
    void clear() { m_size = 0; }
    void update(Value value);

private:
    void swap(Value& a, Value& b);
    void heapify_down(size_t index);
    void heapify_up(size_t index);
    Vector<Value> m_elements;
    size_t m_size { 0 };
};

}
