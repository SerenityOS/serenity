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

#include <AK/Span.h>
#include <AK/Types.h>
#include <AK/Vector.h>
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
        return {}; // error correction code doesnt match

    zlib.m_data_bytes = data.slice(2, data.size() - 2 - 4);
    return zlib;
}

Zlib::Zlib(const ReadonlyBytes& data)
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
