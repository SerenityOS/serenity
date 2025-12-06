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

    bool quiet = false;
    args_parser.add_option(quiet, "Only set exit code, print no output", "quiet", {});

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

    u64 number_of_differences = 0;
    int first_different_x = 0;
    int first_different_y = 0;
    u8 max_error_r = 0;
    u8 max_error_g = 0;
    u8 max_error_b = 0;
    u8 max_error = 0;
    int max_error_x = 0;
    int max_error_y = 0;
    u64 total_error_r = 0;
    u64 total_error_g = 0;
    u64 total_error_b = 0;
    for (int y = 0; y < first_image->physical_height(); ++y) {
        for (int x = 0; x < first_image->physical_width(); ++x) {
            auto first_pixel = first_image->get_pixel(x, y);
            auto second_pixel = second_image->get_pixel(x, y);
            if (first_pixel != second_pixel) {
                if (quiet)
                    return 1;
                if (number_of_differences == 0) {
                    first_different_x = x;
                    first_different_y = y;
                }
                auto error_r = abs((int)first_pixel.red() - (int)second_pixel.red());
                auto error_g = abs((int)first_pixel.green() - (int)second_pixel.green());
                auto error_b = abs((int)first_pixel.blue() - (int)second_pixel.blue());
                max_error_r = max(max_error_r, error_r);
                max_error_g = max(max_error_g, error_g);
                max_error_b = max(max_error_b, error_b);
                auto pixel_max_error = max(max(error_r, error_g), error_b);
                if (pixel_max_error > max_error) {
                    max_error = pixel_max_error;
                    max_error_x = x;
                    max_error_y = y;
                }
                total_error_r += error_r;
                total_error_g += error_g;
                total_error_b += error_b;
                number_of_differences++;
            }
        }
    }
    if (!quiet && number_of_differences > 0) {
        u64 number_of_pixels = first_image->physical_width() * first_image->physical_height();
        warnln("number of differing pixels: {} ({:.2f}%)", number_of_differences, (100.0 * number_of_differences) / number_of_pixels);
        warnln("max error R: {:4}, G: {:4}, B: {:4}", max_error_r, max_error_g, max_error_b);
        warnln("avg error R: {:.2f}, G: {:.2f}, B: {:.2f}",
            (double)total_error_r / number_of_pixels,
            (double)total_error_g / number_of_pixels,
            (double)total_error_b / number_of_pixels);
        warnln("max error at ({}, {}): {} vs {}", max_error_x, max_error_y,
            first_image->get_pixel(max_error_x, max_error_y), second_image->get_pixel(max_error_x, max_error_y));
        warnln("first difference at ({}, {}): {} vs {}", first_different_x, first_different_y,
            first_image->get_pixel(first_different_x, first_different_y), second_image->get_pixel(first_different_x, first_different_y));
    }

    return number_of_differences > 0 ? 1 : 0;
}
