/*
 * Copyright (c) 2023, Tim Schumacher <timschumi@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BitStream.h>
#include <AK/MemoryStream.h>
#include <LibTest/TestCase.h>

// Note: This does not do any checks on the internal representation, it just ensures that the behavior of the input and output streams match.
TEST_CASE(little_endian_bit_stream_input_output_match)
{
    auto memory_stream = make<AllocatingMemoryStream>();

    // Note: The bit stream only ever reads from/writes to the underlying stream in one byte chunks,
    // so testing with sizes that will not trigger a write will yield unexpected results.
    LittleEndianOutputBitStream bit_write_stream { MaybeOwned<Stream>(*memory_stream) };
    LittleEndianInputBitStream bit_read_stream { MaybeOwned<Stream>(*memory_stream) };

    // Test two mirrored chunks of a fully mirrored pattern to check that we are not dropping bits.
    {
        MUST(bit_write_stream.write_bits(0b1111u, 4));
        MUST(bit_write_stream.write_bits(0b1111u, 4));
        MUST(bit_write_stream.flush_buffer_to_stream());

        auto result = MUST(bit_read_stream.read_bits(4));
        EXPECT_EQ(0b1111u, result);
        result = MUST(bit_read_stream.read_bits(4));
        EXPECT_EQ(0b1111u, result);
    }
    {
        MUST(bit_write_stream.write_bits(0b0000u, 4));
        MUST(bit_write_stream.write_bits(0b0000u, 4));
        MUST(bit_write_stream.flush_buffer_to_stream());

        auto result = MUST(bit_read_stream.read_bits(4));
        EXPECT_EQ(0b0000u, result);
        result = MUST(bit_read_stream.read_bits(4));
        EXPECT_EQ(0b0000u, result);
    }

    // Test two mirrored chunks of a non-mirrored pattern to check that we are writing bits within a pattern in the correct order.
    {
        MUST(bit_write_stream.write_bits(0b1000u, 4));
        MUST(bit_write_stream.write_bits(0b1000u, 4));
        MUST(bit_write_stream.flush_buffer_to_stream());

        auto result = MUST(bit_read_stream.read_bits(4));
        EXPECT_EQ(0b1000u, result);
        result = MUST(bit_read_stream.read_bits(4));
        EXPECT_EQ(0b1000u, result);
    }

    // Test two different chunks to check that we are not confusing their order.
    {
        MUST(bit_write_stream.write_bits(0b1000u, 4));
        MUST(bit_write_stream.write_bits(0b0100u, 4));
        MUST(bit_write_stream.flush_buffer_to_stream());

        auto result = MUST(bit_read_stream.read_bits(4));
        EXPECT_EQ(0b1000u, result);
        result = MUST(bit_read_stream.read_bits(4));
        EXPECT_EQ(0b0100u, result);
    }

    // Test a pattern that spans multiple bytes.
    {
        MUST(bit_write_stream.write_bits(0b1101001000100001u, 16));
        MUST(bit_write_stream.flush_buffer_to_stream());

        auto result = MUST(bit_read_stream.read_bits(16));
        EXPECT_EQ(0b1101001000100001u, result);
    }
}

// Note: This does not do any checks on the internal representation, it just ensures that the behavior of the input and output streams match.
TEST_CASE(big_endian_bit_stream_input_output_match)
{
    auto memory_stream = make<AllocatingMemoryStream>();

    // Note: The bit stream only ever reads from/writes to the underlying stream in one byte chunks,
    // so testing with sizes that will not trigger a write will yield unexpected results.
    BigEndianOutputBitStream bit_write_stream { MaybeOwned<Stream>(*memory_stream) };
    BigEndianInputBitStream bit_read_stream { MaybeOwned<Stream>(*memory_stream) };

    // Test two mirrored chunks of a fully mirrored pattern to check that we are not dropping bits.
    {
        MUST(bit_write_stream.write_bits(0b1111u, 4));
        MUST(bit_write_stream.write_bits(0b1111u, 4));
        auto result = MUST(bit_read_stream.read_bits(4));
        EXPECT_EQ(0b1111u, result);
        result = MUST(bit_read_stream.read_bits(4));
        EXPECT_EQ(0b1111u, result);
    }
    {
        MUST(bit_write_stream.write_bits(0b0000u, 4));
        MUST(bit_write_stream.write_bits(0b0000u, 4));
        auto result = MUST(bit_read_stream.read_bits(4));
        EXPECT_EQ(0b0000u, result);
        result = MUST(bit_read_stream.read_bits(4));
        EXPECT_EQ(0b0000u, result);
    }

    // Test two mirrored chunks of a non-mirrored pattern to check that we are writing bits within a pattern in the correct order.
    {
        MUST(bit_write_stream.write_bits(0b1000u, 4));
        MUST(bit_write_stream.write_bits(0b1000u, 4));
        auto result = MUST(bit_read_stream.read_bits(4));
        EXPECT_EQ(0b1000u, result);
        result = MUST(bit_read_stream.read_bits(4));
        EXPECT_EQ(0b1000u, result);
    }

    // Test two different chunks to check that we are not confusing their order.
    {
        MUST(bit_write_stream.write_bits(0b1000u, 4));
        MUST(bit_write_stream.write_bits(0b0100u, 4));
        auto result = MUST(bit_read_stream.read_bits(4));
        EXPECT_EQ(0b1000u, result);
        result = MUST(bit_read_stream.read_bits(4));
        EXPECT_EQ(0b0100u, result);
    }

    // Test a pattern that spans multiple bytes.
    {
        MUST(bit_write_stream.write_bits(0b1101001000100001u, 16));
        auto result = MUST(bit_read_stream.read_bits(16));
        EXPECT_EQ(0b1101001000100001u, result);
    }
}
