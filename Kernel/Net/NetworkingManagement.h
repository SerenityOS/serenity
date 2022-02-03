/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullRefPtr.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/Types.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Locking/SpinlockProtected.h>
#include <Kernel/Memory/Region.h>

namespace Kernel {

class NetworkAdapter;
class NetworkingManagement {
    friend class NetworkAdapter;

public:
    static NetworkingManagement& the();
    static bool is_initialized();
    bool initialize();

    static ErrorOr<NonnullOwnPtr<KString>> generate_interface_name_from_pci_address(PCI::DeviceIdentifier const&);

    NetworkingManagement();

    void for_each(Function<void(NetworkAdapter&)>);

    RefPtr<NetworkAdapter> from_ipv4_address(const IPv4Address&) const;
    RefPtr<NetworkAdapter> lookup_by_name(StringView) const;

    NonnullRefPtr<NetworkAdapter> loopback_adapter() const;

private:
    RefPtr<NetworkAdapter> determine_network_device(PCI::DeviceIdentifier const&) const;

    SpinlockProtected<NonnullRefPtrVector<NetworkAdapter>> m_adapters;
    RefPtr<NetworkAdapter> m_loopback_adapter;
};

}
