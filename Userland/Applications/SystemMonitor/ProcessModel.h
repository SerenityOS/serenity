/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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
        Name,
        CPU,
        Processor,
        State,
        Priority,
        User,
        PID,
        TID,
        PPID,
        PGID,
        SID,
        Virtual,
        Physical,
        DirtyPrivate,
        CleanInode,
        PurgeableVolatile,
        PurgeableNonvolatile,
        Veil,
        Pledge,
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

    static NonnullRefPtr<ProcessModel> create() { return adopt(*new ProcessModel); }
    virtual ~ProcessModel() override;

    virtual int row_count(const GUI::ModelIndex&) const override;
    virtual int column_count(const GUI::ModelIndex&) const override;
    virtual String column_name(int column) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;
    virtual void update() override;

    struct CpuInfo {
        u32 id;
        float total_cpu_percent { 0.0 };
        float total_cpu_percent_kernel { 0.0 };

        explicit CpuInfo(u32 id)
            : id(id)
        {
        }
    };

    Function<void(const NonnullOwnPtrVector<CpuInfo>&)> on_cpu_info_change;
    Function<void(int process_count, int thread_count)> on_state_update;

    const NonnullOwnPtrVector<CpuInfo>& cpus() const { return m_cpus; }

private:
    ProcessModel();

    struct ThreadState {
        pid_t tid;
        pid_t pid;
        pid_t ppid;
        pid_t pgid;
        pid_t sid;
        unsigned ticks_user;
        unsigned ticks_kernel;
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
};
