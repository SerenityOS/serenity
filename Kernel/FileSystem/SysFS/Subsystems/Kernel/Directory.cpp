/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/Try.h>
#include <Kernel/API/DeviceFileTypes.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/CPUInfo.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Configuration/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/ConstantInformation.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/DeviceMajorNumberAllocations.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/DiskUsage.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/GlobalInformation.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Interrupts.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Keymap.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Log.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/MemoryStatus.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Network/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/PowerStateSwitch.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Processes.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Profile.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/RequestPanic.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/SystemStatistics.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Uptime.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<SysFSGlobalKernelStatsDirectory> SysFSGlobalKernelStatsDirectory::must_create(SysFSRootDirectory const& root_directory)
{
    auto global_kernel_stats_directory = adopt_ref_if_nonnull(new (nothrow) SysFSGlobalKernelStatsDirectory(root_directory)).release_nonnull();
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
        list.append(SysFSProfile::must_create(*global_kernel_stats_directory));
        list.append(SysFSPowerStateSwitchNode::must_create(*global_kernel_stats_directory));
        list.append(SysFSSystemRequestPanic::must_create(*global_kernel_stats_directory));

        list.append(SysFSDeviceMajorNumberAllocations::must_create(*global_kernel_stats_directory, DeviceNodeType::Block));
        list.append(SysFSDeviceMajorNumberAllocations::must_create(*global_kernel_stats_directory, DeviceNodeType::Character));

        list.append(SysFSGlobalNetworkStatsDirectory::must_create(*global_kernel_stats_directory));
        list.append(SysFSKernelConfigurationDirectory::must_create(*global_kernel_stats_directory));

        {
            auto builder = TRY(KBufferBuilder::try_create());
            MUST(builder.appendff("{}", g_boot_info.kernel_load_base));
            auto load_base_buffer = builder.build();
            VERIFY(load_base_buffer);
            list.append(SysFSSystemConstantInformation::must_create(*global_kernel_stats_directory, load_base_buffer.release_nonnull(), S_IRUSR, SysFSSystemConstantInformation::ReadableByJailedProcesses::No, SysFSSystemConstantInformation::NodeName::LoadBase));
        }

        {
            auto builder = TRY(KBufferBuilder::try_create());
            MUST(builder.append(kernel_command_line().string()));
            MUST(builder.append('\n'));
            auto command_line_buffer = builder.build();
            VERIFY(command_line_buffer);
            list.append(SysFSSystemConstantInformation::must_create(*global_kernel_stats_directory, command_line_buffer.release_nonnull(), S_IRUSR | S_IRGRP | S_IROTH, SysFSSystemConstantInformation::ReadableByJailedProcesses::No, SysFSSystemConstantInformation::NodeName::CommandLine));
        }

        {
            auto builder = TRY(KBufferBuilder::try_create());
            MUST(builder.append(kernel_command_line().system_mode()));
            MUST(builder.append('\n'));
            auto system_mode_buffer = builder.build();
            VERIFY(system_mode_buffer);
            list.append(SysFSSystemConstantInformation::must_create(*global_kernel_stats_directory, system_mode_buffer.release_nonnull(), S_IRUSR | S_IRGRP | S_IROTH, SysFSSystemConstantInformation::ReadableByJailedProcesses::No, SysFSSystemConstantInformation::NodeName::SystemMode));
        }
        return {};
    }));
    return global_kernel_stats_directory;
}

UNMAP_AFTER_INIT SysFSGlobalKernelStatsDirectory::SysFSGlobalKernelStatsDirectory(SysFSDirectory const& root_directory)
    : SysFSDirectory(root_directory)
{
}

}
