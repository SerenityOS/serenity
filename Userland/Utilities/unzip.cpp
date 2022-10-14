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
#include <LibCore/Directory.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibCore/System.h>
#include <LibCrypto/Checksum/CRC32.h>
#include <sys/stat.h>

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
    MUST(Core::Directory::create(LexicalPath(zip_member.name).parent(), Core::Directory::CreateDirectories::Yes));
    auto new_file = Core::File::construct(zip_member.name);
    if (!new_file->open(Core::OpenMode::WriteOnly)) {
        warnln("Can't write file {}: {}", zip_member.name, new_file->error_string());
        return false;
    }

    if (!quiet)
        outln(" extracting: {}", zip_member.name);

    Crypto::Checksum::CRC32 checksum;
    switch (zip_member.compression_method) {
    case Archive::ZipCompressionMethod::Store: {
        if (!new_file->write(zip_member.compressed_data.data(), zip_member.compressed_data.size())) {
            warnln("Can't write file contents in {}: {}", zip_member.name, new_file->error_string());
            return false;
        }
        checksum.update({ zip_member.compressed_data.data(), zip_member.compressed_data.size() });
        break;
    }
    case Archive::ZipCompressionMethod::Deflate: {
        auto decompressed_data = Compress::DeflateDecompressor::decompress_all(zip_member.compressed_data);
        if (decompressed_data.is_error()) {
            warnln("Failed decompressing file {}: {}", zip_member.name, decompressed_data.error());
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
        checksum.update({ decompressed_data.value().data(), decompressed_data.value().size() });
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }

    if (!new_file->close()) {
        warnln("Can't close file {}: {}", zip_member.name, new_file->error_string());
        return false;
    }

    if (checksum.digest() != zip_member.crc32) {
        warnln("Failed decompressing file {}: CRC32 mismatch", zip_member.name);
        MUST(Core::File::remove(zip_member.name, Core::File::RecursionMode::Disallowed));
        return false;
    }

    return true;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView zip_file_path;
    bool quiet { false };
    StringView output_directory_path;
    Vector<StringView> file_filters;

    Core::ArgsParser args_parser;
    args_parser.add_option(output_directory_path, "Directory to receive the archive content", "output-directory", 'd', "path");
    args_parser.add_option(quiet, "Be less verbose", "quiet", 'q');
    args_parser.add_positional_argument(zip_file_path, "File to unzip", "path", Core::ArgsParser::Required::Yes);
    args_parser.add_positional_argument(file_filters, "Files or filters in the archive to extract", "files", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    struct stat st = TRY(Core::System::stat(zip_file_path));

    // FIXME: Map file chunk-by-chunk once we have mmap() with offset.
    //        This will require mapping some parts then unmapping them repeatedly,
    //        but it would be significantly faster and less syscall heavy than seek()/read() at every read.
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
        TRY(Core::Directory::create(output_directory_path, Core::Directory::CreateDirectories::Yes));
        TRY(Core::System::chdir(output_directory_path));
    }

    auto success = zip_file->for_each_member([&](auto zip_member) {
        bool keep_file = false;

        if (!file_filters.is_empty()) {
            for (auto& filter : file_filters) {
                // Convert underscore wildcards (usual unzip convention) to question marks (as used by StringUtils)
                auto string_filter = filter.replace("_"sv, "?"sv, ReplaceMode::All);
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
