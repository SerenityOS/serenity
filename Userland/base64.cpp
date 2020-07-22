/*
 * Copyright (c) 2020, Tom Lebreux <tomlebreux@hotmail.com>
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

#include <AK/Base64.h>
#include <AK/ByteBuffer.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    bool decode = false;
    const char* filepath = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(decode, "Decode data", "decode", 'd');
    args_parser.add_positional_argument(filepath, "", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    ByteBuffer buffer;
    if (filepath == nullptr || strcmp(filepath, "-") == 0) {
        auto file = Core::File::construct();
        bool success = file->open(
            STDIN_FILENO,
            Core::IODevice::OpenMode::ReadOnly,
            Core::File::ShouldCloseFileDescription::Yes);
        ASSERT(success);
        buffer = file->read_all();
    } else {
        auto result = Core::File::open(filepath, Core::IODevice::OpenMode::ReadOnly);
        ASSERT(!result.is_error());
        auto file = result.value();
        buffer = file->read_all();
    }

    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (decode) {
        auto decoded = decode_base64(StringView(buffer));
        printf("%s\n", String::copy(decoded).characters());
        return 0;
    }

    auto encoded = encode_base64(buffer);
    printf("%s\n", encoded.characters());
}
