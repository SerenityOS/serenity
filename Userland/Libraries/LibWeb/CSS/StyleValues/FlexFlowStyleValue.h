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

class FlexFlowStyleValue final : public StyleValueWithDefaultOperators<FlexFlowStyleValue> {
public:
    static ValueComparingNonnullRefPtr<FlexFlowStyleValue> create(ValueComparingNonnullRefPtr<StyleValue> flex_direction, ValueComparingNonnullRefPtr<StyleValue> flex_wrap)
    {
        return adopt_ref(*new (nothrow) FlexFlowStyleValue(move(flex_direction), move(flex_wrap)));
    }
    virtual ~FlexFlowStyleValue() override = default;

    ValueComparingNonnullRefPtr<StyleValue> flex_direction() const { return m_properties.flex_direction; }
    ValueComparingNonnullRefPtr<StyleValue> flex_wrap() const { return m_properties.flex_wrap; }

    virtual String to_string() const override;

    bool properties_equal(FlexFlowStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    FlexFlowStyleValue(ValueComparingNonnullRefPtr<StyleValue> flex_direction, ValueComparingNonnullRefPtr<StyleValue> flex_wrap)
        : StyleValueWithDefaultOperators(Type::FlexFlow)
        , m_properties { .flex_direction = move(flex_direction), .flex_wrap = move(flex_wrap) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<StyleValue> flex_direction;
        ValueComparingNonnullRefPtr<StyleValue> flex_wrap;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

}
