/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DHCPv4.h"
#include <AK/ByteString.h>
#include <AK/Error.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <LibCore/UDPServer.h>
#include <net/if.h>
#include <net/route.h>
#include <sys/ioctl.h>
#include <sys/socket.h>

struct InterfaceDescriptor {
    ByteString ifname;
    MACAddress mac_address;
    IPv4Address current_ip_address;
};

struct DHCPv4Transaction {
    DHCPv4Transaction(InterfaceDescriptor ifname)
        : interface(ifname)
    {
    }

    InterfaceDescriptor interface;
    bool accepted_offer { false };
    bool has_ip { false };
    u32 offered_lease_time { 0 };
};

class DHCPv4Client final : public Core::EventReceiver {
    C_OBJECT(DHCPv4Client)

public:
    void dhcp_discover(InterfaceDescriptor const& ifname);
    void dhcp_request(DHCPv4Transaction& transaction, DHCPv4Packet const& packet);

    void process_incoming(DHCPv4Packet const& packet);

    bool id_is_registered(u32 id) { return m_ongoing_transactions.contains(id); }

    struct Interfaces {
        Vector<InterfaceDescriptor> ready;
        Vector<InterfaceDescriptor> not_ready;
    };
    static ErrorOr<Interfaces> get_discoverable_interfaces();

private:
    explicit DHCPv4Client(Vector<ByteString> interfaces_with_dhcp_enabled);

    void try_discover_ifs();

    Vector<ByteString> m_interfaces_with_dhcp_enabled;
    HashMap<u32, OwnPtr<DHCPv4Transaction>> m_ongoing_transactions;
    RefPtr<Core::UDPServer> m_server;
    RefPtr<Core::Timer> m_check_timer;
    int m_max_timer_backoff_interval { 600000 }; // 10 minutes

    void handle_offer(DHCPv4Packet const&, ParsedDHCPv4Options const&);
    void handle_ack(DHCPv4Packet const&, ParsedDHCPv4Options const&);
    void handle_nak(DHCPv4Packet const&, ParsedDHCPv4Options const&);
};
