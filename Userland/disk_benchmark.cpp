/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/ByteBuffer.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/ElapsedTimer.h>
#include <fcntl.h>
#include <getopt.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

struct Result {
    u64 write_bps;
    u64 read_bps;
};

static Result average_result(const Vector<Result>& results)
{
    Result average;

    for (auto& res : results) {
        average.write_bps += res.write_bps;
        average.read_bps += res.read_bps;
    }

    average.write_bps /= results.size();
    average.read_bps /= results.size();

    return average;
}

static void exit_with_usage(int rc)
{
    fprintf(stderr, "Usage: disk_benchmark [-h] [-d directory] [-t time_per_benchmark] [-f file_size1,file_size2,...] [-b block_size1,block_size2,...]\n");
    exit(rc);
}

static Result benchmark(const String& filename, int file_size, int block_size, ByteBuffer& buffer, bool allow_cache);

int main(int argc, char** argv)
{
    char* directory = strdup(".");
    int time_per_benchmark = 10;
    Vector<int> file_sizes;
    Vector<int> block_sizes;
    bool allow_cache = false;

    int opt;
    while ((opt = getopt(argc, argv, "chd:t:f:b:")) != -1) {
        switch (opt) {
        case 'h':
            exit_with_usage(0);
            break;
        case 'c':
            allow_cache = true;
            break;
        case 'd':
            directory = strdup(optarg);
            break;
        case 't':
            time_per_benchmark = atoi(optarg);
            break;
        case 'f':
            for (auto size : String(optarg).split(','))
                file_sizes.append(atoi(size.characters()));
            break;
        case 'b':
            for (auto size : String(optarg).split(','))
                block_sizes.append(atoi(size.characters()));
            break;
        }
    }

    if (file_sizes.size() == 0) {
        file_sizes = { 131072, 262144, 524288, 1048576, 5242880 };
    }
    if (block_sizes.size() == 0) {
        block_sizes = { 8192, 32768, 65536 };
    }

    umask(0644);

    auto filename = String::format("%s/disk_benchmark.tmp", directory);

    for (auto file_size : file_sizes) {
        for (auto block_size : block_sizes) {
            if (block_size > file_size)
                continue;

            auto buffer = ByteBuffer::create_uninitialized(block_size);

            Vector<Result> results;

            printf("Running: file_size=%d block_size=%d\n", file_size, block_size);
            Core::ElapsedTimer timer;
            timer.start();
            while (timer.elapsed() < time_per_benchmark * 1000) {
                printf(".");
                fflush(stdout);
                results.append(benchmark(filename, file_size, block_size, buffer, allow_cache));
                usleep(100);
            }
            auto average = average_result(results);
            printf("\nFinished: runs=%zu time=%dms write_bps=%llu read_bps=%llu\n", results.size(), timer.elapsed(), average.write_bps, average.read_bps);

            sleep(1);
        }
    }

    if (isatty(0)) {
        printf("Press any key to exit...\n");
        fgetc(stdin);
    }
}

Result benchmark(const String& filename, int file_size, int block_size, ByteBuffer& buffer, bool allow_cache)
{
    int flags = O_CREAT | O_TRUNC | O_RDWR;
    if (!allow_cache)
        flags |= O_DIRECT;

    int fd = open(filename.characters(), flags, 0644);
    if (fd == -1) {
        perror("open");
        exit(1);
    }

    auto cleanup_and_exit = [fd, filename]() {
        close(fd);
        unlink(filename.characters());
        exit(1);
    };

    Result res;

    Core::ElapsedTimer timer;

    timer.start();
    int nwrote = 0;
    for (int j = 0; j < file_size; j += block_size) {
        int n = write(fd, buffer.data(), block_size);
        if (n < 0) {
            perror("write");
            cleanup_and_exit();
        }
        nwrote += n;
    }

    res.write_bps = (u64)(timer.elapsed() ? (file_size / timer.elapsed()) : file_size) * 1000;

    if (lseek(fd, 0, SEEK_SET) < 0) {
        perror("lseek");
        cleanup_and_exit();
    }

    timer.start();
    int nread = 0;
    while (nread < file_size) {
        int n = read(fd, buffer.data(), block_size);
        if (n < 0) {
            perror("read");
            cleanup_and_exit();
        }
        nread += n;
    }

    res.read_bps = (u64)(timer.elapsed() ? (file_size / timer.elapsed()) : file_size) * 1000;

    if (close(fd) != 0) {
        perror("close");
        cleanup_and_exit();
    }

    if (unlink(filename.characters()) != 0) {
        perror("unlink");
        cleanup_and_exit();
    }

    return res;
}
