#include <AK/AKString.h>
#include <AK/HashMap.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/QuickSort.h>
#include <AK/Vector.h>
#include <LibCore/CProcessStatisticsReader.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

struct ProcessData {
    CProcessStatistics stats;
    unsigned times_scheduled_since_prev { 0 };
    unsigned cpu_percent { 0 };
    unsigned cpu_percent_decimal { 0 };
};

struct Snapshot {
    HashMap<unsigned, ProcessData> map;
    u32 sum_times_scheduled { 0 };
};

static Snapshot get_snapshot()
{
    Snapshot snapshot;

    auto all_processes = CProcessStatisticsReader::get_all();

    for (auto& it : all_processes) {
        auto& stats = it.value;
        snapshot.sum_times_scheduled += stats.times_scheduled;
        ProcessData process_data;
        process_data.stats = stats;
        snapshot.map.set(stats.pid, move(process_data));
    }

    return snapshot;
}

int main(int, char**)
{
    Vector<ProcessData*> processes;
    auto prev = get_snapshot();
    usleep(10000);
    for (;;) {
        auto current = get_snapshot();
        auto sum_diff = current.sum_times_scheduled - prev.sum_times_scheduled;

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
            u32 times_scheduled_now = it.value.stats.times_scheduled;
            auto jt = prev.map.find(pid);
            if (jt == prev.map.end())
                continue;
            u32 times_scheduled_before = (*jt).value.stats.times_scheduled;
            u32 times_scheduled_diff = times_scheduled_now - times_scheduled_before;
            it.value.times_scheduled_since_prev = times_scheduled_diff;
            it.value.cpu_percent = ((times_scheduled_diff * 100) / sum_diff);
            it.value.cpu_percent_decimal = (((times_scheduled_diff * 1000) / sum_diff) % 10);
            processes.append(&it.value);
        }

        quick_sort(processes.begin(), processes.end(), [](auto* p1, auto* p2) {
            return p2->times_scheduled_since_prev < p1->times_scheduled_since_prev;
        });

        for (auto* process : processes) {
            printf("%6d  %c    %-8s  %-10s  %6zu  %6zu  %2u.%1u  %s\n",
                process->stats.pid,
                process->stats.priority[0],
                process->stats.username.characters(),
                process->stats.state.characters(),
                process->stats.amount_virtual / 1024,
                process->stats.amount_resident / 1024,
                process->cpu_percent,
                process->cpu_percent_decimal,
                process->stats.name.characters());
        }
        processes.clear_with_capacity();
        prev = move(current);
        sleep(1);
    }
    return 0;
}
