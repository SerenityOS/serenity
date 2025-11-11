/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/FileWatcher.h>
#include <LibCore/MappedFile.h>
#include <LibCore/Process.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

static void launch_crash_reporter(ByteString const& coredump_path, bool unlink_on_exit)
{
    auto pid = Core::Process::spawn("/bin/CrashReporter"sv,
        unlink_on_exit
            ? Array { "--unlink", coredump_path.characters() }.span()
            : Array { coredump_path.characters() }.span());
    if (pid.is_error())
        warnln("Failed to launch CrashReporter");
}

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath proc exec"));

    Core::BlockingFileWatcher watcher;
    TRY(watcher.add_watch("/tmp/coredump", Core::FileWatcherEvent::Type::ChildCreated));

    while (true) {
        auto event = watcher.wait_for_event().value();
        if (event.type != Core::FileWatcherEvent::Type::ChildCreated)
            continue;
        auto& coredump_path = event.event_path;
        if (coredump_path.ends_with(".partial"sv))
            continue;

        dbgln("New coredump file: {}", coredump_path);

        auto file_or_error = Core::MappedFile::map(coredump_path);
        if (file_or_error.is_error()) {
            dbgln("Unable to map coredump {}: {}", coredump_path, file_or_error.error());
            continue;
        }

        launch_crash_reporter(coredump_path, true);
    }
}
