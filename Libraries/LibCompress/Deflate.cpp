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

#include <AK/Array.h>
#include <AK/Assertions.h>
#include <AK/BinarySearch.h>
#include <AK/LogStream.h>
#include <AK/MemoryStream.h>

#include <LibCompress/Deflate.h>

namespace Compress {

const CanonicalCode& CanonicalCode::fixed_literal_codes()
{
    static CanonicalCode code;
    static bool initialized = false;

    if (initialized)
        return code;

    Array<u8, 288> data;
    data.span().slice(0, 144 - 0).fill(8);
    data.span().slice(144, 256 - 144).fill(9);
    data.span().slice(256, 280 - 256).fill(7);
    data.span().slice(280, 288 - 280).fill(8);

    code = CanonicalCode::from_bytes(data).value();
    initialized = true;

    return code;
}

const CanonicalCode& CanonicalCode::fixed_distance_codes()
{
    static CanonicalCode code;
    static bool initialized = false;

    if (initialized)
        return code;

    Array<u8, 32> data;
    data.span().fill(5);

    code = CanonicalCode::from_bytes(data).value();
    initialized = true;

    return code;
}

Optional<CanonicalCode> CanonicalCode::from_bytes(ReadonlyBytes bytes)
{
    // FIXME: I can't quite follow the algorithm here, but it seems to work.

    CanonicalCode code;

    auto next_code = 0;
    for (size_t code_length = 1; code_length <= 15; ++code_length) {
        next_code <<= 1;
        auto start_bit = 1 << code_length;

        for (size_t symbol = 0; symbol < bytes.size(); ++symbol) {
            if (bytes[symbol] != code_length)
                continue;

            if (next_code > start_bit)
                return {};

            code.m_symbol_codes.append(start_bit | next_code);
            code.m_symbol_values.append(symbol);

            next_code++;
        }
    }

    if (next_code != (1 << 15)) {
        return {};
    }

    return code;
}

u32 CanonicalCode::read_symbol(InputBitStream& stream) const
{
    u32 code_bits = 1;

    for (;;) {
        code_bits = code_bits << 1 | stream.read_bits(1);

        // FIXME: This seems really inefficient, this could be an index into an array instead.
        size_t index;
        if (AK::binary_search(m_symbol_codes.span(), code_bits, AK::integral_compare<u32>, &index))
            return m_symbol_values[index];
    }
}

DeflateDecompressor::CompressedBlock::CompressedBlock(DeflateDecompressor& decompressor, CanonicalCode literal_codes, Optional<CanonicalCode> distance_codes)
    : m_decompressor(decompressor)
    , m_literal_codes(literal_codes)
    , m_distance_codes(distance_codes)
{
}

bool DeflateDecompressor::CompressedBlock::try_read_more()
{
    if (m_eof == true)
        return false;

    const auto symbol = m_literal_codes.read_symbol(m_decompressor.m_input_stream);

    if (symbol < 256) {
        m_decompressor.m_output_stream << static_cast<u8>(symbol);
        return true;
    } else if (symbol == 256) {
        m_eof = true;
        return false;
    } else {
        if (!m_distance_codes.has_value()) {
            m_decompressor.set_fatal_error();
            return false;
        }

        const auto length = m_decompressor.decode_length(symbol);
        const auto distance = m_decompressor.decode_distance(m_distance_codes.value().read_symbol(m_decompressor.m_input_stream));

        for (size_t idx = 0; idx < length; ++idx) {
            u8 byte = 0;
            m_decompressor.m_output_stream.read({ &byte, sizeof(byte) }, distance);
            m_decompressor.m_output_stream << byte;
        }

        return true;
    }
}

DeflateDecompressor::UncompressedBlock::UncompressedBlock(DeflateDecompressor& decompressor, size_t length)
    : m_decompressor(decompressor)
    , m_bytes_remaining(length)
{
}

bool DeflateDecompressor::UncompressedBlock::try_read_more()
{
    if (m_bytes_remaining == 0)
        return false;

    const auto nread = min(m_bytes_remaining, m_decompressor.m_output_stream.remaining_contigous_space());
    m_bytes_remaining -= nread;

    m_decompressor.m_input_stream >> m_decompressor.m_output_stream.reserve_contigous_space(nread);

    return true;
}

DeflateDecompressor::DeflateDecompressor(InputStream& stream)
    : m_input_stream(stream)
{
}

DeflateDecompressor::~DeflateDecompressor()
{
    if (m_state == State::ReadingCompressedBlock)
        m_compressed_block.~CompressedBlock();
    if (m_state == State::ReadingUncompressedBlock)
        m_uncompressed_block.~UncompressedBlock();
}

size_t DeflateDecompressor::read(Bytes bytes)
{
    if (has_any_error())
        return 0;

    if (m_state == State::Idle) {
        if (m_read_final_bock)
            return 0;

        m_read_final_bock = m_input_stream.read_bit();
        const auto block_type = m_input_stream.read_bits(2);

        if (block_type == 0b00) {
            m_input_stream.align_to_byte_boundary();

            LittleEndian<u16> length, negated_length;
            m_input_stream >> length >> negated_length;

            if ((length ^ 0xffff) != negated_length) {
                set_fatal_error();
                return 0;
            }

            m_state = State::ReadingUncompressedBlock;
            new (&m_uncompressed_block) UncompressedBlock(*this, length);

            return read(bytes);
        }

        if (block_type == 0b01) {
            m_state = State::ReadingCompressedBlock;
            new (&m_compressed_block) CompressedBlock(*this, CanonicalCode::fixed_literal_codes(), CanonicalCode::fixed_distance_codes());

            return read(bytes);
        }

        if (block_type == 0b10) {
            CanonicalCode literal_codes;
            Optional<CanonicalCode> distance_codes;
            decode_codes(literal_codes, distance_codes);

            m_state = State::ReadingCompressedBlock;
            new (&m_compressed_block) CompressedBlock(*this, literal_codes, distance_codes);

            return read(bytes);
        }

        set_fatal_error();
        return 0;
    }

    if (m_state == State::ReadingCompressedBlock) {
        auto nread = m_output_stream.read(bytes);

        while (nread < bytes.size() && m_compressed_block.try_read_more()) {
            nread += m_output_stream.read(bytes.slice(nread));
        }

        if (nread == bytes.size())
            return nread;

        m_compressed_block.~CompressedBlock();
        m_state = State::Idle;

        return nread + read(bytes.slice(nread));
    }

    if (m_state == State::ReadingUncompressedBlock) {
        auto nread = m_output_stream.read(bytes);

        while (nread < bytes.size() && m_uncompressed_block.try_read_more()) {
            nread += m_output_stream.read(bytes.slice(nread));
        }

        if (nread == bytes.size())
            return nread;

        m_uncompressed_block.~UncompressedBlock();
        m_state = State::Idle;

        return nread + read(bytes.slice(nread));
    }

    ASSERT_NOT_REACHED();
}

bool DeflateDecompressor::read_or_error(Bytes bytes)
{
    if (read(bytes) < bytes.size()) {
        set_fatal_error();
        return false;
    }

    return true;
}

bool DeflateDecompressor::discard_or_error(size_t count)
{
    u8 buffer[4096];

    size_t ndiscarded = 0;
    while (ndiscarded < count) {
        if (unreliable_eof()) {
            set_fatal_error();
            return false;
        }

        ndiscarded += read({ buffer, min<size_t>(count - ndiscarded, 4096) });
    }

    return true;
}

bool DeflateDecompressor::unreliable_eof() const { return m_state == State::Idle && m_read_final_bock; }

Optional<ByteBuffer> DeflateDecompressor::decompress_all(ReadonlyBytes bytes)
{
    InputMemoryStream memory_stream { bytes };
    DeflateDecompressor deflate_stream { memory_stream };
    DuplexMemoryStream output_stream;

    u8 buffer[4096];
    while (!deflate_stream.has_any_error() && !deflate_stream.unreliable_eof()) {
        const auto nread = deflate_stream.read({ buffer, sizeof(buffer) });
        output_stream.write_or_error({ buffer, nread });
    }

    if (deflate_stream.handle_any_error())
        return {};

    return output_stream.copy_into_contiguous_buffer();
}

u32 DeflateDecompressor::decode_length(u32 symbol)
{
    // FIXME: I can't quite follow the algorithm here, but it seems to work.

    if (symbol <= 264)
        return symbol - 254;

    if (symbol <= 284) {
        auto extra_bits = (symbol - 261) / 4;
        return (((symbol - 265) % 4 + 4) << extra_bits) + 3 + m_input_stream.read_bits(extra_bits);
    }

    if (symbol == 285)
        return 258;

    ASSERT_NOT_REACHED();
}

u32 DeflateDecompressor::decode_distance(u32 symbol)
{
    // FIXME: I can't quite follow the algorithm here, but it seems to work.

    if (symbol <= 3)
        return symbol + 1;

    if (symbol <= 29) {
        auto extra_bits = (symbol / 2) - 1;
        return ((symbol % 2 + 2) << extra_bits) + 1 + m_input_stream.read_bits(extra_bits);
    }

    ASSERT_NOT_REACHED();
}

void DeflateDecompressor::decode_codes(CanonicalCode& literal_code, Optional<CanonicalCode>& distance_code)
{
    auto literal_code_count = m_input_stream.read_bits(5) + 257;
    auto distance_code_count = m_input_stream.read_bits(5) + 1;
    auto code_length_count = m_input_stream.read_bits(4) + 4;

    // First we have to extract the code lengths of the code that was used to encode the code lengths of
    // the code that was used to encode the block.

    u8 code_lengths_code_lengths[19] = { 0 };
    for (size_t i = 0; i < code_length_count; ++i) {
        static const size_t indices[] { 16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15 };
        code_lengths_code_lengths[indices[i]] = m_input_stream.read_bits(3);
    }

    // Now we can extract the code that was used to encode the code lengths of the code that was used to
    // encode the block.

    auto code_length_code_result = CanonicalCode::from_bytes({ code_lengths_code_lengths, sizeof(code_lengths_code_lengths) });
    if (!code_length_code_result.has_value()) {
        set_fatal_error();
        return;
    }
    const auto code_length_code = code_length_code_result.value();

    // Next we extract the code lengths of the code that was used to encode the block.

    Vector<u8> code_lengths;
    while (code_lengths.size() < literal_code_count + distance_code_count) {
        auto symbol = code_length_code.read_symbol(m_input_stream);

        if (symbol <= 15) {
            code_lengths.append(static_cast<u8>(symbol));
            continue;
        } else if (symbol == 17) {
            auto nrepeat = 3 + m_input_stream.read_bits(3);
            for (size_t j = 0; j < nrepeat; ++j)
                code_lengths.append(0);
            continue;
        } else if (symbol == 18) {
            auto nrepeat = 11 + m_input_stream.read_bits(7);
            for (size_t j = 0; j < nrepeat; ++j)
                code_lengths.append(0);
            continue;
        } else {
            ASSERT(symbol == 16);

            if (code_lengths.is_empty()) {
                set_fatal_error();
                return;
            }

            auto nrepeat = 3 + m_input_stream.read_bits(2);
            for (size_t j = 0; j < nrepeat; ++j)
                code_lengths.append(code_lengths.last());
        }
    }

    if (code_lengths.size() != literal_code_count + distance_code_count) {
        set_fatal_error();
        return;
    }

    // Now we extract the code that was used to encode literals and lengths in the block.

    auto literal_code_result = CanonicalCode::from_bytes(code_lengths.span().trim(literal_code_count));
    if (!literal_code_result.has_value()) {
        set_fatal_error();
        return;
    }
    literal_code = literal_code_result.value();

    // Now we extract the code that was used to encode distances in the block.

    if (distance_code_count == 1) {
        auto length = code_lengths[literal_code_count];

        if (length == 0) {
            return;
        } else if (length != 1) {
            set_fatal_error();
            return;
        }
    }

    auto distance_code_result = CanonicalCode::from_bytes(code_lengths.span().slice(literal_code_count));
    if (!distance_code_result.has_value()) {
        set_fatal_error();
        return;
    }
    distance_code = distance_code_result.value();
}

}
