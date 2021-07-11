/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Maxime Friess <M4x1me@pm.me>
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

Optional<Vector<Core::ProcessStatistics>> ProcessStatisticsReader::get_all(RefPtr<Core::File>& proc_all_file)
{
    if (proc_all_file) {
        if (!proc_all_file->seek(0, Core::SeekMode::SetPosition)) {
            warnln("ProcessStatisticsReader: Failed to refresh /proc/all: {}", proc_all_file->error_string());
            return {};
        }
    } else {
        proc_all_file = Core::File::construct("/proc/all");
        if (!proc_all_file->open(Core::OpenMode::ReadOnly)) {
            warnln("ProcessStatisticsReader: Failed to open /proc/all: {}", proc_all_file->error_string());
            return {};
        }
    }

    Vector<Core::ProcessStatistics> processes;

    auto file_contents = proc_all_file->read_all();
    auto json = JsonValue::from_string(file_contents);
    if (!json.has_value())
        return {};
    json.value().as_array().for_each([&](auto& value) {
        const JsonObject& process_object = value.as_object();
        Core::ProcessStatistics process;

        // kernel data first
        process.pid = process_object.get("pid").to_u32();
        process.pgid = process_object.get("pgid").to_u32();
        process.pgp = process_object.get("pgp").to_u32();
        process.sid = process_object.get("sid").to_u32();
        process.uid = process_object.get("uid").to_u32();
        process.gid = process_object.get("gid").to_u32();
        process.ppid = process_object.get("ppid").to_u32();
        process.nfds = process_object.get("nfds").to_u32();
        process.kernel = process_object.get("kernel").to_bool();
        process.name = process_object.get("name").to_string();
        process.executable = process_object.get("executable").to_string();
        process.tty = process_object.get("tty").to_string();
        process.pledge = process_object.get("pledge").to_string();
        process.veil = process_object.get("veil").to_string();
        process.amount_virtual = process_object.get("amount_virtual").to_u32();
        process.amount_resident = process_object.get("amount_resident").to_u32();
        process.amount_shared = process_object.get("amount_shared").to_u32();
        process.amount_dirty_private = process_object.get("amount_dirty_private").to_u32();
        process.amount_clean_inode = process_object.get("amount_clean_inode").to_u32();
        process.amount_purgeable_volatile = process_object.get("amount_purgeable_volatile").to_u32();
        process.amount_purgeable_nonvolatile = process_object.get("amount_purgeable_nonvolatile").to_u32();

        auto& thread_array = process_object.get_ptr("threads")->as_array();
        process.threads.ensure_capacity(thread_array.size());
        thread_array.for_each([&](auto& value) {
            auto& thread_object = value.as_object();
            Core::ThreadStatistics thread;
            thread.tid = thread_object.get("tid").to_u32();
            thread.times_scheduled = thread_object.get("times_scheduled").to_u32();
            thread.name = thread_object.get("name").to_string();
            thread.state = thread_object.get("state").to_string();
            thread.ticks_user = thread_object.get("ticks_user").to_u32();
            thread.ticks_kernel = thread_object.get("ticks_kernel").to_u32();
            thread.cpu = thread_object.get("cpu").to_u32();
            thread.priority = thread_object.get("priority").to_u32();
            thread.syscall_count = thread_object.get("syscall_count").to_u32();
            thread.inode_faults = thread_object.get("inode_faults").to_u32();
            thread.zero_faults = thread_object.get("zero_faults").to_u32();
            thread.cow_faults = thread_object.get("cow_faults").to_u32();
            thread.unix_socket_read_bytes = thread_object.get("unix_socket_read_bytes").to_u32();
            thread.unix_socket_write_bytes = thread_object.get("unix_socket_write_bytes").to_u32();
            thread.ipv4_socket_read_bytes = thread_object.get("ipv4_socket_read_bytes").to_u32();
            thread.ipv4_socket_write_bytes = thread_object.get("ipv4_socket_write_bytes").to_u32();
            thread.file_read_bytes = thread_object.get("file_read_bytes").to_u32();
            thread.file_write_bytes = thread_object.get("file_write_bytes").to_u32();
            process.threads.append(move(thread));
        });

        // and synthetic data last
        process.username = username_from_uid(process.uid);
        processes.append(move(process));
    });

    return processes;
}

Optional<Vector<Core::ProcessStatistics>> ProcessStatisticsReader::get_all()
{
    RefPtr<Core::File> proc_all_file;
    return get_all(proc_all_file);
}

Optional<HashMap<pid_t, Core::ProcessStatistics>> ProcessStatisticsReader::get_all_map(RefPtr<Core::File>& proc_all_file)
{
    auto all = get_all(proc_all_file);
    if (!all.has_value())
        return {};

    auto vec = all.value();
    HashMap<pid_t, Core::ProcessStatistics> processes;
    for (auto it = vec.begin(); it != vec.end(); it++) {
        processes.set((*it).pid, *it);
    }

    return processes;
}

Optional<HashMap<pid_t, Core::ProcessStatistics>> ProcessStatisticsReader::get_all_map()
{
    RefPtr<Core::File> proc_all_file;
    return get_all_map(proc_all_file);
}

template<size_t T>
static void add_process_to_tree(HashMap<pid_t, Core::ProcessStatistics>& map, TreeNode<Core::ProcessStatistics, T>& tn, pid_t pid)
{
    // TODO: Find a better way to implement this (currently O(n^2))

    // Get the process
    auto proc = map.get(pid);
    if (!proc.has_value())
        return;
    auto p = proc.value();

    // Set the process in the treenode
    tn.set(p);

    // Iterate over al the processes, find children
    for (auto it = map.begin(); it != map.end(); ++it) {
        auto p2 = (*it).value;
        // If the parent PID of the process is the same as the PID of the current process,
        // We add it to the tree
        if (p2.ppid == p.pid && p2.ppid != p2.pid) {
            auto child_node = tn.add_child(p2);
            // We add the other processes recursively
            add_process_to_tree(map, *child_node, p2.pid);
        }
    }
}

Optional<Tree<Core::ProcessStatistics>> ProcessStatisticsReader::get_all_tree(RefPtr<Core::File>& proc_all_file)
{
    auto all = get_all_map(proc_all_file);
    if (!all.has_value())
        return {};

    auto map = all.value();

    Tree<Core::ProcessStatistics> processes;

    add_process_to_tree(map, processes.root(), 0);

    return processes;
}

Optional<Tree<Core::ProcessStatistics>> ProcessStatisticsReader::get_all_tree()
{
    RefPtr<Core::File> proc_all_file;
    return get_all_tree(proc_all_file);
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
