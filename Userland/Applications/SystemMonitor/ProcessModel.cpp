/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProcessModel.h"
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibCore/File.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibGUI/FileIconProvider.h>

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
        auto json = JsonValue::from_string({ file->read_all() });
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

ProcessModel::~ProcessModel()
{
}

int ProcessModel::row_count(const GUI::ModelIndex&) const
{
    return m_tids.size();
}

int ProcessModel::column_count(const GUI::ModelIndex&) const
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

static String pretty_byte_size(size_t size)
{
    return String::formatted("{}K", size / 1024);
}

GUI::Variant ProcessModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    VERIFY(is_valid(index));

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

    auto it = m_threads.find(m_tids[index.row()]);
    auto& thread = *(*it).value;

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
        case Column::Icon: {
            if (thread.current_state.kernel)
                return m_kernel_process_icon;
            return GUI::FileIconProvider::icon_for_executable(thread.current_state.executable);
        }
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
            return pretty_byte_size(thread.current_state.amount_virtual);
        case Column::Physical:
            return pretty_byte_size(thread.current_state.amount_resident);
        case Column::DirtyPrivate:
            return pretty_byte_size(thread.current_state.amount_dirty_private);
        case Column::CleanInode:
            return pretty_byte_size(thread.current_state.amount_clean_inode);
        case Column::PurgeableVolatile:
            return pretty_byte_size(thread.current_state.amount_purgeable_volatile);
        case Column::PurgeableNonvolatile:
            return pretty_byte_size(thread.current_state.amount_purgeable_nonvolatile);
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

    return {};
}

void ProcessModel::update()
{
    auto previous_tid_count = m_tids.size();
    auto all_processes = Core::ProcessStatisticsReader::get_all(m_proc_all);

    u64 last_sum_ticks_scheduled = 0, last_sum_ticks_scheduled_kernel = 0;
    for (auto& it : m_threads) {
        auto& current_state = it.value->current_state;
        last_sum_ticks_scheduled += current_state.ticks_user + current_state.ticks_kernel;
        last_sum_ticks_scheduled_kernel += current_state.ticks_kernel;
    }

    HashTable<int> live_tids;
    u64 sum_ticks_scheduled = 0, sum_ticks_scheduled_kernel = 0;
    if (all_processes.has_value()) {
        for (auto& process : all_processes.value()) {
            for (auto& thread : process.threads) {
                ThreadState state;
                state.kernel = process.kernel;
                state.pid = process.pid;
                state.user = process.username;
                state.pledge = process.pledge;
                state.veil = process.veil;
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
                state.amount_virtual = process.amount_virtual;
                state.amount_resident = process.amount_resident;
                state.amount_dirty_private = process.amount_dirty_private;
                state.amount_clean_inode = process.amount_clean_inode;
                state.amount_purgeable_volatile = process.amount_purgeable_volatile;
                state.amount_purgeable_nonvolatile = process.amount_purgeable_nonvolatile;

                state.name = thread.name;
                state.executable = process.executable;

                state.ppid = process.ppid;
                state.tid = thread.tid;
                state.pgid = process.pgid;
                state.sid = process.sid;
                state.ticks_user = thread.ticks_user;
                state.ticks_kernel = thread.ticks_kernel;
                state.cpu = thread.cpu;
                state.cpu_percent = 0;
                state.priority = thread.priority;
                state.state = thread.state;
                sum_ticks_scheduled += thread.ticks_user + thread.ticks_kernel;
                sum_ticks_scheduled_kernel += thread.ticks_kernel;
                {
                    auto pit = m_threads.find(thread.tid);
                    if (pit == m_threads.end())
                        m_threads.set(thread.tid, make<Thread>());
                }
                auto pit = m_threads.find(thread.tid);
                VERIFY(pit != m_threads.end());
                (*pit).value->previous_state = (*pit).value->current_state;
                (*pit).value->current_state = state;

                live_tids.set(thread.tid);
            }
        }
    }

    m_tids.clear();
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
        u32 ticks_scheduled_diff = (thread.current_state.ticks_user + thread.current_state.ticks_kernel)
            - (thread.previous_state.ticks_user + thread.previous_state.ticks_kernel);
        u32 ticks_scheduled_diff_kernel = thread.current_state.ticks_kernel - thread.previous_state.ticks_kernel;
        thread.current_state.cpu_percent = ((float)ticks_scheduled_diff * 100) / (float)(sum_ticks_scheduled - last_sum_ticks_scheduled);
        thread.current_state.cpu_percent_kernel = ((float)ticks_scheduled_diff_kernel * 100) / (float)(sum_ticks_scheduled - last_sum_ticks_scheduled);
        if (it.value->current_state.pid != 0) {
            auto& cpu_info = m_cpus[thread.current_state.cpu];
            cpu_info.total_cpu_percent += thread.current_state.cpu_percent;
            cpu_info.total_cpu_percent_kernel += thread.current_state.cpu_percent_kernel;
            m_tids.append(it.key);
        }
    }
    for (auto tid : tids_to_remove)
        m_threads.remove(tid);

    if (on_cpu_info_change)
        on_cpu_info_change(m_cpus);

    if (on_state_update)
        on_state_update(all_processes->size(), m_threads.size());

    // FIXME: This is a rather hackish way of invalidating indices.
    //        It would be good if GUI::Model had a way to orchestrate removal/insertion while preserving indices.
    did_update(previous_tid_count == m_tids.size() ? GUI::Model::UpdateFlag::DontInvalidateIndices : GUI::Model::UpdateFlag::InvalidateAllIndices);
}
