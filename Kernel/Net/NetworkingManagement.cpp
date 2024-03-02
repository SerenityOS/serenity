/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Boot/Multiboot.h>
#include <Kernel/Library/KString.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Net/LoopbackAdapter.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Sections.h>

namespace Kernel {

static Singleton<NetworkingManagement> s_the;

NetworkingManagement& NetworkingManagement::the()
{
    return *s_the;
}

bool NetworkingManagement::is_initialized()
{
    return s_the.is_initialized();
}

UNMAP_AFTER_INIT NetworkingManagement::NetworkingManagement()
{
}

NonnullRefPtr<NetworkAdapter> NetworkingManagement::loopback_adapter() const
{
    return *m_loopback_adapter;
}

void NetworkingManagement::for_each(Function<void(NetworkAdapter&)> callback)
{
    m_adapters.for_each([&](auto& adapter) {
        callback(adapter);
    });
}

ErrorOr<void> NetworkingManagement::try_for_each(Function<ErrorOr<void>(NetworkAdapter&)> callback)
{
    return m_adapters.with([&](auto& adapters) -> ErrorOr<void> {
        for (auto& adapter : adapters)
            TRY(callback(adapter));
        return {};
    });
}

RefPtr<NetworkAdapter> NetworkingManagement::from_ipv4_address(IPv4Address const& address) const
{
    if (address[0] == 0 && address[1] == 0 && address[2] == 0 && address[3] == 0)
        return m_loopback_adapter;
    if (address[0] == 127)
        return m_loopback_adapter;
    return m_adapters.with([&](auto& adapters) -> RefPtr<NetworkAdapter> {
        for (auto& adapter : adapters) {
            if (adapter.ipv4_address() == address || adapter.ipv4_broadcast() == address)
                return adapter;
        }
        return nullptr;
    });
}

RefPtr<NetworkAdapter> NetworkingManagement::lookup_by_name(StringView name) const
{
    return m_adapters.with([&](auto& adapters) -> RefPtr<NetworkAdapter> {
        for (auto& adapter : adapters) {
            if (adapter.name() == name)
                return adapter;
        }
        return nullptr;
    });
}

ErrorOr<FixedStringBuffer<IFNAMSIZ>> NetworkingManagement::generate_interface_name_from_pci_address(PCI::Device& device)
{
    VERIFY(device.device_id().class_code().value() == 0x2);
    // Note: This stands for e - "Ethernet", p - "Port" as for PCI bus, "s" for slot as for PCI slot
    auto name = TRY(FixedStringBuffer<IFNAMSIZ>::formatted("ep{}s{}", device.device_id().address().bus(), device.device_id().address().device()));
    VERIFY(!NetworkingManagement::the().lookup_by_name(name.representable_view()));
    return name;
}

void NetworkingManagement::attach_adapter(NetworkAdapter& adapter)
{
    m_adapters.with([&](auto& adapters) { adapters.append(adapter); });
}

void NetworkingManagement::detach_adapter(NetworkAdapter& adapter)
{
    m_adapters.with([&](auto& adapters) { adapters.remove(adapter); });
}

bool NetworkingManagement::initialize()
{
    auto loopback = MUST(LoopbackAdapter::try_create());
    m_adapters.with([&](auto& adapters) { adapters.append(*loopback); });
    m_loopback_adapter = *loopback;
    return true;
}
}
