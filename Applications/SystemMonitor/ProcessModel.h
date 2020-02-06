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

#pragma once

#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGUI/Model.h>
#include <unistd.h>

class GraphWidget;

struct PidAndTid {
    bool operator==(const PidAndTid& other) const
    {
        return pid == other.pid && tid == other.tid;
    }
    pid_t pid;
    int tid;
};

class ProcessModel final : public GUI::Model {
public:
    enum Column {
        Icon = 0,
        Name,
        CPU,
        State,
        Priority,
        EffectivePriority,
        User,
        PID,
        TID,
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
    virtual ColumnMetadata column_metadata(int column) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, Role = Role::Display) const override;
    virtual void update() override;

    Function<void(float)> on_new_cpu_data_point;

private:
    ProcessModel();

    struct ThreadState {
        int tid;
        pid_t pid;
        unsigned times_scheduled;
        String name;
        String state;
        String user;
        String pledge;
        String veil;
        u32 priority;
        u32 effective_priority;
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
        int icon_id;
    };

    struct Thread {
        ThreadState current_state;
        ThreadState previous_state;
    };

    HashMap<uid_t, String> m_usernames;
    HashMap<PidAndTid, NonnullOwnPtr<Thread>> m_threads;
    Vector<PidAndTid> m_pids;
    RefPtr<Gfx::Bitmap> m_generic_process_icon;
    RefPtr<Gfx::Bitmap> m_high_priority_icon;
    RefPtr<Gfx::Bitmap> m_low_priority_icon;
    RefPtr<Gfx::Bitmap> m_normal_priority_icon;
};

namespace AK {
template<>
struct Traits<PidAndTid> : public GenericTraits<PidAndTid> {
    static unsigned hash(const PidAndTid& value) { return pair_int_hash(value.pid, value.tid); }
};
}
