/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>

namespace Web::CSS {

// https://www.w3.org/TR/css-syntax-3/#urange-syntax
class UnicodeRange {
public:
    UnicodeRange(u32 min_code_point, u32 max_code_point)
        : m_min_code_point(min_code_point)
        , m_max_code_point(max_code_point)
    {
        VERIFY(min_code_point <= max_code_point);
    }

    u32 min_code_point() const { return m_min_code_point; }
    u32 max_code_point() const { return m_max_code_point; }

    bool contains(u32 code_point) const
    {
        return m_min_code_point <= code_point && code_point <= m_max_code_point;
    }

    String to_string() const
    {
        if (m_min_code_point == m_max_code_point)
            return String::formatted("U+{:x}", m_min_code_point);
        return String::formatted("U+{:x}-{:x}", m_min_code_point, m_max_code_point);
    }

private:
    u32 m_min_code_point;
    u32 m_max_code_point;
};

}
