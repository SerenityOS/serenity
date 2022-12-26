/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Array.h>
#include <LibCompress/Zlib.h>

TEST_CASE(zlib_decompress_simple)
{
    Array<u8, 40> const compressed {
        0x78, 0x01, 0x01, 0x1D, 0x00, 0xE2, 0xFF, 0x54, 0x68, 0x69, 0x73, 0x20,
        0x69, 0x73, 0x20, 0x61, 0x20, 0x73, 0x69, 0x6D, 0x70, 0x6C, 0x65, 0x20,
        0x74, 0x65, 0x78, 0x74, 0x20, 0x66, 0x69, 0x6C, 0x65, 0x20, 0x3A, 0x29,
        0x99, 0x5E, 0x09, 0xE8
    };

    const u8 uncompressed[] = "This is a simple text file :)";

    auto const decompressed = Compress::ZlibDecompressor::decompress_all(compressed);
    EXPECT(decompressed.value().bytes() == (ReadonlyBytes { uncompressed, sizeof(uncompressed) - 1 }));
}

TEST_CASE(zlib_compress_simple)
{
    // Note: This is just the output of our compression function from an arbitrary point in time.
    // This test is intended to ensure that the decompression doesn't change unintentionally,
    // it does not make any guarantees for correctness.

    Array<u8, 37> const compressed {
        0x78, 0x9C, 0x0B, 0xC9, 0xC8, 0x2C, 0x56, 0xC8, 0x2C, 0x56, 0x48, 0x54,
        0x28, 0xCE, 0xCC, 0x2D, 0xC8, 0x49, 0x55, 0x28, 0x49, 0xAD, 0x28, 0x51,
        0x48, 0xCB, 0xCC, 0x49, 0x55, 0xB0, 0xD2, 0x04, 0x00, 0x99, 0x5E, 0x09,
        0xE8
    };

    const u8 uncompressed[] = "This is a simple text file :)";

    auto const freshly_pressed = Compress::ZlibCompressor::compress_all({ uncompressed, sizeof(uncompressed) - 1 });
    EXPECT(freshly_pressed.value().bytes() == compressed.span());
}
