/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCompress/AsyncDeflate.h>
#include <LibCompress/Deflate.h>

namespace Compress::Async {

namespace {
constexpr size_t max_seekback_distance = 32 * KiB;
constexpr size_t max_back_reference_length = 258;

struct CompressedBlock {
public:
    CompressedBlock(StreamSeekbackBuffer& write_buffer, CanonicalCode const& literal_codes, Optional<CanonicalCode const&> distance_codes)
        : m_write_buffer(write_buffer)
        , m_literal_codes(literal_codes)
        , m_distance_codes(distance_codes)
    {
    }

    struct ReadResult {
        bool read_something { false };
        bool is_eof { false };
    };

    ErrorOr<ReadResult> read_current_chunk(AsyncInputLittleEndianBitStream& stream)
    {
        bool read_at_least_one_symbol = false;
        bool is_eof = false;
        TRY(stream.with_bit_view_of_buffer([&](BufferBitView& bit_view) -> ErrorOr<void> {
            while (true) {
                auto result = TRY(read_symbol(bit_view));
                if (result) {
                    read_at_least_one_symbol = true;
                } else {
                    is_eof = true;
                    return {};
                }
            }
        }));
        return ReadResult { read_at_least_one_symbol, is_eof };
    }

private:
    ErrorOr<u32> decode_length(BufferBitView& bit_view, u32 symbol)
    {
        if (symbol <= 264)
            return symbol - 254;

        if (symbol <= 284) {
            auto extra_bits_count = (symbol - 261) / 4;
            return (((symbol - 265) % 4 + 4) << extra_bits_count) + 3 + TRY(bit_view.read_bits(extra_bits_count));
        }

        if (symbol == 285)
            return max_back_reference_length;

        VERIFY_NOT_REACHED();
    }

    ErrorOr<u32> decode_distance(BufferBitView& bit_view, u32 symbol)
    {
        if (symbol <= 3)
            return symbol + 1;

        if (symbol <= 29) {
            auto extra_bits_count = (symbol / 2) - 1;
            return ((symbol % 2 + 2) << extra_bits_count) + 1 + TRY(bit_view.read_bits(extra_bits_count));
        }

        VERIFY_NOT_REACHED();
    }

    ErrorOr<bool> read_symbol(BufferBitView& bit_view) // True if read bytes, false if read EOF
    {
        return bit_view.rollback_group([&] -> ErrorOr<bool> {
            u32 symbol = TRY(m_literal_codes.read_symbol(bit_view));

            if (symbol >= 286)
                return Error::from_string_literal("Invalid deflate literal/length symbol");

            if (symbol < 256) {
                m_write_buffer.write(static_cast<u8>(symbol));
                return true;
            }

            if (symbol == 256)
                return false;

            if (!m_distance_codes.has_value())
                return Error::from_string_literal("Distance codes have not been initialized in this block");

            auto length = TRY(decode_length(bit_view, symbol));

            u32 distance_symbol = TRY(m_distance_codes->read_symbol(bit_view));
            if (distance_symbol >= 30)
                return Error::from_string_literal("Invalid deflate distance symbol");

            auto distance = TRY(decode_distance(bit_view, distance_symbol));

            if (distance > m_write_buffer.max_seekback_distance())
                return Error::from_string_literal("Provided seekback distance is larger than the amount of data available in seekback buffer");

            m_write_buffer.copy_from_seekback(distance, length);
            return true;
        });
    }

    StreamSeekbackBuffer& m_write_buffer;
    CanonicalCode const& m_literal_codes;
    Optional<CanonicalCode const&> m_distance_codes;
};

struct CodeLengthsDecompressor {
public:
    CodeLengthsDecompressor(size_t length, CanonicalCode const& code_length_code)
        : m_required_length(length)
        , m_code_length_code(code_length_code)
    {
    }

    bool is_done() const
    {
        return m_code_lengths.size() >= m_required_length;
    }

    ErrorOr<void> read_current_chunk(AsyncInputLittleEndianBitStream& stream)
    {
        return stream.with_bit_view_of_buffer([&](BufferBitView& bit_view) -> ErrorOr<void> {
            while (!is_done())
                TRY(read_symbol(bit_view));
            return {};
        });
    }

    Vector<u8, 286>&& take_code_lengths() { return move(m_code_lengths); }

private:
    static constexpr u8 deflate_special_code_length_copy = 16;
    static constexpr u8 deflate_special_code_length_zeros = 17;
    static constexpr u8 deflate_special_code_length_long_zeros = 18;

    ErrorOr<void> read_symbol(BufferBitView& bit_view)
    {
        return bit_view.rollback_group([&] -> ErrorOr<void> {
            auto symbol = TRY(m_code_length_code.read_symbol(bit_view));

            if (symbol < deflate_special_code_length_copy) {
                m_code_lengths.append(static_cast<u8>(symbol));
            } else if (symbol == deflate_special_code_length_copy) {
                if (m_code_lengths.is_empty())
                    return Error::from_string_literal("Found no codes to copy before a copy block");
                auto nrepeat = 3 + TRY(bit_view.read_bits(2));
                for (size_t j = 0; j < nrepeat; ++j)
                    m_code_lengths.append(m_code_lengths.last());
            } else if (symbol == deflate_special_code_length_zeros) {
                auto nrepeat = 3 + TRY(bit_view.read_bits(3));
                for (size_t j = 0; j < nrepeat; ++j)
                    m_code_lengths.append(0);
            } else {
                VERIFY(symbol == deflate_special_code_length_long_zeros);
                auto nrepeat = 11 + TRY(bit_view.read_bits(7));
                for (size_t j = 0; j < nrepeat; ++j)
                    m_code_lengths.append(0);
            }
            return {};
        });
    }

    size_t m_required_length { 0 };
    Vector<u8, 286> m_code_lengths;
    CanonicalCode const& m_code_length_code;
};

Coroutine<ErrorOr<void>> decode_codes(AsyncInputLittleEndianBitStream& stream, CanonicalCode& literal_code, Optional<CanonicalCode>& distance_code)
{
    auto literal_code_count = CO_TRY(co_await stream.read_bits(5)) + 257;
    auto distance_code_count = CO_TRY(co_await stream.read_bits(5)) + 1;
    auto code_length_count = CO_TRY(co_await stream.read_bits(4)) + 4;

    // First we have to extract the code lengths of the code that was used to encode the code lengths of
    // the code that was used to encode the block.

    auto packed_code_lengths_code_lengths = CO_TRY(co_await stream.read_bits(code_length_count * 3));
    u8 code_lengths_code_lengths[19] = { 0 };

    for (size_t i = 0; i < code_length_count; ++i) {
        code_lengths_code_lengths[code_lengths_code_lengths_order[i]] = packed_code_lengths_code_lengths & 7;
        packed_code_lengths_code_lengths >>= 3;
    }

    // Now we can extract the code that was used to encode the code lengths of the code that was used to
    // encode the block.
    auto code_length_code_or_error = CanonicalCode::from_bytes({ code_lengths_code_lengths, sizeof(code_lengths_code_lengths) });
    if (code_length_code_or_error.is_error()) {
        stream.reset();
        co_return code_length_code_or_error.release_error();
    }
    auto const& code_length_code = code_length_code_or_error.value();

    // Next we extract the code lengths of the code that was used to encode the block.
    CodeLengthsDecompressor code_lengths_decompressor { literal_code_count + distance_code_count, code_length_code };
    while (true) {
        CO_TRY(code_lengths_decompressor.read_current_chunk(stream));
        if (code_lengths_decompressor.is_done())
            break;
        CO_TRY(co_await stream.peek_bits());
    }
    Vector<u8, 286> code_lengths = code_lengths_decompressor.take_code_lengths();

    if (code_lengths.size() != literal_code_count + distance_code_count) {
        stream.reset();
        co_return Error::from_string_literal("Number of code lengths does not match the sum of codes");
    }

    // Now we extract the code that was used to encode literals and lengths in the block.
    auto literal_code_or_error = CanonicalCode::from_bytes(code_lengths.span().trim(literal_code_count));
    if (literal_code_or_error.is_error()) {
        stream.reset();
        co_return literal_code_or_error.release_error();
    }
    literal_code = literal_code_or_error.release_value();

    // Now we extract the code that was used to encode distances in the block.
    if (distance_code_count == 1) {
        auto length = code_lengths[literal_code_count];

        if (length == 0) {
            co_return {};
        } else if (length != 1) {
            stream.reset();
            co_return Error::from_string_literal("Length for a single distance code is longer than 1");
        }
    }

    auto distance_code_or_error = CanonicalCode::from_bytes(code_lengths.span().slice(literal_code_count));
    if (distance_code_or_error.is_error()) {
        stream.reset();
        co_return distance_code_or_error.release_error();
    }
    distance_code = distance_code_or_error.release_value();

    co_return {};
}
}

DeflateDecompressor::DeflateDecompressor(NonnullOwnPtr<AsyncInputStream>&& input)
    : AsyncStreamTransform(make<AsyncInputLittleEndianBitStream>(move(input)), decompress())
    , m_buffer(max_seekback_distance, max_back_reference_length)
{
}

ReadonlyBytes DeflateDecompressor::buffered_data_unchecked(Badge<AsyncInputStream>) const
{
    return m_buffer.data();
}

void DeflateDecompressor::dequeue(Badge<AsyncInputStream>, size_t bytes)
{
    return m_buffer.dequeue(bytes);
}

auto DeflateDecompressor::decompress() -> Generator
{
    while (true) {
        bool is_final_block = CO_TRY(co_await m_stream->read_bit());
        auto block_type = CO_TRY(co_await m_stream->read_bits(2));

        if (block_type == 0b00) {
            m_stream->align_to_byte_boundary();

            size_t length = CO_TRY(co_await m_stream->read_object<LittleEndian<u16>>());
            size_t negated_length = CO_TRY(co_await m_stream->read_object<LittleEndian<u16>>());

            if ((length ^ 0xffff) != negated_length) {
                m_stream->reset();
                co_return Error::from_string_literal("Calculated negated length does not equal stored negated length");
            }

            while (length > 0) {
                auto data = CO_TRY(co_await m_stream->peek());
                size_t to_copy = min(data.size(), length);
                m_buffer.write(must_sync(m_stream->read(to_copy)));
                length -= to_copy;
                co_yield {};
            }
        } else if (block_type == 0b01 || block_type == 0b10) {
            CanonicalCode literal_codes;
            Optional<CanonicalCode> distance_codes;
            Optional<CompressedBlock> block;

            if (block_type == 0b01) {
                block = CompressedBlock { m_buffer, CanonicalCode::fixed_literal_codes(), CanonicalCode::fixed_distance_codes() };
            } else {
                CO_TRY(co_await decode_codes(*m_stream, literal_codes, distance_codes));
                if (distance_codes.has_value())
                    block = CompressedBlock { m_buffer, literal_codes, distance_codes.value() };
                else
                    block = CompressedBlock { m_buffer, literal_codes, {} };
            }

            while (true) {
                auto [read_something, is_eof] = CO_TRY(block->read_current_chunk(*m_stream));
                if (read_something)
                    co_yield {};
                if (is_eof)
                    break;
                CO_TRY(co_await m_stream->peek_bits());
            }
        } else {
            m_stream->reset();
            co_return Error::from_string_literal("Invalid block type");
        }

        if (is_final_block) {
            m_stream->align_to_byte_boundary();
            co_return {};
        }
    }
}

}
