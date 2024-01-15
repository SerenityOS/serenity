/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <LibFileSystem/FileSystem.h>
#include <LibTest/TestCase.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/wait.h>
#include <unistd.h>

TEST_CASE(test_mktemp_unique_filename)
{
    u8* ptr = (u8*)mmap(nullptr, 0x1000, PROT_READ | PROT_WRITE, MAP_SHARED | MAP_ANONYMOUS, -1, 0);
    EXPECT(ptr != MAP_FAILED);

    if (fork() == 0) {
        char path[] = "/tmp/test.mktemp.XXXXXX";
        auto temp_path = ByteString::formatted("{}", mktemp(path));
        EXPECT(temp_path.characters());
        unlink(path);

        memcpy(&ptr[0], temp_path.characters(), temp_path.length());

        exit(EXIT_SUCCESS);
    } else {
        wait(NULL);

        auto path1 = ByteString::formatted("{}", reinterpret_cast<char const*>(ptr));

        char path[] = "/tmp/test.mktemp.XXXXXX";
        auto path2 = ByteString::formatted("{}", mktemp(path));
        EXPECT(path2.characters());
        unlink(path);

        EXPECT_NE(path1, path2);
    }

    munmap(ptr, sizeof(*ptr));
}

TEST_CASE(test_mkdtemp_unique_filename)
{
    u8* ptr = (u8*)mmap(nullptr, 0x1000, PROT_READ | PROT_WRITE, MAP_SHARED | MAP_ANONYMOUS, -1, 0);
    EXPECT_NE(ptr, MAP_FAILED);

    if (fork() == 0) {
        char path[] = "/tmp/test.mkdtemp.XXXXXX";
        auto temp_path = ByteString::formatted("{}", mkdtemp(path));
        EXPECT(temp_path.characters());
        rmdir(path);

        memcpy(&ptr[0], temp_path.characters(), temp_path.length());

        exit(EXIT_SUCCESS);
    } else {
        wait(NULL);

        auto path1 = ByteString::formatted("{}", reinterpret_cast<char const*>(ptr));

        char path[] = "/tmp/test.mkdtemp.XXXXXX";
        auto path2 = ByteString::formatted("{}", mkdtemp(path));
        EXPECT(path2.characters());
        rmdir(path);

        EXPECT_NE(path1, path2);
    }

    munmap(ptr, sizeof(*ptr));
}

TEST_CASE(test_mkstemp_unique_filename)
{
    u8* ptr = (u8*)mmap(nullptr, 0x1000, PROT_READ | PROT_WRITE, MAP_SHARED | MAP_ANONYMOUS, -1, 0);
    EXPECT_NE(ptr, MAP_FAILED);

    if (fork() == 0) {
        char path[] = "/tmp/test.mkstemp.XXXXXX";
        auto fd = mkstemp(path);
        EXPECT_NE(fd, -1);

        auto temp_path = TRY_OR_FAIL(FileSystem::read_link(ByteString::formatted("/proc/{}/fd/{}", getpid(), fd)));
        EXPECT(temp_path.characters());

        close(fd);
        unlink(path);

        memcpy(&ptr[0], temp_path.characters(), temp_path.length());

        exit(EXIT_SUCCESS);
    } else {
        wait(NULL);

        auto path1 = ByteString::formatted("{}", reinterpret_cast<char const*>(ptr));

        char path[] = "/tmp/test.mkstemp.XXXXXX";
        auto fd = mkstemp(path);
        EXPECT(fd != -1);

        auto path2 = TRY_OR_FAIL(FileSystem::read_link(ByteString::formatted("/proc/{}/fd/{}", getpid(), fd)));
        EXPECT(path2.characters());

        close(fd);
        unlink(path);

        EXPECT_NE(path1, path2);
    }

    munmap(ptr, sizeof(*ptr));
}

TEST_CASE(test_mkstemps_unique_filename)
{
    u8* ptr = (u8*)mmap(nullptr, 0x1000, PROT_READ | PROT_WRITE, MAP_SHARED | MAP_ANONYMOUS, -1, 0);
    EXPECT_NE(ptr, MAP_FAILED);

    if (fork() == 0) {
        char path[] = "/tmp/test.mkstemps.prefixXXXXXXsuffix";
        auto fd = mkstemps(path, 6);
        EXPECT_NE(fd, -1);

        auto temp_path = TRY_OR_FAIL(FileSystem::read_link(ByteString::formatted("/proc/{}/fd/{}", getpid(), fd)));
        EXPECT(temp_path.characters());

        close(fd);
        unlink(path);

        EXPECT(temp_path.starts_with("/tmp/test.mkstemps.prefix"sv));
        EXPECT(temp_path.ends_with("suffix"sv));
        EXPECT_EQ(strlen(path), temp_path.length());

        memcpy(&ptr[0], temp_path.characters(), temp_path.length());

        exit(EXIT_SUCCESS);
    } else {
        wait(NULL);

        auto path1 = ByteString::formatted("{}", reinterpret_cast<char const*>(ptr));

        char path[] = "/tmp/test.mkstemps.prefixXXXXXXsuffix";
        auto fd = mkstemps(path, 6);
        EXPECT(fd != -1);

        auto path2 = TRY_OR_FAIL(FileSystem::read_link(ByteString::formatted("/proc/{}/fd/{}", getpid(), fd)));
        EXPECT(path2.characters());

        close(fd);
        unlink(path);

        EXPECT(path2.starts_with("/tmp/test.mkstemps.prefix"sv));
        EXPECT(path2.ends_with("suffix"sv));
        EXPECT_EQ(strlen(path), path2.length());

        EXPECT_NE(path1, path2);
    }

    munmap(ptr, sizeof(*ptr));
}
