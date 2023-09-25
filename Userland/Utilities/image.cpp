/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibGfx/ICC/Profile.h>
#include <LibGfx/ImageFormats/BMPWriter.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>
#include <LibGfx/ImageFormats/JPEGWriter.h>
#include <LibGfx/ImageFormats/PNGWriter.h>
#include <LibGfx/ImageFormats/PortableFormatWriter.h>
#include <LibGfx/ImageFormats/QOIWriter.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;

    StringView in_path;
    args_parser.add_positional_argument(in_path, "Path to input image file", "FILE");

    StringView out_path;
    args_parser.add_option(out_path, "Path to output image file", "output", 'o', "FILE");

    bool no_output = false;
    args_parser.add_option(no_output, "Do not write output (only useful for benchmarking image decoding)", "no-output", {});

    int frame_index = 0;
    args_parser.add_option(frame_index, "Which frame of a multi-frame input image (0-based)", "frame-index", {}, "INDEX");

    bool move_alpha_to_rgb = false;
    args_parser.add_option(move_alpha_to_rgb, "Copy alpha channel to rgb, clear alpha", "move-alpha-to-rgb", {});

    bool ppm_ascii = false;
    args_parser.add_option(ppm_ascii, "Convert to a PPM in ASCII", "ppm-ascii", {});

    bool strip_alpha = false;
    args_parser.add_option(strip_alpha, "Remove alpha channel", "strip-alpha", {});

    StringView assign_color_profile_path;
    args_parser.add_option(assign_color_profile_path, "Load color profile from file and assign it to output image", "assign-color-profile", {}, "FILE");

    StringView convert_color_profile_path;
    args_parser.add_option(convert_color_profile_path, "Load color profile from file and convert output image from current profile to loaded profile", "convert-to-color-profile", {}, "FILE");

    bool strip_color_profile = false;
    args_parser.add_option(strip_color_profile, "Do not write color profile to output", "strip-color-profile", {});

    u8 quality = 75;
    args_parser.add_option(quality, "Quality used for the JPEG encoder, the default value is 75 on a scale from 0 to 100", "quality", {}, {});

    args_parser.parse(arguments);

    if (out_path.is_empty() ^ no_output) {
        warnln("exactly one of -o or --no-output is required");
        return 1;
    }

    auto file = TRY(Core::MappedFile::map(in_path));
    auto decoder = Gfx::ImageDecoder::try_create_for_raw_bytes(file->bytes());
    if (!decoder) {
        warnln("Failed to decode input file '{}'", in_path);
        return 1;
    }

    auto frame = TRY(decoder->frame(frame_index)).image;

    if (move_alpha_to_rgb) {
        switch (frame->format()) {
        case Gfx::BitmapFormat::Invalid:
        case Gfx::BitmapFormat::Indexed1:
        case Gfx::BitmapFormat::Indexed2:
        case Gfx::BitmapFormat::Indexed4:
        case Gfx::BitmapFormat::Indexed8:
            warnln("Can't --strip-alpha with indexed or invalid bitmaps");
            return 1;
        case Gfx::BitmapFormat::RGBA8888:
            // No image decoder currently produces bitmaps with this format.
            // If that ever changes, preferrably fix the image decoder to use BGRA8888 instead :)
            // If there's a good reason for not doing that, implement support for this, I suppose.
            warnln("Can't --strip-alpha not implemented for RGBA8888");
            return 1;
        case Gfx::BitmapFormat::BGRA8888:
        case Gfx::BitmapFormat::BGRx8888:
            // FIXME: If BitmapFormat::Gray8 existed (and image encoders made use of it to write grayscale images), we could use it here.
            for (auto& pixel : *frame) {
                u8 alpha = pixel >> 24;
                pixel = 0xff000000 | (alpha << 16) | (alpha << 8) | alpha;
            }
        }
    }

    if (strip_alpha) {
        switch (frame->format()) {
        case Gfx::BitmapFormat::Invalid:
        case Gfx::BitmapFormat::Indexed1:
        case Gfx::BitmapFormat::Indexed2:
        case Gfx::BitmapFormat::Indexed4:
        case Gfx::BitmapFormat::Indexed8:
            warnln("Can't --strip-alpha with indexed or invalid bitmaps");
            return 1;
        case Gfx::BitmapFormat::RGBA8888:
            // No image decoder currently produces bitmaps with this format.
            // If that ever changes, preferrably fix the image decoder to use BGRA8888 instead :)
            // If there's a good reason for not doing that, implement support for this, I suppose.
            warnln("Can't --strip-alpha not implemented for RGBA8888");
            return 1;
        case Gfx::BitmapFormat::BGRA8888:
        case Gfx::BitmapFormat::BGRx8888:
            frame->strip_alpha_channel();
        }
    }

    Optional<ReadonlyBytes> icc_data = TRY(decoder->icc_data());

    OwnPtr<Core::MappedFile> icc_file;
    if (!assign_color_profile_path.is_empty()) {
        icc_file = TRY(Core::MappedFile::map(assign_color_profile_path));
        icc_data = icc_file->bytes();
    }

    if (!convert_color_profile_path.is_empty()) {
        if (!icc_data.has_value()) {
            warnln("No source color space embedded in image. Pass one with --assign-color-profile.");
            return 1;
        }

        auto source_icc_file = move(icc_file);
        auto source_icc_data = icc_data.value();
        icc_file = TRY(Core::MappedFile::map(convert_color_profile_path));
        icc_data = icc_file->bytes();

        auto source_profile = TRY(Gfx::ICC::Profile::try_load_from_externally_owned_memory(source_icc_data));
        auto destination_profile = TRY(Gfx::ICC::Profile::try_load_from_externally_owned_memory(icc_file->bytes()));
        TRY(destination_profile->convert_image(*frame, *source_profile));
    }

    if (strip_color_profile)
        icc_data.clear();

    if (no_output)
        return 0;

    auto output_stream = TRY(Core::File::open(out_path, Core::File::OpenMode::Write));
    auto buffered_stream = TRY(Core::OutputBufferedFile::create(move(output_stream)));

    ByteBuffer bytes;
    if (out_path.ends_with(".bmp"sv, CaseSensitivity::CaseInsensitive)) {
        bytes = TRY(Gfx::BMPWriter::encode(*frame, { .icc_data = icc_data }));
    } else if (out_path.ends_with(".png"sv, CaseSensitivity::CaseInsensitive)) {
        bytes = TRY(Gfx::PNGWriter::encode(*frame, { .icc_data = icc_data }));
    } else if (out_path.ends_with(".ppm"sv, CaseSensitivity::CaseInsensitive)) {
        auto const format = ppm_ascii ? Gfx::PortableFormatWriter::Options::Format::ASCII : Gfx::PortableFormatWriter::Options::Format::Raw;
        TRY(Gfx::PortableFormatWriter::encode(*buffered_stream, *frame, { .format = format }));
        return 0;
    } else if (out_path.ends_with(".jpg"sv, CaseSensitivity::CaseInsensitive) || out_path.ends_with(".jpeg"sv, CaseSensitivity::CaseInsensitive)) {
        TRY(Gfx::JPEGWriter::encode(*buffered_stream, *frame, { .quality = quality }));
        return 0;
    } else if (out_path.ends_with(".qoi"sv, CaseSensitivity::CaseInsensitive)) {
        bytes = TRY(Gfx::QOIWriter::encode(*frame));
    } else {
        warnln("can only write .bmp, .png, .ppm, and .qoi");
        return 1;
    }

    TRY(buffered_stream->write_until_depleted(bytes));

    return 0;
}
