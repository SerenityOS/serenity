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

#include <AK/BitStream.h>
#include <AK/ByteBuffer.h>
#include <AK/CircularDuplexStream.h>
#include <AK/Endian.h>
#include <AK/Vector.h>

namespace Compress {

class CanonicalCode {
public:
    CanonicalCode() = default;
    u32 read_symbol(InputBitStream&) const;

    static const CanonicalCode& fixed_literal_codes();
    static const CanonicalCode& fixed_distance_codes();

    static Optional<CanonicalCode> from_bytes(ReadonlyBytes);

private:
    Vector<u32> m_symbol_codes;
    Vector<u32> m_symbol_values;
};

class DeflateDecompressor final : public InputStream {
private:
    class CompressedBlock {
    public:
        CompressedBlock(DeflateDecompressor&, CanonicalCode literal_codes, Optional<CanonicalCode> distance_codes);

        bool try_read_more();

    private:
        bool m_eof { false };

        DeflateDecompressor& m_decompressor;
        CanonicalCode m_literal_codes;
        Optional<CanonicalCode> m_distance_codes;
    };

    class UncompressedBlock {
    public:
        UncompressedBlock(DeflateDecompressor&, size_t);

        bool try_read_more();

    private:
        DeflateDecompressor& m_decompressor;
        size_t m_bytes_remaining;
    };

    enum class State {
        Idle,
        ReadingCompressedBlock,
        ReadingUncompressedBlock
    };

public:
    friend CompressedBlock;
    friend UncompressedBlock;

    DeflateDecompressor(InputStream&);
    ~DeflateDecompressor();

    size_t read(Bytes) override;
    bool read_or_error(Bytes) override;
    bool discard_or_error(size_t) override;

    bool unreliable_eof() const override;

    static Optional<ByteBuffer> decompress_all(ReadonlyBytes);

private:
    u32 decode_length(u32);
    u32 decode_distance(u32);
    void decode_codes(CanonicalCode& literal_code, Optional<CanonicalCode>& distance_code);

    bool m_read_final_bock { false };

    State m_state { State::Idle };
    union {
        CompressedBlock m_compressed_block;
        UncompressedBlock m_uncompressed_block;
    };

    InputBitStream m_input_stream;
    CircularDuplexStream<32 * 1024> m_output_stream;
};

}
