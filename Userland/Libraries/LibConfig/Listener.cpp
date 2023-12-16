/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/Function.h>
#include <AK/HashTable.h>
#include <LibConfig/Listener.h>

namespace Config {

static HashTable<Listener*> s_listeners;

Listener::Listener()
{
    s_listeners.set(this);
}

Listener::~Listener()
{
    s_listeners.remove(this);
}

void Listener::for_each(Function<void(Listener&)> callback)
{
    for (auto* listener : s_listeners)
        callback(*listener);
}

void Listener::config_string_did_change(StringView, StringView, StringView, StringView)
{
}

void Listener::config_i32_did_change(StringView, StringView, StringView, i32)
{
}

void Listener::config_u32_did_change(StringView, StringView, StringView, u32)
{
}

void Listener::config_bool_did_change(StringView, StringView, StringView, bool)
{
}

void Listener::config_key_was_removed(StringView, StringView, StringView)
{
}

void Listener::config_group_was_removed(StringView, StringView)
{
}

void Listener::config_group_was_added(StringView, StringView)
{
}

}
