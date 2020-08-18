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

#include <LibCompress/Deflate.h>
#include <LibCompress/Zlib.h>
#include <LibCore/ArgsParser.h>

auto main(int argc, char** argv) -> int
{
    const char* mode = nullptr;
    const char* type = nullptr;

    Core::ArgsParser parser;
    parser.add_positional_argument(type, "Type of algorithm to apply (Only Zlib and DEFLATE is present at the moment)", "type", Core::ArgsParser::Required::No);
    parser.add_positional_argument(mode, "Mode to operate in (compress|decompress; Only decompress is valid at the moment)", "mode", Core::ArgsParser::Required::No);
    parser.parse(argc, argv);

    if (type == nullptr) {
        type = "deflate";
    }

    if (mode == nullptr) {
        mode = "decompress";
    }

    StringView mode_sv { mode };
    StringView type_sv { type };
    if (mode_sv == "decompress") {
        if (type_sv == "deflate") {
            // Deflated bytes for the string "This is a simple text file :)"
            u8 data_bytes[] = {
                0x0B, 0xC9, 0xC8, 0x2C,
                0x56, 0x00, 0xA2, 0x44,
                0x85, 0xE2, 0xCC, 0xDC,
                0x82, 0x9C, 0x54, 0x85,
                0x92, 0xD4, 0x8A, 0x12,
                0x85, 0xB4, 0x4C, 0x20,
                0xCB, 0x4A, 0x13, 0x00
            };

            auto deflated = Compress::DeflateStream::decompress_all({ data_bytes, 4 * 7 });
            auto decompressed = String((const char*)deflated.data(), deflated.size());

            if (decompressed.equals_ignoring_case("This is a simple text file :)")) {
                printf("Test PASSED");
                return 0;
            } else {
                printf("Test FAILED");
                return 1;
            }
        }

        if (type_sv == "zlib") {
            // zlib bytes for the string "This is a simple text file :)"
            u8 data_bytes[] = {
                0x78, 0x01, 0x01, 0x1D, 0x00, 0xE2, 0xFF, 0x54,
                0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x61,
                0x20, 0x73, 0x69, 0x6D, 0x70, 0x6C, 0x65, 0x20,
                0x74, 0x65, 0x78, 0x74, 0x20, 0x66, 0x69, 0x6C,
                0x65, 0x20, 0x3A, 0x29, 0x99, 0x5E, 0x09, 0xE8
            };

            auto deflater = Compress::Zlib({ data_bytes, 8 * 5 });
            auto deflated = deflater.decompress();
            auto decompressed = String((const char*)deflated.data(), deflated.size());

            if (decompressed.equals_ignoring_case("This is a simple text file :)")) {
                printf("Test PASSED");
                return 0;
            } else {
                printf("Test FAILED");
                return 1;
            }
        }
    }

    printf("Unknown arguments passed to test!");
    return 1;
}
