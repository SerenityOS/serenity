/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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
#include <Kernel/Debug.h>
#include <Kernel/Net/LoopbackAdapter.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Thread.h>

namespace Kernel {

static AK::Singleton<Lockable<HashMap<IPv4Address, MACAddress>>> s_arp_table;

class ARPTableBlocker : public Thread::Blocker {
public:
    ARPTableBlocker(IPv4Address ip_addr, Optional<MACAddress>& addr);

    virtual const char* state_string() const override { return "Routing (ARP)"; }
    virtual Type blocker_type() const override { return Type::Routing; }
    virtual bool should_block() override { return m_should_block; }

    virtual void not_blocking(bool) override;

    bool unblock(bool from_add_blocker, const IPv4Address& ip_addr, const MACAddress& addr)
    {
        if (m_ip_addr != ip_addr)
            return false;

        {
            ScopedSpinLock lock(m_lock);
            if (m_did_unblock)
                return false;
            m_did_unblock = true;
            m_addr = addr;
        }

        if (!from_add_blocker)
            unblock_from_blocker();
        return true;
    }

    const IPv4Address& ip_addr() const { return m_ip_addr; }

private:
    const IPv4Address m_ip_addr;
    Optional<MACAddress>& m_addr;
    bool m_did_unblock { false };
    bool m_should_block { true };
};

class ARPTableBlockCondition : public Thread::BlockCondition {
public:
    void unblock(const IPv4Address& ip_addr, const MACAddress& addr)
    {
        BlockCondition::unblock([&](auto& b, void*, bool&) {
            VERIFY(b.blocker_type() == Thread::Blocker::Type::Routing);
            auto& blocker = static_cast<ARPTableBlocker&>(b);
            return blocker.unblock(false, ip_addr, addr);
        });
    }

protected:
    virtual bool should_add_blocker(Thread::Blocker& b, void*) override
    {
        VERIFY(b.blocker_type() == Thread::Blocker::Type::Routing);
        auto& blocker = static_cast<ARPTableBlocker&>(b);
        auto val = s_arp_table->resource().get(blocker.ip_addr());
        if (!val.has_value())
            return true;
        return blocker.unblock(true, blocker.ip_addr(), val.value());
    }
};

static AK::Singleton<ARPTableBlockCondition> s_arp_table_block_condition;

ARPTableBlocker::ARPTableBlocker(IPv4Address ip_addr, Optional<MACAddress>& addr)
    : m_ip_addr(ip_addr)
    , m_addr(addr)
{
    if (!set_block_condition(*s_arp_table_block_condition))
        m_should_block = false;
}

void ARPTableBlocker::not_blocking(bool timeout_in_past)
{
    VERIFY(timeout_in_past || !m_should_block);
    auto addr = s_arp_table->resource().get(ip_addr());

    ScopedSpinLock lock(m_lock);
    if (!m_did_unblock) {
        m_did_unblock = true;
        m_addr = move(addr);
    }
}

Lockable<HashMap<IPv4Address, MACAddress>>& arp_table()
{
    return *s_arp_table;
}

void update_arp_table(const IPv4Address& ip_addr, const MACAddress& addr)
{
    LOCKER(arp_table().lock());
    arp_table().resource().set(ip_addr, addr);
    s_arp_table_block_condition->unblock(ip_addr, addr);

    dmesgln("ARP table ({} entries):", arp_table().resource().size());
    for (auto& it : arp_table().resource()) {
        dmesgln("{} :: {}", it.value.to_string(), it.key.to_string());
    }
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
        dbgln_if(ROUTING_DEBUG, "Routing: Couldn't find a suitable adapter for route to {}", target);
        return { nullptr, {} };
    }

    RefPtr<NetworkAdapter> adapter = nullptr;
    IPv4Address next_hop_ip;

    if (local_adapter) {
        dbgln_if(ROUTING_DEBUG, "Routing: Got adapter for route (direct): {} ({}/{}) for {}",
            local_adapter->name(),
            local_adapter->ipv4_address(),
            local_adapter->ipv4_netmask(),
            target);

        adapter = local_adapter;
        next_hop_ip = target;
    } else if (gateway_adapter) {
        dbgln_if(ROUTING_DEBUG, "Routing: Got adapter for route (using gateway {}): {} ({}/{}) for {}",
            gateway_adapter->ipv4_gateway(),
            gateway_adapter->name(),
            gateway_adapter->ipv4_address(),
            gateway_adapter->ipv4_netmask(),
            target);
        adapter = gateway_adapter;
        next_hop_ip = gateway_adapter->ipv4_gateway();
    } else {
        return { nullptr, {} };
    }

    // If it's a broadcast, we already know everything we need to know.
    // FIXME: We should also deal with the case where `target_addr` is
    //        a broadcast to a subnet rather than a full broadcast.
    if (target_addr == 0xffffffff && matches(adapter))
        return { adapter, { 0xff, 0xff, 0xff, 0xff, 0xff, 0xff } };

    {
        LOCKER(arp_table().lock());
        auto addr = arp_table().resource().get(next_hop_ip);
        if (addr.has_value()) {
            dbgln_if(ROUTING_DEBUG, "Routing: Using cached ARP entry for {} ({})", next_hop_ip, addr.value().to_string());
            return { adapter, addr.value() };
        }
    }

    dbgln_if(ROUTING_DEBUG, "Routing: Sending ARP request via adapter {} for IPv4 address {}", adapter->name(), next_hop_ip);

    ARPPacket request;
    request.set_operation(ARPOperation::Request);
    request.set_target_hardware_address({ 0xff, 0xff, 0xff, 0xff, 0xff, 0xff });
    request.set_target_protocol_address(next_hop_ip);
    request.set_sender_hardware_address(adapter->mac_address());
    request.set_sender_protocol_address(adapter->ipv4_address());
    adapter->send({ 0xff, 0xff, 0xff, 0xff, 0xff, 0xff }, request);

    Optional<MACAddress> addr;
    if (!Thread::current()->block<ARPTableBlocker>({}, next_hop_ip, addr).was_interrupted()) {
        if (addr.has_value()) {
            dbgln_if(ROUTING_DEBUG, "Routing: Got ARP response using adapter {} for {} ({})",
                adapter->name(),
                next_hop_ip,
                addr.value().to_string());
            return { adapter, addr.value() };
        }
    }

    dbgln_if(ROUTING_DEBUG, "Routing: Couldn't find route using adapter {} for {}", adapter->name(), target);
    return { nullptr, {} };
}

}
