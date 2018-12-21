#pragma once

class Size {
public:
    Size() { }
    Size(int w, int h) : m_width(w), m_height(h) { }

    bool is_empty() const { return !m_width || !m_height; }

    int width() const { return m_width; }
    int height() const { return m_height; }

    void setWidth(int w) { m_width = w; }
    void setHeight(int h) { m_height = h; }

private:
    int m_width { 0 };
    int m_height { 0 };
};

