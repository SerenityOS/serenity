#pragma once

#include "Point.h"

class Rect {
public:
    Rect() { }
    Rect(int x, int y, int width, int height)
        : m_location(x, y)
        , m_width(width)
        , m_height(height)
    {
    }

    bool isEmpty() const
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
    int right() const { return x() + width(); }
    int top() const { return y(); }
    int bottom() const { return y() + height(); }

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
        return left() < other.right()
            && other.left() < right()
            && top() < other.bottom()
            && other.top() < bottom();
    }

    int x() const { return location().x(); }
    int y() const { return location().y(); }
    int width() const { return m_width; }
    int height() const { return m_height; }

    void setX(int x) { m_location.setX(x); }
    void setY(int y) { m_location.setY(y); }
    void setWidth(int width) { m_width = width; }
    void setHeight(int height) { m_height = height; }

    Point location() const { return m_location; }

    bool operator==(const Rect& other) const
    {
        return m_location == other.m_location
            && m_width == other.m_width
            && m_height == other.m_height;
    }

private:
    Point m_location;
    int m_width { 0 };
    int m_height { 0 };
};
