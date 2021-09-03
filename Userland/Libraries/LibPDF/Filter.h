/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/ByteBuffer.h>
#include <YAK/FlyString.h>

namespace PDF {

class Filter {
public:
    static Optional<ByteBuffer> decode(ReadonlyBytes const& bytes, FlyString const& encoding_type);

private:
    static Optional<ByteBuffer> decode_ascii_hex(ReadonlyBytes const& bytes);
    static Optional<ByteBuffer> decode_ascii85(ReadonlyBytes const& bytes);
    static Optional<ByteBuffer> decode_lzw(ReadonlyBytes const& bytes);
    static Optional<ByteBuffer> decode_flate(ReadonlyBytes const& bytes);
    static Optional<ByteBuffer> decode_run_length(ReadonlyBytes const& bytes);
    static Optional<ByteBuffer> decode_ccitt(ReadonlyBytes const& bytes);
    static Optional<ByteBuffer> decode_jbig2(ReadonlyBytes const& bytes);
    static Optional<ByteBuffer> decode_dct(ReadonlyBytes const& bytes);
    static Optional<ByteBuffer> decode_jpx(ReadonlyBytes const& bytes);
    static Optional<ByteBuffer> decode_crypt(ReadonlyBytes const& bytes);
};

}
