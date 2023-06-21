/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObjectSerializer.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/SystemStatistics.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSSystemStatistics::SysFSSystemStatistics(SysFSDirectory const& parent_directory)
    : SysFSGlobalInformation(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSSystemStatistics> SysFSSystemStatistics::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSSystemStatistics(parent_directory)).release_nonnull();
}

ErrorOr<void> SysFSSystemStatistics::try_generate(KBufferBuilder& builder)
{
    auto json = TRY(JsonObjectSerializer<>::try_create(builder));
    auto total_time_scheduled = Scheduler::get_total_time_scheduled();
    TRY(json.add("total_time"sv, total_time_scheduled.total));
    TRY(json.add("kernel_time"sv, total_time_scheduled.total_kernel));
    TRY(json.add("user_time"sv, total_time_scheduled.total - total_time_scheduled.total_kernel));
    u64 idle_time = 0;
    Processor::for_each([&](Processor& processor) {
        idle_time += processor.time_spent_idle();
    });
    TRY(json.add("idle_time"sv, idle_time));
    TRY(json.finish());
    return {};
}

}
