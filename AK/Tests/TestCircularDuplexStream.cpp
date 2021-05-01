/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/CircularDuplexStream.h>

TEST_CASE(works_like_a_queue)
{
    constexpr size_t capacity = 32;

    CircularQueue<u8, capacity> queue;
    CircularDuplexStream<capacity> stream;

    for (size_t idx = 0; idx < capacity; ++idx) {
        queue.enqueue(static_cast<u8>(idx % 256));
        stream << static_cast<u8>(idx % 256);
    }

    for (size_t idx = 0; idx < capacity; ++idx) {
        u8 byte = 0;
        stream >> byte;

        EXPECT_EQ(queue.dequeue(), byte);
    }

    EXPECT(stream.eof());
}

TEST_CASE(overwritting_is_well_defined)
{
    constexpr size_t half_capacity = 16;
    constexpr size_t capacity = 2 * half_capacity;

    CircularDuplexStream<capacity> stream;

    for (size_t idx = 0; idx < capacity; ++idx)
        stream << static_cast<u8>(idx % 256);

    Array<u8, half_capacity> buffer;
    stream >> buffer;

    for (size_t idx = 0; idx < half_capacity; ++idx)
        EXPECT_EQ(buffer[idx], idx % 256);

    for (size_t idx = 0; idx < half_capacity; ++idx)
        stream << static_cast<u8>(idx % 256);

    for (size_t idx = 0; idx < capacity; ++idx) {
        u8 byte = 0;
        stream >> byte;

        if (idx < half_capacity)
            EXPECT_EQ(byte, half_capacity + idx % 256);
        else
            EXPECT_EQ(byte, idx % 256 - half_capacity);
    }

    EXPECT(stream.eof());
}
