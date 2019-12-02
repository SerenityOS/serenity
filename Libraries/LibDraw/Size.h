#pragma once

#include <AK/String.h>
#include <AK/LogStream.h>
#include <LibDraw/Orientation.h>

class Size {
public:
    Size() {}
    Size(int w, int h)
        : m_width(w)
        , m_height(h)
    {
    }

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

    int primary_size_for_orientation(Orientation orientation) const
    {
        return orientation == Orientation::Vertical ? height() : width();
    }

    void set_primary_size_for_orientation(Orientation orientation, int value)
    {
        if (orientation == Orientation::Vertical)
            set_height(value);
        else
            set_width(value);
    }

    int secondary_size_for_orientation(Orientation orientation) const
    {
        return orientation == Orientation::Vertical ? width() : height();
    }

    void set_secondary_size_for_orientation(Orientation orientation, int value)
    {
        if (orientation == Orientation::Vertical)
            set_width(value);
        else
            set_height(value);
    }


    String to_string() const { return String::format("[%dx%d]", m_width, m_height); }

private:
    int m_width { 0 };
    int m_height { 0 };
};

inline const LogStream& operator<<(const LogStream& stream, const Size& value)
{
    return stream << value.to_string();
}
