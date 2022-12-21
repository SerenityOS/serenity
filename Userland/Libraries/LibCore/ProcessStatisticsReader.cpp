/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <pwd.h>

namespace Core {

HashMap<uid_t, DeprecatedString> ProcessStatisticsReader::s_usernames;

ErrorOr<AllProcessesStatistics> ProcessStatisticsReader::get_all(Core::Stream::SeekableStream& proc_all_file, bool include_usernames)
{
    TRY(proc_all_file.seek(0, Core::Stream::SeekMode::SetPosition));

    AllProcessesStatistics all_processes_statistics;

    auto file_contents = TRY(proc_all_file.read_until_eof());
    auto json_obj = TRY(JsonValue::from_string(file_contents)).as_object();
    json_obj.get_deprecated("processes"sv).as_array().for_each([&](auto& value) {
        const JsonObject& process_object = value.as_object();
        Core::ProcessStatistics process;

        // kernel data first
        process.pid = process_object.get_deprecated("pid"sv).to_u32();
        process.pgid = process_object.get_deprecated("pgid"sv).to_u32();
        process.pgp = process_object.get_deprecated("pgp"sv).to_u32();
        process.sid = process_object.get_deprecated("sid"sv).to_u32();
        process.uid = process_object.get_deprecated("uid"sv).to_u32();
        process.gid = process_object.get_deprecated("gid"sv).to_u32();
        process.ppid = process_object.get_deprecated("ppid"sv).to_u32();
        process.nfds = process_object.get_deprecated("nfds"sv).to_u32();
        process.kernel = process_object.get_deprecated("kernel"sv).to_bool();
        process.name = process_object.get_deprecated("name"sv).to_deprecated_string();
        process.executable = process_object.get_deprecated("executable"sv).to_deprecated_string();
        process.tty = process_object.get_deprecated("tty"sv).to_deprecated_string();
        process.pledge = process_object.get_deprecated("pledge"sv).to_deprecated_string();
        process.veil = process_object.get_deprecated("veil"sv).to_deprecated_string();
        process.amount_virtual = process_object.get_deprecated("amount_virtual"sv).to_u32();
        process.amount_resident = process_object.get_deprecated("amount_resident"sv).to_u32();
        process.amount_shared = process_object.get_deprecated("amount_shared"sv).to_u32();
        process.amount_dirty_private = process_object.get_deprecated("amount_dirty_private"sv).to_u32();
        process.amount_clean_inode = process_object.get_deprecated("amount_clean_inode"sv).to_u32();
        process.amount_purgeable_volatile = process_object.get_deprecated("amount_purgeable_volatile"sv).to_u32();
        process.amount_purgeable_nonvolatile = process_object.get_deprecated("amount_purgeable_nonvolatile"sv).to_u32();

        auto& thread_array = process_object.get_ptr("threads"sv)->as_array();
        process.threads.ensure_capacity(thread_array.size());
        thread_array.for_each([&](auto& value) {
            auto& thread_object = value.as_object();
            Core::ThreadStatistics thread;
            thread.tid = thread_object.get_deprecated("tid"sv).to_u32();
            thread.times_scheduled = thread_object.get_deprecated("times_scheduled"sv).to_u32();
            thread.name = thread_object.get_deprecated("name"sv).to_deprecated_string();
            thread.state = thread_object.get_deprecated("state"sv).to_deprecated_string();
            thread.time_user = thread_object.get_deprecated("time_user"sv).to_u64();
            thread.time_kernel = thread_object.get_deprecated("time_kernel"sv).to_u64();
            thread.cpu = thread_object.get_deprecated("cpu"sv).to_u32();
            thread.priority = thread_object.get_deprecated("priority"sv).to_u32();
            thread.syscall_count = thread_object.get_deprecated("syscall_count"sv).to_u32();
            thread.inode_faults = thread_object.get_deprecated("inode_faults"sv).to_u32();
            thread.zero_faults = thread_object.get_deprecated("zero_faults"sv).to_u32();
            thread.cow_faults = thread_object.get_deprecated("cow_faults"sv).to_u32();
            thread.unix_socket_read_bytes = thread_object.get_deprecated("unix_socket_read_bytes"sv).to_u32();
            thread.unix_socket_write_bytes = thread_object.get_deprecated("unix_socket_write_bytes"sv).to_u32();
            thread.ipv4_socket_read_bytes = thread_object.get_deprecated("ipv4_socket_read_bytes"sv).to_u32();
            thread.ipv4_socket_write_bytes = thread_object.get_deprecated("ipv4_socket_write_bytes"sv).to_u32();
            thread.file_read_bytes = thread_object.get_deprecated("file_read_bytes"sv).to_u32();
            thread.file_write_bytes = thread_object.get_deprecated("file_write_bytes"sv).to_u32();
            process.threads.append(move(thread));
        });

        // and synthetic data last
        if (include_usernames) {
            process.username = username_from_uid(process.uid);
        }
        all_processes_statistics.processes.append(move(process));
    });

    all_processes_statistics.total_time_scheduled = json_obj.get_deprecated("total_time"sv).to_u64();
    all_processes_statistics.total_time_scheduled_kernel = json_obj.get_deprecated("total_time_kernel"sv).to_u64();
    return all_processes_statistics;
}

ErrorOr<AllProcessesStatistics> ProcessStatisticsReader::get_all(bool include_usernames)
{
    auto proc_all_file = TRY(Core::Stream::File::open("/sys/kernel/processes"sv, Core::Stream::OpenMode::Read));
    return get_all(*proc_all_file, include_usernames);
}

DeprecatedString ProcessStatisticsReader::username_from_uid(uid_t uid)
{
    if (s_usernames.is_empty()) {
        setpwent();
        while (auto* passwd = getpwent())
            s_usernames.set(passwd->pw_uid, passwd->pw_name);
        endpwent();
    }

    auto it = s_usernames.find(uid);
    if (it != s_usernames.end())
        return (*it).value;
    return DeprecatedString::number(uid);
}
}
