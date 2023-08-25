/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 * Copyright (c) 2022, Martin Falisse <mfalisse@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GridTrackPlacement.h"
#include <AK/StringBuilder.h>

namespace Web::CSS {

String GridTrackPlacement::to_string() const
{
    StringBuilder builder;
    m_value.visit(
        [&](Auto const&) {
            builder.append("auto"sv);
        },
        [&](Area const& area) {
            builder.append(area.name);
        },
        [&](Line const& line) {
            builder.appendff("{}", line.value);
            if (line.name.has_value())
                builder.appendff(" {}", line.name.value());
        },
        [&](Span const& span) {
            builder.appendff("span {}", span.value);
        });
    return MUST(builder.to_string());
}

}
