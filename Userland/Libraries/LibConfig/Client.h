/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <ConfigServer/ConfigClientEndpoint.h>
#include <ConfigServer/ConfigServerEndpoint.h>
#include <LibIPC/ServerConnection.h>

namespace Config {

class Client final
    : public IPC::ServerConnection<ConfigClientEndpoint, ConfigServerEndpoint>
    , public ConfigClientEndpoint {
    C_OBJECT(Client);

public:
    void pledge_domains(Vector<String> const&);
    void monitor_domain(String const&);

    String read_string(StringView domain, StringView group, StringView key, StringView fallback);
    i32 read_i32(StringView domain, StringView group, StringView key, i32 fallback);
    bool read_bool(StringView domain, StringView group, StringView key, bool fallback);

    void write_string(StringView domain, StringView group, StringView key, StringView value);
    void write_i32(StringView domain, StringView group, StringView key, i32 value);
    void write_bool(StringView domain, StringView group, StringView key, bool value);

    static Client& the();

private:
    explicit Client()
        : IPC::ServerConnection<ConfigClientEndpoint, ConfigServerEndpoint>(*this, "/tmp/portal/config")
    {
    }

    void notify_changed_string_value(String const& domain, String const& group, String const& key, String const& value) override;
    void notify_changed_i32_value(String const& domain, String const& group, String const& key, i32 value) override;
    void notify_changed_bool_value(String const& domain, String const& group, String const& key, bool value) override;
};

inline String read_string(StringView domain, StringView group, StringView key, StringView fallback = {})
{
    return Client::the().read_string(domain, group, key, fallback);
}

inline i32 read_i32(StringView domain, StringView group, StringView key, i32 fallback = 0)
{
    return Client::the().read_i32(domain, group, key, fallback);
}

inline bool read_bool(StringView domain, StringView group, StringView key, bool fallback = false)
{
    return Client::the().read_bool(domain, group, key, fallback);
}

inline void write_string(StringView domain, StringView group, StringView key, StringView value)
{
    Client::the().write_string(domain, group, key, value);
}

inline void write_i32(StringView domain, StringView group, StringView key, i32 value)
{
    Client::the().write_i32(domain, group, key, value);
}

inline void write_bool(StringView domain, StringView group, StringView key, bool value)
{
    Client::the().write_bool(domain, group, key, value);
}

inline void pledge_domains(Vector<String> const& domains)
{
    Client::the().pledge_domains(domains);
}

inline void pledge_domains(String const& domains)
{
    Client::the().pledge_domains({ domains });
}

inline void monitor_domain(String const& domain)
{
    Client::the().monitor_domain(domain);
}

}
