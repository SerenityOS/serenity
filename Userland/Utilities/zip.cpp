/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibArchive/Zip.h>
#include <LibCompress/Deflate.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/FileStream.h>
#include <LibCrypto/Checksum/CRC32.h>

int main(int argc, char** argv)
{
    const char* zip_path;
    Vector<StringView> source_paths;
    bool recurse = false;
    bool force = false;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(zip_path, "Zip file path", "zipfile", Core::ArgsParser::Required::Yes);
    args_parser.add_positional_argument(source_paths, "Input files to be archived", "files", Core::ArgsParser::Required::Yes);
    args_parser.add_option(recurse, "Travel the directory structure recursively", "recurse-paths", 'r');
    args_parser.add_option(force, "Overwrite existing zip file", "force", 'f');
    args_parser.parse(argc, argv);

    String zip_file_path { zip_path };
    if (Core::File::exists(zip_file_path)) {
        if (force) {
            outln("{} already exists, overwriting...", zip_file_path);
        } else {
            warnln("{} already exists, aborting!", zip_file_path);
            return 1;
        }
    }

    auto file_stream_or_error = Core::OutputFileStream::open(zip_file_path);
    if (file_stream_or_error.is_error()) {
        warnln("Failed to open zip file: {}", file_stream_or_error.error());
        return 1;
    }

    outln("Archive: {}", zip_file_path);

    auto file_stream = file_stream_or_error.value();
    Archive::ZipOutputStream zip_stream { file_stream };

    auto add_file = [&](String path) {
        auto file = Core::File::construct(path);
        if (!file->open(Core::OpenMode::ReadOnly)) {
            warnln("Failed to open {}: {}", path, file->error_string());
            return;
        }

        auto canonicalized_path = LexicalPath::canonicalized_path(path);
        auto file_buffer = file->read_all();
        Archive::ZipMember member {};
        member.name = canonicalized_path;

        auto deflate_buffer = Compress::DeflateCompressor::compress_all(file_buffer);
        if (deflate_buffer.has_value() && deflate_buffer.value().size() < file_buffer.size()) {
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
        zip_stream.add_member(member);
    };

    auto add_directory = [&](String path, auto handle_directory) -> void {
        auto canonicalized_path = String::formatted("{}/", LexicalPath::canonicalized_path(path));
        Archive::ZipMember member {};
        member.name = canonicalized_path;
        member.compressed_data = {};
        member.compression_method = Archive::ZipCompressionMethod::Store;
        member.uncompressed_size = 0;
        member.crc32 = 0;
        member.is_directory = true;
        zip_stream.add_member(member);
        outln("  adding: {} (stored 0%)", canonicalized_path);

        if (!recurse)
            return;

        Core::DirIterator it(path, Core::DirIterator::Flags::SkipParentAndBaseDir);
        while (it.has_next()) {
            auto child_path = it.next_full_path();
            if (!Core::File::is_directory(child_path)) {
                add_file(child_path);
            } else {
                handle_directory(child_path, handle_directory);
            }
        }
    };

    for (auto const& source_path : source_paths) {
        if (Core::File::is_directory(source_path)) {
            add_directory(source_path, add_directory);
        } else {
            add_file(source_path);
        }
    }

    zip_stream.finish();

    return 0;
}
