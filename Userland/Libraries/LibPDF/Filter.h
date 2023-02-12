/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/DeprecatedFlyString.h>
#include <AK/Error.h>
#include <LibPDF/ObjectDerivatives.h>

namespace PDF {

class Filter {
public:
    static PDFErrorOr<ByteBuffer> decode(ReadonlyBytes bytes, DeprecatedFlyString const& encoding_type, RefPtr<DictObject> decode_parms);

private:
    static PDFErrorOr<ByteBuffer> decode_ascii_hex(ReadonlyBytes bytes);
    static PDFErrorOr<ByteBuffer> decode_ascii85(ReadonlyBytes bytes);
    static PDFErrorOr<ByteBuffer> decode_png_prediction(Bytes bytes, int bytes_per_row);
    static PDFErrorOr<ByteBuffer> decode_lzw(ReadonlyBytes bytes);
    static PDFErrorOr<ByteBuffer> decode_flate(ReadonlyBytes bytes, int predictor, int columns, int colors, int bits_per_component);
    static PDFErrorOr<ByteBuffer> decode_run_length(ReadonlyBytes bytes);
    static PDFErrorOr<ByteBuffer> decode_ccitt(ReadonlyBytes bytes);
    static PDFErrorOr<ByteBuffer> decode_jbig2(ReadonlyBytes bytes);
    static PDFErrorOr<ByteBuffer> decode_dct(ReadonlyBytes bytes);
    static PDFErrorOr<ByteBuffer> decode_jpx(ReadonlyBytes bytes);
    static PDFErrorOr<ByteBuffer> decode_crypt(ReadonlyBytes bytes);
};

}
