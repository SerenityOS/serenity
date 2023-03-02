/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <LibCore/DirectoryEntry.h>
#include <dirent.h>
#include <string.h>

namespace Core {

class DirIterator {
public:
    enum Flags {
        NoFlags = 0x0,
        SkipDots = 0x1,
        SkipParentAndBaseDir = 0x2,
    };

    explicit DirIterator(DeprecatedString path, Flags = Flags::NoFlags);
    ~DirIterator();

    DirIterator(DirIterator&&);
    DirIterator(DirIterator const&) = delete;

    bool has_error() const { return m_error.has_value(); }
    Error error() const { return Error::copy(m_error.value()); }
    bool has_next();
    Optional<DirectoryEntry> next();
    DeprecatedString next_path();
    DeprecatedString next_full_path();
    int fd() const;

private:
    DIR* m_dir = nullptr;
    Optional<Error> m_error;
    Optional<DirectoryEntry> m_next;
    DeprecatedString m_path;
    int m_flags;

    bool advance_next();
};

}
