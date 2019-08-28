#pragma once

#include <Kernel/Net/NetworkAdapter.h>

struct RoutingDecision
{
    WeakPtr<NetworkAdapter> adapter;
    const MACAddress& next_hop;
};

RoutingDecision route_to(const IPv4Address& target, const IPv4Address& source);

Lockable<HashMap<IPv4Address, MACAddress>>& arp_table();
