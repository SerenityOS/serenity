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

#pragma once

#include "DNSRequest.h"
#include <AK/HashMap.h>
#include <LibCore/CObject.h>

class CLocalSocket;
class CLocalServer;

class LookupServer final : public CObject {
    C_OBJECT(LookupServer)

public:
    LookupServer();

private:
    void load_etc_hosts();
    void service_client(RefPtr<CLocalSocket>);
    Vector<String> lookup(const String& hostname, bool& did_timeout, unsigned short record_type, ShouldRandomizeCase = ShouldRandomizeCase::Yes);

    struct CachedLookup {
        time_t timestamp { 0 };
        unsigned short record_type { 0 };
        Vector<String> responses;
    };

    RefPtr<CLocalServer> m_local_server;
    String m_nameserver;
    HashMap<String, String> m_dns_custom_hostnames;
    HashMap<String, CachedLookup> m_lookup_cache;
};
