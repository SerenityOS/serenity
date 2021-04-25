/*
 * Copyright (c) 2020, Denis Campredon <deni_@hotmail.fr>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/SourceLocation.h>
#include <AK/StringBuilder.h>

namespace AK {
class ScopeLogger {
public:
#ifdef DEBUG_SPAM
    ScopeLogger(const SourceLocation& location = SourceLocation::current())
        : m_location(location)
    {
        StringBuilder sb;

        for (auto indent = m_depth++; indent > 0; indent--)
            sb.append(' ');
        dbgln("\033[1;{}m{}entering {}\033[0m", m_depth % 8 + 30, sb.to_string(), m_location);
    }
    ~ScopeLogger()
    {
        StringBuilder sb;

        for (auto indent = --m_depth; indent > 0; indent--)
            sb.append(' ');
        dbgln("\033[1;{}m{}leaving {}\033[0m", (m_depth + 1) % 8 + 30, sb.to_string(), m_location);
    }

private:
    static inline size_t m_depth = 0;
    SourceLocation m_location;
#else
    ScopeLogger() = default;
#endif
};
}

using AK::ScopeLogger;
