/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DHCPv4Client.h"
#include <AK/Array.h>
#include <AK/Debug.h>
#include <AK/IPv4Address.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <AK/Random.h>
#include <AK/ScopeGuard.h>
#include <AK/Try.h>
#include <LibCore/File.h>
#include <LibCore/Timer.h>
#include <stdio.h>

static u8 mac_part(Vector<ByteString> const& parts, size_t index)
{
    auto result = AK::StringUtils::convert_to_uint_from_hex(parts.at(index));
    VERIFY(result.has_value());
    return result.value();
}

static MACAddress mac_from_string(ByteString const& str)
{
    auto chunks = str.split(':');
    VERIFY(chunks.size() == 6); // should we...worry about this?
    return {
        mac_part(chunks, 0), mac_part(chunks, 1), mac_part(chunks, 2),
        mac_part(chunks, 3), mac_part(chunks, 4), mac_part(chunks, 5)
    };
}

static bool send(InterfaceDescriptor const& iface, DHCPv4Packet const& packet, Core::EventReceiver*)
{
    int fd = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
    if (fd < 0) {
        dbgln("ERROR: socket :: {}", strerror(errno));
        return false;
    }

    ScopeGuard socket_close_guard = [&] { close(fd); };

    if (setsockopt(fd, SOL_SOCKET, SO_BINDTODEVICE, iface.ifname.characters(), IFNAMSIZ) < 0) {
        dbgln("ERROR: setsockopt(SO_BINDTODEVICE) :: {}", strerror(errno));
        return false;
    }
    int allow_broadcast = 1;
    if (setsockopt(fd, SOL_SOCKET, SO_BROADCAST, &allow_broadcast, sizeof(int)) < 0) {
        dbgln("ERROR: setsockopt(SO_BROADCAST) :: {}", strerror(errno));
        return false;
    }

    sockaddr_in dst;
    memset(&dst, 0, sizeof(dst));
    dst.sin_family = AF_INET;
    dst.sin_port = htons(67);
    dst.sin_addr.s_addr = IPv4Address { 255, 255, 255, 255 }.to_u32();
    memset(&dst.sin_zero, 0, sizeof(dst.sin_zero));

    dbgln_if(DHCPV4CLIENT_DEBUG, "sendto({} bound to {}, ..., {} at {}) = ...?", fd, iface.ifname, dst.sin_addr.s_addr, dst.sin_port);
    auto rc = sendto(fd, &packet, sizeof(packet), 0, (sockaddr*)&dst, sizeof(dst));
    dbgln_if(DHCPV4CLIENT_DEBUG, "sendto({}) = {}", fd, rc);
    if (rc < 0) {
        dbgln("sendto failed with {}", strerror(errno));
        return false;
    }

    return true;
}

static void set_params(InterfaceDescriptor const& iface, IPv4Address const& ipv4_addr, IPv4Address const& netmask, Optional<IPv4Address> const& gateway)
{
    int fd = socket(AF_INET, SOCK_DGRAM, IPPROTO_IP);
    if (fd < 0) {
        dbgln("ERROR: socket :: {}", strerror(errno));
        return;
    }

    struct ifreq ifr;
    memset(&ifr, 0, sizeof(ifr));

    bool fits = iface.ifname.copy_characters_to_buffer(ifr.ifr_name, IFNAMSIZ);
    if (!fits) {
        dbgln("Interface name doesn't fit into IFNAMSIZ!");
        return;
    }

    // set the IP address
    ifr.ifr_addr.sa_family = AF_INET;
    ((sockaddr_in&)ifr.ifr_addr).sin_addr.s_addr = ipv4_addr.to_in_addr_t();

    if (ioctl(fd, SIOCSIFADDR, &ifr) < 0) {
        dbgln("ERROR: ioctl(SIOCSIFADDR) :: {}", strerror(errno));
    }

    // set the network mask
    ((sockaddr_in&)ifr.ifr_netmask).sin_addr.s_addr = netmask.to_in_addr_t();

    if (ioctl(fd, SIOCSIFNETMASK, &ifr) < 0) {
        dbgln("ERROR: ioctl(SIOCSIFNETMASK) :: {}", strerror(errno));
    }

    if (!gateway.has_value())
        return;

    // set the default gateway
    struct rtentry rt;
    memset(&rt, 0, sizeof(rt));

    rt.rt_dev = const_cast<char*>(iface.ifname.characters());
    rt.rt_gateway.sa_family = AF_INET;
    ((sockaddr_in&)rt.rt_gateway).sin_addr.s_addr = gateway.value().to_in_addr_t();
    rt.rt_flags = RTF_UP | RTF_GATEWAY;

    if (ioctl(fd, SIOCADDRT, &rt) < 0) {
        dbgln("Error: ioctl(SIOCADDRT) :: {}", strerror(errno));
    }
}

DHCPv4Client::DHCPv4Client(Vector<ByteString> interfaces_with_dhcp_enabled)
    : m_interfaces_with_dhcp_enabled(move(interfaces_with_dhcp_enabled))
{
    m_server = Core::UDPServer::construct(this);
    m_server->on_ready_to_receive = [this] {
        // TODO: we need to handle possible errors here somehow
        auto buffer = MUST(m_server->receive(sizeof(DHCPv4Packet)));
        dbgln_if(DHCPV4CLIENT_DEBUG, "Received {} bytes", buffer.size());
        if (buffer.size() < sizeof(DHCPv4Packet) - DHCPV4_OPTION_FIELD_MAX_LENGTH + 1 || buffer.size() > sizeof(DHCPv4Packet)) {
            dbgln("we expected {}-{} bytes, this is a bad packet", sizeof(DHCPv4Packet) - DHCPV4_OPTION_FIELD_MAX_LENGTH + 1, sizeof(DHCPv4Packet));
            return;
        }
        auto& packet = *(DHCPv4Packet*)buffer.data();
        process_incoming(packet);
    };

    if (!m_server->bind({}, 68)) {
        dbgln("The server we just created somehow came already bound, refusing to continue");
        VERIFY_NOT_REACHED();
    }

    m_check_timer = Core::Timer::create_repeating(
        1000, [this] { try_discover_ifs(); }, this);

    m_check_timer->start();

    try_discover_ifs();
}

void DHCPv4Client::try_discover_ifs()
{
    auto ifs_result = get_discoverable_interfaces();
    if (ifs_result.is_error())
        return;

    dbgln_if(DHCPV4CLIENT_DEBUG, "Interfaces with DHCP enabled: {}", m_interfaces_with_dhcp_enabled);
    bool sent_discover_request = false;
    Interfaces& ifs = ifs_result.value();
    for (auto& iface : ifs.ready) {
        dbgln_if(DHCPV4CLIENT_DEBUG, "Checking interface {} / {}", iface.ifname, iface.current_ip_address);
        if (!m_interfaces_with_dhcp_enabled.contains_slow(iface.ifname))
            continue;
        if (iface.current_ip_address != IPv4Address { 0, 0, 0, 0 })
            continue;

        dhcp_discover(iface);
        sent_discover_request = true;
    }

    if (sent_discover_request) {
        auto current_interval = m_check_timer->interval();
        if (current_interval < m_max_timer_backoff_interval)
            current_interval *= 1.9f;
        m_check_timer->set_interval(current_interval);
    } else {
        m_check_timer->set_interval(1000);
    }
}

ErrorOr<DHCPv4Client::Interfaces> DHCPv4Client::get_discoverable_interfaces()
{
    auto file = TRY(Core::File::open("/sys/kernel/net/adapters"sv, Core::File::OpenMode::Read));

    auto file_contents = TRY(file->read_until_eof());
    auto json = JsonValue::from_string(file_contents);

    if (json.is_error() || !json.value().is_array()) {
        dbgln("Error: No network adapters available");
        return Error::from_string_literal("No network adapters available");
    }

    Vector<InterfaceDescriptor> ifnames_to_immediately_discover, ifnames_to_attempt_later;
    json.value().as_array().for_each([&ifnames_to_immediately_discover, &ifnames_to_attempt_later](auto& value) {
        auto if_object = value.as_object();

        if (if_object.get_byte_string("class_name"sv).value_or({}) == "LoopbackAdapter")
            return;

        auto name = if_object.get_byte_string("name"sv).value_or({});
        auto mac = if_object.get_byte_string("mac_address"sv).value_or({});
        auto is_up = if_object.get_bool("link_up"sv).value_or(false);
        auto ipv4_addr_maybe = IPv4Address::from_string(if_object.get_byte_string("ipv4_address"sv).value_or({}));
        auto ipv4_addr = ipv4_addr_maybe.has_value() ? ipv4_addr_maybe.value() : IPv4Address { 0, 0, 0, 0 };
        if (is_up) {
            dbgln_if(DHCPV4_DEBUG, "Found adapter '{}' with mac {}, and it was up!", name, mac);
            ifnames_to_immediately_discover.empend(name, mac_from_string(mac), ipv4_addr);
        } else {
            dbgln_if(DHCPV4_DEBUG, "Found adapter '{}' with mac {}, but it was down", name, mac);
            ifnames_to_attempt_later.empend(name, mac_from_string(mac), ipv4_addr);
        }
    });

    return Interfaces {
        move(ifnames_to_immediately_discover),
        move(ifnames_to_attempt_later)
    };
}

void DHCPv4Client::handle_offer(DHCPv4Packet const& packet, ParsedDHCPv4Options const& options)
{
    dbgln("We were offered {} for {}", packet.yiaddr().to_byte_string(), options.get<u32>(DHCPOption::IPAddressLeaseTime).value_or(0));
    auto* transaction = const_cast<DHCPv4Transaction*>(m_ongoing_transactions.get(packet.xid()).value_or(nullptr));
    if (!transaction) {
        dbgln("we're not looking for {}", packet.xid());
        return;
    }
    if (transaction->has_ip)
        return;
    if (transaction->accepted_offer) {
        // we've accepted someone's offer, but they haven't given us an ack
        // TODO: maybe record this offer?
        return;
    }
    // TAKE IT...
    transaction->offered_lease_time = options.get<u32>(DHCPOption::IPAddressLeaseTime).value();
    dhcp_request(*transaction, packet);
}

void DHCPv4Client::handle_ack(DHCPv4Packet const& packet, ParsedDHCPv4Options const& options)
{
    if constexpr (DHCPV4CLIENT_DEBUG) {
        dbgln("The DHCP server handed us {}", packet.yiaddr().to_byte_string());
        dbgln("Here are the options: {}", options.to_byte_string());
    }

    auto* transaction = const_cast<DHCPv4Transaction*>(m_ongoing_transactions.get(packet.xid()).value_or(nullptr));
    if (!transaction) {
        dbgln("we're not looking for {}", packet.xid());
        return;
    }
    transaction->has_ip = true;
    auto& interface = transaction->interface;
    auto new_ip = packet.yiaddr();
    interface.current_ip_address = new_ip;
    auto lease_time = AK::convert_between_host_and_network_endian(options.get<u32>(DHCPOption::IPAddressLeaseTime).value_or(transaction->offered_lease_time));
    // set a timer for the duration of the lease, we shall renew if needed
    (void)Core::Timer::create_single_shot(
        lease_time * 1000,
        [this, transaction, interface = InterfaceDescriptor { interface }] {
            transaction->accepted_offer = false;
            transaction->has_ip = false;
            dhcp_discover(interface);
        },
        this);

    Optional<IPv4Address> gateway;
    if (auto routers = options.get_many<IPv4Address>(DHCPOption::Router, 1); !routers.is_empty())
        gateway = routers.first();

    set_params(transaction->interface, new_ip, options.get<IPv4Address>(DHCPOption::SubnetMask).value(), gateway);
}

void DHCPv4Client::handle_nak(DHCPv4Packet const& packet, ParsedDHCPv4Options const& options)
{
    dbgln("The DHCP server told us to go chase our own tail about {}", packet.yiaddr().to_byte_string());
    dbgln("Here are the options: {}", options.to_byte_string());
    // make another request a bit later :shrug:
    auto* transaction = const_cast<DHCPv4Transaction*>(m_ongoing_transactions.get(packet.xid()).value_or(nullptr));
    if (!transaction) {
        dbgln("we're not looking for {}", packet.xid());
        return;
    }
    transaction->accepted_offer = false;
    transaction->has_ip = false;
    auto& iface = transaction->interface;
    (void)Core::Timer::create_single_shot(
        10000,
        [this, iface = InterfaceDescriptor { iface }] {
            dhcp_discover(iface);
        },
        this);
}

void DHCPv4Client::process_incoming(DHCPv4Packet const& packet)
{
    auto options = packet.parse_options();

    dbgln_if(DHCPV4CLIENT_DEBUG, "Here are the options: {}", options.to_byte_string());

    auto value_or_error = options.get<DHCPMessageType>(DHCPOption::DHCPMessageType);
    if (!value_or_error.has_value())
        return;

    auto value = value_or_error.value();
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
    case DHCPMessageType::DHCPDiscover:
    case DHCPMessageType::DHCPRequest:
    case DHCPMessageType::DHCPRelease:
        // These are not for us
        // we're just getting them because there are other people on our subnet
        // broadcasting stuff
        break;
    case DHCPMessageType::DHCPDecline:
    default:
        dbgln("I dunno what to do with this {}", (u8)value);
        VERIFY_NOT_REACHED();
        break;
    }
}

void DHCPv4Client::dhcp_discover(InterfaceDescriptor const& iface)
{
    auto transaction_id = get_random<u32>();

    if constexpr (DHCPV4CLIENT_DEBUG) {
        dbgln("Trying to lease an IP for {} with ID {}", iface.ifname, transaction_id);
        if (!iface.current_ip_address.is_zero())
            dbgln("going to request the server to hand us {}", iface.current_ip_address.to_byte_string());
    }

    DHCPv4PacketBuilder builder;

    DHCPv4Packet& packet = builder.peek();
    packet.set_op(DHCPv4Op::BootRequest);
    packet.set_htype(1); // 10mb ethernet
    packet.set_hlen(sizeof(MACAddress));
    packet.set_xid(transaction_id);
    packet.set_flags(DHCPv4Flags::Broadcast);
    packet.ciaddr() = iface.current_ip_address;
    packet.set_chaddr(iface.mac_address);
    packet.set_secs(65535); // we lie

    // set packet options
    builder.set_message_type(DHCPMessageType::DHCPDiscover);
    auto& dhcp_packet = builder.build();

    // broadcast the discover request
    if (!send(iface, dhcp_packet, this))
        return;
    m_ongoing_transactions.set(transaction_id, make<DHCPv4Transaction>(iface));
}

void DHCPv4Client::dhcp_request(DHCPv4Transaction& transaction, DHCPv4Packet const& offer)
{
    auto& iface = transaction.interface;
    dbgln("Leasing the IP {} for adapter {}", offer.yiaddr().to_byte_string(), iface.ifname);
    DHCPv4PacketBuilder builder;

    DHCPv4Packet& packet = builder.peek();
    packet.set_op(DHCPv4Op::BootRequest);
    packet.ciaddr() = iface.current_ip_address;
    packet.set_htype(1); // 10mb ethernet
    packet.set_hlen(sizeof(MACAddress));
    packet.set_xid(offer.xid());
    packet.set_flags(DHCPv4Flags::Broadcast);
    packet.set_chaddr(iface.mac_address);
    packet.set_secs(65535); // we lie

    // set packet options
    builder.set_message_type(DHCPMessageType::DHCPRequest);
    builder.add_option(DHCPOption::RequestedIPAddress, sizeof(IPv4Address), &offer.yiaddr());

    auto maybe_dhcp_server_ip = offer.parse_options().get<IPv4Address>(DHCPOption::ServerIdentifier);
    if (maybe_dhcp_server_ip.has_value())
        builder.add_option(DHCPOption::ServerIdentifier, sizeof(IPv4Address), &maybe_dhcp_server_ip.value());

    AK::Array<DHCPOption, 2> parameter_request_list = {
        DHCPOption::SubnetMask,
        DHCPOption::Router,
    };
    builder.add_option(DHCPOption::ParameterRequestList, parameter_request_list.size(), &parameter_request_list);

    auto& dhcp_packet = builder.build();

    // broadcast the "request" request
    if (!send(iface, dhcp_packet, this))
        return;
    transaction.accepted_offer = true;
}
