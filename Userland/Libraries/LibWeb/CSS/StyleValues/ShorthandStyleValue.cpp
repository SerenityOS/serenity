/*
 * Copyright (c) 2023, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ShorthandStyleValue.h"
#include <LibWeb/CSS/PropertyID.h>
#include <LibWeb/CSS/StyleValues/StyleValueList.h>

namespace Web::CSS {

ShorthandStyleValue::ShorthandStyleValue(PropertyID shorthand, Vector<PropertyID> sub_properties, Vector<ValueComparingNonnullRefPtr<StyleValue const>> values)
    : StyleValueWithDefaultOperators(Type::Shorthand)
    , m_properties { shorthand, move(sub_properties), move(values) }
{
    if (m_properties.sub_properties.size() != m_properties.values.size()) {
        dbgln("ShorthandStyleValue: sub_properties and values must be the same size! {} != {}", m_properties.sub_properties.size(), m_properties.values.size());
        VERIFY_NOT_REACHED();
    }
}

ShorthandStyleValue::~ShorthandStyleValue() = default;

ValueComparingRefPtr<StyleValue const> ShorthandStyleValue::longhand(PropertyID longhand) const
{
    for (auto i = 0u; i < m_properties.sub_properties.size(); ++i) {
        if (m_properties.sub_properties[i] == longhand)
            return m_properties.values[i];
    }
    return nullptr;
}

String ShorthandStyleValue::to_string() const
{
    // Special-cases first
    switch (m_properties.shorthand_property) {
    case PropertyID::Flex:
        return MUST(String::formatted("{} {} {}", longhand(PropertyID::FlexGrow)->to_string(), longhand(PropertyID::FlexShrink)->to_string(), longhand(PropertyID::FlexBasis)->to_string()));
    default:
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

}
