/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/POSIX/dirent.h>
#include <LibTest/TestCase.h>
#include <dirent.h>
#include <fcntl.h>
#include <unistd.h>

TEST_CASE(test_sysfs_root_directory)
{
    auto dirfd = open("/sys/", O_RDONLY | O_DIRECTORY);
    auto cleanup_guard = ScopeGuard([&] {
        close(dirfd);
    });

    DIR* dir = fdopendir(dirfd);
    EXPECT(dir != nullptr);

    auto cleanup_dir_guard = ScopeGuard([&] {
        closedir(dir);
    });

    auto* _dirent = readdir(dir);
    EXPECT(_dirent != nullptr);
    // NOTE: We should see '.'
    EXPECT_EQ(_dirent->d_type, DT_DIR);

    _dirent = readdir(dir);
    EXPECT(_dirent != nullptr);
    // NOTE: We should see '..'
    EXPECT_EQ(_dirent->d_type, DT_DIR);

    while (true) {
        _dirent = readdir(dir);
        if (_dirent == nullptr)
            break;
        // NOTE: We should only see a directory entry in the /sys directory
        EXPECT_EQ(_dirent->d_type, DT_DIR);
    }
}

TEST_CASE(test_devpts_root_directory)
{
    auto dirfd = open("/dev/pts/", O_RDONLY | O_DIRECTORY);
    auto cleanup_guard = ScopeGuard([&] {
        close(dirfd);
    });

    DIR* dir = fdopendir(dirfd);
    EXPECT(dir != nullptr);

    auto cleanup_dir_guard = ScopeGuard([&] {
        closedir(dir);
    });

    auto* _dirent = readdir(dir);
    EXPECT(_dirent != nullptr);
    // NOTE: We should see '.'
    EXPECT_EQ(_dirent->d_type, DT_DIR);

    _dirent = readdir(dir);
    EXPECT(_dirent != nullptr);
    // NOTE: We should see '..'
    EXPECT_EQ(_dirent->d_type, DT_DIR);
}

TEST_CASE(test_devloop_root_directory)
{
    auto dirfd = open("/dev/loop/", O_RDONLY | O_DIRECTORY);
    auto cleanup_guard = ScopeGuard([&] {
        close(dirfd);
    });

    DIR* dir = fdopendir(dirfd);
    EXPECT(dir != nullptr);

    auto cleanup_dir_guard = ScopeGuard([&] {
        closedir(dir);
    });

    auto* _dirent = readdir(dir);
    EXPECT(_dirent != nullptr);
    // NOTE: We should see '.'
    EXPECT_EQ(_dirent->d_type, DT_DIR);

    _dirent = readdir(dir);
    EXPECT(_dirent != nullptr);
    // NOTE: We should see '..'
    EXPECT_EQ(_dirent->d_type, DT_DIR);
}

TEST_CASE(test_procfs_root_directory)
{
    auto dirfd = open("/proc/", O_RDONLY | O_DIRECTORY);
    auto cleanup_guard = ScopeGuard([&] {
        close(dirfd);
    });

    DIR* dir = fdopendir(dirfd);
    EXPECT(dir != nullptr);

    auto cleanup_dir_guard = ScopeGuard([&] {
        closedir(dir);
    });

    auto* _dirent = readdir(dir);
    EXPECT(_dirent != nullptr);
    // NOTE: We should see '.' now
    EXPECT_EQ(_dirent->d_type, DT_DIR);

    _dirent = readdir(dir);
    EXPECT(_dirent != nullptr);
    // NOTE: We should see '..' now
    EXPECT_EQ(_dirent->d_type, DT_DIR);

    _dirent = readdir(dir);
    EXPECT(_dirent != nullptr);
    // NOTE: We should see 'self' now
    EXPECT_EQ(_dirent->d_type, DT_LNK);
}
