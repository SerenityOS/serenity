/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RectStyleValue.h"

namespace Web::CSS {

ErrorOr<ValueComparingNonnullRefPtr<RectStyleValue>> RectStyleValue::create(EdgeRect rect)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) RectStyleValue(move(rect)));
}

ErrorOr<String> RectStyleValue::to_string() const
{
    return String::formatted("rect({} {} {} {})", m_rect.top_edge, m_rect.right_edge, m_rect.bottom_edge, m_rect.left_edge);
}

}
