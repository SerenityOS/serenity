/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

class ListStyleStyleValue final : public StyleValueWithDefaultOperators<ListStyleStyleValue> {
public:
    static ValueComparingNonnullRefPtr<ListStyleStyleValue> create(
        ValueComparingNonnullRefPtr<StyleValue> position,
        ValueComparingNonnullRefPtr<StyleValue> image,
        ValueComparingNonnullRefPtr<StyleValue> style_type)
    {
        return adopt_ref(*new (nothrow) ListStyleStyleValue(move(position), move(image), move(style_type)));
    }
    virtual ~ListStyleStyleValue() override = default;

    ValueComparingNonnullRefPtr<StyleValue> position() const { return m_properties.position; }
    ValueComparingNonnullRefPtr<StyleValue> image() const { return m_properties.image; }
    ValueComparingNonnullRefPtr<StyleValue> style_type() const { return m_properties.style_type; }

    virtual String to_string() const override;

    bool properties_equal(ListStyleStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    ListStyleStyleValue(
        ValueComparingNonnullRefPtr<StyleValue> position,
        ValueComparingNonnullRefPtr<StyleValue> image,
        ValueComparingNonnullRefPtr<StyleValue> style_type)
        : StyleValueWithDefaultOperators(Type::ListStyle)
        , m_properties { .position = move(position), .image = move(image), .style_type = move(style_type) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<StyleValue> position;
        ValueComparingNonnullRefPtr<StyleValue> image;
        ValueComparingNonnullRefPtr<StyleValue> style_type;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

}
