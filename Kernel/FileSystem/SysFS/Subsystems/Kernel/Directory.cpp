/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/Try.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/CPUInfo.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/CommandLine.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/DiskUsage.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/GlobalInformation.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Interrupts.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Keymap.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/LoadBase.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Log.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/MemoryStatus.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Network/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/PowerStateSwitch.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Processes.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Profile.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/SystemMode.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/SystemStatistics.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Uptime.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Variables/Directory.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullLockRefPtr<SysFSGlobalKernelStatsDirectory> SysFSGlobalKernelStatsDirectory::must_create(SysFSRootDirectory const& root_directory)
{
    auto global_kernel_stats_directory = adopt_lock_ref_if_nonnull(new (nothrow) SysFSGlobalKernelStatsDirectory(root_directory)).release_nonnull();
    MUST(global_kernel_stats_directory->m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(SysFSDiskUsage::must_create(*global_kernel_stats_directory));
        list.append(SysFSMemoryStatus::must_create(*global_kernel_stats_directory));
        list.append(SysFSSystemStatistics::must_create(*global_kernel_stats_directory));
        list.append(SysFSOverallProcesses::must_create(*global_kernel_stats_directory));
        list.append(SysFSCPUInformation::must_create(*global_kernel_stats_directory));
        list.append(SysFSKernelLog::must_create(*global_kernel_stats_directory));
        list.append(SysFSInterrupts::must_create(*global_kernel_stats_directory));
        list.append(SysFSKeymap::must_create(*global_kernel_stats_directory));
        list.append(SysFSUptime::must_create(*global_kernel_stats_directory));
        list.append(SysFSCommandLine::must_create(*global_kernel_stats_directory));
        list.append(SysFSSystemMode::must_create(*global_kernel_stats_directory));
        list.append(SysFSProfile::must_create(*global_kernel_stats_directory));
        list.append(SysFSKernelLoadBase::must_create(*global_kernel_stats_directory));
        list.append(SysFSPowerStateSwitchNode::must_create(*global_kernel_stats_directory));

        list.append(SysFSGlobalNetworkStatsDirectory::must_create(*global_kernel_stats_directory));
        list.append(SysFSGlobalKernelVariablesDirectory::must_create(*global_kernel_stats_directory));
        return {};
    }));
    return global_kernel_stats_directory;
}

UNMAP_AFTER_INIT SysFSGlobalKernelStatsDirectory::SysFSGlobalKernelStatsDirectory(SysFSDirectory const& root_directory)
    : SysFSDirectory(root_directory)
{
}

}
