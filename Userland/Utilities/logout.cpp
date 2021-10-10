/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ProcessStatisticsReader.h>
#include <signal.h>

static Core::ProcessStatistics const& get_proc(Core::AllProcessesStatistics const& stats, pid_t pid)
{
    for (auto& proc : stats.processes) {
        if (proc.pid == pid)
            return proc;
    }
    VERIFY_NOT_REACHED();
}

int main(int, char**)
{
    if (pledge("stdio proc rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/proc/all", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/etc/passwd", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

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

    if (kill(-sid, SIGTERM) == -1) {
        perror("kill(2)");
        return 1;
    }

    return 0;
}
