/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ConnectionFromClient.h"
#include "DNSServer.h"
#include "MulticastDNS.h"
#include <LibCore/FileWatcher.h>
#include <LibCore/Object.h>
#include <LibDNS/Name.h>
#include <LibDNS/Packet.h>
#include <LibIPC/MultiServer.h>

namespace LookupServer {

using namespace DNS;

class LookupServer final : public Core::Object {
    C_OBJECT(LookupServer);

public:
    static LookupServer& the();
    ErrorOr<Vector<Answer>> lookup(Name const& name, RecordType record_type);

private:
    LookupServer();

    void load_etc_hosts();
    void put_in_cache(Answer const&);

    ErrorOr<Vector<Answer>> lookup(Name const& hostname, String const& nameserver, bool& did_get_response, RecordType record_type, ShouldRandomizeCase = ShouldRandomizeCase::Yes);

    OwnPtr<IPC::MultiServer<ConnectionFromClient>> m_server;
    RefPtr<DNSServer> m_dns_server;
    RefPtr<MulticastDNS> m_mdns;
    Vector<String> m_nameservers;
    RefPtr<Core::FileWatcher> m_file_watcher;
    HashMap<Name, Vector<Answer>, Name::Traits> m_etc_hosts;
    HashMap<Name, Vector<Answer>, Name::Traits> m_lookup_cache;
};

}
