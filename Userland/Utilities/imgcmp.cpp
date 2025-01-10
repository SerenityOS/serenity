/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibCore/MimeData.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>

static ErrorOr<RefPtr<Gfx::Bitmap>> load_image(StringView path)
{
    auto file = TRY(Core::MappedFile::map(path));
    auto guessed_mime_type = Core::guess_mime_type_based_on_filename(path);
    auto decoder = TRY(Gfx::ImageDecoder::try_create_for_raw_bytes(file->bytes(), guessed_mime_type));
    if (!decoder)
        return Error::from_string_view("Could not find decoder for input file"sv);
    return TRY(decoder->frame(0)).image;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;

    StringView first_image_path;
    args_parser.add_positional_argument(first_image_path, "Path to first input image", "FILE1");

    StringView second_image_path;
    args_parser.add_positional_argument(second_image_path, "Path to second input image", "FILE2");

    args_parser.parse(arguments);

    auto first_image = TRY(load_image(first_image_path));
    auto second_image = TRY(load_image(second_image_path));

    if (first_image->physical_size() != second_image->physical_size()) {
        warnln("different dimensions, {} vs {}", first_image->physical_size(), second_image->physical_size());
        return 1;
    }

    for (int y = 0; y < first_image->physical_height(); ++y) {
        for (int x = 0; x < first_image->physical_width(); ++x) {
            auto first_pixel = first_image->get_pixel(x, y);
            auto second_pixel = second_image->get_pixel(x, y);
            if (first_pixel != second_pixel) {
                warnln("different pixel at ({}, {}), {} vs {}", x, y, first_pixel, second_pixel);
                return 1;
            }
        }
    }

    return 0;
}
