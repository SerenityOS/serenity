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

#include <AK/HashMap.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/QuickSort.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

struct ThreadData {
    int tid;
    pid_t pid;
    unsigned pgid;
    unsigned pgp;
    unsigned sid;
    uid_t uid;
    gid_t gid;
    pid_t ppid;
    unsigned nfds;
    String name;
    String tty;
    size_t amount_virtual;
    size_t amount_resident;
    size_t amount_shared;
    unsigned syscall_count;
    unsigned inode_faults;
    unsigned zero_faults;
    unsigned cow_faults;
    int icon_id;
    unsigned times_scheduled;

    unsigned times_scheduled_since_prev { 0 };
    unsigned cpu_percent { 0 };
    unsigned cpu_percent_decimal { 0 };

    u32 priority;
    String username;
    String state;
};

struct PidAndTid {
    bool operator==(const PidAndTid& other) const
    {
        return pid == other.pid && tid == other.tid;
    }
    pid_t pid;
    int tid;
};

namespace AK {
template<>
struct Traits<PidAndTid> : public GenericTraits<PidAndTid> {
    static unsigned hash(const PidAndTid& value) { return pair_int_hash(value.pid, value.tid); }
};
}

struct Snapshot {
    HashMap<PidAndTid, ThreadData> map;
    u32 sum_times_scheduled { 0 };
};

static Snapshot get_snapshot()
{
    Snapshot snapshot;

    auto all_processes = Core::ProcessStatisticsReader::get_all();

    for (auto& it : all_processes) {
        auto& stats = it.value;
        for (auto& thread : stats.threads) {
            snapshot.sum_times_scheduled += thread.times_scheduled;
            ThreadData thread_data;
            thread_data.tid = thread.tid;
            thread_data.pid = stats.pid;
            thread_data.pgid = stats.pgid;
            thread_data.pgp = stats.pgp;
            thread_data.sid = stats.sid;
            thread_data.uid = stats.uid;
            thread_data.gid = stats.gid;
            thread_data.ppid = stats.ppid;
            thread_data.nfds = stats.nfds;
            thread_data.name = stats.name;
            thread_data.tty = stats.tty;
            thread_data.amount_virtual = stats.amount_virtual;
            thread_data.amount_resident = stats.amount_resident;
            thread_data.amount_shared = stats.amount_shared;
            thread_data.syscall_count = thread.syscall_count;
            thread_data.inode_faults = thread.inode_faults;
            thread_data.zero_faults = thread.zero_faults;
            thread_data.cow_faults = thread.cow_faults;
            thread_data.icon_id = stats.icon_id;
            thread_data.times_scheduled = thread.times_scheduled;
            thread_data.priority = thread.priority;
            thread_data.state = thread.state;
            thread_data.username = stats.username;

            snapshot.map.set({ stats.pid, thread.tid }, move(thread_data));
        }
    }

    return snapshot;
}

int main(int, char**)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/proc/all", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/etc/passwd", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    Vector<ThreadData*> threads;
    auto prev = get_snapshot();
    usleep(10000);
    for (;;) {
        auto current = get_snapshot();
        auto sum_diff = current.sum_times_scheduled - prev.sum_times_scheduled;

        printf("\033[3J\033[H\033[2J");
        printf("\033[47;30m%6s %3s %3s  %-8s  %-10s  %6s  %6s  %4s  %s\033[K\033[0m\n",
            "PID",
            "TID",
            "PRI",
            "USER",
            "STATE",
            "VIRT",
            "PHYS",
            "%CPU",
            "NAME");
        for (auto& it : current.map) {
            auto pid_and_tid = it.key;
            if (pid_and_tid.pid == 0)
                continue;
            u32 times_scheduled_now = it.value.times_scheduled;
            auto jt = prev.map.find(pid_and_tid);
            if (jt == prev.map.end())
                continue;
            u32 times_scheduled_before = (*jt).value.times_scheduled;
            u32 times_scheduled_diff = times_scheduled_now - times_scheduled_before;
            it.value.times_scheduled_since_prev = times_scheduled_diff;
            it.value.cpu_percent = ((times_scheduled_diff * 100) / sum_diff);
            it.value.cpu_percent_decimal = (((times_scheduled_diff * 1000) / sum_diff) % 10);
            threads.append(&it.value);
        }

        quick_sort(threads, [](auto* p1, auto* p2) {
            return p2->times_scheduled_since_prev < p1->times_scheduled_since_prev;
        });

        for (auto* thread : threads) {
            printf("%6d %3d %2u   %-8s  %-10s  %6zu  %6zu  %2u.%1u  %s\n",
                thread->pid,
                thread->tid,
                thread->priority,
                thread->username.characters(),
                thread->state.characters(),
                thread->amount_virtual / 1024,
                thread->amount_resident / 1024,
                thread->cpu_percent,
                thread->cpu_percent_decimal,
                thread->name.characters());
        }
        threads.clear_with_capacity();
        prev = move(current);
        sleep(1);
    }
    return 0;
}
