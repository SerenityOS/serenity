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

static void print_wrapped_output(size_t column, StringView encoded)
{
    VERIFY(column > 0);

    while (!encoded.is_empty()) {
        auto segment_length = min(column, encoded.length());

        outln("{}", encoded.substring_view(0, segment_length));
        encoded = encoded.substring_view(segment_length);
    }
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    bool decode = false;
    Optional<size_t> maybe_column;
    StringView filepath = {};

    Core::ArgsParser args_parser;
    args_parser.add_option(decode, "Decode data", "decode", 'd');
    args_parser.add_option(maybe_column, "When encoding, wrap output after column characters", "wrap", 'w', "column");
    args_parser.add_positional_argument(filepath, "", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto file = TRY(Core::File::open_file_or_standard_stream(filepath, Core::File::OpenMode::Read));
    ByteBuffer buffer = TRY(file->read_until_eof());

    TRY(Core::System::pledge("stdio"));

    if (decode) {
        auto decoded = TRY(decode_base64(buffer));
        out("{}", StringView(decoded.bytes()));
        return 0;
    }

    auto encoded = TRY(encode_base64(buffer));

    if (maybe_column.has_value() && *maybe_column > 0) {
        print_wrapped_output(*maybe_column, encoded);
        return 0;
    }

    outln("{}", encoded);
    return 0;
}
