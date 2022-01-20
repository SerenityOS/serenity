/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Error.h>
#include <AK/FlyString.h>

namespace PDF {

class Filter {
public:
    static ErrorOr<ByteBuffer> decode(ReadonlyBytes bytes, FlyString const& encoding_type);

private:
    static ErrorOr<ByteBuffer> decode_ascii_hex(ReadonlyBytes bytes);
    static ErrorOr<ByteBuffer> decode_ascii85(ReadonlyBytes bytes);
    static ErrorOr<ByteBuffer> decode_lzw(ReadonlyBytes bytes);
    static ErrorOr<ByteBuffer> decode_flate(ReadonlyBytes bytes);
    static ErrorOr<ByteBuffer> decode_run_length(ReadonlyBytes bytes);
    static ErrorOr<ByteBuffer> decode_ccitt(ReadonlyBytes bytes);
    static ErrorOr<ByteBuffer> decode_jbig2(ReadonlyBytes bytes);
    static ErrorOr<ByteBuffer> decode_dct(ReadonlyBytes bytes);
    static ErrorOr<ByteBuffer> decode_jpx(ReadonlyBytes bytes);
    static ErrorOr<ByteBuffer> decode_crypt(ReadonlyBytes bytes);
};

}
