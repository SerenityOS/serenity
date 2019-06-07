#include <AK/AKString.h>
#include <AK/HashMap.h>
#include <AK/QuickSort.h>
#include <AK/Vector.h>
#include <fcntl.h>
#include <pwd.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

static HashMap<unsigned, String>* s_usernames;

struct Process {
    pid_t pid;
    unsigned nsched;
    String name;
    String state;
    String user;
    String priority;
    unsigned linear;
    unsigned committed;
    unsigned nsched_since_prev;
    unsigned cpu_percent;
    unsigned cpu_percent_decimal;
};

struct Snapshot {
    HashMap<unsigned, Process> map;
    dword sum_nsched { 0 };
};

static Snapshot get_snapshot()
{
    Snapshot snapshot;

    FILE* fp = fopen("/proc/all", "r");
    if (!fp) {
        perror("failed to open /proc/all");
        exit(1);
    }
    for (;;) {
        char buf[4096];
        char* ptr = fgets(buf, sizeof(buf), fp);
        if (!ptr)
            break;
        auto parts = String(buf, Chomp).split(',');
        if (parts.size() < 17)
            break;
        bool ok;
        pid_t pid = parts[0].to_uint(ok);
        ASSERT(ok);
        unsigned nsched = parts[1].to_uint(ok);
        ASSERT(ok);
        snapshot.sum_nsched += nsched;
        Process process;
        process.pid = pid;
        process.nsched = nsched;
        unsigned uid = parts[5].to_uint(ok);
        ASSERT(ok);
        process.user = s_usernames->get(uid);
        process.priority = parts[16];
        process.state = parts[7];
        process.name = parts[11];
        process.linear = parts[12].to_uint(ok);
        ASSERT(ok);
        process.committed = parts[13].to_uint(ok);
        ASSERT(ok);
        snapshot.map.set(pid, move(process));
    }
    int rc = fclose(fp);
    ASSERT(rc == 0);
    return snapshot;
}

int main(int, char**)
{
    s_usernames = new HashMap<unsigned, String>();
    setpwent();
    while (auto* passwd = getpwent())
        s_usernames->set(passwd->pw_uid, passwd->pw_name);
    endpwent();

    Vector<Process*> processes;
    auto prev = get_snapshot();
    usleep(10000);
    for (;;) {
        auto current = get_snapshot();
        auto sum_diff = current.sum_nsched - prev.sum_nsched;

        printf("\033[3J\033[H\033[2J");
        printf("\033[47;30m%6s  %3s  % 8s  % 8s  %6s  %6s  %4s  %s\033[K\033[0m\n",
            "PID",
            "PRI",
            "USER",
            "STATE",
            "LINEAR",
            "COMMIT",
            "%CPU",
            "NAME");
        for (auto& it : current.map) {
            pid_t pid = it.key;
            if (pid == 0)
                continue;
            dword nsched_now = it.value.nsched;
            auto jt = prev.map.find(pid);
            if (jt == prev.map.end())
                continue;
            dword nsched_before = (*jt).value.nsched;
            dword nsched_diff = nsched_now - nsched_before;
            it.value.nsched_since_prev = nsched_diff;
            it.value.cpu_percent = ((nsched_diff * 100) / sum_diff);
            it.value.cpu_percent_decimal = (((nsched_diff * 1000) / sum_diff) % 10);
            processes.append(&it.value);
        }

        quick_sort(processes.begin(), processes.end(), [](auto* p1, auto* p2) {
            return p2->nsched_since_prev < p1->nsched_since_prev;
        });

        for (auto* process : processes) {
            printf("%6d  %c    % 8s  % 8s  %6u  %6u  %2u.%1u  %s\n",
                process->pid,
                process->priority[0],
                process->user.characters(),
                process->state.characters(),
                process->linear / 1024,
                process->committed / 1024,
                process->cpu_percent,
                process->cpu_percent_decimal,
                process->name.characters());
        }
        processes.clear_with_capacity();
        prev = move(current);
        sleep(1);
    }
    return 0;
}
