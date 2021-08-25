/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClientConnection.h"
#include <ConfigServer/ConfigClientEndpoint.h>
#include <LibCore/ConfigFile.h>

namespace ConfigServer {

static HashMap<int, RefPtr<ClientConnection>> s_connections;

ClientConnection::ClientConnection(NonnullRefPtr<Core::LocalSocket> client_socket, int client_id)
    : IPC::ClientConnection<ConfigClientEndpoint, ConfigServerEndpoint>(*this, move(client_socket), client_id)
{
    s_connections.set(client_id, *this);
}

ClientConnection::~ClientConnection()
{
}

void ClientConnection::die()
{
    s_connections.remove(client_id());
}

void ClientConnection::pledge_domains(Vector<String> const& domains)
{
    if (m_has_pledged) {
        did_misbehave("Tried to pledge domains twice.");
        return;
    }
    m_has_pledged = true;
    for (auto& domain : domains)
        m_pledged_domains.set(domain);
}

bool ClientConnection::validate_access(String const& domain, String const& group, String const& key)
{
    if (!m_has_pledged)
        return true;
    if (m_pledged_domains.contains(domain))
        return true;
    did_misbehave(String::formatted("Blocked attempt to access domain '{}', group={}, key={}", domain, group, key).characters());
    return false;
}

Messages::ConfigServer::ReadStringValueResponse ClientConnection::read_string_value(String const& domain, String const& group, String const& key)
{
    if (!validate_access(domain, group, key))
        return nullptr;

    auto config = Core::ConfigFile::open_for_app(domain);
    if (!config->has_key(group, key))
        return Optional<String> {};
    return Optional<String> { config->read_entry(group, key) };
}

Messages::ConfigServer::ReadI32ValueResponse ClientConnection::read_i32_value(String const& domain, String const& group, String const& key)
{
    if (!validate_access(domain, group, key))
        return nullptr;

    auto config = Core::ConfigFile::open_for_app(domain);
    if (!config->has_key(group, key))
        return Optional<i32> {};
    return Optional<i32> { config->read_num_entry(group, key) };
}

Messages::ConfigServer::ReadBoolValueResponse ClientConnection::read_bool_value(String const& domain, String const& group, String const& key)
{
    if (!validate_access(domain, group, key))
        return nullptr;

    auto config = Core::ConfigFile::open_for_app(domain);
    if (!config->has_key(group, key))
        return Optional<bool> {};
    return Optional<bool> { config->read_bool_entry(group, key) };
}

void ClientConnection::write_string_value(String const& domain, String const& group, String const& key, String const& value)
{
    if (!validate_access(domain, group, key))
        return;

    auto config = Core::ConfigFile::open_for_app(domain, Core::ConfigFile::AllowWriting::Yes);
    config->write_entry(group, key, value);
}

void ClientConnection::write_i32_value(String const& domain, String const& group, String const& key, i32 value)
{
    if (!validate_access(domain, group, key))
        return;

    auto config = Core::ConfigFile::open_for_app(domain, Core::ConfigFile::AllowWriting::Yes);
    config->write_num_entry(group, key, value);
}

void ClientConnection::write_bool_value(String const& domain, String const& group, String const& key, bool value)
{
    if (!validate_access(domain, group, key))
        return;

    auto config = Core::ConfigFile::open_for_app(domain, Core::ConfigFile::AllowWriting::Yes);
    config->write_bool_entry(group, key, value);
}

}
