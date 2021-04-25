/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/ioctl.h>
#include <unistd.h>

struct ThreadData {
    int tid;
    pid_t pid;
    pid_t pgid;
    pid_t pgp;
    pid_t sid;
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
    auto all_processes = Core::ProcessStatisticsReader::get_all();
    if (!all_processes.has_value())
        return {};

    Snapshot snapshot;
    for (auto& process : all_processes.value()) {
        for (auto& thread : process.threads) {
            snapshot.sum_times_scheduled += thread.times_scheduled;
            ThreadData thread_data;
            thread_data.tid = thread.tid;
            thread_data.pid = process.pid;
            thread_data.pgid = process.pgid;
            thread_data.pgp = process.pgp;
            thread_data.sid = process.sid;
            thread_data.uid = process.uid;
            thread_data.gid = process.gid;
            thread_data.ppid = process.ppid;
            thread_data.nfds = process.nfds;
            thread_data.name = process.name;
            thread_data.tty = process.tty;
            thread_data.amount_virtual = process.amount_virtual;
            thread_data.amount_resident = process.amount_resident;
            thread_data.amount_shared = process.amount_shared;
            thread_data.syscall_count = thread.syscall_count;
            thread_data.inode_faults = thread.inode_faults;
            thread_data.zero_faults = thread.zero_faults;
            thread_data.cow_faults = thread.cow_faults;
            thread_data.times_scheduled = thread.times_scheduled;
            thread_data.priority = thread.priority;
            thread_data.state = thread.state;
            thread_data.username = process.username;

            snapshot.map.set({ process.pid, thread.tid }, move(thread_data));
        }
    }

    return snapshot;
}

static bool g_window_size_changed = true;
static struct winsize g_window_size;

int main(int, char**)
{
#ifdef __serenity__
    if (pledge("stdio rpath tty sigaction ", nullptr) < 0) {
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
#endif

    signal(SIGWINCH, [](int) {
        g_window_size_changed = true;
    });

#ifdef __serenity__
    if (pledge("stdio rpath tty", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
#endif

    Vector<ThreadData*> threads;
    auto prev = get_snapshot();
    usleep(10000);
    for (;;) {
        if (g_window_size_changed) {
            int rc = ioctl(STDOUT_FILENO, TIOCGWINSZ, &g_window_size);
            if (rc < 0) {
                perror("ioctl(TIOCGWINSZ)");
                return 1;
            }
            g_window_size_changed = false;
        }

        auto current = get_snapshot();
        auto sum_diff = current.sum_times_scheduled - prev.sum_times_scheduled;

        printf("\033[3J\033[H\033[2J");
        printf("\033[47;30m%6s %3s %3s  %-9s  %-13s  %6s  %6s  %4s  %s\033[K\033[0m\n",
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
            it.value.cpu_percent = sum_diff > 0 ? ((times_scheduled_diff * 100) / sum_diff) : 0;
            it.value.cpu_percent_decimal = sum_diff > 0 ? (((times_scheduled_diff * 1000) / sum_diff) % 10) : 0;
            threads.append(&it.value);
        }

        quick_sort(threads, [](auto* p1, auto* p2) {
            return p2->times_scheduled_since_prev < p1->times_scheduled_since_prev;
        });

        int row = 0;
        for (auto* thread : threads) {
            int nprinted = printf("%6d %3d %2u   %-9s  %-13s  %6zu  %6zu  %2u.%1u  ",
                thread->pid,
                thread->tid,
                thread->priority,
                thread->username.characters(),
                thread->state.characters(),
                thread->amount_virtual / 1024,
                thread->amount_resident / 1024,
                thread->cpu_percent,
                thread->cpu_percent_decimal);

            int remaining = g_window_size.ws_col - nprinted;
            fwrite(thread->name.characters(), 1, max(0, min(remaining, (int)thread->name.length())), stdout);
            putchar('\n');

            if (++row >= (g_window_size.ws_row - 2))
                break;
        }
        threads.clear_with_capacity();
        prev = move(current);
        sleep(1);
    }
    return 0;
}
