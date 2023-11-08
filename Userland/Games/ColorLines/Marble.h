/*
 * Copyright (c) 2022, Oleg Kosenkov <oleg@kosenkov.ca>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Point.h>

class Marble final {
public:
    using Point = Gfx::IntPoint;
    using Color = u8;

    static constexpr int number_of_colors { 6 };
    static constexpr Color empty_cell = NumericLimits<Color>::max();

    Marble() = default;
    Marble(Point position, Color color)
        : m_position { position }
        , m_color { color }
    {
    }

    bool operator==(Marble const& other) const = default;

    [[nodiscard]] constexpr Point position() const { return m_position; }

    [[nodiscard]] constexpr Color color() const { return m_color; }

private:
    Point m_position {};
    Color m_color {};
};

namespace AK {
template<>
struct Traits<Marble> : public DefaultTraits<Marble> {
    static unsigned hash(Marble const& marble)
    {
        return Traits<Marble::Point>::hash(marble.position());
    }
};
}
