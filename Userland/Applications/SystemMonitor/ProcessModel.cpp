/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProcessModel.h"
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/NonnullRefPtr.h>
#include <AK/NumberFormat.h>
#include <LibCore/File.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/Icon.h>
#include <LibGUI/ModelIndex.h>
#include <LibGUI/ModelRole.h>
#include <unistd.h>

static ProcessModel* s_the;

ProcessModel& ProcessModel::the()
{
    VERIFY(s_the);
    return *s_the;
}

ProcessModel::ProcessModel()
{
    VERIFY(!s_the);
    s_the = this;

    auto file = Core::File::construct("/proc/cpuinfo");
    if (file->open(Core::OpenMode::ReadOnly)) {
        auto buffer = file->read_all();
        auto json = JsonValue::from_string({ buffer });
        auto cpuinfo_array = json.value().as_array();
        cpuinfo_array.for_each([&](auto& value) {
            auto& cpu_object = value.as_object();
            auto cpu_id = cpu_object.get("processor").as_u32();
            m_cpus.append(make<CpuInfo>(cpu_id));
        });
    }

    if (m_cpus.is_empty())
        m_cpus.append(make<CpuInfo>(0));

    m_kernel_process_icon = GUI::Icon::default_icon("gear");
}

int ProcessModel::row_count(GUI::ModelIndex const& index) const
{
    if (!index.is_valid())
        return m_processes.size();
    // Anything in the second level (threads of processes) doesn't have children.
    // This way, we don't get infinitely recursing main threads without having to handle that special case elsewhere.
    if (index.parent().is_valid())
        return 0;
    auto const& thread = *static_cast<Thread const*>(index.internal_data());
    // Only the main thread has the other threads as its children.
    // Also, if there's not more than one thread, we won't draw that.
    if (thread.is_main_thread() && thread.current_state.process.threads.size() > 1)
        return thread.current_state.process.threads.size() - 1;
    return 0;
}

int ProcessModel::column_count(GUI::ModelIndex const&) const
{
    return Column::__Count;
}

String ProcessModel::column_name(int column) const
{
    switch (column) {
    case Column::Icon:
        return "";
    case Column::PID:
        return "PID";
    case Column::TID:
        return "TID";
    case Column::PPID:
        return "PPID";
    case Column::PGID:
        return "PGID";
    case Column::SID:
        return "SID";
    case Column::State:
        return "State";
    case Column::User:
        return "User";
    case Column::Priority:
        return "Pr";
    case Column::Virtual:
        return "Virtual";
    case Column::Physical:
        return "Physical";
    case Column::DirtyPrivate:
        return "Private";
    case Column::CleanInode:
        return "CleanI";
    case Column::PurgeableVolatile:
        return "Purg:V";
    case Column::PurgeableNonvolatile:
        return "Purg:N";
    case Column::CPU:
        return "CPU";
    case Column::Processor:
        return "Processor";
    case Column::Name:
        return "Name";
    case Column::Syscalls:
        return "Syscalls";
    case Column::InodeFaults:
        return "F:Inode";
    case Column::ZeroFaults:
        return "F:Zero";
    case Column::CowFaults:
        return "F:CoW";
    case Column::IPv4SocketReadBytes:
        return "IPv4 In";
    case Column::IPv4SocketWriteBytes:
        return "IPv4 Out";
    case Column::UnixSocketReadBytes:
        return "Unix In";
    case Column::UnixSocketWriteBytes:
        return "Unix Out";
    case Column::FileReadBytes:
        return "File In";
    case Column::FileWriteBytes:
        return "File Out";
    case Column::Pledge:
        return "Pledge";
    case Column::Veil:
        return "Veil";
    default:
        VERIFY_NOT_REACHED();
    }
}

GUI::Variant ProcessModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    VERIFY(is_within_range(index));

    if (role == GUI::ModelRole::TextAlignment) {
        switch (index.column()) {
        case Column::Icon:
        case Column::Name:
        case Column::State:
        case Column::User:
        case Column::Pledge:
        case Column::Veil:
            return Gfx::TextAlignment::CenterLeft;
        case Column::PID:
        case Column::TID:
        case Column::PPID:
        case Column::PGID:
        case Column::SID:
        case Column::Priority:
        case Column::Virtual:
        case Column::Physical:
        case Column::DirtyPrivate:
        case Column::CleanInode:
        case Column::PurgeableVolatile:
        case Column::PurgeableNonvolatile:
        case Column::CPU:
        case Column::Processor:
        case Column::Syscalls:
        case Column::InodeFaults:
        case Column::ZeroFaults:
        case Column::CowFaults:
        case Column::FileReadBytes:
        case Column::FileWriteBytes:
        case Column::UnixSocketReadBytes:
        case Column::UnixSocketWriteBytes:
        case Column::IPv4SocketReadBytes:
        case Column::IPv4SocketWriteBytes:
            return Gfx::TextAlignment::CenterRight;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    auto const& thread = *static_cast<Thread const*>(index.internal_data());

    if (role == GUI::ModelRole::Sort) {
        switch (index.column()) {
        case Column::Icon:
            return 0;
        case Column::PID:
            return thread.current_state.pid;
        case Column::TID:
            return thread.current_state.tid;
        case Column::PPID:
            return thread.current_state.ppid;
        case Column::PGID:
            return thread.current_state.pgid;
        case Column::SID:
            return thread.current_state.sid;
        case Column::State:
            return thread.current_state.state;
        case Column::User:
            return thread.current_state.user;
        case Column::Priority:
            return thread.current_state.priority;
        case Column::Virtual:
            return (int)thread.current_state.amount_virtual;
        case Column::Physical:
            return (int)thread.current_state.amount_resident;
        case Column::DirtyPrivate:
            return (int)thread.current_state.amount_dirty_private;
        case Column::CleanInode:
            return (int)thread.current_state.amount_clean_inode;
        case Column::PurgeableVolatile:
            return (int)thread.current_state.amount_purgeable_volatile;
        case Column::PurgeableNonvolatile:
            return (int)thread.current_state.amount_purgeable_nonvolatile;
        case Column::CPU:
            return thread.current_state.cpu_percent;
        case Column::Processor:
            return thread.current_state.cpu;
        case Column::Name:
            return thread.current_state.name;
        case Column::Syscalls:
            return thread.current_state.syscall_count;
        case Column::InodeFaults:
            return thread.current_state.inode_faults;
        case Column::ZeroFaults:
            return thread.current_state.zero_faults;
        case Column::CowFaults:
            return thread.current_state.cow_faults;
        case Column::IPv4SocketReadBytes:
            return thread.current_state.ipv4_socket_read_bytes;
        case Column::IPv4SocketWriteBytes:
            return thread.current_state.ipv4_socket_write_bytes;
        case Column::UnixSocketReadBytes:
            return thread.current_state.unix_socket_read_bytes;
        case Column::UnixSocketWriteBytes:
            return thread.current_state.unix_socket_write_bytes;
        case Column::FileReadBytes:
            return thread.current_state.file_read_bytes;
        case Column::FileWriteBytes:
            return thread.current_state.file_write_bytes;
        case Column::Pledge:
            return thread.current_state.pledge;
        case Column::Veil:
            return thread.current_state.veil;
        }
        VERIFY_NOT_REACHED();
    }

    if (role == GUI::ModelRole::Display) {
        switch (index.column()) {
        case Column::Icon:
            return icon_for(thread);
        case Column::PID:
            return thread.current_state.pid;
        case Column::TID:
            return thread.current_state.tid;
        case Column::PPID:
            return thread.current_state.ppid;
        case Column::PGID:
            return thread.current_state.pgid;
        case Column::SID:
            return thread.current_state.sid;
        case Column::State:
            return thread.current_state.state;
        case Column::User:
            return thread.current_state.user;
        case Column::Priority:
            return thread.current_state.priority;
        case Column::Virtual:
            return human_readable_size(thread.current_state.amount_virtual);
        case Column::Physical:
            return human_readable_size(thread.current_state.amount_resident);
        case Column::DirtyPrivate:
            return human_readable_size(thread.current_state.amount_dirty_private);
        case Column::CleanInode:
            return human_readable_size(thread.current_state.amount_clean_inode);
        case Column::PurgeableVolatile:
            return human_readable_size(thread.current_state.amount_purgeable_volatile);
        case Column::PurgeableNonvolatile:
            return human_readable_size(thread.current_state.amount_purgeable_nonvolatile);
        case Column::CPU:
            return String::formatted("{:.2}", thread.current_state.cpu_percent);
        case Column::Processor:
            return thread.current_state.cpu;
        case Column::Name:
            if (thread.current_state.kernel)
                return String::formatted("{} (*)", thread.current_state.name);
            return thread.current_state.name;
        case Column::Syscalls:
            return thread.current_state.syscall_count;
        case Column::InodeFaults:
            return thread.current_state.inode_faults;
        case Column::ZeroFaults:
            return thread.current_state.zero_faults;
        case Column::CowFaults:
            return thread.current_state.cow_faults;
        case Column::IPv4SocketReadBytes:
            return thread.current_state.ipv4_socket_read_bytes;
        case Column::IPv4SocketWriteBytes:
            return thread.current_state.ipv4_socket_write_bytes;
        case Column::UnixSocketReadBytes:
            return thread.current_state.unix_socket_read_bytes;
        case Column::UnixSocketWriteBytes:
            return thread.current_state.unix_socket_write_bytes;
        case Column::FileReadBytes:
            return thread.current_state.file_read_bytes;
        case Column::FileWriteBytes:
            return thread.current_state.file_write_bytes;
        case Column::Pledge:
            return thread.current_state.pledge;
        case Column::Veil:
            return thread.current_state.veil;
        }
    }

    if (role == GUI::ModelRole::Icon)
        return icon_for(thread);

    if (role == GUI::ModelRole::IconOpacity) {
        if (thread.current_state.uid != getuid())
            return 0.5f;
        return {};
    }

    return {};
}

GUI::Icon ProcessModel::icon_for(Thread const& thread) const
{
    if (thread.current_state.kernel)
        return m_kernel_process_icon;
    return GUI::FileIconProvider::icon_for_executable(thread.current_state.executable);
}

GUI::ModelIndex ProcessModel::index(int row, int column, GUI::ModelIndex const& parent) const
{
    if (row < 0 || column < 0)
        return {};
    // Process index; we display the main thread here.
    if (!parent.is_valid()) {
        if (row >= static_cast<int>(m_processes.size()))
            return {};
        auto corresponding_thread = m_processes[row].main_thread();
        if (!corresponding_thread.has_value())
            return {};
        return create_index(row, column, corresponding_thread.release_value().ptr());
    }
    // Thread under process.
    auto const& parent_thread = *static_cast<Thread const*>(parent.internal_data());
    auto const& process = parent_thread.current_state.process;
    // dbgln("Getting thread model index in process {} for col {} row {}", process.pid, column, row);
    if (row >= static_cast<int>(process.threads.size()))
        return {};
    return create_index(row, column, &process.non_main_thread(row));
}

int ProcessModel::thread_model_row(Thread const& thread) const
{
    auto const& process = thread.current_state.process;
    // A process's main thread uses the global process index.
    if (process.pid == thread.current_state.pid)
        return m_processes.find_first_index(process).value_or(0);

    return process.threads.find_first_index(thread).value_or(0);
}

GUI::ModelIndex ProcessModel::parent_index(GUI::ModelIndex const& index) const
{
    if (!index.is_valid())
        return {};
    auto const& thread = *static_cast<Thread*>(index.internal_data());
    // There's no parent for the main thread.
    if (thread.current_state.pid == thread.current_state.tid)
        return {};
    // FIXME: We can't use first_matching here (not even a const version) because Optional cannot contain references.
    auto const& parent = thread.current_state.process;
    if (!parent.main_thread().has_value())
        return {};

    return create_index(m_processes.find_first_index(parent).release_value(), index.column(), parent.main_thread().value().ptr());
}

Vector<GUI::ModelIndex> ProcessModel::matches(StringView searching, unsigned flags, GUI::ModelIndex const&)
{
    Vector<GUI::ModelIndex> found_indices;

    for (auto const& thread : m_threads) {
        if (string_matches(thread.value->current_state.name, searching, flags)) {
            auto tid_row = thread_model_row(thread.value);

            found_indices.append(create_index(tid_row, Column::Name, reinterpret_cast<void const*>(thread.value.ptr())));
            if (flags & FirstMatchOnly)
                break;
        }
    }

    return found_indices;
}

void ProcessModel::update()
{
    auto previous_tid_count = m_threads.size();
    auto all_processes = Core::ProcessStatisticsReader::get_all(m_proc_all);

    HashTable<int> live_tids;
    u64 total_time_scheduled_diff = 0;
    if (all_processes.has_value()) {
        if (m_has_total_scheduled_time)
            total_time_scheduled_diff = all_processes->total_time_scheduled - m_total_time_scheduled;

        m_total_time_scheduled = all_processes->total_time_scheduled;
        m_total_time_scheduled_kernel = all_processes->total_time_scheduled_kernel;
        m_has_total_scheduled_time = true;

        for (size_t i = 0; i < all_processes->processes.size(); ++i) {
            auto const& process = all_processes->processes[i];
            NonnullOwnPtr<Process>* process_state = nullptr;
            for (size_t i = 0; i < m_processes.size(); ++i) {
                auto* other_process = &m_processes.ptr_at(i);
                if ((*other_process)->pid == process.pid) {
                    process_state = other_process;
                    break;
                }
            }
            if (!process_state) {
                m_processes.append(make<Process>());
                process_state = &m_processes.ptr_at(m_processes.size() - 1);
            }
            (*process_state)->pid = process.pid;
            for (auto& thread : process.threads) {
                ThreadState state(**process_state);
                state.tid = thread.tid;
                state.pid = process.pid;
                state.ppid = process.ppid;
                state.pgid = process.pgid;
                state.sid = process.sid;
                state.time_user = thread.time_user;
                state.time_kernel = thread.time_kernel;
                state.kernel = process.kernel;
                state.executable = process.executable;
                state.name = thread.name;
                state.uid = process.uid;
                state.state = thread.state;
                state.user = process.username;
                state.pledge = process.pledge;
                state.veil = process.veil;
                state.cpu = thread.cpu;
                state.priority = thread.priority;
                state.amount_virtual = process.amount_virtual;
                state.amount_resident = process.amount_resident;
                state.amount_dirty_private = process.amount_dirty_private;
                state.amount_clean_inode = process.amount_clean_inode;
                state.amount_purgeable_volatile = process.amount_purgeable_volatile;
                state.amount_purgeable_nonvolatile = process.amount_purgeable_nonvolatile;
                state.syscall_count = thread.syscall_count;
                state.inode_faults = thread.inode_faults;
                state.zero_faults = thread.zero_faults;
                state.cow_faults = thread.cow_faults;
                state.unix_socket_read_bytes = thread.unix_socket_read_bytes;
                state.unix_socket_write_bytes = thread.unix_socket_write_bytes;
                state.ipv4_socket_read_bytes = thread.ipv4_socket_read_bytes;
                state.ipv4_socket_write_bytes = thread.ipv4_socket_write_bytes;
                state.file_read_bytes = thread.file_read_bytes;
                state.file_write_bytes = thread.file_write_bytes;
                state.cpu_percent = 0;

                auto thread_data = m_threads.ensure(thread.tid, [&] { return make_ref_counted<Thread>(**process_state); });
                thread_data->previous_state = move(thread_data->current_state);
                thread_data->current_state = move(state);
                if (auto maybe_thread_index = (*process_state)->threads.find_first_index(thread_data); maybe_thread_index.has_value()) {
                    (*process_state)->threads.ptr_at(maybe_thread_index.value()) = thread_data;
                } else {
                    (*process_state)->threads.append(thread_data);
                }

                live_tids.set(thread.tid);
            }
        }
    }

    for (auto& c : m_cpus) {
        c.total_cpu_percent = 0.0;
        c.total_cpu_percent_kernel = 0.0;
    }

    Vector<int, 16> tids_to_remove;
    for (auto& it : m_threads) {
        if (!live_tids.contains(it.key)) {
            tids_to_remove.append(it.key);
            continue;
        }
        auto& thread = *it.value;
        u64 time_scheduled_diff = (thread.current_state.time_user + thread.current_state.time_kernel)
            - (thread.previous_state.time_user + thread.previous_state.time_kernel);
        u64 time_scheduled_diff_kernel = thread.current_state.time_kernel - thread.previous_state.time_kernel;
        thread.current_state.cpu_percent = total_time_scheduled_diff > 0 ? (float)((time_scheduled_diff * 1000) / total_time_scheduled_diff) / 10.0f : 0;
        thread.current_state.cpu_percent_kernel = total_time_scheduled_diff > 0 ? (float)((time_scheduled_diff_kernel * 1000) / total_time_scheduled_diff) / 10.0f : 0;
        if (it.value->current_state.pid != 0) {
            auto& cpu_info = m_cpus[thread.current_state.cpu];
            cpu_info.total_cpu_percent += thread.current_state.cpu_percent;
            cpu_info.total_cpu_percent_kernel += thread.current_state.cpu_percent_kernel;
        }
    }

    // FIXME: Also remove dead threads from processes
    for (auto tid : tids_to_remove) {
        m_threads.remove(tid);
        for (size_t i = 0; i < m_processes.size(); ++i) {
            auto& process = m_processes[i];
            process.threads.remove_all_matching([&](auto const& thread) { return thread->current_state.tid == tid; });
            if (process.threads.size() == 0) {
                m_processes.remove(i);
                --i;
            }
        }
    }

    if (on_cpu_info_change)
        on_cpu_info_change(m_cpus);

    if (on_state_update)
        on_state_update(all_processes.has_value() ? all_processes->processes.size() : 0, m_threads.size());

    // FIXME: This is a rather hackish way of invalidating indices.
    //        It would be good if GUI::Model had a way to orchestrate removal/insertion while preserving indices.
    did_update(previous_tid_count == m_threads.size() ? GUI::Model::UpdateFlag::DontInvalidateIndices : GUI::Model::UpdateFlag::InvalidateAllIndices);
}
