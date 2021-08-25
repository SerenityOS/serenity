/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibConfig/Client.h>

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

}
