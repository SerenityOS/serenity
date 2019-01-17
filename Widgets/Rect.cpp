#include "Rect.h"
#include <AK/StdLibExtras.h>

void Rect::intersect(const Rect& other)
{
    int l = max(left(), other.left());
    int r = min(right(), other.right());
    int t = max(top(), other.top());
    int b = min(bottom(), other.bottom());

    if (l >= r || t >= b) {
        m_location = { };
        m_size = { };
        return;
    }

    m_location.set_x(l);
    m_location.set_y(t);
    m_size.set_width((r - l) + 1);
    m_size.set_height((b - t) + 1);
}

Rect Rect::united(const Rect& other) const
{
    Rect rect;
    rect.set_left(min(left(), other.left()));
    rect.set_top(min(top(), other.top()));
    rect.set_right(max(right(), other.right()));
    rect.set_bottom(max(bottom(), other.bottom()));
    return rect;
}
