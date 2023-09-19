/*
 * Copyright (c) 2023, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ShorthandStyleValue.h"
#include <LibWeb/CSS/StyleValues/StyleValueList.h>

namespace Web::CSS {

ShorthandStyleValue::ShorthandStyleValue(Vector<PropertyID> sub_properties, Vector<ValueComparingNonnullRefPtr<StyleValue const>> values)
    : StyleValueWithDefaultOperators(Type::Shorthand)
    , m_properties { move(sub_properties), move(values) }
{
    if (m_properties.sub_properties.size() != m_properties.values.size()) {
        dbgln("ShorthandStyleValue: sub_properties and values must be the same size! {} != {}", m_properties.sub_properties.size(), m_properties.values.size());
        VERIFY_NOT_REACHED();
    }
}

ShorthandStyleValue::~ShorthandStyleValue() = default;

String ShorthandStyleValue::to_string() const
{
    StringBuilder builder;
    auto first = true;
    for (auto& value : m_properties.values) {
        if (first)
            first = false;
        else
            builder.append(' ');
        builder.append(value->to_string());
    }
    return MUST(builder.to_string());
}

}
