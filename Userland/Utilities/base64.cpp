/*
 * Copyright (c) 2020, Tom Lebreux <tomlebreux@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    bool decode = false;
    StringView filepath = {};

    Core::ArgsParser args_parser;
    args_parser.add_option(decode, "Decode data", "decode", 'd');
    args_parser.add_positional_argument(filepath, "", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto file = TRY(Core::Stream::File::open_file_or_standard_stream(filepath, Core::Stream::OpenMode::Read));
    ByteBuffer buffer = TRY(file->read_until_eof());

    TRY(Core::System::pledge("stdio"));

    if (decode) {
        auto decoded = TRY(decode_base64(buffer));
        out("{}", StringView(decoded.bytes()));
        return 0;
    }

    auto encoded = TRY(encode_base64(buffer));
    outln("{}", encoded);
    return 0;
}
