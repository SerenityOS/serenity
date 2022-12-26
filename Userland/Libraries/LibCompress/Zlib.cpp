/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <AK/Span.h>
#include <AK/TypeCasts.h>
#include <AK/Types.h>
#include <LibCompress/Deflate.h>
#include <LibCompress/Zlib.h>

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

ZlibCompressor::ZlibCompressor(OutputStream& stream, ZlibCompressionLevel compression_level)
    : m_output_stream(stream)
{
    // Zlib only defines Deflate as a compression method.
    auto compression_method = ZlibCompressionMethod::Deflate;

    write_header(compression_method, compression_level);

    // FIXME: Find a way to compress with Deflate's "Best" compression level.
    m_compressor = make<DeflateCompressor>(stream, static_cast<DeflateCompressor::CompressionLevel>(compression_level));
}

ZlibCompressor::~ZlibCompressor()
{
    VERIFY(m_finished);
}

void ZlibCompressor::write_header(ZlibCompressionMethod compression_method, ZlibCompressionLevel compression_level)
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

    m_output_stream << header.as_u16;
}

size_t ZlibCompressor::write(ReadonlyBytes bytes)
{
    VERIFY(!m_finished);

    size_t n_written = m_compressor->write(bytes);
    m_adler32_checksum.update(bytes.trim(n_written));
    return n_written;
}

bool ZlibCompressor::write_or_error(ReadonlyBytes bytes)
{
    if (write(bytes) < bytes.size()) {
        set_fatal_error();
        return false;
    }

    return true;
}

void ZlibCompressor::finish()
{
    VERIFY(!m_finished);

    if (is<DeflateCompressor>(m_compressor.ptr()))
        static_cast<DeflateCompressor*>(m_compressor.ptr())->final_flush();

    NetworkOrdered<u32> adler_sum = m_adler32_checksum.digest();
    m_output_stream << adler_sum;

    m_finished = true;
}

Optional<ByteBuffer> ZlibCompressor::compress_all(ReadonlyBytes bytes, ZlibCompressionLevel compression_level)
{
    DuplexMemoryStream output_stream;
    ZlibCompressor zlib_stream { output_stream, compression_level };

    zlib_stream.write_or_error(bytes);

    zlib_stream.finish();

    if (zlib_stream.handle_any_error())
        return {};

    return output_stream.copy_into_contiguous_buffer();
}

}
