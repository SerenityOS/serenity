#include <SharedGraphics/DisjointRectSet.h>

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
    } while(pass_had_intersections);
}
