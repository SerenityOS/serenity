/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Promise.h>
#include <LibCore/StandardPaths.h>
#include <LibIPC/ConnectionToServer.h>
#include <Userland/Services/ConfigServer/ConfigClientEndpoint.h>
#include <Userland/Services/ConfigServer/ConfigServerEndpoint.h>

namespace Config {

class Client final
    : public IPC::ConnectionToServer<ConfigClientEndpoint, ConfigServerEndpoint>
    , public ConfigClientEndpoint {
    IPC_CLIENT_CONNECTION(Client, "/tmp/session/%sid/portal/config"sv)

public:
    /// Permissive mode makes reads and writes to non-pledged domains into no-ops instead of client misbehavior errors.
    void enable_permissive_mode();
    void pledge_domains(Vector<ByteString> const&);
    void monitor_domain(ByteString const&);

    Vector<ByteString> list_groups(StringView domain);
    Vector<ByteString> list_keys(StringView domain, StringView group);

    ByteString read_string(StringView domain, StringView group, StringView key, StringView fallback);
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
    explicit Client(NonnullOwnPtr<Core::LocalSocket> socket)
        : IPC::ConnectionToServer<ConfigClientEndpoint, ConfigServerEndpoint>(*this, move(socket))
    {
    }

    void notify_changed_string_value(ByteString const& domain, ByteString const& group, ByteString const& key, ByteString const& value) override;
    void notify_changed_i32_value(ByteString const& domain, ByteString const& group, ByteString const& key, i32 value) override;
    void notify_changed_u32_value(ByteString const& domain, ByteString const& group, ByteString const& key, u32 value) override;
    void notify_changed_bool_value(ByteString const& domain, ByteString const& group, ByteString const& key, bool value) override;
    void notify_removed_key(ByteString const& domain, ByteString const& group, ByteString const& key) override;
    void notify_removed_group(ByteString const& domain, ByteString const& group) override;
    void notify_added_group(ByteString const& domain, ByteString const& group) override;
};

inline Vector<ByteString> list_groups(StringView domain)
{
    return Client::the().list_groups(domain);
}

inline Vector<ByteString> list_keys(StringView domain, StringView group)
{
    return Client::the().list_keys(domain, group);
}

inline ByteString read_string(StringView domain, StringView group, StringView key, StringView fallback = {})
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

inline void enable_permissive_mode()
{
    Client::the().enable_permissive_mode();
}

inline void pledge_domains(Vector<ByteString> const& domains)
{
    Client::the().pledge_domains(domains);
}

inline void pledge_domain(ByteString const& domain)
{
    Client::the().pledge_domains({ domain });
}

inline void monitor_domain(ByteString const& domain)
{
    Client::the().monitor_domain(domain);
}

}
