/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/CommandLine.h>
#include <Kernel/KString.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Multiboot.h>
#include <Kernel/Net/Intel/E1000ENetworkAdapter.h>
#include <Kernel/Net/Intel/E1000NetworkAdapter.h>
#include <Kernel/Net/LoopbackAdapter.h>
#include <Kernel/Net/NE2000/NetworkAdapter.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Net/Realtek/RTL8139NetworkAdapter.h>
#include <Kernel/Net/Realtek/RTL8168NetworkAdapter.h>
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

ErrorOr<NonnullOwnPtr<KString>> NetworkingManagement::generate_interface_name_from_pci_address(PCI::DeviceIdentifier const& device_identifier)
{
    VERIFY(device_identifier.class_code().value() == 0x2);
    // Note: This stands for e - "Ethernet", p - "Port" as for PCI bus, "s" for slot as for PCI slot
    auto name = TRY(KString::formatted("ep{}s{}", device_identifier.address().bus(), device_identifier.address().device()));
    VERIFY(!NetworkingManagement::the().lookup_by_name(name->view()));
    return name;
}

UNMAP_AFTER_INIT RefPtr<NetworkAdapter> NetworkingManagement::determine_network_device(PCI::DeviceIdentifier const& device_identifier) const
{
    if (auto candidate = E1000NetworkAdapter::try_to_initialize(device_identifier); !candidate.is_null())
        return candidate;
    if (auto candidate = E1000ENetworkAdapter::try_to_initialize(device_identifier); !candidate.is_null())
        return candidate;
    if (auto candidate = RTL8139NetworkAdapter::try_to_initialize(device_identifier); !candidate.is_null())
        return candidate;
    if (auto candidate = RTL8168NetworkAdapter::try_to_initialize(device_identifier); !candidate.is_null())
        return candidate;
    if (auto candidate = NE2000NetworkAdapter::try_to_initialize(device_identifier); !candidate.is_null())
        return candidate;
    return {};
}

bool NetworkingManagement::initialize()
{
    if (!kernel_command_line().is_physical_networking_disabled()) {
        PCI::enumerate([&](PCI::DeviceIdentifier const& device_identifier) {
            // Note: PCI class 2 is the class of Network devices
            if (device_identifier.class_code().value() != 0x02)
                return;
            if (auto adapter = determine_network_device(device_identifier); !adapter.is_null())
                m_adapters.with([&](auto& adapters) { adapters.append(adapter.release_nonnull()); });
        });
    }
    auto loopback = LoopbackAdapter::try_create();
    VERIFY(loopback);
    m_adapters.with([&](auto& adapters) { adapters.append(*loopback); });
    m_loopback_adapter = loopback;
    return true;
}
}
