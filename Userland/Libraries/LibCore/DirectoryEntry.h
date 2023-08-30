/*
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#if defined(AK_OS_WINDOWS)
#    include <windows.h>
#else
#    include <dirent.h>
#endif

namespace Core {

struct DirectoryEntry {
    enum class Type {
        BlockDevice,
        CharacterDevice,
        Directory,
        File,
        NamedPipe,
        Socket,
        SymbolicLink,
        Unknown,
        Whiteout,
    };
    Type type;
    // FIXME: Once we have a special Path string class, use that.
    DeprecatedString name;
#if !defined(AK_OS_WINDOWS)
    ino_t inode_number;

    static DirectoryEntry from_dirent(dirent const&);
    static DirectoryEntry from_stat(DIR*, dirent const&);
#else
    static DirectoryEntry from_find_data(WIN32_FIND_DATA const&);
#endif
};

}
