/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObjectSerializer.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Network/Route.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSNetworkRouteStats::SysFSNetworkRouteStats(SysFSDirectory const& parent_directory)
    : SysFSGlobalInformation(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSNetworkRouteStats> SysFSNetworkRouteStats::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSNetworkRouteStats(parent_directory)).release_nonnull();
}

ErrorOr<void> SysFSNetworkRouteStats::try_generate(KBufferBuilder& builder)
{
    auto array = TRY(JsonArraySerializer<>::try_create(builder));
    TRY(routing_table().with([&](auto const& table) -> ErrorOr<void> {
        for (auto& it : table) {
            auto obj = TRY(array.add_object());
            auto destination = TRY(it.destination.to_string());
            TRY(obj.add("destination"sv, destination->view()));
            auto gateway = TRY(it.gateway.to_string());
            TRY(obj.add("gateway"sv, gateway->view()));
            auto netmask = TRY(it.netmask.to_string());
            TRY(obj.add("genmask"sv, netmask->view()));
            TRY(obj.add("flags"sv, it.flags));
            TRY(obj.add("interface"sv, it.adapter->name()));
            TRY(obj.finish());
        }
        return {};
    }));
    TRY(array.finish());
    return {};
}

}
