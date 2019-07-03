#include <AK/AKString.h>
#include <AK/HashMap.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/QuickSort.h>
#include <AK/Vector.h>
#include <LibCore/CFile.h>
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
    unsigned virtual_size;
    unsigned physical_size;
    unsigned nsched_since_prev;
    unsigned cpu_percent;
    unsigned cpu_percent_decimal;
};

struct Snapshot {
    HashMap<unsigned, Process> map;
    u32 sum_nsched { 0 };
};

static Snapshot get_snapshot()
{
    CFile file("/proc/all");
    if (!file.open(CIODevice::ReadOnly)) {
        fprintf(stderr, "Failed to open /proc/all: %s\n", file.error_string());
        exit(1);
    }

    Snapshot snapshot;

    auto file_contents = file.read_all();
    auto json = JsonValue::from_string({ file_contents.data(), file_contents.size() });
    json.as_array().for_each([&](auto& value) {
        const JsonObject& process_object = value.as_object();
        pid_t pid = process_object.get("pid").to_u32();
        unsigned nsched = process_object.get("times_scheduled").to_u32();
        snapshot.sum_nsched += nsched;
        Process process;
        process.pid = pid;
        process.nsched = nsched;
        unsigned uid = process_object.get("uid").to_u32();
        process.user = s_usernames->get(uid);
        process.priority = process_object.get("priority").to_string();
        process.state = process_object.get("state").to_string();
        process.name = process_object.get("name").to_string();
        process.virtual_size = process_object.get("amount_virtual").to_u32();
        process.physical_size = process_object.get("amount_resident").to_u32();
        snapshot.map.set(pid, move(process));
    });
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
        printf("\033[47;30m%6s  %3s  %-8s  %-8s  %6s  %6s  %4s  %s\033[K\033[0m\n",
            "PID",
            "PRI",
            "USER",
            "STATE",
            "VIRT",
            "PHYS",
            "%CPU",
            "NAME");
        for (auto& it : current.map) {
            pid_t pid = it.key;
            if (pid == 0)
                continue;
            u32 nsched_now = it.value.nsched;
            auto jt = prev.map.find(pid);
            if (jt == prev.map.end())
                continue;
            u32 nsched_before = (*jt).value.nsched;
            u32 nsched_diff = nsched_now - nsched_before;
            it.value.nsched_since_prev = nsched_diff;
            it.value.cpu_percent = ((nsched_diff * 100) / sum_diff);
            it.value.cpu_percent_decimal = (((nsched_diff * 1000) / sum_diff) % 10);
            processes.append(&it.value);
        }

        quick_sort(processes.begin(), processes.end(), [](auto* p1, auto* p2) {
            return p2->nsched_since_prev < p1->nsched_since_prev;
        });

        for (auto* process : processes) {
            printf("%6d  %c    %-8s  %-8s  %6u  %6u  %2u.%1u  %s\n",
                process->pid,
                process->priority[0],
                process->user.characters(),
                process->state.characters(),
                process->virtual_size / 1024,
                process->physical_size / 1024,
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
