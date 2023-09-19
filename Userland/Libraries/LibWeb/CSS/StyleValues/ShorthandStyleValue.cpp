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
    case PropertyID::Background: {
        auto color = longhand(PropertyID::BackgroundColor);
        auto image = longhand(PropertyID::BackgroundImage);
        auto position = longhand(PropertyID::BackgroundPosition);
        auto size = longhand(PropertyID::BackgroundSize);
        auto repeat = longhand(PropertyID::BackgroundRepeat);
        auto attachment = longhand(PropertyID::BackgroundAttachment);
        auto origin = longhand(PropertyID::BackgroundOrigin);
        auto clip = longhand(PropertyID::BackgroundClip);

        auto get_layer_count = [](auto style_value) -> size_t {
            return style_value->is_value_list() ? style_value->as_value_list().size() : 1;
        };

        auto layer_count = max(get_layer_count(image), max(get_layer_count(position), max(get_layer_count(size), max(get_layer_count(repeat), max(get_layer_count(attachment), max(get_layer_count(origin), get_layer_count(clip)))))));

        if (layer_count == 1) {
            return MUST(String::formatted("{} {} {} {} {} {} {} {}", color->to_string(), image->to_string(), position->to_string(), size->to_string(), repeat->to_string(), attachment->to_string(), origin->to_string(), clip->to_string()));
        }

        auto get_layer_value_string = [](ValueComparingRefPtr<StyleValue const> const& style_value, size_t index) {
            if (style_value->is_value_list())
                return style_value->as_value_list().value_at(index, true)->to_string();
            return style_value->to_string();
        };

        StringBuilder builder;
        for (size_t i = 0; i < layer_count; i++) {
            if (i)
                builder.append(", "sv);
            if (i == layer_count - 1)
                builder.appendff("{} ", color->to_string());
            builder.appendff("{} {} {} {} {} {} {}", get_layer_value_string(image, i), get_layer_value_string(position, i), get_layer_value_string(size, i), get_layer_value_string(repeat, i), get_layer_value_string(attachment, i), get_layer_value_string(origin, i), get_layer_value_string(clip, i));
        }

        return MUST(builder.to_string());
    }
    case PropertyID::Flex:
        return MUST(String::formatted("{} {} {}", longhand(PropertyID::FlexGrow)->to_string(), longhand(PropertyID::FlexShrink)->to_string(), longhand(PropertyID::FlexBasis)->to_string()));
    case PropertyID::FlexFlow:
        return MUST(String::formatted("{} {}", longhand(PropertyID::FlexDirection)->to_string(), longhand(PropertyID::FlexWrap)->to_string()));
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
