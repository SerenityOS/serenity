/*
 * Copyright (c) 2020, Andrés Vieira <anvieiravazquez@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/DOSPackedTime.h>
#include <AK/NumberFormat.h>
#include <AK/StringUtils.h>
#include <LibArchive/Zip.h>
#include <LibCompress/Deflate.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/Directory.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibCore/System.h>
#include <LibCrypto/Checksum/CRC32.h>
#include <LibFileSystem/FileSystem.h>
#include <sys/stat.h>

static ErrorOr<void> adjust_modification_time(Archive::ZipMember const& zip_member)
{
    auto time = time_from_packed_dos(zip_member.modification_date, zip_member.modification_time);
    auto seconds = static_cast<time_t>(time.seconds_since_epoch());
    struct utimbuf buf {
        .actime = seconds,
        .modtime = seconds
    };

    return Core::System::utime(zip_member.name, buf);
}

static bool unpack_zip_member(Archive::ZipMember zip_member, bool quiet)
{
    if (zip_member.is_directory) {
        if (auto maybe_error = Core::System::mkdir(zip_member.name, 0755); maybe_error.is_error()) {
            warnln("Failed to create directory '{}': {}", zip_member.name, maybe_error.error());
            return false;
        }
        if (!quiet)
            outln(" extracting: {}", zip_member.name);
        return true;
    }
    MUST(Core::Directory::create(LexicalPath(zip_member.name.to_byte_string()).parent(), Core::Directory::CreateDirectories::Yes));
    auto new_file_or_error = Core::File::open(zip_member.name.to_byte_string(), Core::File::OpenMode::Write);
    if (new_file_or_error.is_error()) {
        warnln("Can't write file {}: {}", zip_member.name, new_file_or_error.release_error());
        return false;
    }
    auto new_file = new_file_or_error.release_value();

    if (!quiet)
        outln(" extracting: {}", zip_member.name);

    Crypto::Checksum::CRC32 checksum;
    switch (zip_member.compression_method) {
    case Archive::ZipCompressionMethod::Store: {
        if (auto maybe_error = new_file->write_until_depleted(zip_member.compressed_data); maybe_error.is_error()) {
            warnln("Can't write file contents in {}: {}", zip_member.name, maybe_error.release_error());
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
        if (auto maybe_error = new_file->write_until_depleted(decompressed_data.value()); maybe_error.is_error()) {
            warnln("Can't write file contents in {}: {}", zip_member.name, maybe_error.release_error());
            return false;
        }
        checksum.update(decompressed_data.value());
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }

    if (adjust_modification_time(zip_member).is_error()) {
        warnln("Failed setting modification_time for file {}", zip_member.name);
        return false;
    }

    new_file->close();

    if (checksum.digest() != zip_member.crc32) {
        warnln("Failed decompressing file {}: CRC32 mismatch", zip_member.name);
        MUST(FileSystem::remove(zip_member.name, FileSystem::RecursionMode::Disallowed));
        return false;
    }

    return true;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView zip_file_path;
    bool quiet { false };
    bool list_files { false };
    StringView output_directory_path;
    Vector<StringView> file_filters;

    Core::ArgsParser args_parser;
    args_parser.add_option(list_files, "Only list files in the archive", "list", 'l');
    args_parser.add_option(output_directory_path, "Directory to receive the archive content", "output-directory", 'd', "path");
    args_parser.add_option(quiet, "Be less verbose", "quiet", 'q');
    args_parser.add_positional_argument(zip_file_path, "File to unzip", "path", Core::ArgsParser::Required::Yes);
    args_parser.add_positional_argument(file_filters, "Files or filters in the archive to extract", "files", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    struct stat st = TRY(Core::System::stat(zip_file_path));

    // FIXME: Map file chunk-by-chunk once we have mmap() with offset.
    //        This will require mapping some parts then unmapping them repeatedly,
    //        but it would be significantly faster and less syscall heavy than seek()/read() at every read.
    OwnPtr<Core::MappedFile> mapped_file;
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

    if (list_files) {
        outln("  Length     Date      Time     Name");
        outln("--------- ---------- --------   ----");
        TRY(zip_file->for_each_member([&](auto zip_member) -> ErrorOr<IterationDecision> {
            auto time = time_from_packed_dos(zip_member.modification_date, zip_member.modification_time);
            auto time_str = TRY(Core::DateTime::from_timestamp(time.seconds_since_epoch()).to_string());

            outln("{:>9} {}   {}", zip_member.uncompressed_size, time_str, zip_member.name);

            return IterationDecision::Continue;
        }));
        auto statistics = TRY(zip_file->calculate_statistics());
        outln("---------                       ----");
        outln("{:>9}                       {} files", statistics.total_uncompressed_bytes(), statistics.member_count());
        return 0;
    }

    Vector<Archive::ZipMember> zip_directories;

    auto success = TRY(zip_file->for_each_member([&](auto zip_member) {
        bool keep_file = false;

        if (!file_filters.is_empty()) {
            for (auto& filter : file_filters) {
                // Convert underscore wildcards (usual unzip convention) to question marks (as used by StringUtils)
                auto string_filter = filter.replace("_"sv, "?"sv, ReplaceMode::All);
                if (zip_member.name.bytes_as_string_view().matches(string_filter, CaseSensitivity::CaseSensitive)) {
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
            if (zip_member.is_directory)
                zip_directories.append(zip_member);
        }

        return IterationDecision::Continue;
    }));

    if (!success) {
        return 1;
    }

    for (auto& directory : zip_directories) {
        if (adjust_modification_time(directory).is_error()) {
            warnln("Failed setting modification time for directory {}", directory.name);
            return 1;
        }
    }

    return success ? 0 : 1;
}
