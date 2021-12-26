/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath wpath cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    // NOTE: The "force" option is a dummy for now, it's just here to silence scripts that use "mv -f"
    //       In the future, it might be used to cancel out an "-i" interactive option.
    bool force = false;
    bool verbose = false;

    Vector<const char*> paths;

    Core::ArgsParser args_parser;
    args_parser.add_option(force, "Force", "force", 'f');
    args_parser.add_option(verbose, "Verbose", "verbose", 'v');
    args_parser.add_positional_argument(paths, "Paths to files being moved followed by target location", "paths");
    args_parser.parse(argc, argv);

    if (paths.size() < 2) {
        args_parser.print_usage(stderr, argv[0]);
        return 1;
    }

    auto original_new_path = paths.take_last();

    struct stat st;

    int rc = lstat(original_new_path, &st);
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
        String combined_new_path;
        const char* new_path = original_new_path;
        if (S_ISDIR(st.st_mode)) {
            auto old_basename = LexicalPath(old_path).basename();
            combined_new_path = String::formatted("{}/{}", original_new_path, old_basename);
            new_path = combined_new_path.characters();
        }

        rc = rename(old_path, new_path);
        if (rc < 0) {
            if (errno == EXDEV) {
                auto result = Core::File::copy_file_or_directory(
                    new_path, old_path,
                    Core::File::RecursionMode::Allowed,
                    Core::File::LinkMode::Disallowed,
                    Core::File::AddDuplicateFileMarker::No);

                if (result.is_error()) {
                    warnln("mv: could not move '{}': {}", old_path, result.error().error_code);
                    return 1;
                }
                rc = unlink(old_path);
                if (rc < 0)
                    warnln("mv: unlink '{}': {}", old_path, strerror(errno));
            } else {
                warnln("mv: cannot move '{}' : {}", old_path, strerror(errno));
            }
        }

        if (verbose && rc == 0)
            outln("renamed '{}' -> '{}'", old_path, new_path);
    }

    return 0;
}
