/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibIPC/ConnectionFromClient.h>

#include <ConfigServer/ConfigClientEndpoint.h>
#include <ConfigServer/ConfigServerEndpoint.h>

namespace ConfigServer {

class ConnectionFromClient final : public IPC::ConnectionFromClient<ConfigClientEndpoint, ConfigServerEndpoint> {
    C_OBJECT(ConnectionFromClient)
public:
    ~ConnectionFromClient() override = default;

    virtual void die() override;

    bool is_monitoring_domain(ByteString const& domain) const { return m_monitored_domains.contains(domain); }

private:
    explicit ConnectionFromClient(NonnullOwnPtr<Core::LocalSocket>, int client_id);

    virtual void pledge_domains(Vector<ByteString> const&) override;
    virtual void enable_permissive_mode() override;
    virtual void monitor_domain(ByteString const&) override;
    virtual Messages::ConfigServer::ListConfigGroupsResponse list_config_groups([[maybe_unused]] ByteString const& domain) override;
    virtual Messages::ConfigServer::ListConfigKeysResponse list_config_keys([[maybe_unused]] ByteString const& domain, [[maybe_unused]] ByteString const& group) override;
    virtual Messages::ConfigServer::ReadStringValueResponse read_string_value([[maybe_unused]] ByteString const& domain, [[maybe_unused]] ByteString const& group, [[maybe_unused]] ByteString const& key) override;
    virtual Messages::ConfigServer::ReadI32ValueResponse read_i32_value([[maybe_unused]] ByteString const& domain, [[maybe_unused]] ByteString const& group, [[maybe_unused]] ByteString const& key) override;
    virtual Messages::ConfigServer::ReadU32ValueResponse read_u32_value([[maybe_unused]] ByteString const& domain, [[maybe_unused]] ByteString const& group, [[maybe_unused]] ByteString const& key) override;
    virtual Messages::ConfigServer::ReadBoolValueResponse read_bool_value([[maybe_unused]] ByteString const& domain, [[maybe_unused]] ByteString const& group, [[maybe_unused]] ByteString const& key) override;
    virtual void write_string_value([[maybe_unused]] ByteString const& domain, [[maybe_unused]] ByteString const& group, [[maybe_unused]] ByteString const& key, [[maybe_unused]] ByteString const& value) override;
    virtual void write_i32_value([[maybe_unused]] ByteString const& domain, [[maybe_unused]] ByteString const& group, [[maybe_unused]] ByteString const& key, [[maybe_unused]] i32 value) override;
    virtual void write_u32_value([[maybe_unused]] ByteString const& domain, [[maybe_unused]] ByteString const& group, [[maybe_unused]] ByteString const& key, [[maybe_unused]] u32 value) override;
    virtual void write_bool_value([[maybe_unused]] ByteString const& domain, [[maybe_unused]] ByteString const& group, [[maybe_unused]] ByteString const& key, [[maybe_unused]] bool value) override;
    virtual void remove_key_entry([[maybe_unused]] ByteString const& domain, [[maybe_unused]] ByteString const& group, [[maybe_unused]] ByteString const& key) override;
    virtual void remove_group_entry([[maybe_unused]] ByteString const& domain, [[maybe_unused]] ByteString const& group) override;
    virtual void add_group_entry([[maybe_unused]] ByteString const& domain, [[maybe_unused]] ByteString const& group) override;

    bool validate_access(ByteString const& domain, ByteString const& group, ByteString const& key);
    void sync_dirty_domains_to_disk();
    void start_or_restart_sync_timer();

    bool m_has_pledged { false };
    bool m_permissive_mode { false };
    HashTable<ByteString> m_pledged_domains;

    HashTable<ByteString> m_monitored_domains;

    NonnullRefPtr<Core::Timer> m_sync_timer;
    HashTable<ByteString> m_dirty_domains;
};

}
