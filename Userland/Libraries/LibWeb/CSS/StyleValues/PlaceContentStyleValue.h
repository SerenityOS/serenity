/*
 * Copyright (c) 2023, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

class PlaceContentStyleValue final : public StyleValueWithDefaultOperators<PlaceContentStyleValue> {
public:
    static ValueComparingNonnullRefPtr<PlaceContentStyleValue> create(ValueComparingNonnullRefPtr<StyleValue> align_content, ValueComparingNonnullRefPtr<StyleValue> justify_content)
    {
        return adopt_ref(*new (nothrow) PlaceContentStyleValue(move(align_content), move(justify_content)));
    }
    virtual ~PlaceContentStyleValue() override = default;

    ValueComparingNonnullRefPtr<StyleValue> align_content() const { return m_properties.align_content; }
    ValueComparingNonnullRefPtr<StyleValue> justify_content() const { return m_properties.justify_content; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(PlaceContentStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    PlaceContentStyleValue(ValueComparingNonnullRefPtr<StyleValue> align_content, ValueComparingNonnullRefPtr<StyleValue> justify_content)
        : StyleValueWithDefaultOperators(Type::PlaceContent)
        , m_properties { .align_content = move(align_content), .justify_content = move(justify_content) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<StyleValue> align_content;
        ValueComparingNonnullRefPtr<StyleValue> justify_content;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

}
