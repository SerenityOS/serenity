/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <AK/String.h>
#include <LibTest/TestCase.h>

TEST_CASE(allocating_memory_stream_empty)
{
    AllocatingMemoryStream stream;

    EXPECT_EQ(stream.used_buffer_size(), 0ul);

    {
        Array<u8, 32> array;
        auto read_bytes = MUST(stream.read_some(array));
        EXPECT_EQ(read_bytes.size(), 0ul);
    }

    {
        auto offset = MUST(stream.offset_of("test"sv.bytes()));
        EXPECT(!offset.has_value());
    }
}

TEST_CASE(allocating_memory_stream_offset_of)
{
    AllocatingMemoryStream stream;
    MUST(stream.write_until_depleted("Well Hello Friends! :^)"sv.bytes()));

    {
        auto offset = MUST(stream.offset_of(" "sv.bytes()));
        EXPECT(offset.has_value());
        EXPECT_EQ(offset.value(), 4ul);
    }

    {
        auto offset = MUST(stream.offset_of("W"sv.bytes()));
        EXPECT(offset.has_value());
        EXPECT_EQ(offset.value(), 0ul);
    }

    {
        auto offset = MUST(stream.offset_of(")"sv.bytes()));
        EXPECT(offset.has_value());
        EXPECT_EQ(offset.value(), 22ul);
    }

    {
        auto offset = MUST(stream.offset_of("-"sv.bytes()));
        EXPECT(!offset.has_value());
    }

    MUST(stream.discard(1));

    {
        auto offset = MUST(stream.offset_of("W"sv.bytes()));
        EXPECT(!offset.has_value());
    }

    {
        auto offset = MUST(stream.offset_of("e"sv.bytes()));
        EXPECT(offset.has_value());
        EXPECT_EQ(offset.value(), 0ul);
    }
}

TEST_CASE(allocating_memory_stream_offset_of_oob)
{
    AllocatingMemoryStream stream;
    // NOTE: This test is to make sure that offset_of() doesn't read past the end of the "initialized" data.
    //       So we have to assume some things about the behavior of this class:
    //       - A chunk is moved to the end when it's fully read from
    //       - A free chunk is used as-is, no new ones are allocated if one exists.

    // First, fill exactly one chunk (in groups of 16 bytes).
    for (size_t i = 0; i < AllocatingMemoryStream::CHUNK_SIZE / 16; ++i)
        MUST(stream.write_until_depleted("AAAAAAAAAAAAAAAA"sv.bytes()));

    // Then discard it all.
    MUST(stream.discard(AllocatingMemoryStream::CHUNK_SIZE));
    // Now we can write into this chunk again, knowing that it's initialized to all 'A's.
    MUST(stream.write_until_depleted("Well Hello Friends! :^)"sv.bytes()));

    {
        auto offset = MUST(stream.offset_of("A"sv.bytes()));
        EXPECT(!offset.has_value());
    }
}

TEST_CASE(allocating_memory_stream_offset_of_after_chunk_reorder)
{
    AllocatingMemoryStream stream;

    // First, fill exactly one chunk (in groups of 16 bytes). This chunk will be reordered.
    for (size_t i = 0; i < AllocatingMemoryStream::CHUNK_SIZE / 16; ++i)
        MUST(stream.write_until_depleted("AAAAAAAAAAAAAAAA"sv.bytes()));

    // Append a few additional bytes to create a second chunk.
    MUST(stream.write_until_depleted("BCDEFGHIJKLMNOPQ"sv.bytes()));

    // Read back the first chunk, which should reorder it to the end of the list.
    // The chunk that we wrote to the second time is now the first one.
    MUST(stream.discard(AllocatingMemoryStream::CHUNK_SIZE));

    {
        auto offset = MUST(stream.offset_of("A"sv.bytes()));
        EXPECT(!offset.has_value());
    }

    {
        auto offset = MUST(stream.offset_of("B"sv.bytes()));
        EXPECT(offset.has_value());
        EXPECT_EQ(offset.value(), 0ul);
    }

    {
        auto offset = MUST(stream.offset_of("Q"sv.bytes()));
        EXPECT(offset.has_value());
        EXPECT_EQ(offset.value(), 15ul);
    }
}
