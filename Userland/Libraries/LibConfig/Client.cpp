/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibConfig/Client.h>
#include <LibConfig/Listener.h>

namespace Config {

static RefPtr<Client> s_the = nullptr;

Client& Client::the()
{
    if (!s_the || !s_the->is_open()) {
        s_the = Client::try_create().release_value_but_fixme_should_propagate_errors();
    }
    return *s_the;
}

void Client::enable_permissive_mode()
{
    async_enable_permissive_mode();
}

void Client::pledge_domains(Vector<ByteString> const& domains)
{
    async_pledge_domains(domains);
}

void Client::monitor_domain(ByteString const& domain)
{
    async_monitor_domain(domain);
}

Vector<ByteString> Client::list_keys(StringView domain, StringView group)
{
    return list_config_keys(domain, group);
}

Vector<ByteString> Client::list_groups(StringView domain)
{
    return list_config_groups(domain);
}

ByteString Client::read_string(StringView domain, StringView group, StringView key, StringView fallback)
{
    return read_string_value(domain, group, key).value_or(fallback);
}

i32 Client::read_i32(StringView domain, StringView group, StringView key, i32 fallback)
{
    return read_i32_value(domain, group, key).value_or(fallback);
}

u32 Client::read_u32(StringView domain, StringView group, StringView key, u32 fallback)
{
    return read_u32_value(domain, group, key).value_or(fallback);
}

bool Client::read_bool(StringView domain, StringView group, StringView key, bool fallback)
{
    return read_bool_value(domain, group, key).value_or(fallback);
}

void Client::write_string(StringView domain, StringView group, StringView key, StringView value)
{
    write_string_value(domain, group, key, value);
}

void Client::write_i32(StringView domain, StringView group, StringView key, i32 value)
{
    write_i32_value(domain, group, key, value);
}

void Client::write_u32(StringView domain, StringView group, StringView key, u32 value)
{
    write_u32_value(domain, group, key, value);
}

void Client::write_bool(StringView domain, StringView group, StringView key, bool value)
{
    write_bool_value(domain, group, key, value);
}

void Client::remove_key(StringView domain, StringView group, StringView key)
{
    remove_key_entry(domain, group, key);
}

void Client::remove_group(StringView domain, StringView group)
{
    remove_group_entry(domain, group);
}

void Client::add_group(StringView domain, StringView group)
{
    add_group_entry(domain, group);
}

void Client::notify_changed_string_value(ByteString const& domain, ByteString const& group, ByteString const& key, ByteString const& value)
{
    Listener::for_each([&](auto& listener) {
        listener.config_string_did_change(domain, group, key, value);
    });
}

void Client::notify_changed_i32_value(ByteString const& domain, ByteString const& group, ByteString const& key, i32 value)
{
    Listener::for_each([&](auto& listener) {
        listener.config_i32_did_change(domain, group, key, value);
    });
}

void Client::notify_changed_u32_value(ByteString const& domain, ByteString const& group, ByteString const& key, u32 value)
{
    Listener::for_each([&](auto& listener) {
        listener.config_u32_did_change(domain, group, key, value);
    });
}

void Client::notify_changed_bool_value(ByteString const& domain, ByteString const& group, ByteString const& key, bool value)
{
    Listener::for_each([&](auto& listener) {
        listener.config_bool_did_change(domain, group, key, value);
    });
}

void Client::notify_removed_key(ByteString const& domain, ByteString const& group, ByteString const& key)
{
    Listener::for_each([&](auto& listener) {
        listener.config_key_was_removed(domain, group, key);
    });
}

void Client::notify_removed_group(ByteString const& domain, ByteString const& group)
{
    Listener::for_each([&](auto& listener) {
        listener.config_group_was_removed(domain, group);
    });
}

void Client::notify_added_group(ByteString const& domain, ByteString const& group)
{
    Listener::for_each([&](auto& listener) {
        listener.config_group_was_added(domain, group);
    });
}

}
