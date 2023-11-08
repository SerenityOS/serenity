/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Traits.h>

class WindowIdentifier {
public:
    WindowIdentifier() = default;
    WindowIdentifier(int client_id, int window_id)
        : m_client_id(client_id)
        , m_window_id(window_id)
    {
    }

    int client_id() const { return m_client_id; }
    int window_id() const { return m_window_id; }

    bool operator==(WindowIdentifier const& other) const
    {
        return m_client_id == other.m_client_id && m_window_id == other.m_window_id;
    }

    bool is_valid() const
    {
        return m_client_id != -1 && m_window_id != -1;
    }

private:
    int m_client_id { -1 };
    int m_window_id { -1 };
};

namespace AK {
template<>
struct Traits<WindowIdentifier> : public DefaultTraits<WindowIdentifier> {
    static unsigned hash(WindowIdentifier const& w) { return pair_int_hash(w.client_id(), w.window_id()); }
};
}
