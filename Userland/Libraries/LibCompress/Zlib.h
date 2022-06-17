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

class Zlib {
public:
    Optional<ByteBuffer> decompress();
    u32 checksum();

    static Optional<Zlib> try_create(ReadonlyBytes data);
    static Optional<ByteBuffer> decompress_all(ReadonlyBytes);

private:
    Zlib(ZlibHeader, ReadonlyBytes data);

    ZlibHeader m_header;

    u32 m_checksum { 0 };
    ReadonlyBytes m_input_data;
    ReadonlyBytes m_data_bytes;
};

}
