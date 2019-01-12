#pragma once

#include "Point.h"
#include "Size.h"

class Rect {
public:
    Rect() { }
    Rect(int x, int y, int width, int height)
        : m_location(x, y)
        , m_size(width, height)
    {
    }
    Rect(const Point& location, const Size& size)
        : m_location(location)
        , m_size(size)
    {
    }

    bool is_empty() const
    {
        return width() == 0 || height() == 0;
    }

    void moveBy(int dx, int dy)
    {
        m_location.moveBy(dx, dy);
    }

    void moveBy(const Point& delta)
    {
        m_location.moveBy(delta);
    }

    Point center() const
    {
        return { x() + width() / 2, y() + height() / 2 };
    }

    void inflate(int w, int h)
    {
        setX(x() - w / 2);
        setWidth(width() + w);
        setY(y() - h / 2);
        setHeight(height() + h);
    }

    void shrink(int w, int h)
    {
        setX(x() + w / 2);
        setWidth(width() - w);
        setY(y() + h / 2);
        setHeight(height() - h);
    }

    bool contains(int x, int y) const
    {
        return x >= m_location.x() && x < right() && y >= m_location.y() && y < bottom();
    }

    bool contains(const Point& point) const
    {
        return contains(point.x(), point.y());
    }

    int left() const { return x(); }
    int right() const { return x() + width() - 1; }
    int top() const { return y(); }
    int bottom() const { return y() + height() - 1; }

    void setLeft(int left)
    {
        setWidth(x() - left);
        setX(left);
    }

    void setTop(int top)
    {
        setHeight(y() - top);
        setY(top);
    }

    bool intersects(const Rect& other) const
    {
        return left() <= other.right()
            && other.left() <= right()
            && top() <= other.bottom()
            && other.top() <= bottom();
    }

    int x() const { return location().x(); }
    int y() const { return location().y(); }
    int width() const { return m_size.width(); }
    int height() const { return m_size.height(); }

    void setX(int x) { m_location.setX(x); }
    void setY(int y) { m_location.setY(y); }
    void setWidth(int width) { m_size.setWidth(width); }
    void setHeight(int height) { m_size.setHeight(height); }

    Point location() const { return m_location; }
    Size size() const { return m_size; }

    bool operator==(const Rect& other) const
    {
        return m_location == other.m_location
            && m_size == other.m_size;
    }

    void intersect(const Rect&);

    static Rect intersection(const Rect& a, const Rect& b)
    {
        Rect r(a);
        r.intersect(b);
        return a;
    }

private:
    Point m_location;
    Size m_size;
};

inline void Point::constrain(const Rect& rect)
{
    if (x() < rect.left())
        setX(rect.left());
    else if (x() > rect.right())
        setX(rect.right());
    if (y() < rect.top())
        setY(rect.top());
    else if (y() > rect.bottom())
        setY(rect.bottom());
}
