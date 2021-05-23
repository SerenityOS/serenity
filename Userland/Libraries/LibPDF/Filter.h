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
    static Optional<ByteBuffer> decode(const ReadonlyBytes& bytes, const FlyString& encoding_type);

private:
    static Optional<ByteBuffer> decode_ascii_hex(const ReadonlyBytes& bytes);
    static Optional<ByteBuffer> decode_ascii85(const ReadonlyBytes& bytes);
    static Optional<ByteBuffer> decode_lzw(const ReadonlyBytes& bytes);
    static Optional<ByteBuffer> decode_flate(const ReadonlyBytes& bytes);
    static Optional<ByteBuffer> decode_run_length(const ReadonlyBytes& bytes);
    static Optional<ByteBuffer> decode_ccitt(const ReadonlyBytes& bytes);
    static Optional<ByteBuffer> decode_jbig2(const ReadonlyBytes& bytes);
    static Optional<ByteBuffer> decode_dct(const ReadonlyBytes& bytes);
    static Optional<ByteBuffer> decode_jpx(const ReadonlyBytes& bytes);
    static Optional<ByteBuffer> decode_crypt(const ReadonlyBytes& bytes);
};

}
