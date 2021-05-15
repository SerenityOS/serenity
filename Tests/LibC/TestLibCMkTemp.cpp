/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/File.h>
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
        auto temp_path = String::formatted("{}", mktemp(path));
        EXPECT(temp_path.characters());
        unlink(path);

        memcpy(&ptr[0], temp_path.characters(), temp_path.length());

        exit(EXIT_SUCCESS);
    } else {
        wait(NULL);

        auto path1 = String::formatted("{}", reinterpret_cast<const char*>(ptr));

        char path[] = "/tmp/test.mktemp.XXXXXX";
        auto path2 = String::formatted("{}", mktemp(path));
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
        auto temp_path = String::formatted("{}", mkdtemp(path));
        EXPECT(temp_path.characters());
        rmdir(path);

        memcpy(&ptr[0], temp_path.characters(), temp_path.length());

        exit(EXIT_SUCCESS);
    } else {
        wait(NULL);

        auto path1 = String::formatted("{}", reinterpret_cast<const char*>(ptr));

        char path[] = "/tmp/test.mkdtemp.XXXXXX";
        auto path2 = String::formatted("{}", mkdtemp(path));
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

        auto temp_path = Core::File::read_link(String::formatted("/proc/{}/fd/{}", getpid(), fd));
        EXPECT(temp_path.characters());

        close(fd);
        unlink(path);

        memcpy(&ptr[0], temp_path.characters(), temp_path.length());

        exit(EXIT_SUCCESS);
    } else {
        wait(NULL);

        auto path1 = String::formatted("{}", reinterpret_cast<const char*>(ptr));

        char path[] = "/tmp/test.mkstemp.XXXXXX";
        auto fd = mkstemp(path);
        EXPECT(fd != -1);

        auto path2 = Core::File::read_link(String::formatted("/proc/{}/fd/{}", getpid(), fd));
        EXPECT(path2.characters());

        close(fd);
        unlink(path);

        EXPECT_NE(path1, path2);
    }

    munmap(ptr, sizeof(*ptr));
}
