/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibArchive/Zip.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
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

    if (FileSystem::exists(zip_path)) {
        if (force) {
            outln("{} already exists, overwriting...", zip_path);
        } else {
            warnln("{} already exists, aborting!", zip_path);
            return 1;
        }
    }

    outln("Archive: {}", zip_path);
    auto file_stream = TRY(Core::File::open(zip_path, Core::File::OpenMode::Write));
    Archive::ZipOutputStream zip_stream(move(file_stream));

    auto add_file = [&](StringView path) -> ErrorOr<void> {
        auto canonicalized_path = TRY(String::from_byte_string(LexicalPath::canonicalized_path(path)));

        auto file = TRY(Core::File::open(path, Core::File::OpenMode::Read));
        auto stat = TRY(Core::System::fstat(file->fd()));
        auto date = Core::DateTime::from_timestamp(stat.st_mtim.tv_sec);

        auto information = TRY(zip_stream.add_member_from_stream(canonicalized_path, *file, date));
        if (information.compression_ratio < 1.f) {
            outln("  adding: {} (deflated {}%)", canonicalized_path, (int)(information.compression_ratio * 100));
        } else {
            outln("  adding: {} (stored)", canonicalized_path);
        }

        return {};
    };

    auto add_directory = [&](StringView path, auto handle_directory) -> ErrorOr<void> {
        auto canonicalized_path = TRY(String::formatted("{}/", LexicalPath::canonicalized_path(path)));

        auto stat = TRY(Core::System::stat(path));
        auto date = Core::DateTime::from_timestamp(stat.st_mtim.tv_sec);
        TRY(zip_stream.add_directory(canonicalized_path, date));

        if (!recurse)
            return {};

        Core::DirIterator it(path, Core::DirIterator::Flags::SkipParentAndBaseDir);
        while (it.has_next()) {
            auto child_path = it.next_full_path();
            if (FileSystem::is_link(child_path))
                return {};
            if (!FileSystem::is_directory(child_path)) {
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
        if (FileSystem::is_directory(source_path)) {
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
