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
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibCrypto/Checksum/CRC32.h>
#include <LibFileSystem/FileSystem.h>

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
    if (FileSystem::exists(zip_file_path)) {
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

    for (auto const& source_path : source_paths) {
        auto should_recurse = recurse ? Archive::RecurseThroughDirectories::Yes : Archive::RecurseThroughDirectories::No;
        auto result = zip_stream.add_member_from_path(source_path, should_recurse, [](auto const& member) {
            if (member.deflated_amount != 0) {
                outln("   adding: {} (deflated {}%)", member.canonicalized_path, member.deflated_amount);
            } else {
                outln("   adding: {} (stored 0%)", member.canonicalized_path);
            }
        });

        if (result.is_error()) {
            auto type = FileSystem::is_directory(source_path) ? "directory"sv : "file"sv;
            outln("Couldn't add {} '{}': {}", type, source_path, result.error());
        }
    }

    TRY(zip_stream.finish());

    return 0;
}
