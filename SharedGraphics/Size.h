#pragma once

#include <AK/AKString.h>

struct WSAPI_Size;

class Size {
public:
    Size() {}
    Size(int w, int h)
        : m_width(w)
        , m_height(h)
    {
    }
    Size(const WSAPI_Size&);

    bool is_null() const { return !m_width && !m_height; }
    bool is_empty() const { return m_width <= 0 || m_height <= 0; }

    int width() const { return m_width; }
    int height() const { return m_height; }

    int area() const { return width() * height(); }

    void set_width(int w) { m_width = w; }
    void set_height(int h) { m_height = h; }

    bool operator==(const Size& other) const
    {
        return m_width == other.m_width && m_height == other.m_height;
    }

    bool operator!=(const Size& other) const
    {
        return !(*this == other);
    }

    Size& operator-=(const Size& other)
    {
        m_width -= other.m_width;
        m_height -= other.m_height;
        return *this;
    }

    Size& operator+=(const Size& other)
    {
        m_width += other.m_width;
        m_height += other.m_height;
        return *this;
    }

    operator WSAPI_Size() const;

    String to_string() const { return String::format("[%dx%d]", m_width, m_height); }

private:
    int m_width { 0 };
    int m_height { 0 };
};
