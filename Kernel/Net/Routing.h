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

void update_arp_table(const IPv4Address&, const MACAddress&, UpdateArp update);
RoutingDecision route_to(const IPv4Address& target, const IPv4Address& source, const RefPtr<NetworkAdapter> through = nullptr);

MutexProtected<HashMap<IPv4Address, MACAddress>>& arp_table();

}
