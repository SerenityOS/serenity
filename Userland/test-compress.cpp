/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <LibCompress/Deflate.h>
#include <LibCompress/Zlib.h>

bool compare(ReadonlyBytes lhs, ReadonlyBytes rhs)
{
    if (lhs.size() != rhs.size())
        return false;

    for (size_t idx = 0; idx < lhs.size(); ++idx) {
        if (lhs[idx] != rhs[idx])
            return false;
    }

    return true;
}

TEST_CASE(deflate_decompress_compressed_block)
{
    const u8 compressed[] = {
        0x0B, 0xC9, 0xC8, 0x2C, 0x56, 0x00, 0xA2, 0x44, 0x85, 0xE2, 0xCC, 0xDC,
        0x82, 0x9C, 0x54, 0x85, 0x92, 0xD4, 0x8A, 0x12, 0x85, 0xB4, 0x4C, 0x20,
        0xCB, 0x4A, 0x13, 0x00
    };

    const u8 uncompressed[] = "This is a simple text file :)";

    const auto decompressed = Compress::DeflateStream::decompress_all({ compressed, sizeof(compressed) });
    EXPECT(compare({ uncompressed, sizeof(uncompressed) - 1 }, decompressed.span()));
}

TEST_CASE(zlib_simple_decompress)
{
    const u8 compressed[] = {
        0x78, 0x01, 0x01, 0x1D, 0x00, 0xE2, 0xFF, 0x54, 0x68, 0x69, 0x73, 0x20,
        0x69, 0x73, 0x20, 0x61, 0x20, 0x73, 0x69, 0x6D, 0x70, 0x6C, 0x65, 0x20,
        0x74, 0x65, 0x78, 0x74, 0x20, 0x66, 0x69, 0x6C, 0x65, 0x20, 0x3A, 0x29,
        0x99, 0x5E, 0x09, 0xE8
    };

    const u8 uncompressed[] = "This is a simple text file :)";

    const auto decompressed = Compress::Zlib { { compressed, sizeof(compressed) } }.decompress();
    EXPECT(compare({ uncompressed, sizeof(uncompressed) - 1 }, decompressed.span()));
}

TEST_MAIN(Compress)
