#pragma once

#include <AK/Types.h>

typedef dword RGBA32;

inline constexpr dword make_rgb(byte r, byte g, byte b)
{
    return ((r << 16) | (g << 8) | b);
}

class Color {
public:
    enum NamedColor {
        Black,
        White,
        Red,
        Green,
        Blue,
        Yellow,
        Magenta,
        DarkGray,
        MidGray,
        LightGray,
    };

    Color() { }
    Color(NamedColor);
    Color(byte r, byte g, byte b) : m_value(0xff000000 | (r << 16) | (g << 8) | b) { }
    Color(byte r, byte g, byte b, byte a) : m_value((a << 24) | (r << 16) | (g << 8) | b) { }

    static Color from_rgb(unsigned rgb) { return Color(rgb | 0xff000000); }
    static Color from_rgba(unsigned rgba) { return Color(rgba); }

    byte red() const { return (m_value >> 16) & 0xff; }
    byte green() const { return (m_value >> 8) & 0xff; }
    byte blue() const { return m_value & 0xff; }
    byte alpha() const { return (m_value >> 24) & 0xff; }

    void set_alpha(byte value)
    {
        m_value &= 0x00ffffff;
        m_value |= value << 24;
    }

    Color with_alpha(byte alpha)
    {
        return Color((m_value & 0x00ffffff) | alpha << 24);
    }

    Color blend(Color source) const
    {
        if (!alpha() || source.alpha() == 255)
            return source;

        if (!source.alpha())
            return *this;

        int d = 255 * (alpha() + source.alpha()) - alpha() * source.alpha();
        byte r = (red() * alpha() * (255 - source.alpha()) + 255 * source.alpha() * source.red()) / d;
        byte g = (green() * alpha() * (255 - source.alpha()) + 255 * source.alpha() * source.green()) / d;
        byte b = (blue() * alpha() * (255 - source.alpha()) + 255 * source.alpha() * source.blue()) / d;
        byte a = d / 255;
        return Color(r, g, b, a);
    }

    RGBA32 value() const { return m_value; }

private:
    explicit Color(RGBA32 rgba) : m_value(rgba) { }

    RGBA32 m_value { 0 };
};
