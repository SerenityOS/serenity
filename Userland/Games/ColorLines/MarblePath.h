/*
 * Copyright (c) 2022, Oleg Kosenkov <oleg@kosenkov.ca>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGfx/Point.h>

class MarblePath final {
public:
    using Point = Gfx::IntPoint;

    MarblePath() = default;

    void add_point(Point point)
    {
        m_path.append(point);
    }

    [[nodiscard]] bool is_empty() const
    {
        return m_path.is_empty();
    }

    [[nodiscard]] bool contains(Point point) const
    {
        return m_path.contains_slow(point);
    }

    [[nodiscard]] size_t remaining_steps() const
    {
        return m_path.size();
    }

    [[nodiscard]] Point current_point() const
    {
        VERIFY(!m_path.is_empty());
        return m_path.last();
    }

    [[nodiscard]] Point next_point()
    {
        auto const point = current_point();
        m_path.resize(m_path.size() - 1);
        return point;
    }

    [[nodiscard]] Point operator[](size_t index) const
    {
        return m_path[index];
    }

    void reset()
    {
        m_path.clear();
    }

private:
    Vector<Point> m_path;
};
