/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "UnresolvedStyleValue.h"
#include <AK/StringBuilder.h>

namespace Web::CSS {

ErrorOr<String> UnresolvedStyleValue::to_string() const
{
    StringBuilder builder;
    for (auto& value : m_values)
        TRY(builder.try_append(value.to_string()));
    return builder.to_string();
}

bool UnresolvedStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    // This is a case where comparing the strings actually makes sense.
    return to_string().release_value_but_fixme_should_propagate_errors() == other.to_string().release_value_but_fixme_should_propagate_errors();
}

}
