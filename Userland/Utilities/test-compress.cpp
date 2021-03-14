/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
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

#include <AK/TestSuite.h>

#include <AK/Array.h>
#include <AK/MemoryStream.h>
#include <AK/Random.h>
#include <LibCompress/Deflate.h>
#include <LibCompress/Gzip.h>
#include <LibCompress/Zlib.h>
#include <string.h>

TEST_CASE(canonical_code_simple)
{
    const Array<u8, 32> code {
        0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05,
        0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05,
        0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05
    };
    const Array<u8, 6> input {
        0x00, 0x42, 0x84, 0xa9, 0xb0, 0x15
    };
    const Array<u32, 9> output {
        0x00, 0x01, 0x01, 0x02, 0x03, 0x05, 0x08, 0x0d, 0x15
    };

    const auto huffman = Compress::CanonicalCode::from_bytes(code).value();
    auto memory_stream = InputMemoryStream { input };
    auto bit_stream = InputBitStream { memory_stream };

    for (size_t idx = 0; idx < 9; ++idx)
        EXPECT_EQ(huffman.read_symbol(bit_stream), output[idx]);
}

TEST_CASE(canonical_code_complex)
{
    const Array<u8, 6> code {
        0x03, 0x02, 0x03, 0x03, 0x02, 0x03
    };
    const Array<u8, 4> input {
        0xa1, 0xf3, 0xa1, 0xf3
    };
    const Array<u32, 12> output {
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05
    };

    const auto huffman = Compress::CanonicalCode::from_bytes(code).value();
    auto memory_stream = InputMemoryStream { input };
    auto bit_stream = InputBitStream { memory_stream };

    for (size_t idx = 0; idx < 12; ++idx)
        EXPECT_EQ(huffman.read_symbol(bit_stream), output[idx]);
}

TEST_CASE(deflate_decompress_compressed_block)
{
    const Array<u8, 28> compressed {
        0x0B, 0xC9, 0xC8, 0x2C, 0x56, 0x00, 0xA2, 0x44, 0x85, 0xE2, 0xCC, 0xDC,
        0x82, 0x9C, 0x54, 0x85, 0x92, 0xD4, 0x8A, 0x12, 0x85, 0xB4, 0x4C, 0x20,
        0xCB, 0x4A, 0x13, 0x00
    };

    const u8 uncompressed[] = "This is a simple text file :)";

    const auto decompressed = Compress::DeflateDecompressor::decompress_all(compressed);
    EXPECT(decompressed.value().bytes() == ReadonlyBytes({ uncompressed, sizeof(uncompressed) - 1 }));
}

TEST_CASE(deflate_decompress_uncompressed_block)
{
    const Array<u8, 18> compressed {
        0x01, 0x0d, 0x00, 0xf2, 0xff, 0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x2c, 0x20,
        0x57, 0x6f, 0x72, 0x6c, 0x64, 0x21
    };

    const u8 uncompressed[] = "Hello, World!";

    const auto decompressed = Compress::DeflateDecompressor::decompress_all(compressed);
    EXPECT(decompressed.value().bytes() == (ReadonlyBytes { uncompressed, sizeof(uncompressed) - 1 }));
}

TEST_CASE(deflate_decompress_multiple_blocks)
{
    const Array<u8, 84> compressed {
        0x00, 0x1f, 0x00, 0xe0, 0xff, 0x54, 0x68, 0x65, 0x20, 0x66, 0x69, 0x72,
        0x73, 0x74, 0x20, 0x62, 0x6c, 0x6f, 0x63, 0x6b, 0x20, 0x69, 0x73, 0x20,
        0x75, 0x6e, 0x63, 0x6f, 0x6d, 0x70, 0x72, 0x65, 0x73, 0x73, 0x65, 0x64,
        0x53, 0x48, 0xcc, 0x4b, 0x51, 0x28, 0xc9, 0x48, 0x55, 0x28, 0x4e, 0x4d,
        0xce, 0x07, 0x32, 0x93, 0x72, 0xf2, 0x93, 0xb3, 0x15, 0x32, 0x8b, 0x15,
        0x92, 0xf3, 0x73, 0x0b, 0x8a, 0x52, 0x8b, 0x8b, 0x53, 0x53, 0xf4, 0x00
    };

    const u8 uncompressed[] = "The first block is uncompressed and the second block is compressed.";

    const auto decompressed = Compress::DeflateDecompressor::decompress_all(compressed);
    EXPECT(decompressed.value().bytes() == (ReadonlyBytes { uncompressed, sizeof(uncompressed) - 1 }));
}

TEST_CASE(deflate_decompress_zeroes)
{
    const Array<u8, 20> compressed {
        0xed, 0xc1, 0x01, 0x0d, 0x00, 0x00, 0x00, 0xc2, 0xa0, 0xf7, 0x4f, 0x6d,
        0x0f, 0x07, 0x14, 0x00, 0x00, 0x00, 0xf0, 0x6e
    };

    const Array<u8, 4096> uncompressed { 0 };

    const auto decompressed = Compress::DeflateDecompressor::decompress_all(compressed);
    EXPECT(uncompressed == decompressed.value().bytes());
}

TEST_CASE(deflate_round_trip_store)
{
    auto original = ByteBuffer::create_uninitialized(1024);
    fill_with_random(original.data(), 1024);
    auto compressed = Compress::DeflateCompressor::compress_all(original, Compress::DeflateCompressor::CompressionLevel::STORE);
    EXPECT(compressed.has_value());
    auto uncompressed = Compress::DeflateDecompressor::decompress_all(compressed.value());
    EXPECT(uncompressed.has_value());
    EXPECT(uncompressed.value() == original);
}

TEST_CASE(deflate_round_trip_compress)
{
    auto original = ByteBuffer::create_uninitialized(2048);
    fill_with_random(original.data(), 1024);
    memset(original.offset_pointer(1024), 0, 1024); // we fill the second half with 0s to make sure we test back references as well
    // Since the different levels just change how much time is spent looking for better matches, just use fast here to reduce test time
    auto compressed = Compress::DeflateCompressor::compress_all(original, Compress::DeflateCompressor::CompressionLevel::FAST);
    EXPECT(compressed.has_value());
    auto uncompressed = Compress::DeflateDecompressor::decompress_all(compressed.value());
    EXPECT(uncompressed.has_value());
    EXPECT(uncompressed.value() == original);
}

TEST_CASE(deflate_round_trip_compress_large)
{
    auto original = ByteBuffer::create_uninitialized(Compress::DeflateCompressor::block_size * 2); // Compress a buffer larger than the maximum block size to test the sliding window mechanism
    // Since the different levels just change how much time is spent looking for better matches, just use fast here to reduce test time
    auto compressed = Compress::DeflateCompressor::compress_all(original, Compress::DeflateCompressor::CompressionLevel::FAST);
    EXPECT(compressed.has_value());
    auto uncompressed = Compress::DeflateDecompressor::decompress_all(compressed.value());
    EXPECT(uncompressed.has_value());
    EXPECT(uncompressed.value() == original);
}

TEST_CASE(deflate_compress_literals)
{
    // This byte array is known to not produce any back references with our lz77 implementation even at the highest compression settings
    Array<u8, 0x13> test { 0, 0, 0, 0, 0x72, 0, 0, 0xee, 0, 0, 0, 0x26, 0, 0, 0, 0x28, 0, 0, 0x72 };
    auto compressed = Compress::DeflateCompressor::compress_all(test, Compress::DeflateCompressor::CompressionLevel::GOOD);
    EXPECT(compressed.has_value());
}

TEST_CASE(zlib_decompress_simple)
{
    const Array<u8, 40> compressed {
        0x78, 0x01, 0x01, 0x1D, 0x00, 0xE2, 0xFF, 0x54, 0x68, 0x69, 0x73, 0x20,
        0x69, 0x73, 0x20, 0x61, 0x20, 0x73, 0x69, 0x6D, 0x70, 0x6C, 0x65, 0x20,
        0x74, 0x65, 0x78, 0x74, 0x20, 0x66, 0x69, 0x6C, 0x65, 0x20, 0x3A, 0x29,
        0x99, 0x5E, 0x09, 0xE8
    };

    const u8 uncompressed[] = "This is a simple text file :)";

    const auto decompressed = Compress::Zlib::decompress_all(compressed);
    EXPECT(decompressed.value().bytes() == (ReadonlyBytes { uncompressed, sizeof(uncompressed) - 1 }));
}

TEST_CASE(gzip_decompress_simple)
{
    const Array<u8, 33> compressed {
        0x1f, 0x8b, 0x08, 0x00, 0x77, 0xff, 0x47, 0x5f, 0x02, 0xff, 0x2b, 0xcf,
        0x2f, 0x4a, 0x31, 0x54, 0x48, 0x4c, 0x4a, 0x56, 0x28, 0x07, 0xb2, 0x8c,
        0x00, 0xc2, 0x1d, 0x22, 0x15, 0x0f, 0x00, 0x00, 0x00
    };

    const u8 uncompressed[] = "word1 abc word2";

    const auto decompressed = Compress::GzipDecompressor::decompress_all(compressed);
    EXPECT(decompressed.value().bytes() == (ReadonlyBytes { uncompressed, sizeof(uncompressed) - 1 }));
}

TEST_CASE(gzip_decompress_multiple_members)
{
    const Array<u8, 52> compressed {
        0x1f, 0x8b, 0x08, 0x00, 0xe0, 0x03, 0x48, 0x5f, 0x02, 0xff, 0x4b, 0x4c,
        0x4a, 0x4e, 0x4c, 0x4a, 0x06, 0x00, 0x4c, 0x99, 0x6e, 0x72, 0x06, 0x00,
        0x00, 0x00, 0x1f, 0x8b, 0x08, 0x00, 0xe0, 0x03, 0x48, 0x5f, 0x02, 0xff,
        0x4b, 0x4c, 0x4a, 0x4e, 0x4c, 0x4a, 0x06, 0x00, 0x4c, 0x99, 0x6e, 0x72,
        0x06, 0x00, 0x00, 0x00
    };

    const u8 uncompressed[] = "abcabcabcabc";

    const auto decompressed = Compress::GzipDecompressor::decompress_all(compressed);
    EXPECT(decompressed.value().bytes() == (ReadonlyBytes { uncompressed, sizeof(uncompressed) - 1 }));
}

TEST_CASE(gzip_decompress_zeroes)
{
    const Array<u8, 161> compressed {
        0x1f, 0x8b, 0x08, 0x00, 0x6e, 0x7a, 0x4b, 0x5f, 0x02, 0xff, 0xed, 0xc1,
        0x31, 0x01, 0x00, 0x00, 0x00, 0xc2, 0xa0, 0xf5, 0x4f, 0xed, 0x61, 0x0d,
        0xa0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x6e, 0xcd, 0xcd, 0xe8,
        0x7e, 0x00, 0x00, 0x02, 0x00
    };

    const Array<u8, 128 * 1024> uncompressed = { 0 };

    const auto decompressed = Compress::GzipDecompressor::decompress_all(compressed);
    EXPECT(uncompressed == decompressed.value().bytes());
}

TEST_CASE(gzip_decompress_repeat_around_buffer)
{
    const Array<u8, 70> compressed {
        0x1f, 0x8b, 0x08, 0x00, 0xc6, 0x74, 0x53, 0x5f, 0x02, 0xff, 0xed, 0xc1,
        0x01, 0x0d, 0x00, 0x00, 0x0c, 0x02, 0xa0, 0xdb, 0xbf, 0xf4, 0x37, 0x6b,
        0x08, 0x24, 0xdb, 0x0e, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80, 0xca,
        0xb8, 0x07, 0xcd, 0xe5, 0x38, 0xfa, 0x00, 0x80, 0x00, 0x00
    };

    Array<u8, 0x8000> uncompressed;
    uncompressed.span().slice(0x0000, 0x0100).fill(1);
    uncompressed.span().slice(0x0100, 0x7e00).fill(0);
    uncompressed.span().slice(0x7f00, 0x0100).fill(1);

    const auto decompressed = Compress::GzipDecompressor::decompress_all(compressed);
    EXPECT(uncompressed == decompressed.value().bytes());
}

TEST_CASE(gzip_round_trip)
{
    auto original = ByteBuffer::create_uninitialized(1024);
    fill_with_random(original.data(), 1024);
    auto compressed = Compress::GzipCompressor::compress_all(original);
    EXPECT(compressed.has_value());
    auto uncompressed = Compress::GzipDecompressor::decompress_all(compressed.value());
    EXPECT(uncompressed.has_value());
    EXPECT(uncompressed.value() == original);
}

TEST_MAIN(Compress)
