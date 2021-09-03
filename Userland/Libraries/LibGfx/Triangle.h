/*
 * Copyright (c) 2020, Shannon Booth <shannon.ml.booth@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Forward.h>
#include <LibGfx/Point.h>

namespace Gfx {

class Triangle {
public:
    Triangle(IntPoint a, IntPoint b, IntPoint c)
        : m_a(a)
        , m_b(b)
        , m_c(c)
    {
        m_det = (m_b.x() - m_a.x()) * (m_c.y() - m_a.y()) - (m_b.y() - m_a.y()) * (m_c.x() - m_a.x());
    }

    IntPoint a() const { return m_a; }
    IntPoint b() const { return m_b; }
    IntPoint c() const { return m_c; }

    bool contains(IntPoint p) const
    {
        int x = p.x();
        int y = p.y();

        int ax = m_a.x();
        int bx = m_b.x();
        int cx = m_c.x();

        int ay = m_a.y();
        int by = m_b.y();
        int cy = m_c.y();

        if (m_det * ((bx - ax) * (y - ay) - (by - ay) * (x - ax)) <= 0)
            return false;
        if (m_det * ((cx - bx) * (y - by) - (cy - by) * (x - bx)) <= 0)
            return false;
        if (m_det * ((ax - cx) * (y - cy) - (ay - cy) * (x - cx)) <= 0)
            return false;
        return true;
    }

    String to_string() const;

private:
    int m_det;
    IntPoint m_a;
    IntPoint m_b;
    IntPoint m_c;
};

}
