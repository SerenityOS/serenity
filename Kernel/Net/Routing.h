/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Locking/MutexProtected.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Thread.h>

namespace Kernel {

struct RoutingDecision {
    RefPtr<NetworkAdapter> adapter;
    MACAddress next_hop;

    bool is_zero() const;
};

enum class UpdateArp {
    Set,
    Delete,
};

void update_arp_table(IPv4Address const&, MACAddress const&, UpdateArp update);

enum class AllowUsingGateway {
    Yes,
    No,
};

RoutingDecision route_to(IPv4Address const& target, IPv4Address const& source, RefPtr<NetworkAdapter> const through = nullptr, AllowUsingGateway = AllowUsingGateway::Yes);

SpinlockProtected<HashMap<IPv4Address, MACAddress>>& arp_table();

}
