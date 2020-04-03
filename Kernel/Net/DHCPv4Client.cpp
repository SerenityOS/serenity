/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#include <AK/Function.h>
#include <Kernel/Net/DHCPv4Client.h>
#include <Kernel/Net/UDP.h>
#include <Kernel/TimerQueue.h>

//#define DHCPV4CLIENT_DEBUG

namespace Kernel {

static DHCPv4Client* s_client;
DHCPv4Client& DHCPv4Client::the()
{
    if (s_client)
        return *s_client;
    return *(s_client = new DHCPv4Client);
}

DHCPv4Client::DHCPv4Client()
{
}

DHCPv4Client::~DHCPv4Client()
{
}

void DHCPv4Client::handle_offer(const DHCPv4Packet& packet, const ParsedDHCPv4Options& options)
{
    dbg() << "We were offered " << packet.yiaddr().to_string() << " for " << options.get<u32>(DHCPOptions::IPAddressLeaseTime).value_or(0);
    auto* transaction = const_cast<DHCPv4Transaction*>(m_ongoing_transactions.get(packet.xid()).value());
    if (transaction->has_ip)
        return;
    if (transaction->accepted_offer) {
        // we've accepted someone's offer, but they haven't given us an ack
        // TODO: maybe record this offer?
        return;
    }
    // TAKE IT...
    transaction->offered_lease_time = options.get<u32>(DHCPOptions::IPAddressLeaseTime).value();
    dhcp_request(*transaction, packet);
}

void DHCPv4Client::handle_ack(const DHCPv4Packet& packet, const ParsedDHCPv4Options& options)
{
#ifdef DHCPV4CLIENT_DEBUG
    dbg() << "The DHCP server handed us " << packet.yiaddr().to_string();
    dbg() << "Here are the options: " << options.to_string();
#endif
    auto* transaction = const_cast<DHCPv4Transaction*>(m_ongoing_transactions.get(packet.xid()).value());
    transaction->has_ip = true;
    auto& adapter = transaction->adapter;
    auto new_ip = packet.yiaddr();
    auto lease_time = convert_between_host_and_network(options.get<u32>(DHCPOptions::IPAddressLeaseTime).value_or(transaction->offered_lease_time));
    // set a timer for the duration of the lease, we shall renew if needed
    TimerQueue::the().add_timer(lease_time, TimeUnit::S, [&] {
        transaction->accepted_offer = false;
        transaction->has_ip = false;
        dhcp_discover(adapter, new_ip);
    });
    adapter.set_ipv4_address(new_ip);
    adapter.set_ipv4_gateway(options.get_many<IPv4Address>(DHCPOptions::Router, 1).first());
    adapter.set_ipv4_netmask(options.get<IPv4Address>(DHCPOptions::SubnetMask).value());
    dbg() << "DHCPv4Client: Leased for hw=" << adapter.mac_address().to_string() << " address=" << adapter.ipv4_address().to_string() << " netmask=" << adapter.ipv4_netmask().to_string() << " gateway=" << adapter.ipv4_gateway().to_string();
}

void DHCPv4Client::handle_nak(const DHCPv4Packet& packet, const ParsedDHCPv4Options& options)
{
    dbg() << "The DHCP server told us to go chase our own tail about " << packet.yiaddr().to_string();
    dbg() << "Here are the options: " << options.to_string();
    // make another request a bit later :shrug:
    auto* transaction = const_cast<DHCPv4Transaction*>(m_ongoing_transactions.get(packet.xid()).value());
    transaction->accepted_offer = false;
    transaction->has_ip = false;
    auto& adapter = transaction->adapter;
    TimerQueue::the().add_timer(10, TimeUnit::S, [&] {
        dhcp_discover(adapter);
    });
}

void DHCPv4Client::process_incoming(const DHCPv4Packet& packet)
{
    auto options = packet.parse_options();
#ifdef DHCPV4CLIENT_DEBUG
    dbg() << "Here are the options: " << options.to_string();
#endif
    auto value = options.get<DHCPMessageType>(DHCPOptions::DHCPMessageType).value();
    switch (value) {
    case DHCPMessageType::DHCPOffer:
        handle_offer(packet, options);
        break;
    case DHCPMessageType::DHCPAck:
        handle_ack(packet, options);
        break;
    case DHCPMessageType::DHCPNak:
        handle_nak(packet, options);
        break;
    default:
        dbg() << "I dunno what to do with this " << (u8)value;
        ASSERT_NOT_REACHED();
        break;
    }
}

void DHCPv4Client::dhcp_discover(NetworkAdapter& adapter, IPv4Address previous)
{
    auto transaction_id = get_fast_random<u32>();
#ifdef DHCPV4CLIENT_DEBUG
    dbg() << "Trying to lease an IP for " << adapter.class_name() << " with ID " << transaction_id;
    if (!previous.is_zero())
        dbg() << "going to request the server to hand us " << previous.to_string();
#endif
    DHCPv4PacketBuilder builder;

    DHCPv4Packet& packet = builder.peek();
    packet.set_op(DHCPv4Ops::BootRequest);
    packet.set_htype(1); // 10mb ethernet
    packet.set_hlen(sizeof(MACAddress));
    packet.set_xid(transaction_id);
    packet.set_flags(DHCPv4Flags::Broadcast);
    packet.ciaddr() = previous;
    packet.set_chaddr(adapter.mac_address());
    packet.set_secs(65535); // we lie

    // set packet options
    builder.set_message_type(DHCPMessageType::DHCPDiscover);
    auto& dhcp_packet = builder.build();

    // broadcast the discover request
    adapter.send({ 0xff, 0xff, 0xff, 0xff, 0xff, 0xff }, dhcp_packet);
    m_ongoing_transactions.set(transaction_id, make<DHCPv4Transaction>(adapter));
}

void DHCPv4Client::dhcp_request(DHCPv4Transaction& transaction, const DHCPv4Packet& offer)
{
    auto& adapter = transaction.adapter;
    dbg() << "Leasing the IP " << offer.yiaddr().to_string() << " for adapter " << adapter.class_name();
    DHCPv4PacketBuilder builder;

    DHCPv4Packet& packet = builder.peek();
    packet.set_op(DHCPv4Ops::BootRequest);
    packet.set_htype(1); // 10mb ethernet
    packet.set_hlen(sizeof(MACAddress));
    packet.set_xid(offer.xid());
    packet.set_flags(DHCPv4Flags::Broadcast);
    packet.set_chaddr(adapter.mac_address());
    packet.set_secs(65535); // we lie

    // set packet options
    builder.set_message_type(DHCPMessageType::DHCPRequest);
    auto& dhcp_packet = builder.build();

    // broadcast the discover request
    adapter.send({ 0xff, 0xff, 0xff, 0xff, 0xff, 0xff }, dhcp_packet);
    transaction.accepted_offer = true;
}
}
