#pragma once

#include <AK/Vector.h>
#include <LibDraw/Rect.h>

class DisjointRectSet {
public:
    DisjointRectSet() {}
    ~DisjointRectSet() {}
    DisjointRectSet(DisjointRectSet&& other)
        : m_rects(move(other.m_rects))
    {
    }

    void add(const Rect&);

    bool is_empty() const { return m_rects.is_empty(); }
    int size() const { return m_rects.size(); }

    void clear() { m_rects.clear(); }
    void clear_with_capacity() { m_rects.clear_with_capacity(); }
    const Vector<Rect, 32>& rects() const { return m_rects; }

private:
    void shatter();

    Vector<Rect, 32> m_rects;
};
