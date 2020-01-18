/*
 * Copyright (c) 2020, Shannon Booth <shannon.ml.booth@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibDraw/Point.h>

class Triangle {
public:
    Triangle(Point a, Point b, Point c)
        : m_a(a)
        , m_b(b)
        , m_c(c)
    {
        m_det = (m_b.x() - m_a.x()) * (m_c.y() - m_a.y()) - (m_b.y() - m_a.y()) * (m_c.x() - m_a.x());
    }

    Point a() const { return m_a; }
    Point b() const { return m_b; }
    Point c() const { return m_c; }

    bool contains(Point p) const
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

    String to_string() const { return String::format("(%s,%s,%s)", m_a.to_string().characters(), m_b.to_string().characters(), m_c.to_string().characters()); }

private:
    int m_det;
    Point m_a;
    Point m_b;
    Point m_c;
};

inline const LogStream& operator<<(const LogStream& stream, const Triangle& value)
{
    return stream << value.to_string();
}
