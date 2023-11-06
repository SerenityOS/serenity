/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Orientation.h>
#include <LibGfx/Rect.h>

namespace GUI {

class Margins {
public:
    Margins() = default;
    Margins(int all)
        : m_top(all)
        , m_right(all)
        , m_bottom(all)
        , m_left(all)
    {
    }
    Margins(int vertical, int horizontal)
        : m_top(vertical)
        , m_right(horizontal)
        , m_bottom(vertical)
        , m_left(horizontal)
    {
    }
    Margins(int top, int horizontal, int bottom)
        : m_top(top)
        , m_right(horizontal)
        , m_bottom(bottom)
        , m_left(horizontal)
    {
    }
    Margins(int top, int right, int bottom, int left)
        : m_top(top)
        , m_right(right)
        , m_bottom(bottom)
        , m_left(left)
    {
    }

    // GML compatibility constructors only for use in auto-generated code.

    explicit Margins(Array<i64, 1> all)
        : Margins(all[0])
    {
    }

    explicit Margins(Array<i64, 2> vertical_horizontal)
        : Margins(vertical_horizontal[0], vertical_horizontal[1])
    {
    }

    explicit Margins(Array<i64, 3> top_horizontal_bottom)
        : Margins(top_horizontal_bottom[0], top_horizontal_bottom[1], top_horizontal_bottom[2])
    {
    }

    explicit Margins(Array<i64, 4> margins)
        : Margins(margins[0], margins[1], margins[2], margins[3])
    {
    }

    ~Margins() = default;

    [[nodiscard]] Gfx::IntRect applied_to(Gfx::IntRect const& input) const
    {
        Gfx::IntRect output = input;
        output.take_from_left(left());
        output.take_from_top(top());
        output.take_from_right(right());
        output.take_from_bottom(bottom());
        return output;
    }

    bool is_null() const { return !m_left && !m_top && !m_right && !m_bottom; }

    int top() const { return m_top; }
    int right() const { return m_right; }
    int bottom() const { return m_bottom; }
    int left() const { return m_left; }

    void set_top(int value) { m_top = value; }
    void set_right(int value) { m_right = value; }
    void set_bottom(int value) { m_bottom = value; }
    void set_left(int value) { m_left = value; }

    bool operator==(Margins const& other) const
    {
        return m_left == other.m_left
            && m_top == other.m_top
            && m_right == other.m_right
            && m_bottom == other.m_bottom;
    }

    Margins operator+(Margins const& other) const
    {
        return Margins { top() + other.top(), right() + other.right(), bottom() + other.bottom(), left() + other.left() };
    }

    [[nodiscard]] int primary_total_for_orientation(Gfx::Orientation const orientation) const
    {
        if (orientation == Gfx::Orientation::Horizontal)
            return m_left + m_right;
        else
            return m_top + m_bottom;
    }

    [[nodiscard]] int secondary_total_for_orientation(Gfx::Orientation const orientation) const
    {
        if (orientation == Gfx::Orientation::Vertical)
            return m_left + m_right;
        else
            return m_top + m_bottom;
    }

    [[nodiscard]] int horizontal_total() const
    {
        return m_left + m_right;
    }

    [[nodiscard]] int vertical_total() const
    {
        return m_top + m_bottom;
    }

private:
    int m_top { 0 };
    int m_right { 0 };
    int m_bottom { 0 };
    int m_left { 0 };
};

}

#define REGISTER_MARGINS_PROPERTY(property_name, getter, setter) \
    register_property(                                           \
        property_name##sv,                                       \
        [this]() {                                               \
            auto m = getter();                                   \
            JsonObject margins_object;                           \
            margins_object.set("left", m.left());                \
            margins_object.set("right", m.right());              \
            margins_object.set("top", m.top());                  \
            margins_object.set("bottom", m.bottom());            \
            return margins_object;                               \
        },                                                       \
        ::GUI::PropertyDeserializer<::GUI::Margins> {},          \
        [this](auto const& value) { return setter(value); });
