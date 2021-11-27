/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ProcessStatisticsReader.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <signal.h>

static Core::ProcessStatistics const& get_proc(Core::AllProcessesStatistics const& stats, pid_t pid)
{
    for (auto& proc : stats.processes) {
        if (proc.pid == pid)
            return proc;
    }
    VERIFY_NOT_REACHED();
}

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio proc rpath"));
    TRY(Core::System::unveil("/proc/all", "r"));
    TRY(Core::System::unveil("/etc/passwd", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    // logout finds the highest session up all nested sessions, and kills it.
    auto stats = Core::ProcessStatisticsReader::get_all();
    if (!stats.has_value()) {
        warnln("couldn't get process statistics");
        return 1;
    }

    pid_t sid = getsid(0);
    while (true) {
        pid_t parent = get_proc(stats.value(), sid).ppid;
        pid_t parent_sid = get_proc(stats.value(), parent).sid;

        if (parent_sid == 0)
            break;

        sid = parent_sid;
    }

    TRY(Core::System::kill(-sid, SIGTERM));

    return 0;
}
