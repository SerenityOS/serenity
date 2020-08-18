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

#pragma once

#include <AK/CircularQueue.h>
#include <AK/Span.h>
#include <AK/Stream.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <cstring>

namespace Compress {

// Reads one bit at a time starting with the rightmost bit
class BitStreamReader {
public:
    BitStreamReader(ReadonlyBytes data)
        : m_data(data)
    {
    }

    i8 read();
    i8 read_byte();
    u32 read_bits(u8);
    u8 get_bit_byte_offset();

private:
    ReadonlyBytes m_data;
    size_t m_data_index { 0 };

    i8 m_current_byte { 0 };
    u8 m_remaining_bits { 0 };
};

class CanonicalCode {
public:
    CanonicalCode(Vector<u8>);
    u32 next_symbol(BitStreamReader&);

private:
    Vector<u32> m_symbol_codes;
    Vector<u32> m_symbol_values;
};

// Implements a DEFLATE decompressor according to RFC 1951.
class DeflateStream final : public InputStream {
public:
    // FIXME: This should really return a ByteBuffer.
    static Vector<u8> decompress_all(ReadonlyBytes bytes)
    {
        DeflateStream stream { bytes };
        while (stream.read_next_block()) {
        }

        Vector<u8> vector;
        vector.resize(stream.m_intermediate_stream.remaining());
        stream >> vector;

        return vector;
    }

    DeflateStream(ReadonlyBytes data)
        : m_reader(data)
        , m_literal_length_codes(generate_literal_length_codes())
        , m_fixed_distance_codes(generate_fixed_distance_codes())
    {
    }

    // FIXME: Accept an InputStream.

    size_t read(Bytes bytes) override
    {
        if (m_intermediate_stream.remaining() >= bytes.size())
            return m_intermediate_stream.read_or_error(bytes);

        while (read_next_block()) {
            if (m_intermediate_stream.remaining() >= bytes.size())
                return m_intermediate_stream.read_or_error(bytes);
        }

        return m_intermediate_stream.read(bytes);
    }

    bool read_or_error(Bytes bytes) override
    {
        if (m_intermediate_stream.remaining() >= bytes.size()) {
            m_intermediate_stream.read_or_error(bytes);
            return true;
        }

        while (read_next_block()) {
            if (m_intermediate_stream.remaining() >= bytes.size()) {
                m_intermediate_stream.read_or_error(bytes);
                return true;
            }
        }

        m_error = true;
        return false;
    }

    bool eof() const override
    {
        if (!m_intermediate_stream.eof())
            return false;

        while (read_next_block()) {
            if (!m_intermediate_stream.eof())
                return false;
        }

        return true;
    }

    bool discard_or_error(size_t count) override
    {
        if (m_intermediate_stream.remaining() >= count) {
            m_intermediate_stream.discard_or_error(count);
            return true;
        }

        while (read_next_block()) {
            if (m_intermediate_stream.remaining() >= count) {
                m_intermediate_stream.discard_or_error(count);
                return true;
            }
        }

        m_error = true;
        return false;
    }

private:
    void decompress_uncompressed_block() const;
    void decompress_static_block() const;
    void decompress_dynamic_block() const;
    void decompress_huffman_block(CanonicalCode&, CanonicalCode*) const;

    Vector<CanonicalCode> decode_huffman_codes() const;
    u32 decode_run_length(u32) const;
    u32 decode_distance(u32) const;

    void copy_from_history(u32, u32) const;

    Vector<u8> generate_literal_length_codes() const;
    Vector<u8> generate_fixed_distance_codes() const;

    mutable BitStreamReader m_reader;

    mutable CanonicalCode m_literal_length_codes;
    mutable CanonicalCode m_fixed_distance_codes;

    // FIXME: Theoretically, blocks can be extremly large, reading a single block could
    //        exhaust memory. Maybe wait for C++20 coroutines?
    bool read_next_block() const;

    mutable bool m_read_last_block { false };
    mutable DuplexMemoryStream m_intermediate_stream;
};

}
