/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DOSPackedTime.h>
#include <AK/LexicalPath.h>
#include <LibArchive/Zip.h>
#include <LibCompress/Deflate.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/DeprecatedFile.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibCrypto/Checksum/CRC32.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView zip_path;
    Vector<StringView> source_paths;
    bool recurse = false;
    bool force = false;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(zip_path, "Zip file path", "zipfile", Core::ArgsParser::Required::Yes);
    args_parser.add_positional_argument(source_paths, "Input files to be archived", "files", Core::ArgsParser::Required::Yes);
    args_parser.add_option(recurse, "Travel the directory structure recursively", "recurse-paths", 'r');
    args_parser.add_option(force, "Overwrite existing zip file", "force", 'f');
    args_parser.parse(arguments);

    TRY(Core::System::pledge("stdio rpath wpath cpath"));

    auto cwd = TRY(Core::System::getcwd());
    TRY(Core::System::unveil(LexicalPath::absolute_path(cwd, zip_path), "wc"sv));
    for (auto const& source_path : source_paths) {
        TRY(Core::System::unveil(LexicalPath::absolute_path(cwd, source_path), "r"sv));
    }
    TRY(Core::System::unveil(nullptr, nullptr));

    DeprecatedString zip_file_path { zip_path };
    if (Core::DeprecatedFile::exists(zip_file_path)) {
        if (force) {
            outln("{} already exists, overwriting...", zip_file_path);
        } else {
            warnln("{} already exists, aborting!", zip_file_path);
            return 1;
        }
    }

    outln("Archive: {}", zip_file_path);
    auto file_stream = TRY(Core::File::open(zip_file_path, Core::File::OpenMode::Write));
    Archive::ZipOutputStream zip_stream(move(file_stream));

    auto add_file = [&](DeprecatedString path) -> ErrorOr<void> {
        auto canonicalized_path = LexicalPath::canonicalized_path(path);
        auto file = TRY(Core::File::open(path, Core::File::OpenMode::Read));
        auto file_buffer = TRY(file->read_until_eof());
        Archive::ZipMember member {};
        member.name = TRY(String::from_deprecated_string(canonicalized_path));

        auto stat = TRY(Core::System::fstat(file->fd()));
        auto date = Core::DateTime::from_timestamp(stat.st_mtim.tv_sec);
        member.modification_date = to_packed_dos_date(date.year(), date.month(), date.day());
        member.modification_time = to_packed_dos_time(date.hour(), date.minute(), date.second());

        auto deflate_buffer = Compress::DeflateCompressor::compress_all(file_buffer);
        if (!deflate_buffer.is_error() && deflate_buffer.value().size() < file_buffer.size()) {
            member.compressed_data = deflate_buffer.value().bytes();
            member.compression_method = Archive::ZipCompressionMethod::Deflate;
            auto compression_ratio = (double)deflate_buffer.value().size() / file_buffer.size();
            outln("  adding: {} (deflated {}%)", canonicalized_path, (int)(compression_ratio * 100));
        } else {
            member.compressed_data = file_buffer.bytes();
            member.compression_method = Archive::ZipCompressionMethod::Store;
            outln("  adding: {} (stored 0%)", canonicalized_path);
        }
        member.uncompressed_size = file_buffer.size();
        Crypto::Checksum::CRC32 checksum { file_buffer.bytes() };
        member.crc32 = checksum.digest();
        member.is_directory = false;
        return zip_stream.add_member(member);
    };

    auto add_directory = [&](DeprecatedString path, auto handle_directory) -> ErrorOr<void> {
        auto canonicalized_path = DeprecatedString::formatted("{}/", LexicalPath::canonicalized_path(path));
        Archive::ZipMember member {};
        member.name = TRY(String::from_deprecated_string(canonicalized_path));
        member.compressed_data = {};
        member.compression_method = Archive::ZipCompressionMethod::Store;
        member.uncompressed_size = 0;
        member.crc32 = 0;
        member.is_directory = true;

        auto stat = TRY(Core::System::stat(canonicalized_path));
        auto date = Core::DateTime::from_timestamp(stat.st_mtim.tv_sec);
        member.modification_date = to_packed_dos_date(date.year(), date.month(), date.day());
        member.modification_time = to_packed_dos_time(date.hour(), date.minute(), date.second());

        TRY(zip_stream.add_member(member));
        outln("  adding: {} (stored 0%)", canonicalized_path);

        if (!recurse)
            return {};

        Core::DirIterator it(path, Core::DirIterator::Flags::SkipParentAndBaseDir);
        while (it.has_next()) {
            auto child_path = it.next_full_path();
            if (Core::DeprecatedFile::is_link(child_path))
                return {};
            if (!Core::DeprecatedFile::is_directory(child_path)) {
                auto result = add_file(child_path);
                if (result.is_error())
                    warnln("Couldn't add file '{}': {}", child_path, result.error());
            } else {
                auto result = handle_directory(child_path, handle_directory);
                if (result.is_error())
                    warnln("Couldn't add directory '{}': {}", child_path, result.error());
            }
        }
        return {};
    };

    for (auto const& source_path : source_paths) {
        if (Core::DeprecatedFile::is_directory(source_path)) {
            auto result = add_directory(source_path, add_directory);
            if (result.is_error())
                warnln("Couldn't add directory '{}': {}", source_path, result.error());
        } else {
            auto result = add_file(source_path);
            if (result.is_error())
                warnln("Couldn't add file '{}': {}", source_path, result.error());
        }
    }

    TRY(zip_stream.finish());

    return 0;
}
