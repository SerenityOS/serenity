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

#include <AK/Function.h>
#include <AK/URL.h>
#include <LibCore/Object.h>
#include <LibWeb/Loader/Resource.h>

namespace Protocol {
class Client;
}

namespace Web {

class ResourceLoader : public Core::Object {
    C_OBJECT(ResourceLoader)
public:
    static ResourceLoader& the();

    RefPtr<Resource> load_resource(Resource::Type, const LoadRequest&);

    void load(const LoadRequest&, Function<void(const ByteBuffer&, const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers)> success_callback, Function<void(const String&)> error_callback = nullptr);
    void load(const URL&, Function<void(const ByteBuffer&, const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers)> success_callback, Function<void(const String&)> error_callback = nullptr);
    void load_sync(const URL&, Function<void(const ByteBuffer&, const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers)> success_callback, Function<void(const String&)> error_callback = nullptr);

    Function<void()> on_load_counter_change;

    int pending_loads() const { return m_pending_loads; }

    Protocol::Client& protocol_client() { return *m_protocol_client; }

    const String& user_agent() const { return m_user_agent; }

private:
    ResourceLoader();
    static bool is_port_blocked(int port);

    int m_pending_loads { 0 };

    RefPtr<Protocol::Client> m_protocol_client;
    String m_user_agent;
};

}
