/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 * Copyright (c) 2024, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <LibGfx/Color.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

class ColorStyleValue : public StyleValueWithDefaultOperators<ColorStyleValue> {
public:
    static ValueComparingNonnullRefPtr<ColorStyleValue> create(Color color);
    virtual ~ColorStyleValue() override = default;

    Color color() const { return m_color; }
    virtual String to_string() const override;
    virtual bool has_color() const override { return true; }
    virtual Color to_color(Optional<Layout::NodeWithStyle const&>) const override { return m_color; }

    bool properties_equal(ColorStyleValue const& other) const { return m_color == other.m_color; }

protected:
    explicit ColorStyleValue(Color color)
        : StyleValueWithDefaultOperators(Type::Color)
        , m_color(color)
    {
    }

private:
    Color m_color;
};

class NamedColorStyleValue : public ColorStyleValue {
public:
    static ValueComparingNonnullRefPtr<ColorStyleValue> create(Color, FlyString const& color_name);
    virtual ~NamedColorStyleValue() = default;

    virtual String to_string() const override;

private:
    NamedColorStyleValue(Color color, FlyString const& color_name)
        : ColorStyleValue(color)
        , m_color_name(color_name)
    {
    }

    FlyString m_color_name;
};

}
