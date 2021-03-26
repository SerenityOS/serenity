/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
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

#include <AK/LexicalPath.h>
#include <AK/MappedFile.h>
#include <LibCompress/Gzip.h>
#include <LibCore/File.h>
#include <LibCore/FileWatcher.h>
#include <LibCoreDump/Backtrace.h>
#include <LibCoreDump/Reader.h>
#include <serenity.h>
#include <spawn.h>
#include <sys/stat.h>
#include <time.h>
#include <unistd.h>

static void wait_until_coredump_is_ready(const String& coredump_path)
{
    while (true) {
        struct stat statbuf;
        if (stat(coredump_path.characters(), &statbuf) < 0) {
            perror("stat");
            VERIFY_NOT_REACHED();
        }
        if (statbuf.st_mode & 0400) // Check if readable
            break;

        usleep(10000); // sleep for 10ms
    }
}

static bool compress_coredump(const String& coredump_path)
{
    auto file_or_error = MappedFile::map(coredump_path);
    if (file_or_error.is_error()) {
        dbgln("Could not open coredump '{}': {}", coredump_path, file_or_error.error());
        return false;
    }
    auto coredump_file = file_or_error.value();
    auto compressed_coredump = Compress::GzipCompressor::compress_all(coredump_file->bytes());
    if (!compressed_coredump.has_value()) {
        dbgln("Could not compress coredump '{}'", coredump_path);
        return false;
    }
    auto output_path = String::formatted("{}.gz", coredump_path);
    auto output_file_or_error = Core::File::open(output_path, Core::File::WriteOnly);
    if (output_file_or_error.is_error()) {
        dbgln("Could not open '{}' for writing: {}", output_path, output_file_or_error.error());
        return false;
    }
    auto output_file = output_file_or_error.value();
    if (!output_file->write(compressed_coredump.value().data(), compressed_coredump.value().size())) {
        dbgln("Could not write compressed coredump '{}'", output_path);
        return false;
    }
    return true;
}

static void print_backtrace(const String& coredump_path)
{
    auto coredump = CoreDump::Reader::create(coredump_path);
    if (!coredump) {
        dbgln("Could not open coredump '{}'", coredump_path);
        return;
    }

    size_t thread_index = 0;
    coredump->for_each_thread_info([&](auto& thread_info) {
        CoreDump::Backtrace backtrace(*coredump, thread_info);
        if (thread_index > 0)
            dbgln();
        dbgln("--- Backtrace for thread #{} (TID {}) ---", thread_index, thread_info.tid);
        for (auto& entry : backtrace.entries())
            dbgln("{}", entry.to_string(true));
        ++thread_index;
        return IterationDecision::Continue;
    });
}

static void launch_crash_reporter(const String& coredump_path, bool unlink_after_use)
{
    pid_t child;
    const char* argv[] = { "CrashReporter", coredump_path.characters(), unlink_after_use ? "--unlink" : nullptr, nullptr, nullptr };
    if ((errno = posix_spawn(&child, "/bin/CrashReporter", nullptr, nullptr, const_cast<char**>(argv), environ))) {
        perror("posix_spawn");
    } else {
        if (disown(child) < 0)
            perror("disown");
    }
}

int main()
{
    if (pledge("stdio rpath wpath cpath proc exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Core::BlockingFileWatcher watcher { "/tmp/coredump" };
    while (true) {
        auto event = watcher.wait_for_event();
        VERIFY(event.has_value());
        if (event.value().type != Core::FileWatcherEvent::Type::ChildAdded)
            continue;
        auto coredump_path = event.value().child_path;
        if (coredump_path.ends_with(".gz"))
            continue; // stops compress_coredump from accidentally triggering us
        dbgln("New coredump file: {}", coredump_path);
        wait_until_coredump_is_ready(coredump_path);
        auto compressed = compress_coredump(coredump_path);
        print_backtrace(coredump_path);
        launch_crash_reporter(coredump_path, compressed);
    }
}
