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

    auto file_or_error = Core::File::open("/sys/kernel/cpuinfo"sv, Core::File::OpenMode::Read);
    if (!file_or_error.is_error()) {
        auto buffer_or_error = file_or_error.value()->read_until_eof();
        if (!buffer_or_error.is_error()) {
            auto json = JsonValue::from_string({ buffer_or_error.value() });
            auto cpuinfo_array = json.value().as_array();
            cpuinfo_array.for_each([&](auto& value) {
                auto& cpu_object = value.as_object();
                auto cpu_id = cpu_object.get_u32("processor"sv).value();
                m_cpus.append(make<CpuInfo>(cpu_id));
            });
        }
    }

    if (m_cpus.is_empty())
        m_cpus.append(make<CpuInfo>(0));

    m_kernel_process_icon = GUI::Icon::default_icon("gear"sv);
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

ErrorOr<String> ProcessModel::column_name(int column) const
{
    switch (column) {
    case Column::Icon:
        return String {};
    case Column::PID:
        return "PID"_string;
    case Column::TID:
        return "TID"_string;
    case Column::PPID:
        return "PPID"_string;
    case Column::PGID:
        return "PGID"_string;
    case Column::SID:
        return "SID"_string;
    case Column::State:
        return "State"_string;
    case Column::User:
        return "User"_string;
    case Column::Priority:
        return "Pr"_string;
    case Column::Virtual:
        return "Virtual"_string;
    case Column::Physical:
        return "Physical"_string;
    case Column::DirtyPrivate:
        return "Private"_string;
    case Column::CleanInode:
        return "CleanI"_string;
    case Column::PurgeableVolatile:
        return "Purg:V"_string;
    case Column::PurgeableNonvolatile:
        return "Purg:N"_string;
    case Column::CPU:
        return "CPU"_string;
    case Column::Processor:
        return "Processor"_string;
    case Column::Name:
        return "Name"_string;
    case Column::Syscalls:
        return "Syscalls"_string;
    case Column::InodeFaults:
        return "F:Inode"_string;
    case Column::ZeroFaults:
        return "F:Zero"_string;
    case Column::CowFaults:
        return "F:CoW"_string;
    case Column::IPv4SocketReadBytes:
        return "IPv4 In"_string;
    case Column::IPv4SocketWriteBytes:
        return "IPv4 Out"_string;
    case Column::UnixSocketReadBytes:
        return "Unix In"_string;
    case Column::UnixSocketWriteBytes:
        return "Unix Out"_string;
    case Column::FileReadBytes:
        return "File In"_string;
    case Column::FileWriteBytes:
        return "File Out"_string;
    case Column::Pledge:
        return "Pledge"_string;
    case Column::Veil:
        return "Veil"_string;
    case Column::Command:
        return "Command"_string;
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
        case Column::Command:
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
        case Column::Command:
            return thread.current_state.command.visit([](String const& cmdline) { return cmdline; }, [](auto const&) { return ""_string; });
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

    if (role == GUI::ModelRole::Display || role == DISPLAY_VERBOSE) {
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
            return ByteString::formatted("{:.2}", thread.current_state.cpu_percent);
        case Column::Processor:
            return thread.current_state.cpu;
        case Column::Name:
            if (thread.current_state.kernel)
                return ByteString::formatted("{} (*)", thread.current_state.name);
            return thread.current_state.name;
        case Column::Command:
            return thread.current_state.command.visit([](String const& cmdline) { return cmdline; }, [](auto const&) { return ""_string; });
        case Column::Syscalls:
            return thread.current_state.syscall_count;
        case Column::InodeFaults:
            return thread.current_state.inode_faults;
        case Column::ZeroFaults:
            return thread.current_state.zero_faults;
        case Column::CowFaults:
            return thread.current_state.cow_faults;
        case Column::IPv4SocketReadBytes:
            if (role == DISPLAY_VERBOSE)
                return human_readable_size_long(thread.current_state.ipv4_socket_read_bytes, UseThousandsSeparator::Yes);
            return human_readable_size(thread.current_state.ipv4_socket_read_bytes, AK::HumanReadableBasedOn::Base2, UseThousandsSeparator::Yes);
        case Column::IPv4SocketWriteBytes:
            if (role == DISPLAY_VERBOSE)
                return human_readable_size_long(thread.current_state.ipv4_socket_write_bytes, UseThousandsSeparator::Yes);
            return human_readable_size(thread.current_state.ipv4_socket_write_bytes, AK::HumanReadableBasedOn::Base2, UseThousandsSeparator::Yes);
        case Column::UnixSocketReadBytes:
            if (role == DISPLAY_VERBOSE)
                return human_readable_size_long(thread.current_state.unix_socket_read_bytes, UseThousandsSeparator::Yes);
            return human_readable_size(thread.current_state.unix_socket_read_bytes, AK::HumanReadableBasedOn::Base2, UseThousandsSeparator::Yes);
        case Column::UnixSocketWriteBytes:
            if (role == DISPLAY_VERBOSE)
                return human_readable_size_long(thread.current_state.unix_socket_write_bytes, UseThousandsSeparator::Yes);
            return human_readable_size(thread.current_state.unix_socket_write_bytes, AK::HumanReadableBasedOn::Base2, UseThousandsSeparator::Yes);
        case Column::FileReadBytes:
            if (role == DISPLAY_VERBOSE)
                return human_readable_size_long(thread.current_state.file_read_bytes, UseThousandsSeparator::Yes);
            return human_readable_size(thread.current_state.file_read_bytes, AK::HumanReadableBasedOn::Base2, UseThousandsSeparator::Yes);
        case Column::FileWriteBytes:
            if (role == DISPLAY_VERBOSE)
                return human_readable_size_long(thread.current_state.file_write_bytes, UseThousandsSeparator::Yes);
            return human_readable_size(thread.current_state.file_write_bytes, AK::HumanReadableBasedOn::Base2, UseThousandsSeparator::Yes);
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
        auto corresponding_thread = m_processes[row]->main_thread();
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
    if (process.pid == thread.current_state.pid) {
        auto it = m_processes.find_if([&](auto& entry) {
            return entry.ptr() == &process;
        });
        if (it == m_processes.end())
            return 0;
        return it.index();
    }

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

    auto process_index = [&]() -> size_t {
        auto it = m_processes.find_if([&](auto& entry) {
            return entry.ptr() == &parent;
        });
        if (it == m_processes.end())
            return 0;
        return it.index();
    }();
    return create_index(process_index, index.column(), parent.main_thread().value().ptr());
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

ErrorOr<String> ProcessModel::read_command_line(pid_t pid)
{
    auto file = TRY(Core::File::open(TRY(String::formatted("/proc/{}/cmdline", pid)), Core::File::OpenMode::Read));
    auto data = TRY(file->read_until_eof());
    auto json = TRY(JsonValue::from_string(StringView { data.bytes() }));
    auto array = json.as_array().values();
    return String::join(" "sv, array);
}

ErrorOr<void> ProcessModel::ensure_process_statistics_file()
{
    if (!m_process_statistics_file || !m_process_statistics_file->is_open())
        m_process_statistics_file = TRY(Core::File::open("/sys/kernel/processes"sv, Core::File::OpenMode::Read));

    return {};
}

void ProcessModel::update()
{
    auto result = ensure_process_statistics_file();
    if (result.is_error()) {
        dbgln("Process model couldn't be updated: {}", result.release_error());
        return;
    }

    auto all_processes_or_error = Core::ProcessStatisticsReader::get_all(*m_process_statistics_file, true);

    auto previous_tid_count = m_threads.size();

    HashTable<int> live_tids;
    u64 total_time_scheduled_diff = 0;
    size_t process_count = 0;
    if (!all_processes_or_error.is_error()) {
        auto all_processes = all_processes_or_error.value();
        process_count = all_processes.processes.size();
        if (m_has_total_scheduled_time)
            total_time_scheduled_diff = all_processes.total_time_scheduled - m_total_time_scheduled;

        m_total_time_scheduled = all_processes.total_time_scheduled;
        m_total_time_scheduled_kernel = all_processes.total_time_scheduled_kernel;
        m_has_total_scheduled_time = true;

        for (auto const& process : all_processes.processes) {
            // Don't include the Idle Task in process statistics.
            static constexpr pid_t IDLE_TASK_PID = 0;
            if (process.pid == IDLE_TASK_PID) {
                process_count--;
                continue;
            }

            NonnullOwnPtr<Process>* process_state = nullptr;
            for (size_t i = 0; i < m_processes.size(); ++i) {
                auto* other_process = &m_processes[i];
                if ((*other_process)->pid == process.pid) {
                    process_state = other_process;
                    break;
                }
            }
            if (!process_state) {
                m_processes.append(make<Process>());
                process_state = &m_processes.last();
            }

            auto add_thread_data = [&live_tids, this](int tid, Process& process_state, ThreadState state) {
                auto thread_data = m_threads.ensure(tid, [&] { return make_ref_counted<Thread>(process_state); });
                thread_data->previous_state = move(thread_data->current_state);
                thread_data->current_state = move(state);
                thread_data->read_command_line_if_necessary();

                if (auto maybe_thread_index = process_state.threads.find_first_index(thread_data); maybe_thread_index.has_value()) {
                    process_state.threads[maybe_thread_index.value()] = thread_data;
                } else {
                    process_state.threads.append(thread_data);
                }
                live_tids.set(tid);
            };

            (*process_state)->pid = process.pid;
            if (!process.threads.is_empty()) {
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

                    add_thread_data(thread.tid, **process_state, move(state));
                }
            } else {
                // FIXME: If there are no threads left in a process this is an indication
                // for a zombie process, so it should be handled differently - we add a mock thread
                // just to simulate a process with a single thread.
                // Find a way to untie the process representation from a main thread so we can
                // just represent a zombie process without creating a mock thread.
                ThreadState state(**process_state);
                state.tid = process.pid;
                state.pid = process.pid;
                state.ppid = process.ppid;
                state.pgid = process.pgid;
                state.sid = process.sid;
                state.kernel = process.kernel;
                state.executable = process.executable;
                state.name = process.name;
                state.uid = process.uid;
                state.state = "Zombie";
                state.user = process.username;
                state.pledge = process.pledge;
                state.veil = process.veil;
                state.amount_virtual = process.amount_virtual;
                state.amount_resident = process.amount_resident;
                state.amount_dirty_private = process.amount_dirty_private;
                state.amount_clean_inode = process.amount_clean_inode;
                state.amount_purgeable_volatile = process.amount_purgeable_volatile;
                state.amount_purgeable_nonvolatile = process.amount_purgeable_nonvolatile;

                add_thread_data(process.pid, **process_state, move(state));
            }
        }
    }

    for (auto& c : m_cpus) {
        c->total_cpu_percent = 0.0;
        c->total_cpu_percent_kernel = 0.0;
    }

    Vector<int, 16> tids_to_remove;
    for (auto& it : m_threads) {
        if (!live_tids.contains(it.key)) {
            tids_to_remove.append(it.key);
            continue;
        }
        if (it.value->current_state.state == "Zombie") {
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
            cpu_info->total_cpu_percent += thread.current_state.cpu_percent;
            cpu_info->total_cpu_percent_kernel += thread.current_state.cpu_percent_kernel;
        }
    }

    // FIXME: Also remove dead threads from processes
    for (auto tid : tids_to_remove) {
        m_threads.remove(tid);
        for (size_t i = 0; i < m_processes.size(); ++i) {
            auto& process = m_processes[i];
            process->threads.remove_all_matching([&](auto const& thread) { return thread->current_state.tid == tid; });
            if (process->threads.size() == 0) {
                m_processes.remove(i);
                --i;
            }
        }
    }

    if (on_cpu_info_change)
        on_cpu_info_change(m_cpus);

    if (on_state_update)
        on_state_update(process_count, m_threads.size());

    // FIXME: This is a rather hackish way of invalidating indices.
    //        It would be good if GUI::Model had a way to orchestrate removal/insertion while preserving indices.
    did_update(previous_tid_count == m_threads.size() ? GUI::Model::UpdateFlag::DontInvalidateIndices : GUI::Model::UpdateFlag::InvalidateAllIndices);
}

bool ProcessModel::is_default_column(int index) const
{
    switch (index) {
    case Column::PID:
    case Column::TID:
    case Column::Name:
    case Column::CPU:
    case Column::User:
    case Column::Virtual:
    case Column::DirtyPrivate:
        return true;
    default:
        return false;
    }
}
