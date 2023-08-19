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

class FlexStyleValue final : public StyleValueWithDefaultOperators<FlexStyleValue> {
public:
    static ValueComparingNonnullRefPtr<FlexStyleValue> create(
        ValueComparingNonnullRefPtr<StyleValue> grow,
        ValueComparingNonnullRefPtr<StyleValue> shrink,
        ValueComparingNonnullRefPtr<StyleValue> basis)
    {
        return adopt_ref(*new (nothrow) FlexStyleValue(move(grow), move(shrink), move(basis)));
    }
    virtual ~FlexStyleValue() override = default;

    ValueComparingNonnullRefPtr<StyleValue> grow() const { return m_properties.grow; }
    ValueComparingNonnullRefPtr<StyleValue> shrink() const { return m_properties.shrink; }
    ValueComparingNonnullRefPtr<StyleValue> basis() const { return m_properties.basis; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(FlexStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    FlexStyleValue(
        ValueComparingNonnullRefPtr<StyleValue> grow,
        ValueComparingNonnullRefPtr<StyleValue> shrink,
        ValueComparingNonnullRefPtr<StyleValue> basis)
        : StyleValueWithDefaultOperators(Type::Flex)
        , m_properties { .grow = move(grow), .shrink = move(shrink), .basis = move(basis) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<StyleValue> grow;
        ValueComparingNonnullRefPtr<StyleValue> shrink;
        ValueComparingNonnullRefPtr<StyleValue> basis;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

}
