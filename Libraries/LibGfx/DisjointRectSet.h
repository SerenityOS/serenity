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

#pragma once

#include <AK/Vector.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>

namespace Gfx {

class DisjointRectSet {
public:
    DisjointRectSet(const DisjointRectSet&) = delete;
    DisjointRectSet& operator=(const DisjointRectSet&) = delete;

    DisjointRectSet() { }
    ~DisjointRectSet() { }

    DisjointRectSet(const IntRect& rect)
    {
        m_rects.append(rect);
    }

    DisjointRectSet(DisjointRectSet&&) = default;
    DisjointRectSet& operator=(DisjointRectSet&&) = default;

    DisjointRectSet clone() const
    {
        DisjointRectSet rects;
        rects.m_rects = m_rects;
        return rects;
    }

    void move_by(int dx, int dy);
    void move_by(const IntPoint& delta)
    {
        move_by(delta.x(), delta.y());
    }

    void add(const IntRect& rect)
    {
        if (add_no_shatter(rect) && m_rects.size() > 1)
            shatter();
    }

    template<typename Container>
    void add_many(const Container& rects)
    {
        bool added = false;
        for (const auto& rect : rects) {
            if (add_no_shatter(rect))
                added = true;
        }
        if (added && m_rects.size() > 1)
            shatter();
    }

    void add(const DisjointRectSet& rect_set)
    {
        if (this == &rect_set)
            return;
        if (m_rects.is_empty()) {
            m_rects = rect_set.m_rects;
        } else {
            add_many(rect_set.rects());
        }
    }

    DisjointRectSet shatter(const IntRect&) const;
    DisjointRectSet shatter(const DisjointRectSet& hammer) const;

    bool contains(const IntRect&) const;
    bool intersects(const IntRect&) const;
    bool intersects(const DisjointRectSet&) const;
    DisjointRectSet intersected(const IntRect&) const;
    DisjointRectSet intersected(const DisjointRectSet&) const;

    template<typename Function>
    IterationDecision for_each_intersected(const IntRect& rect, Function f) const
    {
        if (is_empty() || rect.is_empty())
            return IterationDecision::Continue;
        for (auto& r : m_rects) {
            auto intersected_rect = r.intersected(rect);
            if (intersected_rect.is_empty())
                continue;
            IterationDecision decision = f(intersected_rect);
            if (decision != IterationDecision::Continue)
                return decision;
        }
        return IterationDecision::Continue;
    }

    template<typename Function>
    IterationDecision for_each_intersected(const DisjointRectSet& rects, Function f) const
    {
        if (is_empty() || rects.is_empty())
            return IterationDecision::Continue;
        if (this == &rects) {
            for (auto& r : m_rects) {
                IterationDecision decision = f(r);
                if (decision != IterationDecision::Continue)
                    return decision;
            }
        } else {
            for (auto& r : m_rects) {
                for (auto& r2 : rects.m_rects) {
                    auto intersected_rect = r.intersected(r2);
                    if (intersected_rect.is_empty())
                        continue;
                    IterationDecision decision = f(intersected_rect);
                    if (decision != IterationDecision::Continue)
                        return decision;
                }
            }
        }
        return IterationDecision::Continue;
    }

    bool is_empty() const { return m_rects.is_empty(); }
    size_t size() const { return m_rects.size(); }

    void clear() { m_rects.clear(); }
    void clear_with_capacity() { m_rects.clear_with_capacity(); }
    const Vector<IntRect, 32>& rects() const { return m_rects; }
    Vector<IntRect, 32> take_rects() { return move(m_rects); }

private:
    bool add_no_shatter(const IntRect&);
    void shatter();

    Vector<IntRect, 32> m_rects;
};

}
