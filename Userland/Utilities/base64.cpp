/*
 * Copyright (c) 2020, Tom Lebreux <tomlebreux@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Base64.h>
#include <AK/ByteBuffer.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    bool decode = false;
    const char* filepath = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(decode, "Decode data", "decode", 'd');
    args_parser.add_positional_argument(filepath, "", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    ByteBuffer buffer;
    if (filepath == nullptr || strcmp(filepath, "-") == 0) {
        auto file = Core::File::construct();
        bool success = file->open(
            STDIN_FILENO,
            Core::OpenMode::ReadOnly,
            Core::File::ShouldCloseFileDescriptor::Yes);
        VERIFY(success);
        buffer = file->read_all();
    } else {
        auto result = Core::File::open(filepath, Core::OpenMode::ReadOnly);
        VERIFY(!result.is_error());
        auto file = result.value();
        buffer = file->read_all();
    }

    TRY(Core::System::pledge("stdio"));

    if (decode) {
        auto decoded = TRY(decode_base64(StringView(buffer)));
        fwrite(decoded.data(), sizeof(u8), decoded.size(), stdout);
        return 0;
    }

    auto encoded = encode_base64(buffer);
    outln("{}", encoded);
    return 0;
}
