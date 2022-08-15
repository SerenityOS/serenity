/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Format.h>
#include <AK/LexicalPath.h>
#include <AK/Noncopyable.h>
#include <AK/Optional.h>
#include <LibCore/Stream.h>
#include <dirent.h>
#include <sys/stat.h>

namespace Core {

class DirIterator;

// Deal with real system directories. Any Directory instance always refers to a valid existing directory.
class Directory {
    AK_MAKE_NONCOPYABLE(Directory);

public:
    Directory(Directory&&);
    ~Directory();

    // When this flag is set, both the directory attempted to instantiate as well as all of its parents are created with mode 0755 if necessary.
    enum class CreateDirectories : bool {
        No,
        Yes,
    };

    static ErrorOr<Directory> create(LexicalPath path, CreateDirectories, mode_t creation_mode = 0755);
    static ErrorOr<Directory> create(String path, CreateDirectories, mode_t creation_mode = 0755);
    static ErrorOr<Directory> adopt_fd(int fd, Optional<LexicalPath> path = {});

    ErrorOr<NonnullOwnPtr<Stream::File>> open(StringView filename, Stream::OpenMode mode) const;
    ErrorOr<struct stat> stat() const;
    ErrorOr<DirIterator> create_iterator() const;

    ErrorOr<LexicalPath> path() const;

    ErrorOr<void> chown(uid_t, gid_t);

    static ErrorOr<bool> is_valid_directory(int fd);

private:
    Directory(int directory_fd, Optional<LexicalPath> path);
    static ErrorOr<void> ensure_directory(LexicalPath const& path, mode_t creation_mode = 0755);

    Optional<LexicalPath> m_path;
    int m_directory_fd;
};

}

namespace AK {
template<>
struct Formatter<Core::Directory> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Core::Directory const& directory)
    {
        auto path = directory.path();
        if (path.is_error())
            TRY(builder.put_string("<unknown>"sv));
        TRY(builder.put_string(path.release_value().string()));
        return {};
    }
};

}
