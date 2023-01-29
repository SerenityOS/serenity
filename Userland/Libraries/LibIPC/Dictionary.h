/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/DeprecatedString.h>
#include <AK/HashMap.h>

namespace IPC {

class Dictionary {
public:
    Dictionary() = default;

    Dictionary(HashMap<DeprecatedString, DeprecatedString> const& initial_entries)
        : m_entries(initial_entries)
    {
    }

    bool is_empty() const { return m_entries.is_empty(); }
    size_t size() const { return m_entries.size(); }

    void add(DeprecatedString key, DeprecatedString value)
    {
        m_entries.set(move(key), move(value));
    }

    template<typename Callback>
    void for_each_entry(Callback callback) const
    {
        for (auto& it : m_entries) {
            callback(it.key, it.value);
        }
    }

    template<FallibleFunction<DeprecatedString const&, DeprecatedString const&> Callback>
    ErrorOr<void> try_for_each_entry(Callback&& callback) const
    {
        for (auto const& it : m_entries)
            TRY(callback(it.key, it.value));
        return {};
    }

    HashMap<DeprecatedString, DeprecatedString> const& entries() const { return m_entries; }

private:
    HashMap<DeprecatedString, DeprecatedString> m_entries;
};

}
