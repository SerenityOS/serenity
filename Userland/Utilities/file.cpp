/*
 * Copyright (c) 2021, Valtteri Koskivuori <vkoskiv@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MappedFile.h>
#include <AK/Vector.h>
#include <LibCompress/Gzip.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/FileStream.h>
#include <LibCore/MimeData.h>
#include <LibGfx/ImageDecoder.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

static Optional<String> description_only(String description, [[maybe_unused]] String const& path)
{
    return description;
}

// FIXME: Ideally Gfx::ImageDecoder could tell us the image type directly.
static Optional<String> image_details(String const& description, String const& path)
{
    auto file_or_error = MappedFile::map(path);
    if (file_or_error.is_error())
        return {};

    auto& mapped_file = *file_or_error.value();
    auto image_decoder = Gfx::ImageDecoder::try_create(mapped_file.bytes());
    if (!image_decoder)
        return {};

    return String::formatted("{}, {} x {}", description, image_decoder->width(), image_decoder->height());
}

static Optional<String> gzip_details(String description, String const& path)
{
    auto file_or_error = MappedFile::map(path);
    if (file_or_error.is_error())
        return {};

    auto& mapped_file = *file_or_error.value();
    if (!Compress::GzipDecompressor::is_likely_compressed(mapped_file.bytes()))
        return {};

    auto gzip_details = Compress::GzipDecompressor::describe_header(mapped_file.bytes());
    if (!gzip_details.has_value())
        return {};

    return String::formatted("{}, {}", description, gzip_details.value());
}

#define ENUMERATE_MIME_TYPE_DESCRIPTIONS                                                                            \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("application/gzip", "gzip compressed data", gzip_details)                     \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("application/javascript", "JavaScript source", description_only)              \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("application/json", "JSON data", description_only)                            \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("application/pdf", "PDF document", description_only)                          \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("application/rtf", "Rich text file", description_only)                        \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("application/tar", "tape archive", description_only)                          \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("application/wasm", "WebAssembly bytecode", description_only)                 \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("application/x-7z-compressed", "7-Zip archive", description_only)             \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("audio/midi", "MIDI sound", description_only)                                 \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/blender", "Blender project file", description_only)                    \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/ext", "ext filesystem", description_only)                              \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/flac", "FLAC audio", description_only)                                 \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/iso-9660", "ISO 9660 CD/DVD image", description_only)                  \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/isz", "Compressed ISO image", description_only)                        \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/lua-bytecode", "Lua bytecode", description_only)                       \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/matroska", "Matroska container", description_only)                     \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/nes-rom", "Nintendo Entertainment System ROM", description_only)       \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/qcow", "qcow file", description_only)                                  \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/raw-zlib", "raw zlib stream", description_only)                        \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/sqlite", "sqlite database", description_only)                          \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/win-31x-compressed", "Windows 3.1X compressed file", description_only) \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/win-95-compressed", "Windows 95 compressed file", description_only)    \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("image/bmp", "BMP image data", image_details)                                 \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("image/gif", "GIF image data", image_details)                                 \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("image/jpeg", "JPEG image data", image_details)                               \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("image/png", "PNG image data", image_details)                                 \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("image/x-portable-bitmap", "PBM image data", image_details)                   \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("image/x-portable-graymap", "PGM image data", image_details)                  \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("image/x-portable-pixmap", "PPM image data", image_details)                   \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("text/markdown", "Markdown document", description_only)                       \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("text/x-shellscript", "POSIX shell script text executable", description_only)

static Optional<String> get_description_from_mime_type(String const& mime, String const& path)
{
#define __ENUMERATE_MIME_TYPE_DESCRIPTION(mime_type, description, details) \
    if (String(mime_type) == mime)                                         \
        return details(String(description), path);
    ENUMERATE_MIME_TYPE_DESCRIPTIONS;
#undef __ENUMERATE_MIME_TYPE_DESCRIPTION
    return {};
}

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Vector<char const*> paths;
    bool flag_mime_only = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Determine type of files");
    args_parser.add_option(flag_mime_only, "Only print mime type", "mime-type", 'I');
    args_parser.add_positional_argument(paths, "Files to identify", "files", Core::ArgsParser::Required::Yes);
    args_parser.parse(argc, argv);

    bool all_ok = true;

    for (auto path : paths) {
        auto file = Core::File::construct(path);
        if (!file->open(Core::OpenMode::ReadOnly)) {
            perror(path);
            all_ok = false;
            continue;
        }

        struct stat file_stat;
        if (lstat(path, &file_stat) < 0) {
            perror("lstat");
            return 1;
        }

        auto file_size_in_bytes = file_stat.st_size;
        if (file->is_directory()) {
            outln("{}: directory", path);
        } else if (!file_size_in_bytes) {
            outln("{}: empty", path);
        } else {
            // Read accounts for longest possible offset + signature we currently match against.
            auto bytes = file->read(0x9006);
            auto file_name_guess = Core::guess_mime_type_based_on_filename(path);
            auto mime_type = Core::guess_mime_type_based_on_sniffed_bytes(bytes.bytes()).value_or(file_name_guess);
            auto human_readable_description = get_description_from_mime_type(mime_type, String(path)).value_or(mime_type);
            outln("{}: {}", path, flag_mime_only ? mime_type : human_readable_description);
        }
    }

    return all_ok ? 0 : 1;
}
