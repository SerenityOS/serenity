/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibTextCodec/Decoder.h>
#include <LibTextCodec/Encoder.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;

    StringView in_path = "-"sv;
    args_parser.add_positional_argument(in_path, "Path to input file (reads STDIN if this is omitted)", "FILE", Core::ArgsParser::Required::No);

    StringView from = "utf-8"sv;
    args_parser.add_option(from, "Source encoding (default utf-8)", "from", 'f', "ENCODING");

    StringView to = "utf-8"sv;
    args_parser.add_option(to, "Destination encoding (default utf-8)", "to", 't', "ENCODING");

    args_parser.parse(arguments);

    auto decoder = TextCodec::decoder_for(from);
    if (!decoder.has_value()) {
        warnln("Unknown source encoding '{}'", from);
        return 1;
    }

    auto encoder = TextCodec::encoder_for(to);
    if (!encoder.has_value()) {
        warnln("Unknown destination encoding '{}'", to);
        return 1;
    }

    auto file = TRY(Core::File::open_file_or_standard_stream(in_path, Core::File::OpenMode::Read));
    auto input = TRY(file->read_until_eof());

    auto decoded = TRY(decoder->to_utf8(input));

    TRY(encoder->process(
        Utf8View(decoded),

        [](u8 byte) -> ErrorOr<void> {
            out("{}", static_cast<char>(byte));
            return {};
        },

        [](u32) -> ErrorOr<void> {
            return Error::from_string_literal("failure during conversion");
        }));

    return 0;
}
