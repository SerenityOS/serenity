/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/ByteString.h>
#include <AK/ScopeGuard.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

struct Result {
    u64 write_bps {};
    u64 read_bps {};
};

static Result average_result(Vector<Result> const& results)
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

static ErrorOr<Result> benchmark(ByteString const& filename, int file_size, ByteBuffer& buffer, bool allow_cache);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    using namespace AK::TimeLiterals;

    ByteString directory = ".";
    i64 time_per_benchmark_sec = 10;
    Vector<size_t> file_sizes;
    Vector<size_t> block_sizes;
    bool allow_cache = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(allow_cache, "Allow using disk cache", "cache", 'c');
    args_parser.add_option(directory, "Path to a directory where we can store the disk benchmark temp file", "directory", 'd', "directory");
    args_parser.add_option(time_per_benchmark_sec, "Time elapsed per benchmark (seconds)", "time-per-benchmark", 't', "time-per-benchmark");
    args_parser.add_option(file_sizes, "A comma-separated list of file sizes", "file-size", 'f', "file-size");
    args_parser.add_option(block_sizes, "A comma-separated list of block sizes", "block-size", 'b', "block-size");
    args_parser.parse(arguments);

    Duration const time_per_benchmark = Duration::from_seconds(time_per_benchmark_sec);

    if (file_sizes.size() == 0) {
        file_sizes = { 131072, 262144, 524288, 1048576, 5242880 };
    }
    if (block_sizes.size() == 0) {
        block_sizes = { 8192, 32768, 65536 };
    }

    auto filename = ByteString::formatted("{}/disk_benchmark.tmp", directory);

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
            while (timer.elapsed_time() < time_per_benchmark) {
                out(".");
                fflush(stdout);
                auto result = TRY(benchmark(filename, file_size, buffer_result.value(), allow_cache));
                results.append(result);
                usleep(100);
            }
            auto average = average_result(results);
            outln("Finished: runs={} time={}ms write_bps={} read_bps={}", results.size(), timer.elapsed_milliseconds(), average.write_bps, average.read_bps);

            sleep(1);
        }
    }

    return 0;
}

ErrorOr<Result> benchmark(ByteString const& filename, int file_size, ByteBuffer& buffer, bool allow_cache)
{
    int flags = O_CREAT | O_TRUNC | O_RDWR;
    if (!allow_cache)
        flags |= O_DIRECT;

    int fd = TRY(Core::System::open(filename, flags, 0644));

    auto fd_cleanup = ScopeGuard([fd, filename] {
        auto void_or_error = Core::System::close(fd);
        if (void_or_error.is_error())
            warnln("{}", void_or_error.release_error());

        void_or_error = Core::System::unlink(filename);
        if (void_or_error.is_error())
            warnln("{}", void_or_error.release_error());
    });

    Result result;

    auto timer = Core::ElapsedTimer::start_new();

    ssize_t total_written = 0;
    while (total_written < file_size) {
        auto nwritten = TRY(Core::System::write(fd, buffer));
        total_written += nwritten;
    }

    result.write_bps = (u64)(timer.elapsed_milliseconds() ? (file_size / timer.elapsed_milliseconds()) : file_size) * 1000;

    TRY(Core::System::lseek(fd, 0, SEEK_SET));

    timer.start();
    ssize_t total_read = 0;
    while (total_read < file_size) {
        auto nread = TRY(Core::System::read(fd, buffer));
        total_read += nread;
    }

    result.read_bps = (u64)(timer.elapsed_milliseconds() ? (file_size / timer.elapsed_milliseconds()) : file_size) * 1000;
    return result;
}
