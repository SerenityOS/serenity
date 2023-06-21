/*
 * Copyright (c) 2022, networkException <networkexception@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/Scripting/ModuleMap.h>

namespace Web::HTML {

void ModuleMap::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto& it : m_values) {
        visitor.visit(it.value.module_script);
    }
}

bool ModuleMap::is_fetching(AK::URL const& url, DeprecatedString const& type) const
{
    return is(url, type, EntryType::Fetching);
}

bool ModuleMap::is_failed(AK::URL const& url, DeprecatedString const& type) const
{
    return is(url, type, EntryType::Failed);
}

bool ModuleMap::is(AK::URL const& url, DeprecatedString const& type, EntryType entry_type) const
{
    auto value = m_values.get({ url, type });
    if (!value.has_value())
        return false;

    return value->type == entry_type;
}

Optional<ModuleMap::Entry> ModuleMap::get(AK::URL const& url, DeprecatedString const& type) const
{
    return m_values.get({ url, type });
}

AK::HashSetResult ModuleMap::set(AK::URL const& url, DeprecatedString const& type, Entry entry)
{
    auto callbacks = m_callbacks.get({ url, type });
    if (callbacks.has_value())
        for (auto const& callback : *callbacks)
            callback(entry);

    return m_values.set({ url, type }, entry);
}

void ModuleMap::wait_for_change(AK::URL const& url, DeprecatedString const& type, Function<void(Entry)> callback)
{
    m_callbacks.ensure({ url, type }).append(move(callback));
}

}
