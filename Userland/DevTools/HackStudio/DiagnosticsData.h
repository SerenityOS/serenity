/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Diagnostics.h"

namespace HackStudio {

struct DiagnosticsData {
    static DiagnosticsData& the() { return s_this; }

    void set_diagnostics_for(String filename, Vector<Diagnostic> diagnostics)
    {
        m_all_diagnostics_cache.remove_all_matching([&filename](auto const& diagnostic) { return diagnostic.start_position.file == filename; });
        m_all_diagnostics_cache.ensure_capacity(m_all_diagnostics_cache.size() + diagnostics.size());
        m_diagnostics.set(filename, move(diagnostics));

        for (auto& entry : m_diagnostics.find(filename)->value)
            m_all_diagnostics_cache.unchecked_append(entry);

        if (on_update)
            on_update();
    }

    Vector<Diagnostic> const* diagnostics_for(StringView filename) const
    {
        auto it = m_diagnostics.find(filename);
        if (it == m_diagnostics.end())
            return nullptr;
        return &it->value;
    }

    Vector<Diagnostic const&> const& diagnostics() const { return m_all_diagnostics_cache; }

    Function<void()> on_update;

private:
    DiagnosticsData() = default;

    HashMap<String, Vector<Diagnostic>> m_diagnostics;
    Vector<Diagnostic const&> m_all_diagnostics_cache;
    static DiagnosticsData s_this;
};

}
