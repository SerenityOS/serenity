/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace GUI {

class Margins {
public:
    Margins() { }
    Margins(int top, int right, int bottom, int left)
        : m_top(top)
        , m_right(right)
        , m_bottom(bottom)
        , m_left(left)
    {
    }
    ~Margins() { }

    bool is_null() const { return !m_left && !m_top && !m_right && !m_bottom; }

    int top() const { return m_top; }
    int right() const { return m_right; }
    int bottom() const { return m_bottom; }
    int left() const { return m_left; }

    void set_top(int value) { m_top = value; }
    void set_right(int value) { m_right = value; }
    void set_bottom(int value) { m_bottom = value; }
    void set_left(int value) { m_left = value; }

    bool operator==(const Margins& other) const
    {
        return m_left == other.m_left
            && m_top == other.m_top
            && m_right == other.m_right
            && m_bottom == other.m_bottom;
    }

private:
    int m_top { 0 };
    int m_right { 0 };
    int m_bottom { 0 };
    int m_left { 0 };
};

}

#define REGISTER_MARGINS_PROPERTY(property_name, getter, setter)   \
    register_property(                                             \
        property_name, [this]() {                                  \
            auto m = getter();                                     \
            JsonObject margins_object;                             \
            margins_object.set("left", m.left());                  \
            margins_object.set("right", m.right());                \
            margins_object.set("top", m.top());                    \
            margins_object.set("bottom", m.bottom());              \
            return margins_object; },                               \
        [this](auto& value) {                                      \
            if (!value.is_array() || value.as_array().size() != 4) \
                return false;                                      \
            int m[4];                                              \
            for (size_t i = 0; i < 4; ++i)                         \
                m[i] = value.as_array().at(i).to_i32();            \
            setter({ m[0], m[1], m[2], m[3] });                    \
            return true;                                           \
        });
