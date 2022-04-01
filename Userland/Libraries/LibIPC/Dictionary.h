/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/String.h>

namespace IPC {

class Dictionary {
public:
    Dictionary() = default;

    Dictionary(HashMap<String, String> const& initial_entries)
        : m_entries(initial_entries)
    {
    }

    bool is_empty() const { return m_entries.is_empty(); }
    size_t size() const { return m_entries.size(); }

    void add(String key, String value)
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

    HashMap<String, String> const& entries() const { return m_entries; }

private:
    HashMap<String, String> m_entries;
};

}
