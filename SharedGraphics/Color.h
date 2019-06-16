#pragma once

#include <AK/AKString.h>
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
        Cyan,
        Blue,
        Yellow,
        Magenta,
        DarkGray,
        MidGray,
        LightGray,
        DarkGreen,
        DarkBlue,
        DarkRed,
        MidGreen,
        MidRed,
        MidBlue,
        MidMagenta,
    };

    Color() {}
    Color(NamedColor);
    Color(byte r, byte g, byte b)
        : m_value(0xff000000 | (r << 16) | (g << 8) | b)
    {
    }
    Color(byte r, byte g, byte b, byte a)
        : m_value((a << 24) | (r << 16) | (g << 8) | b)
    {
    }

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

    void set_red(byte value)
    {
        m_value &= 0xff00ffff;
        m_value |= value << 16;
    }

    void set_green(byte value)
    {
        m_value &= 0xffff00ff;
        m_value |= value << 8;
    }

    void set_blue(byte value)
    {
        m_value &= 0xffffff00;
        m_value |= value;
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

    Color to_grayscale() const
    {
        int gray = (red() + green() + blue()) / 3;
        return Color(gray, gray, gray, alpha());
    }

    Color darkened(float amount = 0.5) const
    {
        return Color(red() * amount, green() * amount, blue() * amount, alpha());
    }

    Color lightened() const
    {
        return Color(min(255.0, red() * 1.2), min(255.0, green() * 1.2), min(255.0, blue() * 1.2), alpha());
    }

    Color inverted() const
    {
        return Color(~red(), ~green(), ~blue());
    }

    RGBA32 value() const { return m_value; }

    bool operator==(const Color& other) const
    {
        return m_value == other.m_value;
    }

    bool operator!=(const Color& other) const
    {
        return m_value != other.m_value;
    }

    String to_string() const;

private:
    explicit Color(RGBA32 rgba)
        : m_value(rgba)
    {
    }

    RGBA32 m_value { 0 };
};
