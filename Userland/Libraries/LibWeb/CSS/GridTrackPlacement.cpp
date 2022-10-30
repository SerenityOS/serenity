/*
 * Copyright (c) 2022, Martin Falisse <mfalisse@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GridTrackPlacement.h"
#include <AK/String.h>

namespace Web::CSS {

GridTrackPlacement::GridTrackPlacement(int span_count_or_position, bool has_span)
    : m_type(has_span ? Type::Span : Type::Position)
    , m_span_count_or_position(span_count_or_position)
{
}

GridTrackPlacement::GridTrackPlacement()
    : m_type(Type::Auto)
{
}

String GridTrackPlacement::to_string() const
{
    StringBuilder builder;
    if (is_auto()) {
        builder.append("auto"sv);
        return builder.to_string();
    }
    if (is_span()) {
        builder.append("span"sv);
        builder.append(" "sv);
    }
    if (m_span_count_or_position != 0) {
        builder.append(String::number(m_span_count_or_position));
        builder.append(" "sv);
    }
    return builder.to_string();
}

}
