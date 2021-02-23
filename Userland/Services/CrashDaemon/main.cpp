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

static void launch_crash_reporter(const String& coredump_path)
{
    pid_t child;
    const char* argv[] = { "CrashReporter", coredump_path.characters(), nullptr, nullptr };
    if ((errno = posix_spawn(&child, "/bin/CrashReporter", nullptr, nullptr, const_cast<char**>(argv), environ))) {
        perror("posix_spawn");
    } else {
        if (disown(child) < 0)
            perror("disown");
    }
}

int main()
{
    if (pledge("stdio rpath proc exec", nullptr) < 0) {
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
        dbgln("New coredump file: {}", coredump_path);
        wait_until_coredump_is_ready(coredump_path);
        print_backtrace(coredump_path);
        launch_crash_reporter(coredump_path);
    }
}
