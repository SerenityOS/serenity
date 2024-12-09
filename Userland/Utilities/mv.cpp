/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath fattr"));

    bool force = false;
    bool no_clobber = false;
    bool verbose = false;

    Vector<ByteString> paths;

    Core::ArgsParser args_parser;
    args_parser.add_option(force, "Force", "force", 'f');
    args_parser.add_option(no_clobber, "Do not overwrite existing files", "no-clobber", 'n');
    args_parser.add_option(verbose, "Verbose", "verbose", 'v');
    args_parser.add_positional_argument(paths, "Paths to files being moved followed by target location", "paths");
    args_parser.parse(arguments);

    if (paths.size() < 2) {
        args_parser.print_usage(stderr, arguments.strings[0]);
        return 1;
    }

    if (force && no_clobber) {
        warnln("-f (--force) overrides -n (--no-clobber)");
        no_clobber = false;
    }

    auto original_new_path = paths.take_last();

    struct stat st;

    int rc = lstat(original_new_path.characters(), &st);
    if (rc != 0 && errno != ENOENT) {
        perror("lstat");
        return 1;
    }

    if (paths.size() > 1 && !S_ISDIR(st.st_mode)) {
        warnln("Target is not a directory: {}", original_new_path);
        return 1;
    }

    auto my_umask = umask(0);
    umask(my_umask);

    for (auto& old_path : paths) {
        ByteString combined_new_path;
        auto new_path = original_new_path;
        if (S_ISDIR(st.st_mode)) {
            auto old_basename = LexicalPath::basename(old_path);
            combined_new_path = ByteString::formatted("{}/{}", original_new_path, old_basename);
            new_path = combined_new_path.characters();
        }

        if (no_clobber && FileSystem::exists(new_path))
            continue;

        rc = rename(old_path.characters(), new_path.characters());
        if (rc < 0) {
            if (errno == EXDEV) {
                if (auto result = FileSystem::copy_file_or_directory(
                        new_path, old_path,
                        FileSystem::RecursionMode::Allowed,
                        FileSystem::LinkMode::Disallowed,
                        FileSystem::AddDuplicateFileMarker::No);
                    result.is_error()) {
                    warnln("mv: could not move '{}': {}", old_path, result.error());
                    return 1;
                }

                if (auto result = FileSystem::remove(old_path.view(), FileSystem::RecursionMode::Allowed); result.is_error())
                    warnln("mv: could not remove '{}': {}", old_path, result.error());
            } else {
                warnln("mv: cannot move '{}' : {}", old_path, strerror(errno));
            }
        }

        if (verbose && rc == 0)
            outln("renamed '{}' -> '{}'", old_path, new_path);
    }

    return 0;
}
