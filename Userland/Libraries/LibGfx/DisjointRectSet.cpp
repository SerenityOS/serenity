/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/DisjointRectSet.h>

namespace Gfx {

bool DisjointRectSet::add_no_shatter(IntRect const& new_rect)
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
        r.translate_by(dx, dy);
}

bool DisjointRectSet::contains(IntRect const& rect) const
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

bool DisjointRectSet::intersects(IntRect const& rect) const
{
    for (auto& r : m_rects) {
        if (r.intersects(rect))
            return true;
    }
    return false;
}

bool DisjointRectSet::intersects(DisjointRectSet const& rects) const
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

DisjointRectSet DisjointRectSet::intersected(IntRect const& rect) const
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

DisjointRectSet DisjointRectSet::intersected(DisjointRectSet const& rects) const
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

DisjointRectSet DisjointRectSet::shatter(IntRect const& hammer) const
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

DisjointRectSet DisjointRectSet::shatter(DisjointRectSet const& hammer) const
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
