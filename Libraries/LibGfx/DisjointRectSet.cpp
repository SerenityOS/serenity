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

#include <LibGfx/DisjointRectSet.h>

namespace Gfx {

bool DisjointRectSet::add_no_shatter(const IntRect& new_rect)
{
    if (new_rect.is_empty())
        return false;
    for (auto& rect : m_rects) {
        if (rect.contains(new_rect))
            return false;
    }

    m_rects.append(new_rect);
    return true;
}

void DisjointRectSet::shatter()
{
    Vector<IntRect, 32> output;
    output.ensure_capacity(m_rects.size());
    bool pass_had_intersections = false;
    do {
        pass_had_intersections = false;
        output.clear_with_capacity();
        for (size_t i = 0; i < m_rects.size(); ++i) {
            auto& r1 = m_rects[i];
            for (size_t j = 0; j < m_rects.size(); ++j) {
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

void DisjointRectSet::move_by(int dx, int dy)
{
    for (auto& r : m_rects)
        r.move_by(dx, dy);
}

bool DisjointRectSet::contains(const IntRect& rect) const
{
    if (is_empty() || rect.is_empty())
        return false;

    // TODO: This could use some optimization
    DisjointRectSet remainder(rect);
    for (auto& r : m_rects) {
        auto shards = remainder.shatter(r);
        if (shards.is_empty())
            return true;
        remainder = move(shards);
    }
    return false;
}

bool DisjointRectSet::intersects(const IntRect& rect) const
{
    for (auto& r : m_rects) {
        if (r.intersects(rect))
            return true;
    }
    return false;
}

bool DisjointRectSet::intersects(const DisjointRectSet& rects) const
{
    if (this == &rects)
        return true;

    for (auto& r : m_rects) {
        for (auto& r2 : rects.m_rects) {
            if (r.intersects(r2))
                return true;
        }
    }
    return false;
}

DisjointRectSet DisjointRectSet::intersected(const IntRect& rect) const
{
    DisjointRectSet intersected_rects;
    intersected_rects.m_rects.ensure_capacity(m_rects.capacity());
    for (auto& r : m_rects) {
        auto intersected_rect = r.intersected(rect);
        if (!intersected_rect.is_empty())
            intersected_rects.m_rects.append(intersected_rect);
    }
    // Since there should be no overlaps, we don't need to call shatter()
    return intersected_rects;
}

DisjointRectSet DisjointRectSet::intersected(const DisjointRectSet& rects) const
{
    if (&rects == this)
        return clone();
    if (is_empty() || rects.is_empty())
        return {};

    DisjointRectSet intersected_rects;
    intersected_rects.m_rects.ensure_capacity(m_rects.capacity());
    for (auto& r : m_rects) {
        for (auto& r2 : rects.m_rects) {
            auto intersected_rect = r.intersected(r2);
            if (!intersected_rect.is_empty())
                intersected_rects.m_rects.append(intersected_rect);
        }
    }
    // Since there should be no overlaps, we don't need to call shatter()
    return intersected_rects;
}

DisjointRectSet DisjointRectSet::shatter(const IntRect& hammer) const
{
    if (hammer.is_empty())
        return clone();

    DisjointRectSet shards;
    for (auto& rect : m_rects) {
        for (auto& shard : rect.shatter(hammer))
            shards.add_no_shatter(shard);
    }
    // Since there should be no overlaps, we don't need to call shatter()
    return shards;
}

DisjointRectSet DisjointRectSet::shatter(const DisjointRectSet& hammer) const
{
    if (this == &hammer)
        return {};
    if (hammer.is_empty() || !intersects(hammer))
        return clone();

    // TODO: This could use some optimization
    DisjointRectSet shards = shatter(hammer.m_rects[0]);
    auto rects_count = hammer.m_rects.size();
    for (size_t i = 1; i < rects_count && !shards.is_empty(); i++) {
        if (hammer.m_rects[i].intersects(shards.m_rects)) {
            auto shattered = shards.shatter(hammer.m_rects[i]);
            shards = move(shattered);
        }
    }
    // Since there should be no overlaps, we don't need to call shatter()
    return shards;
}

}
