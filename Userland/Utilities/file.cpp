/*
 * Copyright (c) 2021, Valtteri Koskivuori <vkoskiv@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCompress/Gzip.h>
#include <LibCore/ArgsParser.h>
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

static ErrorOr<Optional<String>> description_only(StringView description, [[maybe_unused]] StringView path)
{
    return String::from_utf8(description);
}

// FIXME: Ideally Gfx::ImageDecoder could tell us the image type directly.
static ErrorOr<Optional<String>> image_details(StringView description, StringView path)
{
    auto mapped_file = TRY(Core::MappedFile::map(path));
    auto mime_type = Core::guess_mime_type_based_on_filename(path);
    auto image_decoder = Gfx::ImageDecoder::try_create_for_raw_bytes(mapped_file->bytes(), mime_type);
    if (!image_decoder)
        return OptionalNone {};

    return TRY(String::formatted("{}, {} x {}", description, image_decoder->width(), image_decoder->height()));
}

static ErrorOr<Optional<String>> gzip_details(StringView description, StringView path)
{
    auto mapped_file = TRY(Core::MappedFile::map(path));
    if (!Compress::GzipDecompressor::is_likely_compressed(mapped_file->bytes()))
        return OptionalNone {};

    auto gzip_details = Compress::GzipDecompressor::describe_header(mapped_file->bytes());
    if (!gzip_details.has_value())
        return OptionalNone {};

    return TRY(String::formatted("{}, {}", description, gzip_details.value()));
}

static ErrorOr<Optional<String>> elf_details(StringView description, StringView path)
{
    auto mapped_file = TRY(Core::MappedFile::map(path));
    auto elf_data = mapped_file->bytes();
    ELF::Image elf_image(elf_data);
    if (!elf_image.is_valid())
        return OptionalNone {};

    StringBuilder interpreter_path_builder;
    auto result_or_error = ELF::validate_program_headers(*(const ElfW(Ehdr)*)elf_data.data(), elf_data.size(), elf_data, &interpreter_path_builder);
    if (result_or_error.is_error() || !result_or_error.value())
        return OptionalNone {};
    auto interpreter_path = interpreter_path_builder.string_view();

    auto& header = *reinterpret_cast<const ElfW(Ehdr)*>(elf_data.data());

    auto bitness = header.e_ident[EI_CLASS] == ELFCLASS64 ? "64" : "32";
    auto byteorder = header.e_ident[EI_DATA] == ELFDATA2LSB ? "LSB" : "MSB";

    bool is_dynamically_linked = !interpreter_path.is_empty();
    auto dynamic_section = TRY(String::formatted(", dynamically linked, interpreter {}", interpreter_path));

    return TRY(String::formatted("{} {}-bit {} {}, {}, version {} ({}){}",
        description,
        bitness,
        byteorder,
        ELF::Image::object_file_type_to_string(header.e_type).value_or("(?)"sv),
        ELF::Image::object_machine_type_to_string(header.e_machine).value_or("(?)"sv),
        header.e_ident[EI_ABIVERSION],
        ELF::Image::object_abi_type_to_string(header.e_ident[EI_OSABI]).value_or("(?)"sv),
        is_dynamically_linked ? dynamic_section : String {}));
}

#define ENUMERATE_MIME_TYPE_DESCRIPTIONS                                                                                \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("application/gzip"sv, "gzip compressed data"sv, gzip_details)                     \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("application/javascript"sv, "JavaScript source"sv, description_only)              \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("application/json"sv, "JSON data"sv, description_only)                            \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("application/pdf"sv, "PDF document"sv, description_only)                          \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("application/rtf"sv, "Rich text file"sv, description_only)                        \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("application/tar"sv, "tape archive"sv, description_only)                          \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("application/wasm"sv, "WebAssembly bytecode"sv, description_only)                 \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("application/x-7z-compressed"sv, "7-Zip archive"sv, description_only)             \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("audio/flac"sv, "FLAC audio"sv, description_only)                                 \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("audio/midi"sv, "MIDI notes"sv, description_only)                                 \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("audio/mpeg"sv, "MP3 audio"sv, description_only)                                  \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("audio/wave"sv, "WAVE audio"sv, description_only)                                 \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/blender"sv, "Blender project file"sv, description_only)                    \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/elf"sv, "ELF"sv, elf_details)                                              \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/ext"sv, "ext filesystem"sv, description_only)                              \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/iso-9660"sv, "ISO 9660 CD/DVD image"sv, description_only)                  \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/isz"sv, "Compressed ISO image"sv, description_only)                        \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/lua-bytecode"sv, "Lua bytecode"sv, description_only)                       \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/matroska"sv, "Matroska container"sv, description_only)                     \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/nes-rom"sv, "Nintendo Entertainment System ROM"sv, description_only)       \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/qcow"sv, "qcow file"sv, description_only)                                  \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/raw-zlib"sv, "raw zlib stream"sv, description_only)                        \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/sqlite"sv, "sqlite database"sv, description_only)                          \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/win-31x-compressed"sv, "Windows 3.1X compressed file"sv, description_only) \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("extra/win-95-compressed"sv, "Windows 95 compressed file"sv, description_only)    \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("image/bmp"sv, "BMP image data"sv, image_details)                                 \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("image/gif"sv, "GIF image data"sv, image_details)                                 \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("image/jpeg"sv, "JPEG image data"sv, image_details)                               \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("image/png"sv, "PNG image data"sv, image_details)                                 \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("image/x-portable-bitmap"sv, "PBM image data"sv, image_details)                   \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("image/x-portable-graymap"sv, "PGM image data"sv, image_details)                  \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("image/x-portable-pixmap"sv, "PPM image data"sv, image_details)                   \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("image/x-qoi"sv, "QOI image data"sv, image_details)                               \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("text/markdown"sv, "Markdown document"sv, description_only)                       \
    __ENUMERATE_MIME_TYPE_DESCRIPTION("text/x-shellscript"sv, "POSIX shell script text executable"sv, description_only)

static ErrorOr<Optional<String>> get_description_from_mime_type(StringView mime, StringView path)
{
#define __ENUMERATE_MIME_TYPE_DESCRIPTION(mime_type, description, details) \
    if (mime_type == mime)                                                 \
        return details(description, path);
    ENUMERATE_MIME_TYPE_DESCRIPTIONS;
#undef __ENUMERATE_MIME_TYPE_DESCRIPTION
    return OptionalNone {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    Vector<StringView> paths;
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
            warnln("{}: {}", path, file_or_error.error());
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
            auto fallback = TRY(String::from_utf8(mime_type));
            auto human_readable_description = TRY(get_description_from_mime_type(mime_type, path)).value_or(fallback);
            outln("{}: {}", path, flag_mime_only ? mime_type : human_readable_description);
        }
    }

    return all_ok ? 0 : 1;
}
