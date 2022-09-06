/*
 * Copyright (c) 2022, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Directory.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibCore/SessionManagement.h>
#include <LibCore/System.h>

namespace Core::SessionManagement {

static ErrorOr<Core::ProcessStatistics const*> get_proc(Core::AllProcessesStatistics const& stats, pid_t pid)
{
    for (auto& proc : stats.processes) {
        if (proc.pid == pid)
            return &proc;
    }
    return Error::from_string_literal("Could not find pid in process statistics.");
}

ErrorOr<pid_t> root_session_id(Optional<pid_t> force_sid)
{
    auto stats = Core::ProcessStatisticsReader::get_all(false);
    if (!stats.has_value())
        return Error::from_string_literal("Failed to get all process statistics");

    pid_t sid = (force_sid.has_value()) ? force_sid.value() : TRY(System::getsid());
    while (true) {
        pid_t parent = TRY(get_proc(stats.value(), sid))->ppid;
        pid_t parent_sid = TRY(get_proc(stats.value(), parent))->sid;

        if (parent_sid == 0)
            break;

        sid = parent_sid;
    }

    return sid;
}

ErrorOr<void> logout(Optional<pid_t> force_sid)
{
    pid_t sid = TRY(root_session_id(force_sid));
    TRY(System::kill(-sid, SIGTERM));
    return {};
}

ErrorOr<String> parse_path_with_sid(StringView general_path, Optional<pid_t> force_sid)
{
    if (general_path.contains("%sid"sv)) {
        pid_t sid = TRY(root_session_id(force_sid));
        return general_path.replace("%sid"sv, String::number(sid), ReplaceMode::All);
    }
    return String(general_path);
}

ErrorOr<void> create_session_temporary_directory_if_needed(uid_t uid, gid_t gid, Optional<pid_t> force_sid)
{
    pid_t sid = TRY(root_session_id(force_sid));
    auto const temporary_directory = String::formatted("/tmp/session/{}", sid);
    auto directory = TRY(Core::Directory::create(temporary_directory, Core::Directory::CreateDirectories::Yes));
    TRY(directory.chown(uid, gid));
    return {};
}

}
