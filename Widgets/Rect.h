#pragma once

class Rect {
public:
    Rect() { }
    Rect(int x, int y, int width, int height)
        : m_x(x)
        , m_y(y)
        , m_width(width)
        , m_height(height)
    {
    }

    void moveBy(int dx, int dy)
    {
        m_x += dx;
        m_y += dy;
    }

    bool contains(int x, int y) const
    {
        return x >= m_x && x <= right() && y >= m_y && y <= bottom();
    }

    int left() const { return m_x; }
    int right() const { return m_x + m_width; }
    int top() const { return m_y; }
    int bottom() const { return m_y + m_height; }

    int x() const { return m_x; }
    int y() const { return m_y; }
    int width() const { return m_width; }
    int height() const { return m_height; }

    void setX(int x) { m_x = x; }
    void setY(int y) { m_y = y; }
    void setWidth(int width) { m_width = width; }
    void setHeight(int height) { m_height = height; }

private:
    int m_x { 0 };
    int m_y { 0 };
    int m_width { 0 };
    int m_height { 0 };
};
