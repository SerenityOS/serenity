/*
 * Copyright (c) 2020, Tom Lebreux <tomlebreux@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
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

    ByteBuffer buffer;
    if (filepath == nullptr || filepath == "-") {
        buffer = Core::File::standard_input()->read_all();
    } else {
        auto file = TRY(Core::File::open(filepath, Core::OpenMode::ReadOnly));
        buffer = file->read_all();
    }

    TRY(Core::System::pledge("stdio"));

    if (decode) {
        auto decoded = TRY(decode_base64(StringView(buffer)));
        Core::File::standard_output()->write(decoded.data(), decoded.size());
        return 0;
    }

    auto encoded = encode_base64(buffer);
    outln("{}", encoded);
    return 0;
}
