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
#include <Kernel/Bus/PCI/Definitions.h>

namespace Kernel {

class NetworkAdapter;
class NetworkingManagement {
    friend class NetworkAdapter;
    AK_MAKE_ETERNAL

public:
    static NetworkingManagement& the();
    static bool is_initialized();
    bool initialize();

    NetworkingManagement();

    void for_each(Function<void(NetworkAdapter&)>);

    RefPtr<NetworkAdapter> from_ipv4_address(const IPv4Address&) const;
    RefPtr<NetworkAdapter> lookup_by_name(const StringView&) const;

    NonnullRefPtr<NetworkAdapter> loopback_adapter() const;

private:
    RefPtr<NetworkAdapter> determine_network_device(PCI::Address address) const;

    NonnullRefPtrVector<NetworkAdapter> m_adapters;
    RefPtr<NetworkAdapter> m_loopback_adapter;
    mutable Mutex m_lock { "Networking" };
};

}
