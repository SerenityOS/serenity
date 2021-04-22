/*
 * Copyright (c) 2020, Denis Campredon <deni_@hotmail.fr>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringBuilder.h>

#ifdef DEBUG_SPAM

namespace AK {
class ScopeLogger {
public:
    ScopeLogger(StringView&& fun)
        : m_fun(fun)
    {
        StringBuilder sb;

        for (auto indent = m_depth++; indent > 0; indent--)
            sb.append(' ');
        dbgln("\033[1;{}m{}entering {}\033[0m", m_depth % 8 + 30, sb.to_string(), m_fun);
    }
    ~ScopeLogger()
    {
        StringBuilder sb;

        for (auto indent = --m_depth; indent > 0; indent--)
            sb.append(' ');
        dbgln("\033[1;{}m{}leaving {}\033[0m", (m_depth + 1) % 8 + 30, sb.to_string(), m_fun);
    }

private:
    static inline size_t m_depth = 0;
    StringView m_fun;
};
}

using AK::ScopeLogger;
#    define SCOPE_LOGGER() auto tmp##__COUNTER__ = ScopeLogger(__PRETTY_FUNCTION__);

#else
#    define SCOPE_LOGGER()
#endif
