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

#include "DNSPacket.h"
#include "DNSName.h"
#include <LibCore/Object.h>

namespace LookupServer {

class DNSAnswer;

class LookupServer final : public Core::Object {
    C_OBJECT(LookupServer);

public:
    static LookupServer& the();
    Vector<String> lookup(const DNSName& name, unsigned short record_type);

private:
    LookupServer();

    void load_etc_hosts();
    void put_in_cache(const DNSAnswer&);

    Vector<String> lookup(const DNSName& hostname, const String& nameserver, bool& did_get_response, unsigned short record_type, ShouldRandomizeCase = ShouldRandomizeCase::Yes);


    RefPtr<Core::LocalServer> m_local_server;
    Vector<String> m_nameservers;
    HashMap<DNSName, Vector<DNSAnswer>, DNSName::Traits> m_etc_hosts;
    HashMap<DNSName, Vector<DNSAnswer>, DNSName::Traits> m_lookup_cache;
};

}
