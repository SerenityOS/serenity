#pragma once

struct GUI_Size;

class Size {
public:
    Size() { }
    Size(int w, int h) : m_width(w), m_height(h) { }
    Size(const GUI_Size&);

    bool is_empty() const { return !m_width || !m_height; }

    int width() const { return m_width; }
    int height() const { return m_height; }

    void set_width(int w) { m_width = w; }
    void set_height(int h) { m_height = h; }

    bool operator==(const Size& other) const
    {
        return m_width == other.m_width &&
               m_height == other.m_height;
    }

    operator GUI_Size() const;

private:
    int m_width { 0 };
    int m_height { 0 };
};

