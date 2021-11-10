/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGUI/Model.h>
#include <unistd.h>

class GraphWidget;

class ProcessModel final : public GUI::Model {
public:
    enum Column {
        Icon = 0,
        PID,
        Name,
        CPU,
        State,
        User,
        Virtual,
        DirtyPrivate,
        Pledge,
        Physical,
        CleanInode,
        PurgeableVolatile,
        PurgeableNonvolatile,
        Veil,
        Processor,
        Priority,
        TID,
        PPID,
        PGID,
        SID,
        Syscalls,
        InodeFaults,
        ZeroFaults,
        CowFaults,
        FileReadBytes,
        FileWriteBytes,
        UnixSocketReadBytes,
        UnixSocketWriteBytes,
        IPv4SocketReadBytes,
        IPv4SocketWriteBytes,
        __Count
    };

    static ProcessModel& the();

    static NonnullRefPtr<ProcessModel> create() { return adopt_ref(*new ProcessModel); }
    virtual ~ProcessModel() override;

    virtual int row_count(GUI::ModelIndex const&) const override;
    virtual int column_count(GUI::ModelIndex const&) const override;
    virtual String column_name(int column) const override;
    virtual GUI::Variant data(GUI::ModelIndex const&, GUI::ModelRole) const override;
    virtual bool is_searchable() const override { return true; }
    virtual Vector<GUI::ModelIndex> matches(StringView, unsigned = MatchesFlag::AllMatching, GUI::ModelIndex const& = GUI::ModelIndex()) override;
    virtual bool is_column_sortable(int column_index) const override { return column_index != Column::Icon; }
    void update();

    struct CpuInfo {
        u32 id;
        float total_cpu_percent { 0.0 };
        float total_cpu_percent_kernel { 0.0 };

        explicit CpuInfo(u32 id)
            : id(id)
        {
        }
    };

    Function<void(NonnullOwnPtrVector<CpuInfo> const&)> on_cpu_info_change;
    Function<void(int process_count, int thread_count)> on_state_update;

    NonnullOwnPtrVector<CpuInfo> const& cpus() const { return m_cpus; }

private:
    ProcessModel();

    struct ThreadState {
        pid_t tid;
        pid_t pid;
        pid_t ppid;
        pid_t pgid;
        pid_t sid;
        u64 time_user;
        u64 time_kernel;
        bool kernel;
        String executable;
        String name;
        String state;
        String user;
        String pledge;
        String veil;
        u32 cpu;
        u32 priority;
        size_t amount_virtual;
        size_t amount_resident;
        size_t amount_dirty_private;
        size_t amount_clean_inode;
        size_t amount_purgeable_volatile;
        size_t amount_purgeable_nonvolatile;
        unsigned syscall_count;
        unsigned inode_faults;
        unsigned zero_faults;
        unsigned cow_faults;
        unsigned unix_socket_read_bytes;
        unsigned unix_socket_write_bytes;
        unsigned ipv4_socket_read_bytes;
        unsigned ipv4_socket_write_bytes;
        unsigned file_read_bytes;
        unsigned file_write_bytes;
        float cpu_percent;
        float cpu_percent_kernel;
    };

    struct Thread {
        ThreadState current_state;
        ThreadState previous_state;
    };

    HashMap<int, NonnullOwnPtr<Thread>> m_threads;
    NonnullOwnPtrVector<CpuInfo> m_cpus;
    Vector<int> m_tids;
    RefPtr<Core::File> m_proc_all;
    GUI::Icon m_kernel_process_icon;
    u64 m_total_time_scheduled { 0 };
    u64 m_total_time_scheduled_kernel { 0 };
    bool m_has_total_scheduled_time { false };
};
