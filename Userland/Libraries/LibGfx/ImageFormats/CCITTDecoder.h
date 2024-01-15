/*
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>

namespace Gfx::CCITT {

// You can find a great overview of CCITT compression schemes here:
// https://www.fileformat.info/mirror/egff/ch09_05.htm

// The CCITT3 specification is accessible at this page:
// https://www.itu.int/rec/T-REC-T.4/en

// The unidimensional scheme is originally described in:
// 4.1 One-dimensional coding scheme
// However, this function implements the TIFF variant (see TIFFLoader.h for a spec link),
// differences are detailed in section:
// Section 10: Modified Huffman Compression
ErrorOr<ByteBuffer> decode_ccitt_rle(ReadonlyBytes bytes, u32 image_width, u32 image_height);

}
