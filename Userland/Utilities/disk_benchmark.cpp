/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/ElapsedTimer.h>
#include <fcntl.h>
#include <getopt.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <unistd.h>

struct Result {
    u64 write_bps {};
    u64 read_bps {};
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
    warnln("Usage: disk_benchmark [-h] [-d directory] [-t time_per_benchmark] [-f file_size1,file_size2,...] [-b block_size1,block_size2,...]");
    exit(rc);
}

static Optional<Result> benchmark(const String& filename, int file_size, int block_size, ByteBuffer& buffer, bool allow_cache);

int main(int argc, char** argv)
{
    String directory = ".";
    int time_per_benchmark = 10;
    Vector<size_t> file_sizes;
    Vector<size_t> block_sizes;
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
            directory = optarg;
            break;
        case 't':
            time_per_benchmark = atoi(optarg);
            break;
        case 'f':
            for (const auto& size : String(optarg).split(','))
                file_sizes.append(atoi(size.characters()));
            break;
        case 'b':
            for (const auto& size : String(optarg).split(','))
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

    auto filename = String::formatted("{}/disk_benchmark.tmp", directory);

    for (auto file_size : file_sizes) {
        for (auto block_size : block_sizes) {
            if (block_size > file_size)
                continue;

            auto buffer_result = ByteBuffer::create_uninitialized(block_size);
            if (buffer_result.is_error()) {
                warnln("Not enough memory to allocate space for block size = {}", block_size);
                continue;
            }
            Vector<Result> results;

            outln("Running: file_size={} block_size={}", file_size, block_size);
            auto timer = Core::ElapsedTimer::start_new();
            while (timer.elapsed() < time_per_benchmark * 1000) {
                out(".");
                fflush(stdout);
                auto result = benchmark(filename, file_size, block_size, buffer_result.value(), allow_cache);
                if (!result.has_value())
                    return 1;
                results.append(result.release_value());
                usleep(100);
            }
            auto average = average_result(results);
            outln("Finished: runs={} time={}ms write_bps={} read_bps={}", results.size(), timer.elapsed(), average.write_bps, average.read_bps);

            sleep(1);
        }
    }

    return 0;
}

Optional<Result> benchmark(const String& filename, int file_size, int block_size, ByteBuffer& buffer, bool allow_cache)
{
    int flags = O_CREAT | O_TRUNC | O_RDWR;
    if (!allow_cache)
        flags |= O_DIRECT;

    int fd = open(filename.characters(), flags, 0644);
    if (fd == -1) {
        perror("open");
        exit(1);
    }

    auto fd_cleanup = ScopeGuard([fd, filename] {
        if (close(fd) < 0)
            perror("close");
        if (unlink(filename.characters()) < 0)
            perror("unlink");
    });

    Result result;

    auto timer = Core::ElapsedTimer::start_new();

    ssize_t total_written = 0;
    while (total_written < file_size) {
        auto nwritten = write(fd, buffer.data(), block_size);
        if (nwritten < 0) {
            perror("write");
            return {};
        }
        total_written += nwritten;
    }

    result.write_bps = (u64)(timer.elapsed() ? (file_size / timer.elapsed()) : file_size) * 1000;

    if (lseek(fd, 0, SEEK_SET) < 0) {
        perror("lseek");
        return {};
    }

    timer.start();
    ssize_t total_read = 0;
    while (total_read < file_size) {
        auto nread = read(fd, buffer.data(), block_size);
        if (nread < 0) {
            perror("read");
            return {};
        }
        total_read += nread;
    }

    result.read_bps = (u64)(timer.elapsed() ? (file_size / timer.elapsed()) : file_size) * 1000;
    return result;
}
