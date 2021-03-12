/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@gmail.com>
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

#include <LibCompress/Deflate.h>
#include <LibCrypto/Checksum/CRC32.h>

namespace Compress {

constexpr u8 gzip_magic_1 = 0x1f;
constexpr u8 gzip_magic_2 = 0x8b;
struct [[gnu::packed]] BlockHeader {
    u8 identification_1;
    u8 identification_2;
    u8 compression_method;
    u8 flags;
    LittleEndian<u32> modification_time;
    u8 extra_flags;
    u8 operating_system;

    bool valid_magic_number() const;
    bool supported_by_implementation() const;
};

struct Flags {
    static constexpr u8 FTEXT = 1 << 0;
    static constexpr u8 FHCRC = 1 << 1;
    static constexpr u8 FEXTRA = 1 << 2;
    static constexpr u8 FNAME = 1 << 3;
    static constexpr u8 FCOMMENT = 1 << 4;

    static constexpr u8 MAX = FTEXT | FHCRC | FEXTRA | FNAME | FCOMMENT;
};

class GzipDecompressor final : public InputStream {
public:
    GzipDecompressor(InputStream&);
    ~GzipDecompressor();

    size_t read(Bytes) override;
    bool read_or_error(Bytes) override;
    bool discard_or_error(size_t) override;

    bool unreliable_eof() const override;

    static Optional<ByteBuffer> decompress_all(ReadonlyBytes);
    static bool is_likely_compressed(ReadonlyBytes bytes);

private:
    class Member {
    public:
        Member(BlockHeader header, InputStream& stream)
            : m_header(header)
            , m_stream(stream)
        {
        }

        BlockHeader m_header;
        DeflateDecompressor m_stream;
        Crypto::Checksum::CRC32 m_checksum;
        size_t m_nread { 0 };
    };

    const Member& current_member() const { return m_current_member.value(); }
    Member& current_member() { return m_current_member.value(); }

    InputStream& m_input_stream;
    Optional<Member> m_current_member;

    bool m_eof { false };
};

class GzipCompressor final : public OutputStream {
public:
    GzipCompressor(OutputStream&);
    ~GzipCompressor();

    size_t write(ReadonlyBytes) override;
    bool write_or_error(ReadonlyBytes) override;

    static Optional<ByteBuffer> compress_all(const ReadonlyBytes& bytes);

private:
    OutputStream& m_output_stream;
};

}
