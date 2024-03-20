/*
 * Copyright (c) 2020, Tom Lebreux <tomlebreux@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
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

    Variant<Empty, ByteBuffer, NonnullOwnPtr<Core::MappedFile>> buffer_or_file;
    ReadonlyBytes input_bytes;

    if (filepath.is_empty() || filepath == "-"sv) {
        auto file = TRY(Core::File::standard_input());
        buffer_or_file = TRY(file->read_until_eof());
        input_bytes = buffer_or_file.get<ByteBuffer>();
    } else if (TRY(Core::System::stat(filepath)).st_size > 0) {
        buffer_or_file = TRY(Core::MappedFile::map(filepath));
        input_bytes = buffer_or_file.get<NonnullOwnPtr<Core::MappedFile>>()->bytes();
    }

    TRY(Core::System::pledge("stdio"));

    if (decode) {
        auto decoded = TRY(decode_base64(input_bytes));
        out("{}", StringView(decoded.bytes()));
        return 0;
    }

    auto encoded = TRY(encode_base64(input_bytes));

    if (maybe_column.has_value() && *maybe_column > 0) {
        print_wrapped_output(*maybe_column, encoded);
        return 0;
    }

    outln("{}", encoded);
    return 0;
}
