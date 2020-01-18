/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <LibDraw/DisjointRectSet.h>

void DisjointRectSet::add(const Rect& new_rect)
{
    for (auto& rect : m_rects) {
        if (rect.contains(new_rect))
            return;
    }

    m_rects.append(new_rect);
    if (m_rects.size() > 1)
        shatter();
}

void DisjointRectSet::shatter()
{
    Vector<Rect, 32> output;
    output.ensure_capacity(m_rects.size());
    bool pass_had_intersections = false;
    do {
        pass_had_intersections = false;
        output.clear_with_capacity();
        for (int i = 0; i < m_rects.size(); ++i) {
            auto& r1 = m_rects[i];
            for (int j = 0; j < m_rects.size(); ++j) {
                if (i == j)
                    continue;
                auto& r2 = m_rects[j];
                if (!r1.intersects(r2))
                    continue;
                pass_had_intersections = true;
                auto pieces = r1.shatter(r2);
                for (auto& piece : pieces)
                    output.append(piece);
                m_rects.remove(i);
                for (; i < m_rects.size(); ++i)
                    output.append(m_rects[i]);
                goto next_pass;
            }
            output.append(r1);
        }
    next_pass:
        swap(output, m_rects);
    } while (pass_had_intersections);
}
