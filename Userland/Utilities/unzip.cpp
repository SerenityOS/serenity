/*
 * Copyright (c) 2020, Andrés Vieira <anvieiravazquez@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/NumberFormat.h>
#include <AK/StringUtils.h>
#include <LibArchive/Zip.h>
#include <LibCompress/Deflate.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibCore/System.h>
#include <sys/stat.h>
#include <unistd.h>

static bool unpack_zip_member(Archive::ZipMember zip_member, bool quiet)
{
    if (zip_member.is_directory) {
        if (mkdir(zip_member.name.characters(), 0755) < 0) {
            perror("mkdir");
            return false;
        }
        if (!quiet)
            outln(" extracting: {}", zip_member.name);
        return true;
    }
    auto new_file = Core::File::construct(zip_member.name);
    if (!new_file->open(Core::OpenMode::WriteOnly)) {
        warnln("Can't write file {}: {}", zip_member.name, new_file->error_string());
        return false;
    }

    if (!quiet)
        outln(" extracting: {}", zip_member.name);

    // TODO: verify CRC32s match!
    switch (zip_member.compression_method) {
    case Archive::ZipCompressionMethod::Store: {
        if (!new_file->write(zip_member.compressed_data.data(), zip_member.compressed_data.size())) {
            warnln("Can't write file contents in {}: {}", zip_member.name, new_file->error_string());
            return false;
        }
        break;
    }
    case Archive::ZipCompressionMethod::Deflate: {
        auto decompressed_data = Compress::DeflateDecompressor::decompress_all(zip_member.compressed_data);
        if (!decompressed_data.has_value()) {
            warnln("Failed decompressing file {}", zip_member.name);
            return false;
        }
        if (decompressed_data.value().size() != zip_member.uncompressed_size) {
            warnln("Failed decompressing file {}", zip_member.name);
            return false;
        }
        if (!new_file->write(decompressed_data.value().data(), decompressed_data.value().size())) {
            warnln("Can't write file contents in {}: {}", zip_member.name, new_file->error_string());
            return false;
        }
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }

    if (!new_file->close()) {
        warnln("Can't close file {}: {}", zip_member.name, new_file->error_string());
        return false;
    }

    return true;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    const char* path;
    int map_size_limit = 32 * MiB;
    bool quiet { false };
    String output_directory_path;
    Vector<StringView> file_filters;

    Core::ArgsParser args_parser;
    args_parser.add_option(map_size_limit, "Maximum chunk size to map", "map-size-limit", 0, "size");
    args_parser.add_option(output_directory_path, "Directory to receive the archive content", "output-directory", 'd', "path");
    args_parser.add_option(quiet, "Be less verbose", "quiet", 'q');
    args_parser.add_positional_argument(path, "File to unzip", "path", Core::ArgsParser::Required::Yes);
    args_parser.add_positional_argument(file_filters, "Files or filters in the archive to extract", "files", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    String zip_file_path { path };

    struct stat st = TRY(Core::System::stat(zip_file_path));

    // FIXME: Map file chunk-by-chunk once we have mmap() with offset.
    //        This will require mapping some parts then unmapping them repeatedly,
    //        but it would be significantly faster and less syscall heavy than seek()/read() at every read.
    if (st.st_size >= map_size_limit) {
        warnln("unzip warning: Refusing to map file since it is larger than {}, pass '--map-size-limit {}' to get around this",
            human_readable_size(map_size_limit),
            round_up_to_power_of_two(st.st_size, 16));
        return 1;
    }

    RefPtr<Core::MappedFile> mapped_file;
    ReadonlyBytes input_bytes;
    if (st.st_size > 0) {
        mapped_file = TRY(Core::MappedFile::map(zip_file_path));
        input_bytes = mapped_file->bytes();
    }

    if (!quiet)
        warnln("Archive: {}", zip_file_path);

    auto zip_file = Archive::Zip::try_create(input_bytes);
    if (!zip_file.has_value()) {
        warnln("Invalid zip file {}", zip_file_path);
        return 1;
    }

    if (!output_directory_path.is_null()) {
        auto mkdir_error = Core::System::mkdir(output_directory_path, 0755);
        if (mkdir_error.is_error() && mkdir_error.error().code() != EEXIST)
            return mkdir_error.release_error();
        TRY(Core::System::chdir(output_directory_path));
    }

    auto success = zip_file->for_each_member([&](auto zip_member) {
        bool keep_file = false;

        if (!file_filters.is_empty()) {
            for (auto& filter : file_filters) {
                // Convert underscore wildcards (usual unzip convention) to question marks (as used by StringUtils)
                auto string_filter = filter.replace("_", "?", true);
                if (zip_member.name.matches(string_filter, CaseSensitivity::CaseSensitive)) {
                    keep_file = true;
                    break;
                }
            }
        } else {
            keep_file = true;
        }

        if (keep_file) {
            if (!unpack_zip_member(zip_member, quiet))
                return IterationDecision::Break;
        }

        return IterationDecision::Continue;
    });

    return success ? 0 : 1;
}
