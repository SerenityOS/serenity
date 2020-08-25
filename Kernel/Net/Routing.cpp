/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/HashMap.h>
#include <AK/Singleton.h>
#include <Kernel/Net/LoopbackAdapter.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Thread.h>

//#define ROUTING_DEBUG

namespace Kernel {

static AK::Singleton<Lockable<HashMap<IPv4Address, MACAddress>>> s_arp_table;

Lockable<HashMap<IPv4Address, MACAddress>>& arp_table()
{
    return *s_arp_table;
}

bool RoutingDecision::is_zero() const
{
    return adapter.is_null() || next_hop.is_zero();
}

RoutingDecision route_to(const IPv4Address& target, const IPv4Address& source, const RefPtr<NetworkAdapter> through)
{
    auto matches = [&](auto& adapter) {
        if (!through)
            return true;

        return through == adapter;
    };
    auto if_matches = [&](auto& adapter, const auto& mac) -> RoutingDecision {
        if (!matches(adapter))
            return { nullptr, {} };
        return { adapter, mac };
    };

    if (target[0] == 127)
        return if_matches(LoopbackAdapter::the(), LoopbackAdapter::the().mac_address());

    auto target_addr = target.to_u32();
    auto source_addr = source.to_u32();

    RefPtr<NetworkAdapter> local_adapter = nullptr;
    RefPtr<NetworkAdapter> gateway_adapter = nullptr;

    NetworkAdapter::for_each([source_addr, &target_addr, &local_adapter, &gateway_adapter, &matches](auto& adapter) {
        auto adapter_addr = adapter.ipv4_address().to_u32();
        auto adapter_mask = adapter.ipv4_netmask().to_u32();

        if (source_addr != 0 && source_addr != adapter_addr)
            return;

        if ((target_addr & adapter_mask) == (adapter_addr & adapter_mask) && matches(adapter))
            local_adapter = adapter;

        if (adapter.ipv4_gateway().to_u32() != 0 && matches(adapter))
            gateway_adapter = adapter;
    });

    if (local_adapter && target == local_adapter->ipv4_address())
        return { local_adapter, local_adapter->mac_address() };

    if (!local_adapter && !gateway_adapter) {
#ifdef ROUTING_DEBUG
        klog() << "Routing: Couldn't find a suitable adapter for route to " << target.to_string().characters();
#endif
        return { nullptr, {} };
    }

    RefPtr<NetworkAdapter> adapter = nullptr;
    IPv4Address next_hop_ip;

    if (local_adapter) {
#ifdef ROUTING_DEBUG
        klog() << "Routing: Got adapter for route (direct): " << local_adapter->name().characters() << " (" << local_adapter->ipv4_address().to_string().characters() << "/" << local_adapter->ipv4_netmask().to_string().characters() << ") for " << target.to_string().characters();
#endif
        adapter = local_adapter;
        next_hop_ip = target;
    } else if (gateway_adapter) {
#ifdef ROUTING_DEBUG
        klog() << "Routing: Got adapter for route (using gateway " << gateway_adapter->ipv4_gateway().to_string().characters() << "): " << gateway_adapter->name().characters() << " (" << gateway_adapter->ipv4_address().to_string().characters() << "/" << gateway_adapter->ipv4_netmask().to_string().characters() << ") for " << target.to_string().characters();
#endif
        adapter = gateway_adapter;
        next_hop_ip = gateway_adapter->ipv4_gateway();
    } else {
        return { nullptr, {} };
    }

    {
        LOCKER(arp_table().lock());
        auto addr = arp_table().resource().get(next_hop_ip);
        if (addr.has_value()) {
#ifdef ROUTING_DEBUG
            klog() << "Routing: Using cached ARP entry for " << next_hop_ip.to_string().characters() << " (" << addr.value().to_string().characters() << ")";
#endif
            return { adapter, addr.value() };
        }
    }

#ifdef ROUTING_DEBUG
    klog() << "Routing: Sending ARP request via adapter " << adapter->name().characters() << " for IPv4 address " << next_hop_ip.to_string().characters();
#endif

    ARPPacket request;
    request.set_operation(ARPOperation::Request);
    request.set_target_hardware_address({ 0xff, 0xff, 0xff, 0xff, 0xff, 0xff });
    request.set_target_protocol_address(next_hop_ip);
    request.set_sender_hardware_address(adapter->mac_address());
    request.set_sender_protocol_address(adapter->ipv4_address());
    adapter->send({ 0xff, 0xff, 0xff, 0xff, 0xff, 0xff }, request);

    (void)Thread::current()->block_until("Routing (ARP)", [next_hop_ip] {
        return arp_table().resource().get(next_hop_ip).has_value();
    });

    {
        LOCKER(arp_table().lock());
        auto addr = arp_table().resource().get(next_hop_ip);
        if (addr.has_value()) {
#ifdef ROUTING_DEBUG
            klog() << "Routing: Got ARP response using adapter " << adapter->name().characters() << " for " << next_hop_ip.to_string().characters() << " (" << addr.value().to_string().characters() << ")";
#endif
            return { adapter, addr.value() };
        }
    }

#ifdef ROUTING_DEBUG
    klog() << "Routing: Couldn't find route using adapter " << adapter->name().characters() << " for " << target.to_string().characters();
#endif

    return { nullptr, {} };
}

}
