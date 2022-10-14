/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibCore/File.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <pwd.h>

namespace Core {

HashMap<uid_t, String> ProcessStatisticsReader::s_usernames;

Optional<AllProcessesStatistics> ProcessStatisticsReader::get_all(RefPtr<Core::File>& proc_all_file, bool include_usernames)
{
    if (proc_all_file) {
        if (!proc_all_file->seek(0, Core::SeekMode::SetPosition)) {
            warnln("ProcessStatisticsReader: Failed to refresh /sys/kernel/processes: {}", proc_all_file->error_string());
            return {};
        }
    } else {
        proc_all_file = Core::File::construct("/sys/kernel/processes");
        if (!proc_all_file->open(Core::OpenMode::ReadOnly)) {
            warnln("ProcessStatisticsReader: Failed to open /sys/kernel/processes: {}", proc_all_file->error_string());
            return {};
        }
    }

    AllProcessesStatistics all_processes_statistics;

    auto file_contents = proc_all_file->read_all();
    auto json = JsonValue::from_string(file_contents);
    if (json.is_error())
        return {};

    auto& json_obj = json.value().as_object();
    json_obj.get("processes"sv).as_array().for_each([&](auto& value) {
        const JsonObject& process_object = value.as_object();
        Core::ProcessStatistics process;

        // kernel data first
        process.pid = process_object.get("pid"sv).to_u32();
        process.pgid = process_object.get("pgid"sv).to_u32();
        process.pgp = process_object.get("pgp"sv).to_u32();
        process.sid = process_object.get("sid"sv).to_u32();
        process.uid = process_object.get("uid"sv).to_u32();
        process.gid = process_object.get("gid"sv).to_u32();
        process.ppid = process_object.get("ppid"sv).to_u32();
        process.nfds = process_object.get("nfds"sv).to_u32();
        process.kernel = process_object.get("kernel"sv).to_bool();
        process.name = process_object.get("name"sv).to_string();
        process.executable = process_object.get("executable"sv).to_string();
        process.tty = process_object.get("tty"sv).to_string();
        process.pledge = process_object.get("pledge"sv).to_string();
        process.veil = process_object.get("veil"sv).to_string();
        process.amount_virtual = process_object.get("amount_virtual"sv).to_u32();
        process.amount_resident = process_object.get("amount_resident"sv).to_u32();
        process.amount_shared = process_object.get("amount_shared"sv).to_u32();
        process.amount_dirty_private = process_object.get("amount_dirty_private"sv).to_u32();
        process.amount_clean_inode = process_object.get("amount_clean_inode"sv).to_u32();
        process.amount_purgeable_volatile = process_object.get("amount_purgeable_volatile"sv).to_u32();
        process.amount_purgeable_nonvolatile = process_object.get("amount_purgeable_nonvolatile"sv).to_u32();

        auto& thread_array = process_object.get_ptr("threads"sv)->as_array();
        process.threads.ensure_capacity(thread_array.size());
        thread_array.for_each([&](auto& value) {
            auto& thread_object = value.as_object();
            Core::ThreadStatistics thread;
            thread.tid = thread_object.get("tid"sv).to_u32();
            thread.times_scheduled = thread_object.get("times_scheduled"sv).to_u32();
            thread.name = thread_object.get("name"sv).to_string();
            thread.state = thread_object.get("state"sv).to_string();
            thread.time_user = thread_object.get("time_user"sv).to_u64();
            thread.time_kernel = thread_object.get("time_kernel"sv).to_u64();
            thread.cpu = thread_object.get("cpu"sv).to_u32();
            thread.priority = thread_object.get("priority"sv).to_u32();
            thread.syscall_count = thread_object.get("syscall_count"sv).to_u32();
            thread.inode_faults = thread_object.get("inode_faults"sv).to_u32();
            thread.zero_faults = thread_object.get("zero_faults"sv).to_u32();
            thread.cow_faults = thread_object.get("cow_faults"sv).to_u32();
            thread.unix_socket_read_bytes = thread_object.get("unix_socket_read_bytes"sv).to_u32();
            thread.unix_socket_write_bytes = thread_object.get("unix_socket_write_bytes"sv).to_u32();
            thread.ipv4_socket_read_bytes = thread_object.get("ipv4_socket_read_bytes"sv).to_u32();
            thread.ipv4_socket_write_bytes = thread_object.get("ipv4_socket_write_bytes"sv).to_u32();
            thread.file_read_bytes = thread_object.get("file_read_bytes"sv).to_u32();
            thread.file_write_bytes = thread_object.get("file_write_bytes"sv).to_u32();
            process.threads.append(move(thread));
        });

        // and synthetic data last
        if (include_usernames) {
            process.username = username_from_uid(process.uid);
        }
        all_processes_statistics.processes.append(move(process));
    });

    all_processes_statistics.total_time_scheduled = json_obj.get("total_time"sv).to_u64();
    all_processes_statistics.total_time_scheduled_kernel = json_obj.get("total_time_kernel"sv).to_u64();
    return all_processes_statistics;
}

Optional<AllProcessesStatistics> ProcessStatisticsReader::get_all(bool include_usernames)
{
    RefPtr<Core::File> proc_all_file;
    return get_all(proc_all_file, include_usernames);
}

String ProcessStatisticsReader::username_from_uid(uid_t uid)
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
    return String::number(uid);
}
}
