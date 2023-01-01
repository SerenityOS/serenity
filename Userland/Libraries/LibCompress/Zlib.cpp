/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Span.h>
#include <AK/TypeCasts.h>
#include <AK/Types.h>
#include <LibCompress/Deflate.h>
#include <LibCompress/Zlib.h>
#include <LibCore/MemoryStream.h>

namespace Compress {

constexpr static size_t Adler32Size = sizeof(u32);

Optional<ZlibDecompressor> ZlibDecompressor::try_create(ReadonlyBytes data)
{
    if (data.size() < (sizeof(ZlibHeader) + Adler32Size))
        return {};

    ZlibHeader header { .as_u16 = data.at(0) << 8 | data.at(1) };

    if (header.compression_method != ZlibCompressionMethod::Deflate || header.compression_info > 7)
        return {}; // non-deflate compression

    if (header.present_dictionary)
        return {}; // we dont support pre-defined dictionaries

    if (header.as_u16 % 31 != 0)
        return {}; // error correction code doesn't match

    ZlibDecompressor zlib { header, data };
    zlib.m_data_bytes = data.slice(2, data.size() - sizeof(ZlibHeader) - Adler32Size);
    return zlib;
}

ZlibDecompressor::ZlibDecompressor(ZlibHeader header, ReadonlyBytes data)
    : m_header(header)
    , m_input_data(data)
{
}

Optional<ByteBuffer> ZlibDecompressor::decompress()
{
    auto buffer_or_error = DeflateDecompressor::decompress_all(m_data_bytes);
    if (buffer_or_error.is_error())
        return {};
    return buffer_or_error.release_value();
}

Optional<ByteBuffer> ZlibDecompressor::decompress_all(ReadonlyBytes bytes)
{
    auto zlib = try_create(bytes);
    if (!zlib.has_value())
        return {};
    return zlib->decompress();
}

u32 ZlibDecompressor::checksum()
{
    if (!m_checksum) {
        auto bytes = m_input_data.slice_from_end(Adler32Size);
        m_checksum = bytes.at(0) << 24 | bytes.at(1) << 16 | bytes.at(2) << 8 || bytes.at(3);
    }

    return m_checksum;
}

ErrorOr<NonnullOwnPtr<ZlibCompressor>> ZlibCompressor::construct(Core::Stream::Handle<Core::Stream::Stream> stream, ZlibCompressionLevel compression_level)
{
    // Zlib only defines Deflate as a compression method.
    auto compression_method = ZlibCompressionMethod::Deflate;

    // FIXME: Find a way to compress with Deflate's "Best" compression level.
    auto compressor_stream = TRY(DeflateCompressor::construct(Core::Stream::Handle(*stream), static_cast<DeflateCompressor::CompressionLevel>(compression_level)));

    auto zlib_compressor = TRY(adopt_nonnull_own_or_enomem(new (nothrow) ZlibCompressor(move(stream), move(compressor_stream))));
    TRY(zlib_compressor->write_header(compression_method, compression_level));

    return zlib_compressor;
}

ZlibCompressor::ZlibCompressor(Core::Stream::Handle<Core::Stream::Stream> stream, NonnullOwnPtr<Core::Stream::Stream> compressor_stream)
    : m_output_stream(move(stream))
    , m_compressor(move(compressor_stream))
{
}

ZlibCompressor::~ZlibCompressor()
{
    VERIFY(m_finished);
}

ErrorOr<void> ZlibCompressor::write_header(ZlibCompressionMethod compression_method, ZlibCompressionLevel compression_level)
{
    u8 compression_info = 0;
    if (compression_method == ZlibCompressionMethod::Deflate) {
        compression_info = AK::log2(DeflateCompressor::window_size) - 8;
        VERIFY(compression_info <= 7);
    }

    ZlibHeader header {
        .compression_method = compression_method,
        .compression_info = compression_info,
        .check_bits = 0,
        .present_dictionary = false,
        .compression_level = compression_level,
    };
    header.check_bits = 0b11111 - header.as_u16 % 31;

    // FIXME: Support pre-defined dictionaries.

    TRY(m_output_stream->write(header.as_u16.bytes()));

    return {};
}

ErrorOr<Bytes> ZlibCompressor::read(Bytes)
{
    return Error::from_errno(EBADF);
}

ErrorOr<size_t> ZlibCompressor::write(ReadonlyBytes bytes)
{
    VERIFY(!m_finished);

    size_t n_written = TRY(m_compressor->write(bytes));
    m_adler32_checksum.update(bytes.trim(n_written));
    return n_written;
}

bool ZlibCompressor::is_eof() const
{
    return false;
}

bool ZlibCompressor::is_open() const
{
    return m_output_stream->is_open();
}

void ZlibCompressor::close()
{
}

ErrorOr<void> ZlibCompressor::finish()
{
    VERIFY(!m_finished);

    if (is<DeflateCompressor>(m_compressor.ptr()))
        TRY(static_cast<DeflateCompressor*>(m_compressor.ptr())->final_flush());

    NetworkOrdered<u32> adler_sum = m_adler32_checksum.digest();
    TRY(m_output_stream->write(adler_sum.bytes()));

    m_finished = true;

    return {};
}

ErrorOr<ByteBuffer> ZlibCompressor::compress_all(ReadonlyBytes bytes, ZlibCompressionLevel compression_level)
{
    auto output_stream = TRY(try_make<Core::Stream::AllocatingMemoryStream>());
    auto zlib_stream = TRY(ZlibCompressor::construct(Core::Stream::Handle<Core::Stream::Stream>(*output_stream), compression_level));

    TRY(zlib_stream->write_entire_buffer(bytes));

    TRY(zlib_stream->finish());

    auto buffer = TRY(ByteBuffer::create_uninitialized(output_stream->used_buffer_size()));
    TRY(output_stream->read_entire_buffer(buffer.bytes()));

    return buffer;
}

}
