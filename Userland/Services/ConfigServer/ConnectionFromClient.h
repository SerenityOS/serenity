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
    ~ConnectionFromClient() override;

    virtual void die() override;

    bool is_monitoring_domain(String const& domain) const { return m_monitored_domains.contains(domain); }

private:
    explicit ConnectionFromClient(NonnullOwnPtr<Core::Stream::LocalSocket>, int client_id);

    virtual void pledge_domains(Vector<String> const&) override;
    virtual void monitor_domain(String const&) override;
    virtual Messages::ConfigServer::ListConfigGroupsResponse list_config_groups([[maybe_unused]] String const& domain) override;
    virtual Messages::ConfigServer::ListConfigKeysResponse list_config_keys([[maybe_unused]] String const& domain, [[maybe_unused]] String const& group) override;
    virtual Messages::ConfigServer::ReadStringValueResponse read_string_value([[maybe_unused]] String const& domain, [[maybe_unused]] String const& group, [[maybe_unused]] String const& key) override;
    virtual Messages::ConfigServer::ReadI32ValueResponse read_i32_value([[maybe_unused]] String const& domain, [[maybe_unused]] String const& group, [[maybe_unused]] String const& key) override;
    virtual Messages::ConfigServer::ReadBoolValueResponse read_bool_value([[maybe_unused]] String const& domain, [[maybe_unused]] String const& group, [[maybe_unused]] String const& key) override;
    virtual void write_string_value([[maybe_unused]] String const& domain, [[maybe_unused]] String const& group, [[maybe_unused]] String const& key, [[maybe_unused]] String const& value) override;
    virtual void write_i32_value([[maybe_unused]] String const& domain, [[maybe_unused]] String const& group, [[maybe_unused]] String const& key, [[maybe_unused]] i32 value) override;
    virtual void write_bool_value([[maybe_unused]] String const& domain, [[maybe_unused]] String const& group, [[maybe_unused]] String const& key, [[maybe_unused]] bool value) override;
    virtual void remove_key([[maybe_unused]] String const& domain, [[maybe_unused]] String const& group, [[maybe_unused]] String const& key) override;

    bool validate_access(String const& domain, String const& group, String const& key);
    void sync_dirty_domains_to_disk();
    void start_or_restart_sync_timer();

    bool m_has_pledged { false };
    HashTable<String> m_pledged_domains;

    HashTable<String> m_monitored_domains;

    NonnullRefPtr<Core::Timer> m_sync_timer;
    HashTable<String> m_dirty_domains;
};

}
