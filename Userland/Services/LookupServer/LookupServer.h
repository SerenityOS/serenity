/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DNSName.h"
#include "DNSPacket.h"
#include "DNSServer.h"
#include "MulticastDNS.h"
#include <LibCore/Object.h>

namespace LookupServer {

class DNSAnswer;

class LookupServer final : public Core::Object {
    C_OBJECT(LookupServer);

public:
    static LookupServer& the();
    Vector<DNSAnswer> lookup(const DNSName& name, unsigned short record_type);

private:
    LookupServer();

    void load_etc_hosts();
    void put_in_cache(const DNSAnswer&);

    Vector<DNSAnswer> lookup(const DNSName& hostname, const String& nameserver, bool& did_get_response, unsigned short record_type, ShouldRandomizeCase = ShouldRandomizeCase::Yes);

    RefPtr<Core::LocalServer> m_local_server;
    RefPtr<DNSServer> m_dns_server;
    RefPtr<MulticastDNS> m_mdns;
    Vector<String> m_nameservers;
    HashMap<DNSName, Vector<DNSAnswer>, DNSName::Traits> m_etc_hosts;
    HashMap<DNSName, Vector<DNSAnswer>, DNSName::Traits> m_lookup_cache;
};

}
