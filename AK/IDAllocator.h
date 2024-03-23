/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/HashTable.h>
#include <AK/Random.h>

namespace AK {

enum class IDAllocatorMode {
    Random,
    Increasing,
};

// This class manages a pool of random ID's in the range N1 (default of 1) to N2 (default of INT32_MAX)
class IDAllocator {

public:
    IDAllocator(int minimum_value = 1, int maximum_value = INT32_MAX, IDAllocatorMode mode = IDAllocatorMode::Random)
        : m_minimum_value(minimum_value)
        , m_maximum_value(maximum_value)
        , m_mode(mode)
    {
    }

    ~IDAllocator() = default;

    int allocate()
    {
        VERIFY(static_cast<int>(m_allocated_ids.size()) < (m_maximum_value - 2));
        int id = 0;
        if (m_mode == IDAllocatorMode::Random) {
            for (;;) {
                id = static_cast<int>(get_random_uniform(m_maximum_value));
                if (id < m_minimum_value)
                    continue;
                if (m_allocated_ids.set(id) == AK::HashSetResult::InsertedNewEntry)
                    break;
            }
        } else if (m_mode == IDAllocatorMode::Increasing) {
            id = m_minimum_value;
            for (;;) {
                if (m_allocated_ids.set(id) == AK::HashSetResult::InsertedNewEntry)
                    break;
                ++id;
            }
        }

        return id;
    }

    void deallocate(int id)
    {
        m_allocated_ids.remove(id);
    }

private:
    HashTable<int> m_allocated_ids;
    int m_minimum_value;
    int m_maximum_value;
    IDAllocatorMode m_mode;
};
}

#if USING_AK_GLOBALLY
using AK::IDAllocator;
#endif
