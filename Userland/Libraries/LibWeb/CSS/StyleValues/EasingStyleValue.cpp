/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 * Copyright (c) 2023, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EasingStyleValue.h"
#include <AK/StringBuilder.h>

namespace Web::CSS {

String EasingStyleValue::to_string() const
{
    if (m_properties.easing_function == EasingFunction::StepStart)
        return "steps(1, start)"_string;
    if (m_properties.easing_function == EasingFunction::StepEnd)
        return "steps(1, end)"_string;

    StringBuilder builder;
    builder.append(CSS::to_string(m_properties.easing_function));

    if (m_properties.values.is_empty())
        return MUST(builder.to_string());

    builder.append('(');
    for (size_t i = 0; i < m_properties.values.size(); ++i) {
        builder.append(m_properties.values[i]->to_string());
        if (i != m_properties.values.size() - 1)
            builder.append(", "sv);
    }
    builder.append(')');

    return MUST(builder.to_string());
}

bool EasingStyleValue::Properties::operator==(Properties const& other) const
{
    return easing_function == other.easing_function && values == other.values;
}

}
