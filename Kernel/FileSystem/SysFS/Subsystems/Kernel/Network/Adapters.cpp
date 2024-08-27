/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObjectSerializer.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Network/Adapters.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSNetworkAdaptersStats::SysFSNetworkAdaptersStats(SysFSDirectory const& parent_directory)
    : SysFSGlobalInformation(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSNetworkAdaptersStats> SysFSNetworkAdaptersStats::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSNetworkAdaptersStats(parent_directory)).release_nonnull();
}

ErrorOr<void> SysFSNetworkAdaptersStats::try_generate(KBufferBuilder& builder)
{
    auto array = TRY(JsonArraySerializer<>::try_create(builder));
    TRY(NetworkingManagement::the().try_for_each([&array](auto& adapter) -> ErrorOr<void> {
        auto obj = TRY(array.add_object());
        TRY(obj.add("name"sv, adapter.name()));
        TRY(obj.add("class_name"sv, adapter.class_name()));
        auto mac_address = TRY(adapter.mac_address().to_string());
        TRY(obj.add("mac_address"sv, mac_address->view()));
        if (!adapter.ipv4_address().is_zero()) {
            auto ipv4_address = TRY(adapter.ipv4_address().to_string());
            TRY(obj.add("ipv4_address"sv, ipv4_address->view()));
            auto ipv4_netmask = TRY(adapter.ipv4_netmask().to_string());
            TRY(obj.add("ipv4_netmask"sv, ipv4_netmask->view()));
        }
        if (!adapter.ipv6_address().is_zero()) {
            auto ipv6_address = TRY(adapter.ipv6_address().to_string());
            TRY(obj.add("ipv6_address"sv, ipv6_address->view()));
            auto ipv6_netmask = TRY(adapter.ipv6_netmask().to_string());
            TRY(obj.add("ipv6_netmask"sv, ipv6_netmask->view()));
        }
        TRY(obj.add("packets_in"sv, adapter.packets_in()));
        TRY(obj.add("bytes_in"sv, adapter.bytes_in()));
        TRY(obj.add("packets_out"sv, adapter.packets_out()));
        TRY(obj.add("bytes_out"sv, adapter.bytes_out()));
        TRY(obj.add("link_up"sv, adapter.link_up()));
        TRY(obj.add("link_speed"sv, adapter.link_speed()));
        TRY(obj.add("link_full_duplex"sv, adapter.link_full_duplex()));
        TRY(obj.add("mtu"sv, adapter.mtu()));
        TRY(obj.add("packets_dropped"sv, adapter.packets_dropped()));
        TRY(obj.finish());
        return {};
    }));
    TRY(array.finish());
    return {};
}

}
