/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "ProcessModel.h"
#include "GraphWidget.h"
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/SharedBuffer.h>
#include <LibCore/File.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <fcntl.h>
#include <stdio.h>

static ProcessModel* s_the;

ProcessModel& ProcessModel::the()
{
    ASSERT(s_the);
    return *s_the;
}

ProcessModel::ProcessModel()
{
    ASSERT(!s_the);
    s_the = this;
    m_generic_process_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/gear.png");
    m_high_priority_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/highpriority.png");
    m_low_priority_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/lowpriority.png");
    m_normal_priority_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/normalpriority.png");

    auto file = Core::File::construct("/proc/cpuinfo");
    if (file->open(Core::IODevice::ReadOnly)) {
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
}

ProcessModel::~ProcessModel()
{
}

int ProcessModel::row_count(const GUI::ModelIndex&) const
{
    return m_pids.size();
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
    case Column::EffectivePriority:
        return "EPr";
    case Column::Virtual:
        return "Virtual";
    case Column::Physical:
        return "Physical";
    case Column::DirtyPrivate:
        return "DirtyP";
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
        ASSERT_NOT_REACHED();
    }
}

static String pretty_byte_size(size_t size)
{
    return String::formatted("{}K", size / 1024);
}

GUI::Variant ProcessModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    ASSERT(is_valid(index));

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
        case Column::EffectivePriority:
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
            ASSERT_NOT_REACHED();
        }
    }

    auto it = m_threads.find(m_pids[index.row()]);
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
        case Column::EffectivePriority:
            return thread.current_state.effective_priority;
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
        ASSERT_NOT_REACHED();
        return {};
    }

    if (role == GUI::ModelRole::Display) {
        switch (index.column()) {
        case Column::Icon:
            if (thread.current_state.icon_id != -1) {
                auto icon_buffer = SharedBuffer::create_from_shbuf_id(thread.current_state.icon_id);
                if (icon_buffer) {
                    auto icon_bitmap = Gfx::Bitmap::create_with_shared_buffer(Gfx::BitmapFormat::RGBA32, *icon_buffer, { 16, 16 });
                    if (icon_bitmap)
                        return *icon_bitmap;
                }
            }
            return *m_generic_process_icon;
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
        case Column::EffectivePriority:
            return thread.current_state.effective_priority;
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
    }

    return {};
}

void ProcessModel::update()
{
    auto previous_pid_count = m_pids.size();
    auto all_processes = Core::ProcessStatisticsReader::get_all();

    unsigned last_sum_times_scheduled = 0;
    for (auto& it : m_threads)
        last_sum_times_scheduled += it.value->current_state.times_scheduled;

    HashTable<PidAndTid> live_pids;
    unsigned sum_times_scheduled = 0;
    for (auto& it : all_processes) {
        for (auto& thread : it.value.threads) {
            ThreadState state;
            state.pid = it.value.pid;
            state.user = it.value.username;
            state.pledge = it.value.pledge;
            state.veil = it.value.veil;
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
            state.amount_virtual = it.value.amount_virtual;
            state.amount_resident = it.value.amount_resident;
            state.amount_dirty_private = it.value.amount_dirty_private;
            state.amount_clean_inode = it.value.amount_clean_inode;
            state.amount_purgeable_volatile = it.value.amount_purgeable_volatile;
            state.amount_purgeable_nonvolatile = it.value.amount_purgeable_nonvolatile;
            state.icon_id = it.value.icon_id;

            state.name = thread.name;

            state.ppid = it.value.ppid;
            state.tid = thread.tid;
            state.pgid = it.value.pgid;
            state.sid = it.value.sid;
            state.times_scheduled = thread.times_scheduled;
            state.cpu = thread.cpu;
            state.cpu_percent = 0;
            state.priority = thread.priority;
            state.effective_priority = thread.effective_priority;
            state.state = thread.state;
            sum_times_scheduled += thread.times_scheduled;
            {
                auto pit = m_threads.find({ it.value.pid, thread.tid });
                if (pit == m_threads.end())
                    m_threads.set({ it.value.pid, thread.tid }, make<Thread>());
            }
            auto pit = m_threads.find({ it.value.pid, thread.tid });
            ASSERT(pit != m_threads.end());
            (*pit).value->previous_state = (*pit).value->current_state;
            (*pit).value->current_state = state;

            live_pids.set({ it.value.pid, thread.tid });
        }
    }

    m_pids.clear();
    for (auto& c : m_cpus)
        c.total_cpu_percent = 0.0;
    Vector<PidAndTid, 16> pids_to_remove;
    for (auto& it : m_threads) {
        if (!live_pids.contains(it.key)) {
            pids_to_remove.append(it.key);
            continue;
        }
        auto& process = *it.value;
        u32 times_scheduled_diff = process.current_state.times_scheduled - process.previous_state.times_scheduled;
        process.current_state.cpu_percent = ((float)times_scheduled_diff * 100) / (float)(sum_times_scheduled - last_sum_times_scheduled);
        if (it.key.pid != 0) {
            m_cpus[process.current_state.cpu].total_cpu_percent += process.current_state.cpu_percent;
            m_pids.append(it.key);
        }
    }
    for (auto pid : pids_to_remove)
        m_threads.remove(pid);

    if (on_cpu_info_change)
        on_cpu_info_change(m_cpus);

    // FIXME: This is a rather hackish way of invalidating indexes.
    //        It would be good if GUI::Model had a way to orchestrate removal/insertion while preserving indexes.
    did_update(previous_pid_count == m_pids.size() ? GUI::Model::UpdateFlag::DontInvalidateIndexes : GUI::Model::UpdateFlag::InvalidateAllIndexes);
}
