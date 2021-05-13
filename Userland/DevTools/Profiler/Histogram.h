/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>

template<typename Timestamp = u64, typename Value = u16, size_t inline_capacity = 4096>
class Histogram {
public:
    Histogram(Timestamp start, Timestamp end, size_t bucket_count)
        : m_start(start)
        , m_end(end)
    {
        m_buckets.resize(bucket_count);
    }

    void insert(Timestamp const& timestamp, Value value = 1)
    {
        VERIFY(timestamp >= m_start && timestamp <= m_end);
        auto bucket = (timestamp - m_start) * (m_buckets.size() - 1) / (m_end - m_start);
        m_buckets[bucket] += value;
    }

    Value at(size_t index) const
    {
        return m_buckets[index];
    }

    size_t size() const
    {
        return m_buckets.size();
    }

private:
    Timestamp m_start { 0 };
    Timestamp m_end { 0 };
    Vector<Value, inline_capacity> m_buckets;
};
