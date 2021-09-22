/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/MappedFile.h>
#include <Kernel/API/InodeWatcherEvent.h>
#include <LibCompress/Gzip.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/File.h>
#include <LibCore/FileWatcher.h>
#include <LibCoredump/Backtrace.h>
#include <LibCoredump/Reader.h>
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

static bool compress_coredump(String const& coredump_path, NonnullRefPtr<MappedFile> coredump_file)
{
    auto compressed_coredump = Compress::GzipCompressor::compress_all(coredump_file->bytes());
    if (!compressed_coredump.has_value()) {
        dbgln("Could not compress coredump '{}'", coredump_path);
        return false;
    }
    auto output_path = String::formatted("{}.gz", coredump_path);
    auto output_file_or_error = Core::File::open(output_path, Core::OpenMode::WriteOnly);
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

static void launch_crash_reporter(const String& coredump_path, bool unlink_after_use)
{
    pid_t child;
    const char* argv[4] = { "CrashReporter" };
    if (unlink_after_use) {
        argv[1] = "--unlink";
        argv[2] = coredump_path.characters();
        argv[3] = nullptr;
    } else {
        argv[1] = coredump_path.characters();
        argv[2] = nullptr;
    }
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

    Core::BlockingFileWatcher watcher;
    auto watch_result = watcher.add_watch("/tmp/coredump", Core::FileWatcherEvent::Type::ChildCreated);
    if (watch_result.is_error()) {
        warnln("Failed to watch the coredump directory: {}", watch_result.error());
        VERIFY_NOT_REACHED();
    }

    while (true) {
        auto event = watcher.wait_for_event();
        VERIFY(event.has_value());
        if (event.value().type != Core::FileWatcherEvent::Type::ChildCreated)
            continue;
        auto& coredump_path = event.value().event_path;
        if (coredump_path.ends_with(".gz"))
            continue; // stops compress_coredump from accidentally triggering us
        dbgln("New coredump file: {}", coredump_path);
        wait_until_coredump_is_ready(coredump_path);

        // this might not be needed, compression failure is not a concern, if it failed we'll still have the original coredump file
        // alse why skipping on launching the crash reporter if mapping the compression file is unsuccessful!
        auto file_or_error = MappedFile::map(coredump_path);
        if (file_or_error.is_error()) {
            dbgln("Unable to map coredump {}: {}", coredump_path, file_or_error.error().string());
            continue;
        }
        
        // I believe the purpose of the second argument is to cleanup the coredump when compression is successful
        // the second argument should be removed (if not needed elsewhere) as it's not needed if we are compressing after launching crash reporter
        launch_crash_reporter(coredump_path, false);

        // FIXME: This is a hack to give CrashReporter time to parse the coredump
        //        before we start compressing it.
        //        I'm sure there's a much smarter approach to this whole thing,
        //        and we should find it. :^)
        sleep(3);

        auto compress_timer = Core::ElapsedTimer::start_new();
        // since we won't rely on launch_crash_reporter's second argument to cleanup coredump file after compression
        // we should delete it ourselves if compression is successful
        if (compress_coredump(coredump_path, file_or_error.release_value()))
            // delete coredump file as it's compressed successfully
            dbgln("Compressing coredump took {} ms", compress_timer.elapsed());
    }
}
