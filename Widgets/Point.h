#pragma once

class Rect;

class Point {
public:
    Point() { }
    Point(int x, int y) : m_x(x) , m_y(y) { }

    int x() const { return m_x; }
    int y() const { return m_y; }

    void setX(int x) { m_x = x; }
    void setY(int y) { m_y = y; }

    void moveBy(int dx, int dy)
    {
        m_x += dx;
        m_y += dy;
    }

    void moveBy(const Point& delta)
    {
        moveBy(delta.x(), delta.y());
    }

    void constrain(const Rect&);

    bool operator==(const Point& other) const
    {
        return m_x == other.m_x
            && m_y == other.m_y;
    }

    bool operator!=(const Point& other) const
    {
        return !(*this == other);
    }

private:
    int m_x { 0 };
    int m_y { 0 };
};
