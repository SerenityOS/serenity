/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Files.h"
#include <AK/LexicalPath.h>
#include <LibCore/StandardPaths.h>
#include <LibFileSystem/FileSystem.h>
#include <fcntl.h>
#include <unistd.h>

namespace BitTorrent {

ErrorOr<void> create_file_with_subdirs(AK::DeprecatedString const& absolute_path)
{
    // Most of this code is copied from Userland/Utilities/mkdir/mkdir.cpp
    mode_t const default_mode = 0755;

    LexicalPath lexical_path(absolute_path);
    auto& parts = lexical_path.parts_view();
    size_t num_parts = parts.size();

    StringBuilder path_builder;

    for (size_t idx = 0; idx < num_parts; ++idx) {
        bool const is_final = (idx == (num_parts - 1));
        path_builder.append('/');
        path_builder.append(parts[idx]);
        auto const path = path_builder.to_deprecated_string();

        // We start at the downloads directory which is unveiled.
        if (path.length() <= Core::StandardPaths::downloads_directory().length())
            continue;

        struct stat st;
        if (stat(path.characters(), &st) < 0) {
            if (errno != ENOENT) {
                perror("stat");
                warnln("Error other than 'no such file or directory' with: {}", path);
                return Error::from_string_literal("Error creating directory");
            }

            if (is_final) {
                dbgln("creating file '{}'", path);
                int fd = creat(path.characters(), default_mode);
                if (fd < 0) {
                    perror("creat");
                    warnln("Error creating file '{}'", path);
                    return Error::from_string_literal("Cannot create file");
                }
                close(fd);
                return {};
            }

            dbgln("creating directory '{}'", path);
            if (mkdir(path.characters(), default_mode) < 0) {
                perror("mkdir");
                warnln("Error creating directory '{}'", path);
                return Error::from_string_literal("Cannot create directory");
            }
        } else {
            if (is_final) {
                if (!S_ISREG(st.st_mode)) {
                    warnln("Error: file already exists but isn't a regular file: '{}'", path);
                    return Error::from_string_literal("File alrady exists but isn't a regular file");
                } else {
                    dbgln("file '{}' already exists", path);
                }
            } else if (!S_ISDIR(st.st_mode)) {
                warnln("Cannot create directory, a non-directory file already exists for path '{}'", path);
                return Error::from_string_literal("Cannot create directory");
            } else {
                dbgln("directory '{}' already exists", path);
            }
        }
    }
    return {};
}

}
