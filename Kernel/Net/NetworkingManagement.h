/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Library/NonnullLockRefPtr.h>
#include <Kernel/Library/NonnullLockRefPtrVector.h>
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

    static ErrorOr<NonnullOwnPtr<KString>> generate_interface_name_from_pci_address(PCI::DeviceIdentifier const&);

    NetworkingManagement();

    void for_each(Function<void(NetworkAdapter&)>);
    ErrorOr<void> try_for_each(Function<ErrorOr<void>(NetworkAdapter&)>);

    LockRefPtr<NetworkAdapter> from_ipv4_address(IPv4Address const&) const;
    LockRefPtr<NetworkAdapter> lookup_by_name(StringView) const;

    NonnullLockRefPtr<NetworkAdapter> loopback_adapter() const;

private:
    LockRefPtr<NetworkAdapter> determine_network_device(PCI::DeviceIdentifier const&) const;

    SpinlockProtected<NonnullLockRefPtrVector<NetworkAdapter>> m_adapters { LockRank::None };
    LockRefPtr<NetworkAdapter> m_loopback_adapter;
};

}
