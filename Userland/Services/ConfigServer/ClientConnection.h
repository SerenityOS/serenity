/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibIPC/ClientConnection.h>

#include <ConfigServer/ConfigClientEndpoint.h>
#include <ConfigServer/ConfigServerEndpoint.h>

namespace ConfigServer {

class ClientConnection final : public IPC::ClientConnection<ConfigClientEndpoint, ConfigServerEndpoint> {
    C_OBJECT(ClientConnection)
public:
    ~ClientConnection() override;

    virtual void die() override;

private:
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>, int client_id);

    virtual void pledge_domains(Vector<String> const&) override;
    virtual void monitor_domain(String const&) override;
    virtual Messages::ConfigServer::ReadStringValueResponse read_string_value([[maybe_unused]] String const& domain, [[maybe_unused]] String const& group, [[maybe_unused]] String const& key) override;
    virtual Messages::ConfigServer::ReadI32ValueResponse read_i32_value([[maybe_unused]] String const& domain, [[maybe_unused]] String const& group, [[maybe_unused]] String const& key) override;
    virtual Messages::ConfigServer::ReadBoolValueResponse read_bool_value([[maybe_unused]] String const& domain, [[maybe_unused]] String const& group, [[maybe_unused]] String const& key) override;
    virtual void write_string_value([[maybe_unused]] String const& domain, [[maybe_unused]] String const& group, [[maybe_unused]] String const& key, [[maybe_unused]] String const& value) override;
    virtual void write_i32_value([[maybe_unused]] String const& domain, [[maybe_unused]] String const& group, [[maybe_unused]] String const& key, [[maybe_unused]] i32 value) override;
    virtual void write_bool_value([[maybe_unused]] String const& domain, [[maybe_unused]] String const& group, [[maybe_unused]] String const& key, [[maybe_unused]] bool value) override;

    bool validate_access(String const& domain, String const& group, String const& key);

    bool m_has_pledged { false };
    HashTable<String> m_pledged_domains;

    HashTable<String> m_monitored_domains;
};

}
