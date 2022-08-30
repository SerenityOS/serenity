/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Model.h>
#include <LibGUI/ModelIndex.h>
#include <sys/types.h>
#include <unistd.h>

class GraphWidget;

class ProcessModel final : public GUI::Model {
public:
    enum Column {
        Icon = 0,
        Name,
        PID,
        TID,
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
    virtual ~ProcessModel() override = default;

    virtual int tree_column() const override { return Column::Name; }
    virtual int row_count(GUI::ModelIndex const&) const override;
    virtual int column_count(GUI::ModelIndex const&) const override;
    virtual String column_name(int column) const override;
    virtual GUI::Variant data(GUI::ModelIndex const&, GUI::ModelRole) const override;
    virtual GUI::ModelIndex index(int row, int column, GUI::ModelIndex const& parent = {}) const override;
    virtual GUI::ModelIndex parent_index(GUI::ModelIndex const&) const override;
    virtual bool is_searchable() const override { return true; }
    virtual Vector<GUI::ModelIndex> matches(StringView, unsigned = MatchesFlag::AllMatching, GUI::ModelIndex const& = GUI::ModelIndex()) override;
    virtual bool is_column_sortable(int column_index) const override { return column_index != Column::Icon; }
    void update();
    bool is_default_column(int index) const;

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

    struct Process;

    struct ThreadState {
        pid_t tid { 0 };
        pid_t pid { 0 };
        pid_t ppid { 0 };
        pid_t pgid { 0 };
        pid_t sid { 0 };
        u64 time_user { 0 };
        u64 time_kernel { 0 };
        bool kernel { false };
        String executable { "" };
        String name { "" };
        uid_t uid { 0 };
        String state { "" };
        String user { "" };
        String pledge { "" };
        String veil { "" };
        u32 cpu { 0 };
        u32 priority { 0 };
        size_t amount_virtual { 0 };
        size_t amount_resident { 0 };
        size_t amount_dirty_private { 0 };
        size_t amount_clean_inode { 0 };
        size_t amount_purgeable_volatile { 0 };
        size_t amount_purgeable_nonvolatile { 0 };
        unsigned syscall_count { 0 };
        unsigned inode_faults { 0 };
        unsigned zero_faults { 0 };
        unsigned cow_faults { 0 };
        unsigned unix_socket_read_bytes { 0 };
        unsigned unix_socket_write_bytes { 0 };
        unsigned ipv4_socket_read_bytes { 0 };
        unsigned ipv4_socket_write_bytes { 0 };
        unsigned file_read_bytes { 0 };
        unsigned file_write_bytes { 0 };
        float cpu_percent { 0 };
        float cpu_percent_kernel { 0 };
        Process& process;

        ThreadState(Process& argument_process)
            : process(argument_process)
        {
        }
        ThreadState(ThreadState&& other) = default;
        ThreadState& operator=(ThreadState&& other)
        {
            this->tid = other.tid;
            this->pid = other.pid;
            this->ppid = other.ppid;
            this->pgid = other.pgid;
            this->sid = other.sid;
            this->time_user = other.time_user;
            this->time_kernel = other.time_kernel;
            this->kernel = other.kernel;
            this->executable = other.executable;
            this->name = other.name;
            this->uid = other.uid;
            this->state = other.state;
            this->user = other.user;
            this->pledge = other.pledge;
            this->veil = other.veil;
            this->cpu = other.cpu;
            this->priority = other.priority;
            this->amount_virtual = other.amount_virtual;
            this->amount_resident = other.amount_resident;
            this->amount_dirty_private = other.amount_dirty_private;
            this->amount_clean_inode = other.amount_clean_inode;
            this->amount_purgeable_volatile = other.amount_purgeable_volatile;
            this->amount_purgeable_nonvolatile = other.amount_purgeable_nonvolatile;
            this->syscall_count = other.syscall_count;
            this->inode_faults = other.inode_faults;
            this->zero_faults = other.zero_faults;
            this->cow_faults = other.cow_faults;
            this->unix_socket_read_bytes = other.unix_socket_read_bytes;
            this->unix_socket_write_bytes = other.unix_socket_write_bytes;
            this->ipv4_socket_read_bytes = other.ipv4_socket_read_bytes;
            this->ipv4_socket_write_bytes = other.ipv4_socket_write_bytes;
            this->file_read_bytes = other.file_read_bytes;
            this->file_write_bytes = other.file_write_bytes;
            this->cpu_percent = other.cpu_percent;
            this->cpu_percent_kernel = other.cpu_percent_kernel;
            this->process = other.process;

            return *this;
        }
        ~ThreadState() = default;
    };

    struct Thread : public RefCounted<Thread> {
        ThreadState current_state;
        ThreadState previous_state;

        Thread(Process& process)
            : current_state(process)
            , previous_state(process)
        {
        }

        bool operator==(Thread const& other) const
        {
            return current_state.tid == other.current_state.tid;
        }

        bool is_main_thread() const
        {
            return current_state.tid == current_state.process.pid;
        }
    };

    struct Process {
        pid_t pid;
        NonnullRefPtrVector<Thread> threads;

        bool operator==(Process const& other) const
        {
            return this->pid == other.pid;
        }

        Optional<NonnullRefPtr<Thread>> main_thread() const
        {
            return threads.first_matching([this](auto const thread) { return thread->current_state.tid == pid; });
        }

        // Return anything but the main thread; therefore, valid indices are anything up to threads.size()-1 exclusive.
        Thread const& non_main_thread(size_t index) const
        {
            auto main_thread_index = -1;
            for (size_t i = 0; i < threads.size(); ++i) {
                if (threads[i].is_main_thread()) {
                    main_thread_index = static_cast<int>(i);
                    break;
                }
            }
            VERIFY(main_thread_index >= 0);
            // Shift all indices starting from the main thread's index upwards, so that the user doesn't have to worry about index discontinuities.
            if (index >= static_cast<size_t>(main_thread_index))
                return threads[index + 1];
            return threads[index];
        }
    };

    GUI::Icon icon_for(Thread const& thread) const;

    int thread_model_row(Thread const& thread) const;

    // The thread list contains the same threads as the Process structs.
    HashMap<int, NonnullRefPtr<Thread>> m_threads;
    NonnullOwnPtrVector<Process> m_processes;
    NonnullOwnPtrVector<CpuInfo> m_cpus;
    RefPtr<Core::File> m_proc_all;
    GUI::Icon m_kernel_process_icon;
    u64 m_total_time_scheduled { 0 };
    u64 m_total_time_scheduled_kernel { 0 };
    bool m_has_total_scheduled_time { false };
};
