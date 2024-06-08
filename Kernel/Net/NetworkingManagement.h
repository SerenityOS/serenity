/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Devices/Generic/NetworkDeviceControlDevice.h>
#include <Kernel/Locking/SpinlockProtected.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Net/NetworkAdapter.h>

namespace Kernel {

class NetworkAdapter;
class NetworkingManagement {
    friend class NetworkAdapter;

public:
    static NetworkingManagement& the();
    static bool is_initialized();
    bool initialize();

    static ErrorOr<FixedStringBuffer<IFNAMSIZ>> generate_interface_name_from_pci_address(PCI::DeviceIdentifier const&);

    NetworkingManagement();

    void for_each(Function<void(NetworkAdapter&)>);
    ErrorOr<void> try_for_each(Function<ErrorOr<void>(NetworkAdapter&)>);

    ErrorOr<NonnullRefPtr<NetworkAdapter>> from_ipv4_address(IPv4Address const&) const;
    ErrorOr<NonnullRefPtr<NetworkAdapter>> lookup_by_name(StringView) const;
    ErrorOr<NonnullRefPtr<NetworkAdapter>> lookup_by_index(NetworkAdapter::AdapterIndex) const;

    NonnullRefPtr<NetworkAdapter> loopback_adapter() const;

    size_t allocate_adapter_index(Badge<NetworkAdapter>)
    {
        return m_adapter_index++;
    }

private:
    ErrorOr<NonnullRefPtr<NetworkAdapter>> determine_network_device(PCI::DeviceIdentifier const&) const;

    SpinlockProtected<Vector<NonnullRefPtr<NetworkAdapter>>, LockRank::None> m_adapters {};
    RefPtr<NetworkAdapter> m_loopback_adapter;
    NonnullRefPtr<NetworkDeviceControlDevice> const m_netdevctl_device;
    Atomic<size_t> m_adapter_index { 1 };
};

}
