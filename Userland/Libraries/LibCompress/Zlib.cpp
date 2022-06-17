/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Span.h>
#include <AK/Types.h>
#include <LibCompress/Deflate.h>
#include <LibCompress/Zlib.h>

namespace Compress {

constexpr static size_t Adler32Size = sizeof(u32);

Optional<Zlib> Zlib::try_create(ReadonlyBytes data)
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

    Zlib zlib { header, data };
    zlib.m_data_bytes = data.slice(2, data.size() - sizeof(ZlibHeader) - Adler32Size);
    return zlib;
}

Zlib::Zlib(ZlibHeader header, ReadonlyBytes data)
    : m_header(header)
    , m_input_data(data)
{
}

Optional<ByteBuffer> Zlib::decompress()
{
    return DeflateDecompressor::decompress_all(m_data_bytes);
}

Optional<ByteBuffer> Zlib::decompress_all(ReadonlyBytes bytes)
{
    auto zlib = try_create(bytes);
    if (!zlib.has_value())
        return {};
    return zlib->decompress();
}

u32 Zlib::checksum()
{
    if (!m_checksum) {
        auto bytes = m_input_data.slice_from_end(Adler32Size);
        m_checksum = bytes.at(0) << 24 | bytes.at(1) << 16 | bytes.at(2) << 8 || bytes.at(3);
    }

    return m_checksum;
}

}
