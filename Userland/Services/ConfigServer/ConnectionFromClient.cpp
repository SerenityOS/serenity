/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConnectionFromClient.h"
#include <ConfigServer/ConfigClientEndpoint.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/FileWatcher.h>
#include <LibCore/Timer.h>

namespace ConfigServer {

static HashMap<int, RefPtr<ConnectionFromClient>> s_connections;

struct CachedDomain {
    ByteString domain;
    NonnullRefPtr<Core::ConfigFile> config;
    RefPtr<Core::FileWatcher> watcher;
};

static HashMap<ByteString, NonnullOwnPtr<CachedDomain>> s_cache;
static constexpr int s_disk_sync_delay_ms = 5'000;

static void for_each_monitoring_connection(ByteString const& domain, ConnectionFromClient* excluded_connection, Function<void(ConnectionFromClient&)> callback)
{
    for (auto& it : s_connections) {
        if (it.value->is_monitoring_domain(domain) && (!excluded_connection || it.value != excluded_connection))
            callback(*it.value);
    }
}

static Core::ConfigFile& ensure_domain_config(ByteString const& domain)
{
    auto it = s_cache.find(domain);
    if (it != s_cache.end())
        return *it->value->config;

    auto config = Core::ConfigFile::open_for_app(domain, Core::ConfigFile::AllowWriting::Yes).release_value_but_fixme_should_propagate_errors();
    // FIXME: Use a single FileWatcher with multiple watches inside.
    auto watcher_or_error = Core::FileWatcher::create(Core::FileWatcherFlags::Nonblock);
    VERIFY(!watcher_or_error.is_error());
    auto result = watcher_or_error.value()->add_watch(config->filename(), Core::FileWatcherEvent::Type::ContentModified);
    VERIFY(!result.is_error());
    watcher_or_error.value()->on_change = [config, domain](auto&) {
        auto new_config = Core::ConfigFile::open(config->filename(), Core::ConfigFile::AllowWriting::Yes).release_value_but_fixme_should_propagate_errors();
        for (auto& group : config->groups()) {
            for (auto& key : config->keys(group)) {
                if (!new_config->has_key(group, key)) {
                    for_each_monitoring_connection(domain, nullptr, [&domain, &group, &key](ConnectionFromClient& connection) {
                        connection.async_notify_removed_key(domain, group, key);
                    });
                }
            }
        }
        // FIXME: Detect type of keys.
        for (auto& group : new_config->groups()) {
            for (auto& key : new_config->keys(group)) {
                auto old_value = config->read_entry(group, key);
                auto new_value = new_config->read_entry(group, key);
                if (old_value != new_value) {
                    for_each_monitoring_connection(domain, nullptr, [&domain, &group, &key, &new_value](ConnectionFromClient& connection) {
                        connection.async_notify_changed_string_value(domain, group, key, new_value);
                    });
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

ConnectionFromClient::ConnectionFromClient(NonnullOwnPtr<Core::LocalSocket> client_socket, int client_id)
    : IPC::ConnectionFromClient<ConfigClientEndpoint, ConfigServerEndpoint>(*this, move(client_socket), client_id)
    , m_sync_timer(Core::Timer::create_single_shot(s_disk_sync_delay_ms, [this]() { sync_dirty_domains_to_disk(); }))
{
    s_connections.set(client_id, *this);
}

void ConnectionFromClient::die()
{
    s_connections.remove(client_id());
    m_sync_timer->stop();
    sync_dirty_domains_to_disk();
}

void ConnectionFromClient::pledge_domains(Vector<ByteString> const& domains)
{
    if (m_has_pledged) {
        did_misbehave("Tried to pledge domains twice.");
        return;
    }
    m_has_pledged = true;
    for (auto& domain : domains)
        m_pledged_domains.set(domain);
}

void ConnectionFromClient::enable_permissive_mode()
{
    if (m_has_pledged) {
        did_misbehave("Tried to enable permissive mode after pledging.");
        return;
    }
    m_permissive_mode = true;
}

void ConnectionFromClient::monitor_domain(ByteString const& domain)
{
    if (m_has_pledged && !m_pledged_domains.contains(domain)) {
        if (!m_permissive_mode)
            did_misbehave("Attempt to monitor non-pledged domain");
        return;
    }

    m_monitored_domains.set(domain);
}

bool ConnectionFromClient::validate_access(ByteString const& domain, ByteString const& group, ByteString const& key)
{
    if (!m_has_pledged)
        return true;
    if (m_pledged_domains.contains(domain))
        return true;
    if (!m_permissive_mode)
        did_misbehave(ByteString::formatted("Blocked attempt to access domain '{}', group={}, key={}", domain, group, key).characters());
    return false;
}

void ConnectionFromClient::sync_dirty_domains_to_disk()
{
    if (m_dirty_domains.is_empty())
        return;
    auto dirty_domains = move(m_dirty_domains);
    dbgln("Syncing {} dirty domains to disk", dirty_domains.size());
    for (auto domain : dirty_domains) {
        auto& config = ensure_domain_config(domain);
        if (auto result = config.sync(); result.is_error()) {
            dbgln("Failed to write config '{}' to disk: {}", domain, result.error());
            // Put it back in the list since it's still dirty.
            m_dirty_domains.set(domain);
        }
    }
}

Messages::ConfigServer::ListConfigKeysResponse ConnectionFromClient::list_config_keys(ByteString const& domain, ByteString const& group)
{
    if (!validate_access(domain, group, ""))
        return Vector<ByteString> {};
    auto& config = ensure_domain_config(domain);
    return { config.keys(group) };
}

Messages::ConfigServer::ListConfigGroupsResponse ConnectionFromClient::list_config_groups(ByteString const& domain)
{
    if (!validate_access(domain, "", ""))
        return Vector<ByteString> {};
    auto& config = ensure_domain_config(domain);
    return { config.groups() };
}

Messages::ConfigServer::ReadStringValueResponse ConnectionFromClient::read_string_value(ByteString const& domain, ByteString const& group, ByteString const& key)
{
    if (!validate_access(domain, group, key)) {
        if (m_permissive_mode)
            return Optional<ByteString> {};
        return nullptr;
    }

    auto& config = ensure_domain_config(domain);
    if (!config.has_key(group, key))
        return Optional<ByteString> {};
    return Optional<ByteString> { config.read_entry(group, key) };
}

Messages::ConfigServer::ReadI32ValueResponse ConnectionFromClient::read_i32_value(ByteString const& domain, ByteString const& group, ByteString const& key)
{
    if (!validate_access(domain, group, key)) {
        if (m_permissive_mode)
            return Optional<i32> {};
        return nullptr;
    }

    auto& config = ensure_domain_config(domain);
    if (!config.has_key(group, key))
        return Optional<i32> {};
    return Optional<i32> { config.read_num_entry(group, key) };
}

Messages::ConfigServer::ReadU32ValueResponse ConnectionFromClient::read_u32_value(ByteString const& domain, ByteString const& group, ByteString const& key)
{
    if (!validate_access(domain, group, key)) {
        if (m_permissive_mode)
            return Optional<u32> {};
        return nullptr;
    }

    auto& config = ensure_domain_config(domain);
    if (!config.has_key(group, key))
        return Optional<u32> {};
    return Optional<u32> { config.read_num_entry<u32>(group, key) };
}

Messages::ConfigServer::ReadBoolValueResponse ConnectionFromClient::read_bool_value(ByteString const& domain, ByteString const& group, ByteString const& key)
{
    if (!validate_access(domain, group, key)) {
        if (m_permissive_mode)
            return Optional<bool> {};
        return nullptr;
    }

    auto& config = ensure_domain_config(domain);
    if (!config.has_key(group, key))
        return Optional<bool> {};
    return Optional<bool> { config.read_bool_entry(group, key) };
}

void ConnectionFromClient::start_or_restart_sync_timer()
{
    if (m_sync_timer->is_active())
        m_sync_timer->restart();
    else
        m_sync_timer->start();
}

void ConnectionFromClient::write_string_value(ByteString const& domain, ByteString const& group, ByteString const& key, ByteString const& value)
{
    if (!validate_access(domain, group, key))
        return;

    auto& config = ensure_domain_config(domain);

    if (config.has_key(group, key) && config.read_entry(group, key) == value)
        return;

    config.write_entry(group, key, value);
    m_dirty_domains.set(domain);
    start_or_restart_sync_timer();

    for_each_monitoring_connection(domain, this, [&domain, &group, &key, &value](ConnectionFromClient& connection) {
        connection.async_notify_changed_string_value(domain, group, key, value);
    });
}

void ConnectionFromClient::write_i32_value(ByteString const& domain, ByteString const& group, ByteString const& key, i32 value)
{
    if (!validate_access(domain, group, key))
        return;

    auto& config = ensure_domain_config(domain);

    if (config.has_key(group, key) && config.read_num_entry(group, key) == value)
        return;

    config.write_num_entry(group, key, value);
    m_dirty_domains.set(domain);
    start_or_restart_sync_timer();

    for_each_monitoring_connection(domain, this, [&domain, &group, &key, &value](ConnectionFromClient& connection) {
        connection.async_notify_changed_i32_value(domain, group, key, value);
    });
}

void ConnectionFromClient::write_u32_value(ByteString const& domain, ByteString const& group, ByteString const& key, u32 value)
{
    if (!validate_access(domain, group, key))
        return;

    auto& config = ensure_domain_config(domain);

    if (config.has_key(group, key) && config.read_num_entry<u32>(group, key) == value)
        return;

    config.write_num_entry(group, key, value);
    m_dirty_domains.set(domain);
    start_or_restart_sync_timer();

    for_each_monitoring_connection(domain, this, [&domain, &group, &key, &value](ConnectionFromClient& connection) {
        connection.async_notify_changed_u32_value(domain, group, key, value);
    });
}

void ConnectionFromClient::write_bool_value(ByteString const& domain, ByteString const& group, ByteString const& key, bool value)
{
    if (!validate_access(domain, group, key))
        return;

    auto& config = ensure_domain_config(domain);

    if (config.has_key(group, key) && config.read_bool_entry(group, key) == value)
        return;

    config.write_bool_entry(group, key, value);
    m_dirty_domains.set(domain);
    start_or_restart_sync_timer();

    for_each_monitoring_connection(domain, this, [&domain, &group, &key, &value](ConnectionFromClient& connection) {
        connection.async_notify_changed_bool_value(domain, group, key, value);
    });
}

void ConnectionFromClient::remove_key_entry(ByteString const& domain, ByteString const& group, ByteString const& key)
{
    if (!validate_access(domain, group, key))
        return;

    auto& config = ensure_domain_config(domain);
    if (!config.has_key(group, key))
        return;

    config.remove_entry(group, key);
    m_dirty_domains.set(domain);
    start_or_restart_sync_timer();

    for_each_monitoring_connection(domain, this, [&domain, &group, &key](ConnectionFromClient& connection) {
        connection.async_notify_removed_key(domain, group, key);
    });
}

void ConnectionFromClient::remove_group_entry(ByteString const& domain, ByteString const& group)
{
    if (!validate_access(domain, group, {}))
        return;

    auto& config = ensure_domain_config(domain);
    if (!config.has_group(group))
        return;

    config.remove_group(group);
    m_dirty_domains.set(domain);
    start_or_restart_sync_timer();

    for_each_monitoring_connection(domain, this, [&domain, &group](ConnectionFromClient& connection) {
        connection.async_notify_removed_group(domain, group);
    });
}

void ConnectionFromClient::add_group_entry(ByteString const& domain, ByteString const& group)
{
    if (!validate_access(domain, group, {}))
        return;

    auto& config = ensure_domain_config(domain);
    if (config.has_group(group))
        return;

    config.add_group(group);
    m_dirty_domains.set(domain);
    start_or_restart_sync_timer();

    for_each_monitoring_connection(domain, this, [&domain, &group](ConnectionFromClient& connection) {
        connection.async_notify_added_group(domain, group);
    });
}

}
