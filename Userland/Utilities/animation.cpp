/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>
#include <LibGfx/ImageFormats/WebPWriter.h>

struct Options {
    StringView in_path;
    StringView out_path;
};

static ErrorOr<Options> parse_options(Main::Arguments arguments)
{
    Options options;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(options.in_path, "Path to input image file", "FILE");
    args_parser.add_option(options.out_path, "Path to output image file", "output", 'o', "FILE");
    args_parser.parse(arguments);

    if (options.out_path.is_empty())
        return Error::from_string_view("-o is required "sv);

    return options;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Options options = TRY(parse_options(arguments));

    // FIXME: Allow multiple single frames as input too, and allow manually setting their duration.

    auto file = TRY(Core::MappedFile::map(options.in_path));
    auto decoder = TRY(Gfx::ImageDecoder::try_create_for_raw_bytes(file->bytes()));
    if (!decoder)
        return Error::from_string_view("Could not find decoder for input file"sv);

    auto output_file = TRY(Core::File::open(options.out_path, Core::File::OpenMode::Write));
    auto output_stream = TRY(Core::OutputBufferedFile::create(move(output_file)));

    auto animation_writer = TRY(Gfx::WebPWriter::start_encoding_animation(*output_stream, decoder->size(), decoder->loop_count()));

    for (size_t i = 0; i < decoder->frame_count(); ++i) {
        auto frame = TRY(decoder->frame(i));
        TRY(animation_writer->add_frame(*frame.image, frame.duration));
    }

    return 0;
}
