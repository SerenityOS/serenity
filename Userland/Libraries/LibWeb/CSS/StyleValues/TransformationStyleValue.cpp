/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TransformationStyleValue.h"
#include <AK/StringBuilder.h>

namespace Web::CSS {

ErrorOr<String> TransformationStyleValue::to_string() const
{
    StringBuilder builder;
    TRY(builder.try_append(CSS::to_string(m_properties.transform_function)));
    TRY(builder.try_append('('));
    for (size_t i = 0; i < m_properties.values.size(); ++i) {
        TRY(builder.try_append(TRY(m_properties.values[i]->to_string())));
        if (i != m_properties.values.size() - 1)
            TRY(builder.try_append(", "sv));
    }
    TRY(builder.try_append(')'));

    return builder.to_string();
}

bool TransformationStyleValue::Properties::operator==(Properties const& other) const
{
    return transform_function == other.transform_function && values.span() == other.values.span();
}

}
