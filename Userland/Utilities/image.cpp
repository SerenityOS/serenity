/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibGfx/BMPWriter.h>
#include <LibGfx/ImageDecoder.h>
#include <LibGfx/PNGWriter.h>
#include <LibGfx/PortableFormatWriter.h>
#include <LibGfx/QOIWriter.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;

    StringView in_path;
    args_parser.add_positional_argument(in_path, "Path to input image file", "FILE");

    StringView out_path;
    args_parser.add_option(out_path, "Path to output image file", "output", 'o', "FILE");

    bool ppm_ascii;
    args_parser.add_option(ppm_ascii, "Convert to a PPM in ASCII", "ppm-ascii", {});

    args_parser.parse(arguments);

    if (out_path.is_empty()) {
        warnln("-o is required");
        return 1;
    }

    auto file = TRY(Core::MappedFile::map(in_path));
    auto decoder = Gfx::ImageDecoder::try_create_for_raw_bytes(file->bytes());

    // This uses ImageDecoder instead of Bitmap::load_from_file() to have more control
    // over selecting a frame, access color profile data, and so on in the future.
    auto frame = TRY(decoder->frame(0)).image;
    Optional<ReadonlyBytes> icc_data = TRY(decoder->icc_data());

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
