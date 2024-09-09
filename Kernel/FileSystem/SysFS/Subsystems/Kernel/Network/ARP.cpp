/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObjectSerializer.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Network/ARP.h>
#include <Kernel/Net/IP/ARP.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSNetworkARPStats::SysFSNetworkARPStats(SysFSDirectory const& parent_directory)
    : SysFSGlobalInformation(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSNetworkARPStats> SysFSNetworkARPStats::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSNetworkARPStats(parent_directory)).release_nonnull();
}

ErrorOr<void> SysFSNetworkARPStats::try_generate(KBufferBuilder& builder)
{
    auto array = TRY(JsonArraySerializer<>::try_create(builder));
    TRY(arp_table().with([&](auto const& table) -> ErrorOr<void> {
        for (auto& it : table) {
            auto obj = TRY(array.add_object());
            auto mac_address = TRY(it.value.to_string());
            TRY(obj.add("mac_address"sv, mac_address->view()));
            auto ip_address = TRY(it.key.to_string());
            TRY(obj.add("ip_address"sv, ip_address->view()));
            TRY(obj.finish());
        }
        return {};
    }));
    TRY(array.finish());
    return {};
}

}
