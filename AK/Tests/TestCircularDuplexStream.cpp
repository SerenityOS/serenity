/*
 * Copyright (c) 2020, the SerenityOS developers
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/TestSuite.h>

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
        u8 byte;
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

    u8 bytes[half_capacity];

    stream >> Bytes { bytes, sizeof(bytes) };

    for (size_t idx = 0; idx < half_capacity; ++idx)
        EXPECT_EQ(bytes[idx], idx % 256);

    for (size_t idx = 0; idx < half_capacity; ++idx)
        stream << static_cast<u8>(idx % 256);

    for (size_t idx = 0; idx < capacity; ++idx) {
        u8 byte;
        stream >> byte;

        if (idx < half_capacity)
            EXPECT_EQ(byte, half_capacity + idx % 256);
        else
            EXPECT_EQ(byte, idx % 256 - half_capacity);
    }

    EXPECT(stream.eof());
}

TEST_CASE(of_by_one)
{
    constexpr size_t half_capacity = 32;
    constexpr size_t capacity = half_capacity * 2;

    CircularDuplexStream<capacity> stream;

    for (size_t idx = 0; idx < half_capacity; ++idx)
        stream << static_cast<u8>(0);

    for (size_t idx = 0; idx < half_capacity; ++idx)
        stream << static_cast<u8>(1);

    stream.discard_or_error(capacity);

    for (size_t idx = 0; idx < capacity; ++idx) {
        u8 byte;
        stream.read({ &byte, sizeof(byte) }, capacity);
        stream << byte;

        if (idx < half_capacity)
            EXPECT_EQ(byte, 0);
        else
            EXPECT_EQ(byte, 1);
    }
}

TEST_MAIN(CircularDuplexStream)
