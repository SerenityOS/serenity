/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClientConnection.h"
#include <ConfigServer/ConfigClientEndpoint.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/FileWatcher.h>

namespace ConfigServer {

static HashMap<int, RefPtr<ClientConnection>> s_connections;

struct CachedDomain {
    String domain;
    NonnullRefPtr<Core::ConfigFile> config;
    RefPtr<Core::FileWatcher> watcher;
};

static HashMap<String, NonnullOwnPtr<CachedDomain>> s_cache;

static Core::ConfigFile& ensure_domain_config(String const& domain)
{
    auto it = s_cache.find(domain);
    if (it != s_cache.end())
        return *it->value->config;

    auto config = Core::ConfigFile::open_for_app(domain, Core::ConfigFile::AllowWriting::Yes);
    // FIXME: Use a single FileWatcher with multiple watches inside.
    auto watcher_or_error = Core::FileWatcher::create(InodeWatcherFlags::Nonblock);
    VERIFY(!watcher_or_error.is_error());
    auto result = watcher_or_error.value()->add_watch(config->filename(), Core::FileWatcherEvent::Type::ContentModified);
    VERIFY(!result.is_error());
    watcher_or_error.value()->on_change = [config, domain](auto&) {
        auto new_config = Core::ConfigFile::open(config->filename());
        // FIXME: Detect removed keys.
        // FIXME: Detect type of keys.
        for (auto& group : new_config->groups()) {
            for (auto& key : new_config->keys(group)) {
                auto old_value = config->read_entry(group, key);
                auto new_value = new_config->read_entry(group, key);
                if (old_value != new_value) {
                    for (auto& it : s_connections) {
                        if (it.value->is_monitoring_domain(domain)) {
                            it.value->async_notify_changed_string_value(domain, group, key, new_value);
                        }
                    }
                }
            }
        }
        // FIXME: Refactor this whole thing so that we don't need a cache lookup here.
        s_cache.get(domain).value()->config = new_config;
    };
    auto cache_entry = make<CachedDomain>(domain, config, watcher_or_error.release_value());
    s_cache.set(domain, move(cache_entry));
    return *config;
}

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

void ClientConnection::monitor_domain(String const& domain)
{
    if (m_has_pledged && !m_pledged_domains.contains(domain)) {
        did_misbehave("Attempt to monitor non-pledged domain");
        return;
    }

    m_monitored_domains.set(domain);
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

    auto& config = ensure_domain_config(domain);
    if (!config.has_key(group, key))
        return Optional<String> {};
    return Optional<String> { config.read_entry(group, key) };
}

Messages::ConfigServer::ReadI32ValueResponse ClientConnection::read_i32_value(String const& domain, String const& group, String const& key)
{
    if (!validate_access(domain, group, key))
        return nullptr;

    auto& config = ensure_domain_config(domain);
    if (!config.has_key(group, key))
        return Optional<i32> {};
    return Optional<i32> { config.read_num_entry(group, key) };
}

Messages::ConfigServer::ReadBoolValueResponse ClientConnection::read_bool_value(String const& domain, String const& group, String const& key)
{
    if (!validate_access(domain, group, key))
        return nullptr;

    auto& config = ensure_domain_config(domain);
    if (!config.has_key(group, key))
        return Optional<bool> {};
    return Optional<bool> { config.read_bool_entry(group, key) };
}

void ClientConnection::write_string_value(String const& domain, String const& group, String const& key, String const& value)
{
    if (!validate_access(domain, group, key))
        return;

    auto& config = ensure_domain_config(domain);

    if (config.has_key(group, key) && config.read_entry(group, key) == value)
        return;

    config.write_entry(group, key, value);

    for (auto& it : s_connections) {
        if (it.value != this && it.value->m_monitored_domains.contains(domain))
            it.value->async_notify_changed_string_value(domain, group, key, value);
    }
}

void ClientConnection::write_i32_value(String const& domain, String const& group, String const& key, i32 value)
{
    if (!validate_access(domain, group, key))
        return;

    auto& config = ensure_domain_config(domain);

    if (config.has_key(group, key) && config.read_num_entry(group, key) == value)
        return;

    config.write_num_entry(group, key, value);

    for (auto& it : s_connections) {
        if (it.value != this && it.value->m_monitored_domains.contains(domain))
            it.value->async_notify_changed_i32_value(domain, group, key, value);
    }
}

void ClientConnection::write_bool_value(String const& domain, String const& group, String const& key, bool value)
{
    if (!validate_access(domain, group, key))
        return;

    auto& config = ensure_domain_config(domain);

    if (config.has_key(group, key) && config.read_bool_entry(group, key) == value)
        return;

    config.write_bool_entry(group, key, value);

    for (auto& it : s_connections) {
        if (it.value != this && it.value->m_monitored_domains.contains(domain))
            it.value->async_notify_changed_bool_value(domain, group, key, value);
    }
}

}
