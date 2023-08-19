/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 * Copyright (c) 2023, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/EasingFunctions.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

class EasingStyleValue final : public StyleValueWithDefaultOperators<EasingStyleValue> {
public:
    static ValueComparingNonnullRefPtr<EasingStyleValue> create(CSS::EasingFunction easing_function, StyleValueVector&& values)
    {
        return adopt_ref(*new (nothrow) EasingStyleValue(easing_function, move(values)));
    }
    virtual ~EasingStyleValue() override = default;

    CSS::EasingFunction easing_function() const { return m_properties.easing_function; }
    StyleValueVector values() const { return m_properties.values; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(EasingStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    EasingStyleValue(CSS::EasingFunction easing_function, StyleValueVector&& values)
        : StyleValueWithDefaultOperators(Type::Easing)
        , m_properties { .easing_function = easing_function, .values = move(values) }
    {
    }

    struct Properties {
        CSS::EasingFunction easing_function;
        StyleValueVector values;
        bool operator==(Properties const& other) const;
    } m_properties;
};

}
