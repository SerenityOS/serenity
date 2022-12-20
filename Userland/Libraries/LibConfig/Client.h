/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <ConfigServer/ConfigClientEndpoint.h>
#include <ConfigServer/ConfigServerEndpoint.h>
#include <LibCore/File.h>
#include <LibCore/Promise.h>
#include <LibCore/StandardPaths.h>
#include <LibIPC/ConnectionToServer.h>

namespace Config {

class Client final
    : public IPC::ConnectionToServer<ConfigClientEndpoint, ConfigServerEndpoint>
    , public ConfigClientEndpoint {
    IPC_CLIENT_CONNECTION(Client, "/tmp/session/%sid/portal/config"sv)

public:
    void pledge_domains(Vector<DeprecatedString> const&);
    void monitor_domain(DeprecatedString const&);

    Vector<DeprecatedString> list_groups(StringView domain);
    Vector<DeprecatedString> list_keys(StringView domain, StringView group);

    DeprecatedString read_string(StringView domain, StringView group, StringView key, StringView fallback);
    i32 read_i32(StringView domain, StringView group, StringView key, i32 fallback);
    u32 read_u32(StringView domain, StringView group, StringView key, u32 fallback);
    bool read_bool(StringView domain, StringView group, StringView key, bool fallback);

    void write_string(StringView domain, StringView group, StringView key, StringView value);
    void write_i32(StringView domain, StringView group, StringView key, i32 value);
    void write_u32(StringView domain, StringView group, StringView key, u32 value);
    void write_bool(StringView domain, StringView group, StringView key, bool value);
    void remove_key(StringView domain, StringView group, StringView key);
    void remove_group(StringView domain, StringView group);
    void add_group(StringView domain, StringView group);

    static Client& the();

private:
    explicit Client(NonnullOwnPtr<Core::Stream::LocalSocket> socket)
        : IPC::ConnectionToServer<ConfigClientEndpoint, ConfigServerEndpoint>(*this, move(socket))
    {
    }

    void notify_changed_string_value(DeprecatedString const& domain, DeprecatedString const& group, DeprecatedString const& key, DeprecatedString const& value) override;
    void notify_changed_i32_value(DeprecatedString const& domain, DeprecatedString const& group, DeprecatedString const& key, i32 value) override;
    void notify_changed_u32_value(DeprecatedString const& domain, DeprecatedString const& group, DeprecatedString const& key, u32 value) override;
    void notify_changed_bool_value(DeprecatedString const& domain, DeprecatedString const& group, DeprecatedString const& key, bool value) override;
    void notify_removed_key(DeprecatedString const& domain, DeprecatedString const& group, DeprecatedString const& key) override;
    void notify_removed_group(DeprecatedString const& domain, DeprecatedString const& group) override;
    void notify_added_group(DeprecatedString const& domain, DeprecatedString const& group) override;
};

inline Vector<DeprecatedString> list_groups(StringView domain)
{
    return Client::the().list_groups(domain);
}

inline Vector<DeprecatedString> list_keys(StringView domain, StringView group)
{
    return Client::the().list_keys(domain, group);
}

inline DeprecatedString read_string(StringView domain, StringView group, StringView key, StringView fallback = {})
{
    return Client::the().read_string(domain, group, key, fallback);
}

inline i32 read_i32(StringView domain, StringView group, StringView key, i32 fallback = 0)
{
    return Client::the().read_i32(domain, group, key, fallback);
}

inline u32 read_u32(StringView domain, StringView group, StringView key, u32 fallback = 0)
{
    return Client::the().read_u32(domain, group, key, fallback);
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

inline void write_u32(StringView domain, StringView group, StringView key, u32 value)
{
    Client::the().write_u32(domain, group, key, value);
}

inline void write_bool(StringView domain, StringView group, StringView key, bool value)
{
    Client::the().write_bool(domain, group, key, value);
}

inline void remove_key(StringView domain, StringView group, StringView key)
{
    Client::the().remove_key(domain, group, key);
}

inline void remove_group(StringView domain, StringView group)
{
    Client::the().remove_group(domain, group);
}

inline void add_group(StringView domain, StringView group)
{
    Client::the().add_group(domain, group);
}

inline void pledge_domains(Vector<DeprecatedString> const& domains)
{
    Client::the().pledge_domains(domains);
}

inline void pledge_domain(DeprecatedString const& domain)
{
    Client::the().pledge_domains({ domain });
}

inline void monitor_domain(DeprecatedString const& domain)
{
    Client::the().monitor_domain(domain);
}

}
