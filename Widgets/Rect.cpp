#include "Rect.h"
#include <AK/StdLibExtras.h>
#include "kstdio.h"

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

    m_location.setX(l);
    m_location.setY(t);
    m_size.setWidth((r - l) + 1);
    m_size.setHeight((b - t) + 1);

    dbgprintf("intersection result: %d,%d %dx%d\n", x(), y(), width(), height());
}
