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
#include <LibGfx/ImageFormats/PNGWriter.h>
#include <LibGfx/ImageFormats/WebPWriter.h>

static ErrorOr<RefPtr<Gfx::Bitmap>> load_image(StringView path)
{
    auto file = TRY(Core::MappedFile::map(path));
    auto guessed_mime_type = Core::guess_mime_type_based_on_filename(path);
    auto decoder = TRY(Gfx::ImageDecoder::try_create_for_raw_bytes(file->bytes(), guessed_mime_type));
    if (!decoder)
        return Error::from_string_view("Could not find decoder for input file"sv);
    return TRY(decoder->frame(0)).image;
}

static ErrorOr<void> save_image(NonnullRefPtr<Gfx::Bitmap> bitmap, StringView out_path)
{
    if (!out_path.ends_with(".png"sv, CaseSensitivity::CaseInsensitive) && !out_path.ends_with(".webp"sv, CaseSensitivity::CaseInsensitive))
        return Error::from_string_view("can only save to .png and .webp files"sv);

    auto output_stream = TRY(Core::File::open(out_path, Core::File::OpenMode::Write));
    auto buffered_stream = TRY(Core::OutputBufferedFile::create(move(output_stream)));

    if (out_path.ends_with(".png"sv, CaseSensitivity::CaseInsensitive))
        return Gfx::PNGWriter::encode(*buffered_stream, *bitmap);

    VERIFY(out_path.ends_with(".webp"sv, CaseSensitivity::CaseInsensitive));
    return Gfx::WebPWriter::encode(*buffered_stream, *bitmap);
}

static ErrorOr<NonnullRefPtr<Gfx::Bitmap>> make_diff_image(NonnullRefPtr<Gfx::Bitmap> first_image, NonnullRefPtr<Gfx::Bitmap> second_image)
{
    VERIFY(first_image->size() == second_image->size());

    auto diff_image = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, first_image->size()));

    for (int y = 0; y < first_image->height(); ++y) {
        for (int x = 0; x < first_image->width(); ++x) {
            auto first_pixel = first_image->get_pixel<Gfx::StorageFormat::BGRA8888>(x, y);
            auto second_pixel = second_image->get_pixel<Gfx::StorageFormat::BGRA8888>(x, y);
            if (first_pixel == second_pixel) {
                diff_image->set_pixel(x, y, first_pixel.interpolate(Gfx::Color::White, 0.5f));
            } else {
                diff_image->set_pixel(x, y, Gfx::Color::Red);
            }
        }
    }

    return diff_image;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;

    StringView write_diff_image_path;
    args_parser.add_option(write_diff_image_path, "Write image that highlights differing pixels", "write-diff-image", {}, "FILE");

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

    if (!write_diff_image_path.is_empty()) {
        auto diff_image = TRY(make_diff_image(*first_image, *second_image));
        TRY(save_image(diff_image, write_diff_image_path));
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
