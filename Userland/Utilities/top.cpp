/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/HashMap.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/QuickSort.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/ioctl.h>
#include <unistd.h>

struct TopOption {
    enum class SortBy {
        Pid,
        Tid,
        Priority,
        UserName,
        State,
        Virt,
        Phys,
        Cpu,
        Name
    };

    SortBy sort_by { SortBy::Cpu };
    int delay_time { 1 };
    HashTable<pid_t> pids_to_filter_by;
};

struct ThreadData {
    int tid;
    pid_t pid;
    pid_t pgid;
    pid_t pgp;
    pid_t sid;
    uid_t uid;
    gid_t gid;
    pid_t ppid;
    ByteString name;
    ByteString tty;
    size_t amount_virtual;
    size_t amount_resident;
    size_t amount_shared;
    unsigned syscall_count;
    unsigned inode_faults;
    unsigned zero_faults;
    unsigned cow_faults;
    u64 time_scheduled;

    u64 time_scheduled_since_prev { 0 };
    unsigned cpu_percent { 0 };
    unsigned cpu_percent_decimal { 0 };

    u32 priority;
    ByteString username;
    ByteString state;
};

struct PidAndTid {
    bool operator==(PidAndTid const& other) const
    {
        return pid == other.pid && tid == other.tid;
    }
    pid_t pid;
    int tid;
};

namespace AK {
template<>
struct Traits<PidAndTid> : public DefaultTraits<PidAndTid> {
    static unsigned hash(PidAndTid const& value) { return pair_int_hash(value.pid, value.tid); }
};
}

struct Snapshot {
    HashMap<PidAndTid, ThreadData> map;
    u64 total_time_scheduled { 0 };
    u64 total_time_scheduled_kernel { 0 };
};

static ErrorOr<Snapshot> get_snapshot(HashTable<pid_t> const& pids)
{
    auto all_processes = TRY(Core::ProcessStatisticsReader::get_all());

    Snapshot snapshot;
    for (auto& process : all_processes.processes) {
        if (!pids.is_empty() && !pids.contains(process.pid))
            continue;

        for (auto& thread : process.threads) {
            ThreadData thread_data;
            thread_data.tid = thread.tid;
            thread_data.pid = process.pid;
            thread_data.pgid = process.pgid;
            thread_data.pgp = process.pgp;
            thread_data.sid = process.sid;
            thread_data.uid = process.uid;
            thread_data.gid = process.gid;
            thread_data.ppid = process.ppid;
            thread_data.name = process.name;
            thread_data.tty = process.tty;
            thread_data.amount_virtual = process.amount_virtual;
            thread_data.amount_resident = process.amount_resident;
            thread_data.amount_shared = process.amount_shared;
            thread_data.syscall_count = thread.syscall_count;
            thread_data.inode_faults = thread.inode_faults;
            thread_data.zero_faults = thread.zero_faults;
            thread_data.cow_faults = thread.cow_faults;
            thread_data.time_scheduled = (u64)thread.time_user + (u64)thread.time_kernel;
            thread_data.priority = thread.priority;
            thread_data.state = thread.state;
            thread_data.username = process.username;

            snapshot.map.set({ process.pid, thread.tid }, move(thread_data));
        }
    }

    snapshot.total_time_scheduled = all_processes.total_time_scheduled;
    snapshot.total_time_scheduled_kernel = all_processes.total_time_scheduled_kernel;

    return snapshot;
}

static bool g_window_size_changed = true;
static struct winsize g_window_size;

static void parse_args(Main::Arguments arguments, TopOption& top_option)
{
    Core::ArgsParser::Option sort_by_option {
        Core::ArgsParser::OptionArgumentMode::Required,
        "Sort by field [pid, tid, pri, user, state, virt, phys, cpu, name]",
        "sort-by",
        's',
        nullptr,
        [&top_option](StringView sort_by_option) {
            if (sort_by_option == "pid"sv)
                top_option.sort_by = TopOption::SortBy::Pid;
            else if (sort_by_option == "tid"sv)
                top_option.sort_by = TopOption::SortBy::Tid;
            else if (sort_by_option == "pri"sv)
                top_option.sort_by = TopOption::SortBy::Priority;
            else if (sort_by_option == "user"sv)
                top_option.sort_by = TopOption::SortBy::UserName;
            else if (sort_by_option == "state"sv)
                top_option.sort_by = TopOption::SortBy::State;
            else if (sort_by_option == "virt"sv)
                top_option.sort_by = TopOption::SortBy::Virt;
            else if (sort_by_option == "phys"sv)
                top_option.sort_by = TopOption::SortBy::Phys;
            else if (sort_by_option == "cpu"sv)
                top_option.sort_by = TopOption::SortBy::Cpu;
            else if (sort_by_option == "name"sv)
                top_option.sort_by = TopOption::SortBy::Name;
            else
                return false;
            return true;
        }
    };
    HashTable<pid_t> pids;
    Core::ArgsParser args_parser;

    args_parser.set_general_help("Display information about processes");
    args_parser.add_option(top_option.delay_time, "Delay time interval in seconds", "delay-time", 'd', nullptr);
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "A comma-separated list of pids to filter by",
        .long_name = "pids",
        .short_name = 'p',
        .accept_value = [&pids](auto comma_separated_pids) {
            for (auto pid : comma_separated_pids.split_view(',')) {
                auto maybe_integer = pid.template to_number<pid_t>();
                if (!maybe_integer.has_value())
                    return false;

                pids.set(maybe_integer.value());
            }

            return true;
        },
    });
    args_parser.add_option(move(sort_by_option));
    args_parser.parse(arguments);
    top_option.pids_to_filter_by = move(pids);
}

static bool check_quit()
{
    char c = '\0';
    read(STDIN_FILENO, &c, sizeof(c));
    return c == 'q' || c == 'Q';
}

static struct termios g_previous_tty_settings;
static int g_old_stdin_status_flags;

static ErrorOr<void> setup_tty()
{
    g_old_stdin_status_flags = TRY(Core::System::fcntl(STDIN_FILENO, F_GETFL));
    TRY(Core::System::fcntl(STDIN_FILENO, F_SETFL, g_old_stdin_status_flags | O_NONBLOCK));
    g_previous_tty_settings = TRY(Core::System::tcgetattr(STDOUT_FILENO));

    struct termios raw = g_previous_tty_settings;
    raw.c_lflag &= ~(ECHO | ICANON);

    // Disable echo and line buffering
    TRY(Core::System::tcsetattr(STDOUT_FILENO, TCSAFLUSH, raw));

    return {};
}

static void restore_tty()
{
    auto maybe_error = Core::System::tcsetattr(STDOUT_FILENO, TCSAFLUSH, g_previous_tty_settings);
    if (maybe_error.is_error())
        warnln("Failed to reset original terminal state: {}", strerror(maybe_error.error().code()));

    auto maybe_fcntl_error = Core::System::fcntl(STDIN_FILENO, F_SETFL, g_old_stdin_status_flags);
    if (maybe_fcntl_error.is_error())
        warnln("Error restoring STDIN status flags: {}", strerror(maybe_error.error().code()));
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath tty sigaction"));
    TRY(Core::System::unveil("/sys/kernel/processes", "r"));
    TRY(Core::System::unveil("/etc/passwd", "r"));
    unveil(nullptr, nullptr);

    TRY(Core::System::signal(SIGWINCH, [](int) {
        g_window_size_changed = true;
    }));

    TopOption top_option;
    parse_args(arguments, top_option);

    TRY(setup_tty());
    ScopeGuard restore_tty_guard([] {
        restore_tty();
    });
    auto restore_tty_sigaction_handler = [](auto) {
        restore_tty();
        exit(1);
    };
    struct sigaction restore_tty_action;
    restore_tty_action.sa_handler = restore_tty_sigaction_handler;
    TRY(Core::System::sigaction(SIGINT, &restore_tty_action, nullptr));
    TRY(Core::System::sigaction(SIGTERM, &restore_tty_action, nullptr));

    TRY(Core::System::pledge("stdio rpath tty"));

    Vector<ThreadData*> threads;
    auto prev = TRY(get_snapshot(top_option.pids_to_filter_by));
    usleep(10000);
    bool should_quit = false;
    while (!should_quit) {
        if (g_window_size_changed) {
            TRY(Core::System::ioctl(STDOUT_FILENO, TIOCGWINSZ, &g_window_size));
            g_window_size_changed = false;
        }

        auto current = TRY(get_snapshot(top_option.pids_to_filter_by));
        auto total_scheduled_diff = current.total_time_scheduled - prev.total_time_scheduled;

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
            auto jt = prev.map.find(pid_and_tid);
            if (jt == prev.map.end())
                continue;
            auto time_scheduled_before = (*jt).value.time_scheduled;
            auto time_scheduled_diff = it.value.time_scheduled - time_scheduled_before;
            it.value.time_scheduled_since_prev = time_scheduled_diff;
            it.value.cpu_percent = total_scheduled_diff > 0 ? ((time_scheduled_diff * 100) / total_scheduled_diff) : 0;
            it.value.cpu_percent_decimal = total_scheduled_diff > 0 ? (((time_scheduled_diff * 1000) / total_scheduled_diff) % 10) : 0;
            threads.append(&it.value);
        }

        quick_sort(threads, [&top_option](auto* p1, auto* p2) {
            switch (top_option.sort_by) {
            case TopOption::SortBy::Pid:
                return p2->pid > p1->pid;
            case TopOption::SortBy::Tid:
                return p2->tid > p1->tid;
            case TopOption::SortBy::Priority:
                return p2->priority > p1->priority;
            case TopOption::SortBy::UserName:
                return p2->username > p1->username;
            case TopOption::SortBy::State:
                return p2->state > p1->state;
            case TopOption::SortBy::Virt:
                return p2->amount_virtual < p1->amount_virtual;
            case TopOption::SortBy::Phys:
                return p2->amount_resident < p1->amount_resident;
            case TopOption::SortBy::Name:
                return p2->name > p1->name;
            case TopOption::SortBy::Cpu:
                return p2->cpu_percent * 10 + p2->cpu_percent_decimal < p1->cpu_percent * 10 + p1->cpu_percent_decimal;
            default:
                return p2->time_scheduled_since_prev < p1->time_scheduled_since_prev;
            }
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

        for (int sleep_slice = 0; sleep_slice < top_option.delay_time * 1000; sleep_slice += 100) {
            should_quit = check_quit();
            if (should_quit)
                break;
            usleep(100 * 1000);
        }
    }

    return 0;
}
