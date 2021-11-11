/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Optional.h>
#include <AK/Span.h>
#include <AK/Types.h>

namespace Compress {

class Zlib {
public:
    Optional<ByteBuffer> decompress();
    u32 checksum();

    static Optional<Zlib> try_create(ReadonlyBytes data);
    static Optional<ByteBuffer> decompress_all(ReadonlyBytes);

private:
    Zlib(ReadonlyBytes data);

    u8 m_compression_method;
    u8 m_compression_info;
    u8 m_check_bits;
    u8 m_has_dictionary;
    u8 m_compression_level;

    u32 m_checksum { 0 };
    ReadonlyBytes m_input_data;
    ReadonlyBytes m_data_bytes;
};

}
