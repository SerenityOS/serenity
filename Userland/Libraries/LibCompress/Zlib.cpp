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

Optional<Zlib> Zlib::try_create(ReadonlyBytes data)
{
    if (data.size() < 6)
        return {}; // header + footer size is 6

    Zlib zlib { data };

    u8 compression_info = data.at(0);
    u8 flags = data.at(1);

    zlib.m_compression_method = compression_info & 0xF;
    zlib.m_compression_info = (compression_info >> 4) & 0xF;
    zlib.m_check_bits = flags & 0xF;
    zlib.m_has_dictionary = (flags >> 5) & 0x1;
    zlib.m_compression_level = (flags >> 6) & 0x3;

    if (zlib.m_compression_method != 8 || zlib.m_compression_info > 7)
        return {}; // non-deflate compression

    if (zlib.m_has_dictionary)
        return {}; // we dont support pre-defined dictionaries

    if ((compression_info * 256 + flags) % 31 != 0)
        return {}; // error correction code doesn't match

    zlib.m_data_bytes = data.slice(2, data.size() - 2 - 4);
    return zlib;
}

Zlib::Zlib(ReadonlyBytes data)
    : m_input_data(data)
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
        auto bytes = m_input_data.slice(m_input_data.size() - 4, 4);
        m_checksum = bytes.at(0) << 24 | bytes.at(1) << 16 | bytes.at(2) << 8 || bytes.at(3);
    }

    return m_checksum;
}

}
