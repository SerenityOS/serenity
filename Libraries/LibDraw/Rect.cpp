#include "Rect.h"
#include <AK/StdLibExtras.h>

void Rect::intersect(const Rect& other)
{
    int l = max(left(), other.left());
    int r = min(right(), other.right());
    int t = max(top(), other.top());
    int b = min(bottom(), other.bottom());

    if (l > r || t > b) {
        m_location = {};
        m_size = {};
        return;
    }

    m_location.set_x(l);
    m_location.set_y(t);
    m_size.set_width((r - l) + 1);
    m_size.set_height((b - t) + 1);
}

Rect Rect::united(const Rect& other) const
{
    if (is_null())
        return other;
    if (other.is_null())
        return *this;
    Rect rect;
    rect.set_left(min(left(), other.left()));
    rect.set_top(min(top(), other.top()));
    rect.set_right(max(right(), other.right()));
    rect.set_bottom(max(bottom(), other.bottom()));
    return rect;
}

Vector<Rect, 4> Rect::shatter(const Rect& hammer) const
{
    Vector<Rect, 4> pieces;
    if (!intersects(hammer)) {
        pieces.unchecked_append(*this);
        return pieces;
    }
    Rect top_shard {
        x(),
        y(),
        width(),
        hammer.y() - y()
    };
    Rect bottom_shard {
        x(),
        hammer.y() + hammer.height(),
        width(),
        (y() + height()) - (hammer.y() + hammer.height())
    };
    Rect left_shard {
        x(),
        max(hammer.y(), y()),
        hammer.x() - x(),
        min((hammer.y() + hammer.height()), (y() + height())) - max(hammer.y(), y())
    };
    Rect right_shard {
        hammer.x() + hammer.width(),
        max(hammer.y(), y()),
        right() - hammer.right(),
        min((hammer.y() + hammer.height()), (y() + height())) - max(hammer.y(), y())
    };
    if (intersects(top_shard))
        pieces.unchecked_append(top_shard);
    if (intersects(bottom_shard))
        pieces.unchecked_append(bottom_shard);
    if (intersects(left_shard))
        pieces.unchecked_append(left_shard);
    if (intersects(right_shard))
        pieces.unchecked_append(right_shard);

    return pieces;
}

void Rect::align_within(const Rect& other, TextAlignment alignment)
{
    switch (alignment) {
    case TextAlignment::Center:
        center_within(other);
        return;
    case TextAlignment::TopLeft:
        set_location(other.location());
        return;
    case TextAlignment::CenterLeft:
        set_x(other.x());
        center_vertically_within(other);
        return;
    case TextAlignment::CenterRight:
        set_x(other.x() + other.width() - width());
        center_vertically_within(other);
        return;
    }
}
