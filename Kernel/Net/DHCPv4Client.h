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

#pragma once

#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <Kernel/Net/DHCPv4.h>
#include <Kernel/Net/NetworkAdapter.h>

namespace Kernel {

struct DHCPv4Transaction {
    DHCPv4Transaction(NetworkAdapter& adapter)
        : adapter(adapter)
    {
    }

    NetworkAdapter& adapter;
    bool accepted_offer { false };
    bool has_ip { false };
    u32 offered_lease_time { 0 };
};

struct DHCPv4Client {
    DHCPv4Client();
    ~DHCPv4Client();
    void dhcp_discover(NetworkAdapter& adapter, IPv4Address previous = IPv4Address { 0, 0, 0, 0 });
    void dhcp_request(DHCPv4Transaction& transaction, const DHCPv4Packet& packet);

    void process_incoming(const DHCPv4Packet& packet);

    bool id_is_registered(u32 id) { return m_ongoing_transactions.contains(id); }

    static DHCPv4Client& the();

private:
    HashMap<u32, OwnPtr<DHCPv4Transaction>> m_ongoing_transactions;
    void handle_offer(const DHCPv4Packet&, const ParsedDHCPv4Options&);
    void handle_ack(const DHCPv4Packet&, const ParsedDHCPv4Options&);
    void handle_nak(const DHCPv4Packet&, const ParsedDHCPv4Options&);
};
}
