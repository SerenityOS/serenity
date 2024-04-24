/*
 * Copyright (c) 2020, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/ByteString.h>
#include <AK/QuickSort.h>
#include <AK/Random.h>
#include <AK/Vector.h>
#include <stdlib.h>

size_t const NUM_RUNS = 10;

struct SortableObject {
    int m_key;
    int m_payload;
};

static int compare_sortable_object(void const* a, void const* b)
{
    int const key1 = static_cast<SortableObject const*>(a)->m_key;
    int const key2 = static_cast<SortableObject const*>(b)->m_key;
    if (key1 < key2) {
        return -1;
    } else if (key1 == key2) {
        return 0;
    } else {
        return 1;
    }
}

static int calc_payload_for_pos(size_t pos)
{
    pos *= 231;
    return pos ^ (pos << 8) ^ (pos << 16) ^ (pos << 24);
}

TEST_CASE(quick_sort)
{
    // Generate vector of SortableObjects in sorted order, with payloads determined by their sorted positions
    Vector<SortableObject> test_objects;
    for (auto i = 0; i < 1024; ++i) {
        test_objects.append({ i * 137, calc_payload_for_pos(i) });
    }
    for (size_t i = 0; i < NUM_RUNS; i++) {
        // Shuffle the vector, then sort it again
        shuffle(test_objects);
        qsort(test_objects.data(), test_objects.size(), sizeof(SortableObject), compare_sortable_object);
        // Check that the objects are sorted by key
        for (auto i = 0u; i + 1 < test_objects.size(); ++i) {
            auto const& key1 = test_objects[i].m_key;
            auto const& key2 = test_objects[i + 1].m_key;
            if (key1 > key2) {
                FAIL(ByteString::formatted("saw key {} before key {}\n", key1, key2));
            }
        }
        // Check that the object's payloads have not been corrupted
        for (auto i = 0u; i < test_objects.size(); ++i) {
            auto const expected = calc_payload_for_pos(i);
            auto const payload = test_objects[i].m_payload;
            if (payload != expected) {
                FAIL(ByteString::formatted("Expected payload {} for pos {}, got payload {}", expected, i, payload));
            }
        }
    }
}
