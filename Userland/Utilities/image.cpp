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

    bool ppm_ascii;
    args_parser.add_option(ppm_ascii, "Convert to a PPM in ASCII", "ppm-ascii", {});

    StringView assign_color_profile_path;
    args_parser.add_option(assign_color_profile_path, "Load color profile from file and assign it to output image", "assign-color-profile", {}, "FILE");

    StringView convert_color_profile_path;
    args_parser.add_option(convert_color_profile_path, "Load color profile from file and convert output image from current profile to loaded profile", "convert-to-color-profile", {}, "FILE");

    bool strip_color_profile;
    args_parser.add_option(strip_color_profile, "Do not write color profile to output", "strip-color-profile", {});

    args_parser.parse(arguments);

    if (out_path.is_empty()) {
        warnln("-o is required");
        return 1;
    }

    auto file = TRY(Core::MappedFile::map(in_path));
    auto decoder = Gfx::ImageDecoder::try_create_for_raw_bytes(file->bytes());
    if (!decoder) {
        warnln("Failed to decode input file '{}'", in_path);
        return 1;
    }

    // This uses ImageDecoder instead of Bitmap::load_from_file() to have more control
    // over selecting a frame, access color profile data, and so on in the future.
    auto frame = TRY(decoder->frame(0)).image;
    Optional<ReadonlyBytes> icc_data = TRY(decoder->icc_data());

    RefPtr<Core::MappedFile> icc_file;
    if (!assign_color_profile_path.is_empty()) {
        icc_file = TRY(Core::MappedFile::map(assign_color_profile_path));
        icc_data = icc_file->bytes();
    }

    if (!convert_color_profile_path.is_empty()) {
        if (!icc_data.has_value()) {
            warnln("No source color space embedded in image. Pass one with --assign-color-profile.");
            return 1;
        }

        auto source_icc_file = icc_file;
        auto source_icc_data = icc_data.value();
        icc_file = TRY(Core::MappedFile::map(convert_color_profile_path));
        icc_data = icc_file->bytes();

        auto source_profile = TRY(Gfx::ICC::Profile::try_load_from_externally_owned_memory(source_icc_data));
        auto destination_profile = TRY(Gfx::ICC::Profile::try_load_from_externally_owned_memory(icc_file->bytes()));
        TRY(destination_profile->convert_image(*frame, *source_profile));
    }

    if (strip_color_profile)
        icc_data.clear();

    ByteBuffer bytes;
    if (out_path.ends_with(".bmp"sv, CaseSensitivity::CaseInsensitive)) {
        bytes = TRY(Gfx::BMPWriter::encode(*frame, { .icc_data = icc_data }));
    } else if (out_path.ends_with(".png"sv, CaseSensitivity::CaseInsensitive)) {
        bytes = TRY(Gfx::PNGWriter::encode(*frame, { .icc_data = icc_data }));
    } else if (out_path.ends_with(".ppm"sv, CaseSensitivity::CaseInsensitive)) {
        auto const format = ppm_ascii ? Gfx::PortableFormatWriter::Options::Format::ASCII : Gfx::PortableFormatWriter::Options::Format::Raw;
        bytes = TRY(Gfx::PortableFormatWriter::encode(*frame, { .format = format }));
    } else if (out_path.ends_with(".qoi"sv, CaseSensitivity::CaseInsensitive)) {
        bytes = TRY(Gfx::QOIWriter::encode(*frame));
    } else {
        warnln("can only write .bmp, .png, .ppm, and .qoi");
        return 1;
    }

    auto output_stream = TRY(Core::File::open(out_path, Core::File::OpenMode::Write));
    TRY(output_stream->write_until_depleted(bytes));

    return 0;
}
