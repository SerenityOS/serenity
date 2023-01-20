/*
 * Copyright (c) 2021, Valtteri Koskivuori <vkoskiv@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibCompress/Gzip.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/FileStream.h>
#include <LibCore/MappedFile.h>
#include <LibCore/MimeData.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibELF/Image.h>
#include <LibELF/Validation.h>
#include <LibGfx/ImageDecoder.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

static Optional<DeprecatedString> description_only(DeprecatedString description, [[maybe_unused]] DeprecatedString const& path)
{
    return description;
}

// FIXME: Ideally Gfx::ImageDecoder could tell us the image type directly.
static Optional<DeprecatedString> image_details(DeprecatedString const& description, DeprecatedString const& path)
{
    auto file_or_error = Core::MappedFile::map(path);
    if (file_or_error.is_error())
        return {};

    auto& mapped_file = *file_or_error.value();
    auto mime_type = Core::guess_mime_type_based_on_filename(path);
    auto image_decoder = Gfx::ImageDecoder::try_create_for_raw_bytes(mapped_file.bytes(), mime_type);
    if (!image_decoder)
        return {};

    return DeprecatedString::formatted("{}, {} x {}", description, image_decoder->width(), image_decoder->height());
}

static Optional<DeprecatedString> gzip_details(DeprecatedString description, DeprecatedString const& path)
{
    auto file_or_error = Core::MappedFile::map(path);
    if (file_or_error.is_error())
        return {};

    auto& mapped_file = *file_or_error.value();
    if (!Compress::GzipDecompressor::is_likely_compressed(mapped_file.bytes()))
        return {};

    auto gzip_details = Compress::GzipDecompressor::describe_header(mapped_file.bytes());
    if (!gzip_details.has_value())
        return {};

    return DeprecatedString::formatted("{}, {}", description, gzip_details.value());
}

static Optional<DeprecatedString> elf_details(DeprecatedString description, DeprecatedString const& path)
{
    auto file_or_error = Core::MappedFile::map(path);
    if (file_or_error.is_error())
        return {};
    auto& mapped_file = *file_or_error.value();
    auto elf_data = mapped_file.bytes();
    ELF::Image elf_image(elf_data);
    if (!elf_image.is_valid())
        return {};

    StringBuilder interpreter_path_builder;
    auto result_or_error = ELF::validate_program_headers(*(const ElfW(Ehdr)*)elf_data.data(), elf_data.size(), elf_data, &interpreter_path_builder);
    if (result_or_error.is_error() || !result_or_error.value())
        return {};
    auto interpreter_path = interpreter_path_builder.string_view();

    auto& header = *reinterpret_cast<const ElfW(Ehdr)*>(elf_data.data());

    auto bitness = header.e_ident[EI_CLASS] == ELFCLASS64 ? "64" : "32";
    auto byteorder = header.e_ident[EI_DATA] == ELFDATA2LSB ? "LSB" : "MSB";

    bool is_dynamically_linked = !interpreter_path.is_empty();
    DeprecatedString dynamic_section = DeprecatedString::formatted(", dynamically linked, interpreter {}", interpreter_path);

    return DeprecatedString::formatted("{} {}-bit {} {}, {}, version {} ({}){}",
        description,
        bitness,
        byteorder,
        ELF::Image::object_file_type_to_string(header.e_type).value_or("(?)"sv),
        ELF::Image::object_machine_type_to_string(header.e_machine).value_or("(?)"sv),
        header.e_ident[EI_ABIVERSION],
        ELF::Image::object_abi_type_to_string(header.e_ident[EI_OSABI]).value_or("(?)"sv),
        is_dynamically_linked ? dynamic_section : "");
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
    __ENUMERATE_MIME_TYPE_DESCRIPTION("audio/flac", "FLAC audio", description_only)                                 \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("audio/midi", "MIDI notes", description_only)                                 \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("audio/mpeg", "MP3 audio", description_only)                                  \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("audio/wave", "WAVE audio", description_only)                                 \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/blender", "Blender project file", description_only)                    \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/elf", "ELF", elf_details)                                              \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/ext", "ext filesystem", description_only)                              \
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
    __ENUMERATE_MIME_TYPE_DESCRIPTION("image/x-qoi", "QOI image data", image_details)                               \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("text/markdown", "Markdown document", description_only)                       \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("text/x-shellscript", "POSIX shell script text executable", description_only)

static Optional<DeprecatedString> get_description_from_mime_type(DeprecatedString const& mime, DeprecatedString const& path)
{
#define __ENUMERATE_MIME_TYPE_DESCRIPTION(mime_type, description, details) \
    if (DeprecatedString(mime_type) == mime)                               \
        return details(DeprecatedString(description), path);
    ENUMERATE_MIME_TYPE_DESCRIPTIONS;
#undef __ENUMERATE_MIME_TYPE_DESCRIPTION
    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    Vector<DeprecatedString> paths;
    bool flag_mime_only = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Determine type of files");
    args_parser.add_option(flag_mime_only, "Only print mime type", "mime-type", 'I');
    args_parser.add_positional_argument(paths, "Files to identify", "files", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    bool all_ok = true;
    // Read accounts for longest possible offset + signature we currently match against.
    auto buffer = TRY(ByteBuffer::create_uninitialized(0x9006));

    for (auto const& path : paths) {
        auto file_or_error = Core::Stream::File::open(path, Core::Stream::OpenMode::Read);
        if (file_or_error.is_error()) {
            perror(path.characters());
            all_ok = false;
            continue;
        }
        auto file = file_or_error.release_value();

        struct stat file_stat = TRY(Core::System::lstat(path));

        auto file_size_in_bytes = file_stat.st_size;
        if (S_ISDIR(file_stat.st_mode)) {
            outln("{}: directory", path);
        } else if (!file_size_in_bytes) {
            outln("{}: empty", path);
        } else {
            auto bytes = TRY(file->read(buffer));
            auto file_name_guess = Core::guess_mime_type_based_on_filename(path);
            auto mime_type = Core::guess_mime_type_based_on_sniffed_bytes(bytes).value_or(file_name_guess);
            auto human_readable_description = get_description_from_mime_type(mime_type, DeprecatedString(path)).value_or(mime_type);
            outln("{}: {}", path, flag_mime_only ? mime_type : human_readable_description);
        }
    }

    return all_ok ? 0 : 1;
}
