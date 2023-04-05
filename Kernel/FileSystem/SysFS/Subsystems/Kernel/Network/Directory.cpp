/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/Try.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Network/ARP.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Network/Adapters.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Network/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Network/Local.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Network/Route.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Network/TCP.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Network/UDP.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<SysFSGlobalNetworkStatsDirectory> SysFSGlobalNetworkStatsDirectory::must_create(SysFSDirectory const& parent_directory)
{
    auto global_network_stats_directory = adopt_ref_if_nonnull(new (nothrow) SysFSGlobalNetworkStatsDirectory(parent_directory)).release_nonnull();
    MUST(global_network_stats_directory->m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(SysFSNetworkAdaptersStats::must_create(*global_network_stats_directory));
        list.append(SysFSNetworkARPStats::must_create(*global_network_stats_directory));
        list.append(SysFSNetworkRouteStats::must_create(*global_network_stats_directory));
        list.append(SysFSNetworkTCPStats::must_create(*global_network_stats_directory));
        list.append(SysFSLocalNetStats::must_create(*global_network_stats_directory));
        list.append(SysFSNetworkUDPStats::must_create(*global_network_stats_directory));
        return {};
    }));
    return global_network_stats_directory;
}

UNMAP_AFTER_INIT SysFSGlobalNetworkStatsDirectory::SysFSGlobalNetworkStatsDirectory(SysFSDirectory const& parent_directory)
    : SysFSDirectory(parent_directory)
{
}

}
