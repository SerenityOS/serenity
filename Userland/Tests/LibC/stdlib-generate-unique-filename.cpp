/*
 * Copyright (c) 2021, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/String.h>
#include <LibCore/File.h>
#include <assert.h>
#include <fcntl.h>
#include <mman.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/wait.h>
#include <unistd.h>

static void test_mktemp_unique_filename()
{
    u8* ptr = (u8*)mmap(nullptr, 0x1000, PROT_READ | PROT_WRITE, MAP_SHARED | MAP_ANONYMOUS, -1, 0);
    assert(ptr != MAP_FAILED);

    if (fork() == 0) {
        char path[] = "/tmp/test.mktemp.XXXXXX";
        auto temp_path = String::formatted("{}", mktemp(path));
        assert(temp_path.characters());
        unlink(path);

        memcpy(&ptr[0], temp_path.characters(), temp_path.length());

        exit(EXIT_SUCCESS);
    } else {
        wait(NULL);

        auto path1 = String::formatted("{}", reinterpret_cast<const char*>(ptr));

        char path[] = "/tmp/test.mktemp.XXXXXX";
        auto path2 = String::formatted("{}", mktemp(path));
        assert(path2.characters());
        unlink(path);

        assert(path1 != path2);
    }

    munmap(ptr, sizeof(*ptr));
}

static void test_mkdtemp_unique_filename()
{
    u8* ptr = (u8*)mmap(nullptr, 0x1000, PROT_READ | PROT_WRITE, MAP_SHARED | MAP_ANONYMOUS, -1, 0);
    assert(ptr != MAP_FAILED);

    if (fork() == 0) {
        char path[] = "/tmp/test.mkdtemp.XXXXXX";
        auto temp_path = String::formatted("{}", mkdtemp(path));
        assert(temp_path.characters());
        rmdir(path);

        memcpy(&ptr[0], temp_path.characters(), temp_path.length());

        exit(EXIT_SUCCESS);
    } else {
        wait(NULL);

        auto path1 = String::formatted("{}", reinterpret_cast<const char*>(ptr));

        char path[] = "/tmp/test.mkdtemp.XXXXXX";
        auto path2 = String::formatted("{}", mkdtemp(path));
        assert(path2.characters());
        rmdir(path);

        assert(path1 != path2);
    }

    munmap(ptr, sizeof(*ptr));
}
static void test_mkstemp_unique_filename()
{
    u8* ptr = (u8*)mmap(nullptr, 0x1000, PROT_READ | PROT_WRITE, MAP_SHARED | MAP_ANONYMOUS, -1, 0);
    assert(ptr != MAP_FAILED);

    if (fork() == 0) {
        char path[] = "/tmp/test.mkstemp.XXXXXX";
        auto fd = mkstemp(path);
        assert(fd != -1);

        auto temp_path = Core::File::read_link(String::formatted("/proc/{}/fd/{}", getpid(), fd));
        assert(temp_path.characters());

        close(fd);
        unlink(path);

        memcpy(&ptr[0], temp_path.characters(), temp_path.length());

        exit(EXIT_SUCCESS);
    } else {
        wait(NULL);

        auto path1 = String::formatted("{}", reinterpret_cast<const char*>(ptr));

        char path[] = "/tmp/test.mkstemp.XXXXXX";
        auto fd = mkstemp(path);
        assert(fd != -1);

        auto path2 = Core::File::read_link(String::formatted("/proc/{}/fd/{}", getpid(), fd));
        assert(path2.characters());

        close(fd);
        unlink(path);

        assert(path1 != path2);
    }

    munmap(ptr, sizeof(*ptr));
}

int main()
{
#define RUNTEST(x)                      \
    {                                   \
        printf("Running " #x " ...\n"); \
        x();                            \
        printf("Success!\n");           \
    }
    RUNTEST(test_mktemp_unique_filename);
    RUNTEST(test_mkstemp_unique_filename);
    RUNTEST(test_mkdtemp_unique_filename);
    printf("PASS\n");

    return 0;
}
