/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Time.h>
#include <LibCore/DirIterator.h>
#include <LibFileSystem/FileSystem.h>
#include <fcntl.h>
#include <sys/stat.h>

#if !defined(AK_OS_WINDOWS)
#    include <sys/time.h>
#else
#    include <winsock2.h>
#endif

namespace Test {

inline double get_time_in_ms()
{
    struct timeval tv1;
    auto return_code = AK::get_time_of_day(&tv1);
    VERIFY(return_code >= 0);
    return static_cast<double>(tv1.tv_sec) * 1000.0 + static_cast<double>(tv1.tv_usec) / 1000.0;
}

template<typename Callback>
inline void iterate_directory_recursively(DeprecatedString const& directory_path, Callback callback)
{
    Core::DirIterator directory_iterator(directory_path, Core::DirIterator::Flags::SkipDots);

    while (directory_iterator.has_next()) {
        auto name = directory_iterator.next_path();
#if !defined(AK_OS_WINDOWS)
        struct stat st = {};
        if (fstatat(directory_iterator.fd(), name.characters(), &st, AT_SYMLINK_NOFOLLOW) < 0)
            continue;
#endif
        auto full_path = DeprecatedString::formatted("{}/{}", directory_path, name);
        bool is_directory = FileSystem::is_directory(full_path);

        if (is_directory && name != "/Fixtures"sv) {
            iterate_directory_recursively(full_path, callback);
        } else if (!is_directory) {
            callback(full_path);
        }
    }
}

}
