/*
 * Copyright (c) 2020, Andrés Vieira <anvieiravazquez@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/NumberFormat.h>
#include <LibArchive/Zip.h>
#include <LibCompress/Deflate.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
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

int main(int argc, char** argv)
{
    const char* path;
    int map_size_limit = 32 * MiB;
    bool quiet { false };
    String output_directory_path;

    Core::ArgsParser args_parser;
    args_parser.add_option(map_size_limit, "Maximum chunk size to map", "map-size-limit", 0, "size");
    args_parser.add_option(output_directory_path, "Directory to receive the archive content", "output-directory", 'd', "path");
    args_parser.add_option(quiet, "Be less verbose", "quiet", 'q');
    args_parser.add_positional_argument(path, "File to unzip", "path", Core::ArgsParser::Required::Yes);
    args_parser.parse(argc, argv);

    String zip_file_path { path };

    struct stat st;
    int rc = stat(zip_file_path.characters(), &st);
    if (rc < 0) {
        perror("stat");
        return 1;
    }

    // FIXME: Map file chunk-by-chunk once we have mmap() with offset.
    //        This will require mapping some parts then unmapping them repeatedly,
    //        but it would be significantly faster and less syscall heavy than seek()/read() at every read.
    if (st.st_size >= map_size_limit) {
        warnln("unzip warning: Refusing to map file since it is larger than {}, pass '--map-size-limit {}' to get around this",
            human_readable_size(map_size_limit).characters(),
            round_up_to_power_of_two(st.st_size, 16));
        return 1;
    }

    auto file_or_error = Core::MappedFile::map(zip_file_path);
    if (file_or_error.is_error()) {
        warnln("Failed to open {}: {}", zip_file_path, file_or_error.error());
        return 1;
    }
    auto& mapped_file = *file_or_error.value();

    if (!quiet)
        warnln("Archive: {}", zip_file_path);

    auto zip_file = Archive::Zip::try_create(mapped_file.bytes());
    if (!zip_file.has_value()) {
        warnln("Invalid zip file {}", zip_file_path);
        return 1;
    }

    if (!output_directory_path.is_null()) {
        rc = mkdir(output_directory_path.characters(), 0755);
        if (rc < 0 && errno != EEXIST) {
            perror("mkdir");
            return 1;
        }

        rc = chdir(output_directory_path.characters());
        if (rc < 0) {
            perror("chdir");
            return 1;
        }
    }

    auto success = zip_file->for_each_member([&](auto zip_member) {
        return unpack_zip_member(zip_member, quiet) ? IterationDecision::Continue : IterationDecision::Break;
    });

    return success ? 0 : 1;
}
