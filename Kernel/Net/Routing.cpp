/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashMap.h>
#include <AK/Singleton.h>
#include <Kernel/Debug.h>
#include <Kernel/Locking/MutexProtected.h>
#include <Kernel/Net/LoopbackAdapter.h>
#include <Kernel/Net/NetworkTask.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Thread.h>

namespace Kernel {

static Singleton<SpinlockProtected<HashMap<IPv4Address, MACAddress>>> s_arp_table;

class ARPTableBlocker final : public Thread::Blocker {
public:
    ARPTableBlocker(IPv4Address ip_addr, Optional<MACAddress>& addr);

    virtual StringView state_string() const override { return "Routing (ARP)"sv; }
    virtual Type blocker_type() const override { return Type::Routing; }
    virtual bool setup_blocker() override;

    virtual void will_unblock_immediately_without_blocking(UnblockImmediatelyReason) override;

    bool unblock_if_matching_ip_address(bool from_add_blocker, IPv4Address const& ip_address, MACAddress const& mac_address)
    {
        if (m_ip_address != ip_address)
            return false;

        {
            SpinlockLocker lock(m_lock);
            if (m_did_unblock)
                return false;
            m_did_unblock = true;
            m_mac_address = mac_address;
        }

        if (!from_add_blocker)
            unblock_from_blocker();
        return true;
    }

    IPv4Address const& ip_address() const { return m_ip_address; }

private:
    IPv4Address const m_ip_address;
    Optional<MACAddress>& m_mac_address;
    bool m_did_unblock { false };
};

class ARPTableBlockerSet final : public Thread::BlockerSet {
public:
    void unblock_blockers_waiting_for_ipv4_address(IPv4Address const& ipv4_address, MACAddress const& mac_address)
    {
        BlockerSet::unblock_all_blockers_whose_conditions_are_met([&](auto& b, void*, bool&) {
            VERIFY(b.blocker_type() == Thread::Blocker::Type::Routing);
            auto& blocker = static_cast<ARPTableBlocker&>(b);
            return blocker.unblock_if_matching_ip_address(false, ipv4_address, mac_address);
        });
    }

protected:
    virtual bool should_add_blocker(Thread::Blocker& b, void*) override
    {
        VERIFY(b.blocker_type() == Thread::Blocker::Type::Routing);
        auto& blocker = static_cast<ARPTableBlocker&>(b);
        auto maybe_mac_address = arp_table().with([&](auto const& table) -> auto {
            return table.get(blocker.ip_address());
        });
        if (!maybe_mac_address.has_value())
            return true;
        return blocker.unblock_if_matching_ip_address(true, blocker.ip_address(), maybe_mac_address.value());
    }
};

static Singleton<ARPTableBlockerSet> s_arp_table_blocker_set;

ARPTableBlocker::ARPTableBlocker(IPv4Address ip_addr, Optional<MACAddress>& addr)
    : m_ip_address(ip_addr)
    , m_mac_address(addr)
{
}

bool ARPTableBlocker::setup_blocker()
{
    return add_to_blocker_set(*s_arp_table_blocker_set);
}

void ARPTableBlocker::will_unblock_immediately_without_blocking(UnblockImmediatelyReason)
{
    auto addr = arp_table().with([&](auto const& table) -> auto {
        return table.get(ip_address());
    });

    SpinlockLocker lock(m_lock);
    if (!m_did_unblock) {
        m_did_unblock = true;
        m_mac_address = move(addr);
    }
}

SpinlockProtected<HashMap<IPv4Address, MACAddress>>& arp_table()
{
    return *s_arp_table;
}

void update_arp_table(IPv4Address const& ip_addr, MACAddress const& addr, UpdateArp update)
{
    arp_table().with([&](auto& table) {
        if (update == UpdateArp::Set)
            table.set(ip_addr, addr);
        if (update == UpdateArp::Delete)
            table.remove(ip_addr);
    });
    s_arp_table_blocker_set->unblock_blockers_waiting_for_ipv4_address(ip_addr, addr);

    if constexpr (ARP_DEBUG) {
        arp_table().with([&](auto const& table) {
            dmesgln("ARP table ({} entries):", table.size());
            for (auto& it : table)
                dmesgln("{} :: {}", it.value.to_string(), it.key.to_string());
        });
    }
}

bool RoutingDecision::is_zero() const
{
    return adapter.is_null() || next_hop.is_zero();
}

static MACAddress multicast_ethernet_address(IPv4Address const& address)
{
    return MACAddress { 0x01, 0x00, 0x5e, (u8)(address[1] & 0x7f), address[2], address[3] };
}

RoutingDecision route_to(IPv4Address const& target, IPv4Address const& source, RefPtr<NetworkAdapter> const through, AllowUsingGateway allow_using_gateway)
{
    auto matches = [&](auto& adapter) {
        if (!through)
            return true;

        return through == adapter;
    };
    auto if_matches = [&](auto& adapter, auto const& mac) -> RoutingDecision {
        if (!matches(adapter))
            return { nullptr, {} };
        return { adapter, mac };
    };

    if (target[0] == 0 && target[1] == 0 && target[2] == 0 && target[3] == 0)
        return if_matches(*NetworkingManagement::the().loopback_adapter(), NetworkingManagement::the().loopback_adapter()->mac_address());
    if (target[0] == 127)
        return if_matches(*NetworkingManagement::the().loopback_adapter(), NetworkingManagement::the().loopback_adapter()->mac_address());

    auto target_addr = target.to_u32();
    auto source_addr = source.to_u32();

    RefPtr<NetworkAdapter> local_adapter = nullptr;
    RefPtr<NetworkAdapter> gateway_adapter = nullptr;

    NetworkingManagement::the().for_each([source_addr, &target_addr, &local_adapter, &gateway_adapter, &matches, &through](NetworkAdapter& adapter) {
        auto adapter_addr = adapter.ipv4_address().to_u32();
        auto adapter_mask = adapter.ipv4_netmask().to_u32();

        if (target_addr == adapter_addr) {
            local_adapter = NetworkingManagement::the().loopback_adapter();
            return;
        }

        if (!adapter.link_up() || (adapter_addr == 0 && !through))
            return;

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
    } else if (gateway_adapter && allow_using_gateway == AllowUsingGateway::Yes) {
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

    if (adapter == NetworkingManagement::the().loopback_adapter())
        return { adapter, adapter->mac_address() };

    if ((target_addr & IPv4Address { 240, 0, 0, 0 }.to_u32()) == IPv4Address { 224, 0, 0, 0 }.to_u32())
        return { adapter, multicast_ethernet_address(target) };

    {
        auto addr = arp_table().with([&](auto const& table) -> auto {
            return table.get(next_hop_ip);
        });
        if (addr.has_value()) {
            dbgln_if(ARP_DEBUG, "Routing: Using cached ARP entry for {} ({})", next_hop_ip, addr.value().to_string());
            return { adapter, addr.value() };
        }
    }

    dbgln_if(ARP_DEBUG, "Routing: Sending ARP request via adapter {} for IPv4 address {}", adapter->name(), next_hop_ip);

    ARPPacket request;
    request.set_operation(ARPOperation::Request);
    request.set_target_hardware_address({ 0xff, 0xff, 0xff, 0xff, 0xff, 0xff });
    request.set_target_protocol_address(next_hop_ip);
    request.set_sender_hardware_address(adapter->mac_address());
    request.set_sender_protocol_address(adapter->ipv4_address());
    adapter->send({ 0xff, 0xff, 0xff, 0xff, 0xff, 0xff }, request);

    if (NetworkTask::is_current()) {
        // FIXME: Waiting for the ARP response from inside the NetworkTask would
        // deadlock, so let's hope that whoever called route_to() tries again in a bit.
        dbgln_if(ARP_DEBUG, "Routing: Not waiting for ARP response from inside NetworkTask, sent ARP request using adapter {} for {}", adapter->name(), target);
        return { nullptr, {} };
    }

    Optional<MACAddress> addr;
    if (!Thread::current()->block<ARPTableBlocker>({}, next_hop_ip, addr).was_interrupted()) {
        if (addr.has_value()) {
            dbgln_if(ARP_DEBUG, "Routing: Got ARP response using adapter {} for {} ({})",
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
