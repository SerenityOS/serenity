/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BitStream.h>
#include <AK/ByteBuffer.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <AK/Span.h>
#include <AK/Types.h>
#include <LibCore/Stream.h>
#include <LibCrypto/Checksum/Adler32.h>

namespace Compress {

enum class ZlibCompressionMethod : u8 {
    Deflate = 8,
};

enum class ZlibCompressionLevel : u8 {
    Fastest,
    Fast,
    Default,
    Best,
};

struct ZlibHeader {
    union {
        struct {
            ZlibCompressionMethod compression_method : 4;
            u8 compression_info : 4;

            u8 check_bits : 5;
            bool present_dictionary : 1;
            ZlibCompressionLevel compression_level : 2;
        };
        NetworkOrdered<u16> as_u16;
    };
};
static_assert(sizeof(ZlibHeader) == sizeof(u16));

class ZlibDecompressor {
public:
    Optional<ByteBuffer> decompress();
    u32 checksum();

    static Optional<ZlibDecompressor> try_create(ReadonlyBytes data);
    static Optional<ByteBuffer> decompress_all(ReadonlyBytes);

private:
    ZlibDecompressor(ZlibHeader, ReadonlyBytes data);

    ZlibHeader m_header;

    u32 m_checksum { 0 };
    ReadonlyBytes m_input_data;
    ReadonlyBytes m_data_bytes;
};

class ZlibCompressor : public Core::Stream::Stream {
public:
    static ErrorOr<NonnullOwnPtr<ZlibCompressor>> construct(Core::Stream::Handle<Core::Stream::Stream>, ZlibCompressionLevel = ZlibCompressionLevel::Default);
    ~ZlibCompressor();

    virtual ErrorOr<Bytes> read(Bytes) override;
    virtual ErrorOr<size_t> write(ReadonlyBytes) override;
    virtual bool is_eof() const override;
    virtual bool is_open() const override;
    virtual void close() override;
    ErrorOr<void> finish();

    static ErrorOr<ByteBuffer> compress_all(ReadonlyBytes bytes, ZlibCompressionLevel = ZlibCompressionLevel::Default);

private:
    ZlibCompressor(Core::Stream::Handle<Core::Stream::Stream>, ZlibCompressionLevel);
    ErrorOr<void> write_header(ZlibCompressionMethod, ZlibCompressionLevel);

    bool m_finished { false };
    Core::Stream::Handle<Core::Stream::Stream> m_output_stream;
    NonnullOwnPtr<Core::Stream::Stream> m_compressor;
    Crypto::Checksum::Adler32 m_adler32_checksum;
};

}
