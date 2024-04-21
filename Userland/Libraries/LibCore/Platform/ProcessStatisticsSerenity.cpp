/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>

#if !defined(AK_OS_SERENITY)
#    error "This file is for Serenity OS only"
#endif

#include <LibCore/File.h>
#include <LibCore/Platform/ProcessStatistics.h>
#include <LibCore/ProcessStatisticsReader.h>

namespace Core::Platform {

ErrorOr<void> update_process_statistics(ProcessStatistics& statistics)
{
    static auto proc_all_file = TRY(Core::File::open("/sys/kernel/processes"sv, Core::File::OpenMode::Read));

    auto const all_processes = TRY(Core::ProcessStatisticsReader::get_all(*proc_all_file, false));

    auto const total_time_scheduled = all_processes.total_time_scheduled;
    auto const total_time_scheduled_diff = total_time_scheduled - statistics.total_time_scheduled;
    statistics.total_time_scheduled = total_time_scheduled;

    for (auto& process : statistics.processes) {
        auto it = all_processes.processes.find_if([&](auto& entry) { return entry.pid == process->pid; });
        if (!it.is_end()) {
            process->memory_usage_bytes = it->amount_resident;

            u64 time_process = 0;
            for (auto& thread : it->threads) {
                time_process += thread.time_user + thread.time_kernel;
            }
            u64 time_scheduled_diff = time_process - process->time_spent_in_process;

            process->time_spent_in_process = time_process;
            process->cpu_percent = 0.0;
            if (total_time_scheduled_diff > 0) {
                process->cpu_percent = static_cast<float>((time_scheduled_diff * 1000) / total_time_scheduled_diff) / 10.0f;
            }
        } else {
            process->memory_usage_bytes = 0;
            process->cpu_percent = 0.0;
            process->time_spent_in_process = 0;
        }
    }

    return {};
}

}
