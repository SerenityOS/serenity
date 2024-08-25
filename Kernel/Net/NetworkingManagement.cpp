/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Boot/Multiboot.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Library/KString.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Net/Intel/E1000ENetworkAdapter.h>
#include <Kernel/Net/Intel/E1000NetworkAdapter.h>
#include <Kernel/Net/LoopbackAdapter.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Net/Realtek/RTL8168NetworkAdapter.h>
#include <Kernel/Net/VirtIO/VirtIONetworkAdapter.h>
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
            if (adapter->ipv4_address() == address || adapter->ipv4_broadcast() == address)
                return adapter;
        }
        return nullptr;
    });
}

RefPtr<NetworkAdapter> NetworkingManagement::from_ipv6_address(IPv6Address const& address) const
{
    if (address.is_loopback())
        return m_loopback_adapter;
    return m_adapters.with([&](auto& adapters) -> RefPtr<NetworkAdapter> {
        for (auto& adapter : adapters) {
            if (adapter->ipv6_address() == address || adapter->ipv6_multicast() == address)
                return adapter;
        }
        return nullptr;
    });
}

RefPtr<NetworkAdapter> NetworkingManagement::lookup_by_name(StringView name) const
{
    return m_adapters.with([&](auto& adapters) -> RefPtr<NetworkAdapter> {
        for (auto& adapter : adapters) {
            if (adapter->name() == name)
                return adapter;
        }
        return nullptr;
    });
}

ErrorOr<FixedStringBuffer<IFNAMSIZ>> NetworkingManagement::generate_interface_name_from_pci_address(PCI::DeviceIdentifier const& device_identifier)
{
    VERIFY(device_identifier.class_code().value() == 0x2);
    // Note: This stands for e - "Ethernet", p - "Port" as for PCI bus, "s" for slot as for PCI slot
    auto name = TRY(FixedStringBuffer<IFNAMSIZ>::formatted("ep{}s{}", device_identifier.address().bus(), device_identifier.address().device()));
    VERIFY(!NetworkingManagement::the().lookup_by_name(name.representable_view()));
    return name;
}

struct PCINetworkDriverInitializer {
    ErrorOr<bool> (*probe)(PCI::DeviceIdentifier const&) = nullptr;
    ErrorOr<NonnullRefPtr<NetworkAdapter>> (*create)(PCI::DeviceIdentifier const&) = nullptr;
};

static constexpr PCINetworkDriverInitializer s_initializers[] = {
    { RTL8168NetworkAdapter::probe, RTL8168NetworkAdapter::create },
    { E1000NetworkAdapter::probe, E1000NetworkAdapter::create },
    { E1000ENetworkAdapter::probe, E1000ENetworkAdapter::create },
    { VirtIONetworkAdapter::probe, VirtIONetworkAdapter::create },
};

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<NetworkAdapter>> NetworkingManagement::determine_network_device(PCI::DeviceIdentifier const& device_identifier) const
{
    for (auto& initializer : s_initializers) {
        auto initializer_probe_found_driver_match_or_error = initializer.probe(device_identifier);
        if (initializer_probe_found_driver_match_or_error.is_error()) {
            dmesgln("Networking: Failed to probe device {}, due to {}", device_identifier.address(), initializer_probe_found_driver_match_or_error.error());
            continue;
        }
        auto initializer_probe_found_driver_match = initializer_probe_found_driver_match_or_error.release_value();
        if (initializer_probe_found_driver_match) {
            auto adapter = TRY(initializer.create(device_identifier));
            TRY(adapter->initialize({}));
            return adapter;
        }
    }
    dmesgln("Networking: Failed to initialize device {}, unsupported network adapter", device_identifier.address());
    return Error::from_errno(ENODEV);
}

bool NetworkingManagement::initialize()
{
    if (!kernel_command_line().is_physical_networking_disabled() && !PCI::Access::is_disabled()) {
        MUST(PCI::enumerate([&](PCI::DeviceIdentifier const& device_identifier) {
            // Note: PCI class 2 is the class of Network devices
            if (device_identifier.class_code().value() != 0x02)
                return;
            auto result = determine_network_device(device_identifier);
            if (result.is_error()) {
                dmesgln("Failed to initialize network adapter ({} {}): {}", device_identifier.address(), device_identifier.hardware_id(), result.error());
                return;
            }
            m_adapters.with([&](auto& adapters) { adapters.append(*result.release_value()); });
        }));
    }
    auto loopback = MUST(LoopbackAdapter::try_create());
    m_adapters.with([&](auto& adapters) { adapters.append(*loopback); });
    m_loopback_adapter = *loopback;
    return true;
}
}
