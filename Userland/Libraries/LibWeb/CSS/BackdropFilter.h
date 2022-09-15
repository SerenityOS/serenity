/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Variant.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

class BackdropFilter {
public:
    BackdropFilter() = default;
    BackdropFilter(FilterValueListStyleValue const& filter_value_list)
        : m_filter_value_list { filter_value_list } {};

    static inline BackdropFilter make_none()
    {
        return BackdropFilter {};
    }

    bool has_filters() const { return m_filter_value_list; }
    bool is_none() const { return !has_filters(); }

    Span<FilterFunction const> filters() const
    {
        VERIFY(has_filters());
        return m_filter_value_list->filter_value_list().span();
    }

private:
    RefPtr<FilterValueListStyleValue const> m_filter_value_list { nullptr };
};

}
