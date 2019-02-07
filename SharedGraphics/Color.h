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
    Color(byte r, byte g, byte b) : m_value((r << 16) | (g << 8) | b) { }
    Color(RGBA32 rgba) : m_value(rgba) { }

    byte red() const { return (m_value >> 16) & 0xff; }
    byte green() const { return (m_value >> 8) & 0xff; }
    byte blue() const { return m_value & 0xff; }
    byte alpha() const { return (m_value >> 24) & 0xff; }

    Color blend(Color source) const
    {
        RGBA32 redblue1 = ((0x100u - source.alpha()) * (m_value & 0xff00ff)) >> 8;
        RGBA32 redblue2 = (source.alpha() * (source.m_value & 0xff00ff)) >> 8;
        RGBA32 green1  = ((0x100u - source.alpha()) * (m_value & 0x00ff00)) >> 8;
        RGBA32 green2  = (source.alpha() * (source.m_value & 0x00ff00)) >> 8;
        return Color(((redblue1 | redblue2) & 0xff00ff) + ((green1 | green2) & 0x00ff00));
    }


    RGBA32 value() const { return m_value; }

private:
    RGBA32 m_value { 0 };
};
