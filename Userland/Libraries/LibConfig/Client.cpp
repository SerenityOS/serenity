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
        VERIFY(Core::EventLoop::has_been_instantiated());
        s_the = Client::construct();
    }
    return *s_the;
}

void Client::pledge_domains(Vector<String> const& domains)
{
    async_pledge_domains(domains);
}

void Client::monitor_domain(String const& domain)
{
    async_monitor_domain(domain);
}

Vector<String> Client::list_keys(StringView domain, StringView group)
{
    return list_config_keys(domain, group);
}

Vector<String> Client::list_groups(StringView domain)
{
    return list_config_groups(domain);
}

String Client::read_string(StringView domain, StringView group, StringView key, StringView fallback)
{
    return read_string_value(domain, group, key).value_or(fallback);
}

i32 Client::read_i32(StringView domain, StringView group, StringView key, i32 fallback)
{
    return read_i32_value(domain, group, key).value_or(fallback);
}

bool Client::read_bool(StringView domain, StringView group, StringView key, bool fallback)
{
    return read_bool_value(domain, group, key).value_or(fallback);
}

void Client::write_string(StringView domain, StringView group, StringView key, StringView value)
{
    async_write_string_value(domain, group, key, value);
}

void Client::write_i32(StringView domain, StringView group, StringView key, i32 value)
{
    async_write_i32_value(domain, group, key, value);
}

void Client::write_bool(StringView domain, StringView group, StringView key, bool value)
{
    async_write_bool_value(domain, group, key, value);
}

void Client::remove_key(StringView domain, StringView group, StringView key)
{
    async_remove_key(domain, group, key);
}

void Client::notify_changed_string_value(String const& domain, String const& group, String const& key, String const& value)
{
    Listener::for_each([&](auto& listener) {
        listener.config_string_did_change(domain, group, key, value);
    });
}

void Client::notify_changed_i32_value(String const& domain, String const& group, String const& key, i32 value)
{
    Listener::for_each([&](auto& listener) {
        listener.config_i32_did_change(domain, group, key, value);
    });
}

void Client::notify_changed_bool_value(String const& domain, String const& group, String const& key, bool value)
{
    Listener::for_each([&](auto& listener) {
        listener.config_bool_did_change(domain, group, key, value);
    });
}

void Client::notify_removed_key(const String& domain, const String& group, const String& key)
{
    Listener::for_each([&](auto& listener) {
        listener.config_key_was_removed(domain, group, key);
    });
}

}
