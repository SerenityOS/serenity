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

TEST_CASE(allocating_memory_stream_offset_of_with_write_offset_multiple_of_chunk_size)
{
    // This tests a specific edge case where we would erroneously trim the last searched block
    // to size 0 if the current write offset is a multiple of the chunk size.

    AllocatingMemoryStream stream;

    // First, fill exactly one chunk (in groups of 16 bytes).
    for (size_t i = 0; i < (AllocatingMemoryStream::CHUNK_SIZE / 16) - 1; ++i)
        MUST(stream.write_until_depleted("AAAAAAAAAAAAAAAA"sv.bytes()));
    MUST(stream.write_until_depleted("BCDEFGHIJKLMNOPQ"sv.bytes()));

    // Read a few bytes from the beginning to ensure that we are trying to slice into the zero-sized block.
    MUST(stream.discard(32));

    {
        auto offset = MUST(stream.offset_of("B"sv.bytes()));
        EXPECT(offset.has_value());
        EXPECT_EQ(offset.value(), AllocatingMemoryStream::CHUNK_SIZE - 32 - 16);
    }

    {
        auto offset = MUST(stream.offset_of("Q"sv.bytes()));
        EXPECT(offset.has_value());
        EXPECT_EQ(offset.value(), AllocatingMemoryStream::CHUNK_SIZE - 32 - 1);
    }
}

TEST_CASE(fixed_memory_read_write)
{
    constexpr auto some_words = "These are some words"sv;

    auto empty = TRY_OR_FAIL(ByteBuffer::create_uninitialized(some_words.length()));
    FixedMemoryStream stream { empty.bytes() };

    ReadonlyBytes buffer { some_words.characters_without_null_termination(), some_words.length() };
    TRY_OR_FAIL(stream.write_some(buffer));

    EXPECT_EQ(TRY_OR_FAIL(stream.tell()), some_words.length());
    EXPECT(stream.is_eof());

    TRY_OR_FAIL(stream.seek(0));
    auto contents = TRY_OR_FAIL(stream.read_until_eof());
    EXPECT_EQ(contents.bytes(), some_words.bytes());
}

TEST_CASE(fixed_memory_close)
{
    auto empty = TRY_OR_FAIL(ByteBuffer::create_uninitialized(64));
    FixedMemoryStream stream { empty.bytes() };
    EXPECT(stream.is_open());
    stream.close();
    EXPECT(stream.is_open());
}

TEST_CASE(fixed_memory_read_only)
{
    constexpr auto some_words = "These are some words"sv;

    FixedMemoryStream stream { ReadonlyBytes { some_words.bytes() } };

    auto contents = TRY_OR_FAIL(stream.read_until_eof());
    EXPECT_EQ(contents.bytes(), some_words.bytes());

    TRY_OR_FAIL(stream.seek(0));
    ReadonlyBytes buffer { some_words.characters_without_null_termination(), some_words.length() };
    EXPECT_CRASH("Write protection assert", [&] {
        (void)stream.write_some(buffer);
        return Test::Crash::Failure::DidNotCrash;
    });

    EXPECT_EQ(TRY_OR_FAIL(stream.tell()), 0ull);
    EXPECT(!stream.is_eof());
}

TEST_CASE(fixed_memory_seeking_around)
{
    auto stream_buffer = TRY_OR_FAIL(ByteBuffer::create_uninitialized(8702ul));
    FixedMemoryStream stream { ReadonlyBytes { stream_buffer.bytes() } };

    auto buffer = TRY_OR_FAIL(ByteBuffer::create_uninitialized(16));

    TRY_OR_FAIL(stream.seek(500, SeekMode::SetPosition));
    EXPECT_EQ(stream.tell().release_value(), 500ul);
    TRY_OR_FAIL(stream.read_until_filled(buffer));

    TRY_OR_FAIL(stream.seek(234, SeekMode::FromCurrentPosition));
    EXPECT_EQ(stream.tell().release_value(), 750ul);
    TRY_OR_FAIL(stream.read_until_filled(buffer));

    TRY_OR_FAIL(stream.seek(-105, SeekMode::FromEndPosition));
    EXPECT_EQ(stream.tell().release_value(), 8597ul);
    TRY_OR_FAIL(stream.read_until_filled(buffer));
}

BENCHMARK_CASE(fixed_memory_tell)
{
    auto stream_buffer = TRY_OR_FAIL(ByteBuffer::create_uninitialized(10 * KiB));
    FixedMemoryStream stream { ReadonlyBytes { stream_buffer.bytes() } };

    auto expected_fixed_memory_offset = 0u;
    auto ten_byte_buffer = TRY_OR_FAIL(ByteBuffer::create_uninitialized(1));
    for (auto i = 0u; i < 4000; ++i) {
        TRY_OR_FAIL(stream.read_until_filled(ten_byte_buffer));
        expected_fixed_memory_offset += 1u;
        EXPECT_EQ(expected_fixed_memory_offset, TRY_OR_FAIL(stream.tell()));
    }

    for (auto i = 0u; i < 4000; ++i) {
        auto seek_fixed_memory_offset = TRY_OR_FAIL(stream.seek(-1, SeekMode::FromCurrentPosition));
        expected_fixed_memory_offset -= 1;
        EXPECT_EQ(seek_fixed_memory_offset, TRY_OR_FAIL(stream.tell()));
        EXPECT_EQ(expected_fixed_memory_offset, TRY_OR_FAIL(stream.tell()));
    }
}

TEST_CASE(fixed_memory_truncate)
{
    auto stream_buffer = TRY_OR_FAIL(ByteBuffer::create_uninitialized(10 * KiB));
    FixedMemoryStream stream { ReadonlyBytes { stream_buffer.bytes() } };

    EXPECT(stream.truncate(999).is_error());
}
