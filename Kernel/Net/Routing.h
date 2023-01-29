/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IPv4Address.h>
#include <Kernel/Library/NonnullLockRefPtr.h>
#include <Kernel/Locking/MutexProtected.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Thread.h>

namespace Kernel {

struct Route final : public AtomicRefCounted<Route> {
    Route(IPv4Address const& destination, IPv4Address const& gateway, IPv4Address const& netmask, u16 flags, NonnullLockRefPtr<NetworkAdapter> adapter)
        : destination(destination)
        , gateway(gateway)
        , netmask(netmask)
        , flags(flags)
        , adapter(adapter)
    {
    }

    bool operator==(Route const& other) const
    {
        return destination == other.destination && netmask == other.netmask && flags == other.flags && adapter.ptr() == other.adapter.ptr();
    }

    bool matches(Route const& other) const
    {
        return destination == other.destination && (gateway == other.gateway || other.gateway.is_zero()) && netmask == other.netmask && flags == other.flags && adapter.ptr() == other.adapter.ptr();
    }

    const IPv4Address destination;
    const IPv4Address gateway;
    const IPv4Address netmask;
    const u16 flags;
    NonnullLockRefPtr<NetworkAdapter> adapter;

    IntrusiveListNode<Route, LockRefPtr<Route>> route_list_node {};
    using RouteList = IntrusiveList<&Route::route_list_node>;
};

struct RoutingDecision {
    LockRefPtr<NetworkAdapter> adapter;
    MACAddress next_hop;

    bool is_zero() const;
};

enum class UpdateTable {
    Set,
    Delete,
};

void update_arp_table(IPv4Address const&, MACAddress const&, UpdateTable update);
ErrorOr<void> update_routing_table(IPv4Address const& destination, IPv4Address const& gateway, IPv4Address const& netmask, u16 flags, LockRefPtr<NetworkAdapter> const adapter, UpdateTable update);

enum class AllowUsingGateway {
    Yes,
    No,
};

RoutingDecision route_to(IPv4Address const& target, IPv4Address const& source, LockRefPtr<NetworkAdapter> const through = nullptr, AllowUsingGateway = AllowUsingGateway::Yes);

SpinlockProtected<HashMap<IPv4Address, MACAddress>, LockRank::None>& arp_table();
SpinlockProtected<Route::RouteList, LockRank::None>& routing_table();

}
