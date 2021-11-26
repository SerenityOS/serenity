/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/FlyString.h>

namespace PDF {

class Filter {
public:
    static Optional<ByteBuffer> decode(ReadonlyBytes bytes, FlyString const& encoding_type);

private:
    static Optional<ByteBuffer> decode_ascii_hex(ReadonlyBytes bytes);
    static Optional<ByteBuffer> decode_ascii85(ReadonlyBytes bytes);
    static Optional<ByteBuffer> decode_lzw(ReadonlyBytes bytes);
    static Optional<ByteBuffer> decode_flate(ReadonlyBytes bytes);
    static Optional<ByteBuffer> decode_run_length(ReadonlyBytes bytes);
    static Optional<ByteBuffer> decode_ccitt(ReadonlyBytes bytes);
    static Optional<ByteBuffer> decode_jbig2(ReadonlyBytes bytes);
    static Optional<ByteBuffer> decode_dct(ReadonlyBytes bytes);
    static Optional<ByteBuffer> decode_jpx(ReadonlyBytes bytes);
    static Optional<ByteBuffer> decode_crypt(ReadonlyBytes bytes);
};

}
