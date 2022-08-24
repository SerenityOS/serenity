/*
 * Copyright (c) 2022, Martin Falisse <mfalisse@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GridTrackPlacement.h"
#include <AK/String.h>

namespace Web::CSS {

GridTrackPlacement::GridTrackPlacement(int position, bool has_span)
    : m_position(position)
    , m_has_span(has_span)
{
}

GridTrackPlacement::GridTrackPlacement(int position)
    : m_position(position)
{
}

GridTrackPlacement::GridTrackPlacement()
{
}

String GridTrackPlacement::to_string() const
{
    StringBuilder builder;
    if (m_has_span)
        builder.append("span "sv);
    builder.append(String::number(m_position));
    return builder.to_string();
}

}
