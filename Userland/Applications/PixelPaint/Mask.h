/*
 * Copyright (c) 2021, Davipb <daviparca@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>

namespace PixelPaint {

class Mask {
public:
    Mask() = default;

    Mask(Mask const&) = default;
    Mask& operator=(Mask const&) = default;

    Mask(Mask&&) = default;
    Mask& operator=(Mask&&) = default;

    [[nodiscard]] static Mask empty(Gfx::IntRect rect) { return { rect, 0x00 }; }
    [[nodiscard]] static Mask full(Gfx::IntRect rect) { return { rect, 0xFF }; }

    [[nodiscard]] bool is_null() const { return m_data.is_empty(); }
    [[nodiscard]] Gfx::IntRect bounding_rect() const { return m_bounding_rect; }

    [[nodiscard]] u8 get(int x, int y) const
    {
        if (is_null() || !m_bounding_rect.contains(x, y))
            return 0;

        return m_data[to_index(x, y)];
    }
    [[nodiscard]] u8 get(Gfx::IntPoint point) const { return get(point.x(), point.y()); }
    [[nodiscard]] float getf(int x, int y) const { return (float)get(x, y) / 255.0f; }
    [[nodiscard]] float getf(Gfx::IntPoint point) const { return getf(point.x(), point.y()); }

    void set(int x, int y, u8 value)
    {
        VERIFY(!is_null());
        VERIFY(m_bounding_rect.contains(x, y));

        m_data[to_index(x, y)] = value;
    }
    void set(Gfx::IntPoint point, u8 value) { set(point.x(), point.y(), value); }
    void setf(int x, int y, float value) { set(x, y, (u8)clamp(value * 255.0f, 0.0f, 255.0f)); }
    void setf(Gfx::IntPoint point, float value) { setf(point.x(), point.y(), value); }

    void shrink_to_fit();
    [[nodiscard]] Mask with_bounding_rect(Gfx::IntRect) const;

    void invert();
    void add(Mask const& other);
    void subtract(Mask const& other);
    void intersect(Mask const& other);

    template<typename Func>
    void for_each_pixel(Func func) const
    {
        for (int x = m_bounding_rect.left(); x < m_bounding_rect.right(); x++) {
            for (int y = m_bounding_rect.top(); y < m_bounding_rect.bottom(); y++)
                func(x, y);
        }
    }

private:
    Gfx::IntRect m_bounding_rect {};
    Vector<u8> m_data {};

    Mask(Gfx::IntRect, u8 default_value);

    [[nodiscard]] size_t to_index(int x, int y) const
    {
        VERIFY(m_bounding_rect.contains(x, y));

        int dx = x - m_bounding_rect.x();
        int dy = y - m_bounding_rect.y();
        return dy * m_bounding_rect.width() + dx;
    }

    template<typename Func>
    void combine(Mask const& other, Func func)
    {
        auto new_bounding_rect = m_bounding_rect.united(other.m_bounding_rect);
        auto new_me = Mask::empty(new_bounding_rect);

        new_me.for_each_pixel([&](auto x, auto y) {
            // Widen to int then clamp before narrowing back to avoid annoying overflow checks in the combine functions
            auto my_alpha = static_cast<int>(get(x, y));
            auto other_alpha = static_cast<int>(other.get(x, y));
            auto new_alpha = static_cast<u8>(clamp(func(my_alpha, other_alpha), 0, 0xFF));

            new_me.set(x, y, new_alpha);
        });

        *this = move(new_me);
        shrink_to_fit();
    }

    template<typename Func>
    void combinef(Mask const& other, Func func)
    {
        auto new_bounding_rect = m_bounding_rect.united(other.m_bounding_rect);
        auto new_me = Mask::empty(new_bounding_rect);

        new_me.for_each_pixel([&](auto x, auto y) {
            auto my_alpha = getf(x, y);
            auto other_alpha = other.getf(x, y);
            auto new_alpha = func(my_alpha, other_alpha);

            new_me.setf(x, y, new_alpha);
        });

        *this = move(new_me);
        shrink_to_fit();
    }
};

}
