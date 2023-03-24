/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BackgroundStyleValue.h"

namespace Web::CSS {

BackgroundStyleValue::BackgroundStyleValue(
    ValueComparingNonnullRefPtr<StyleValue const> color,
    ValueComparingNonnullRefPtr<StyleValue const> image,
    ValueComparingNonnullRefPtr<StyleValue const> position,
    ValueComparingNonnullRefPtr<StyleValue const> size,
    ValueComparingNonnullRefPtr<StyleValue const> repeat,
    ValueComparingNonnullRefPtr<StyleValue const> attachment,
    ValueComparingNonnullRefPtr<StyleValue const> origin,
    ValueComparingNonnullRefPtr<StyleValue const> clip)
    : StyleValueWithDefaultOperators(Type::Background)
    , m_properties {
        .color = move(color),
        .image = move(image),
        .position = move(position),
        .size = move(size),
        .repeat = move(repeat),
        .attachment = move(attachment),
        .origin = move(origin),
        .clip = move(clip),
        .layer_count = 0
    }
{
    auto layer_count = [](auto style_value) -> size_t {
        if (style_value->is_value_list())
            return style_value->as_value_list().size();
        else
            return 1;
    };

    m_properties.layer_count = max(layer_count(m_properties.image), layer_count(m_properties.position));
    m_properties.layer_count = max(m_properties.layer_count, layer_count(m_properties.size));
    m_properties.layer_count = max(m_properties.layer_count, layer_count(m_properties.repeat));
    m_properties.layer_count = max(m_properties.layer_count, layer_count(m_properties.attachment));
    m_properties.layer_count = max(m_properties.layer_count, layer_count(m_properties.origin));
    m_properties.layer_count = max(m_properties.layer_count, layer_count(m_properties.clip));

    VERIFY(!m_properties.color->is_value_list());
}

BackgroundStyleValue::~BackgroundStyleValue() = default;

ErrorOr<String> BackgroundStyleValue::to_string() const
{
    if (m_properties.layer_count == 1) {
        return String::formatted("{} {} {} {} {} {} {} {}", TRY(m_properties.color->to_string()), TRY(m_properties.image->to_string()), TRY(m_properties.position->to_string()), TRY(m_properties.size->to_string()), TRY(m_properties.repeat->to_string()), TRY(m_properties.attachment->to_string()), TRY(m_properties.origin->to_string()), TRY(m_properties.clip->to_string()));
    }

    auto get_layer_value_string = [](ValueComparingNonnullRefPtr<StyleValue const> const& style_value, size_t index) {
        if (style_value->is_value_list())
            return style_value->as_value_list().value_at(index, true)->to_string();
        return style_value->to_string();
    };

    StringBuilder builder;
    for (size_t i = 0; i < m_properties.layer_count; i++) {
        if (i)
            TRY(builder.try_append(", "sv));
        if (i == m_properties.layer_count - 1)
            TRY(builder.try_appendff("{} ", TRY(m_properties.color->to_string())));
        TRY(builder.try_appendff("{} {} {} {} {} {} {}", TRY(get_layer_value_string(m_properties.image, i)), TRY(get_layer_value_string(m_properties.position, i)), TRY(get_layer_value_string(m_properties.size, i)), TRY(get_layer_value_string(m_properties.repeat, i)), TRY(get_layer_value_string(m_properties.attachment, i)), TRY(get_layer_value_string(m_properties.origin, i)), TRY(get_layer_value_string(m_properties.clip, i))));
    }

    return builder.to_string();
}

}
