#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/Types.h>

typedef u32 RGBA32;

inline constexpr u32 make_rgb(u8 r, u8 g, u8 b)
{
    return ((r << 16) | (g << 8) | b);
}

enum class SystemColor {
    DesktopBackground,
    ActiveWindowBorder1,
    ActiveWindowBorder2,
    ActiveWindowTitle,
    InactiveWindowBorder1,
    InactiveWindowBorder2,
    InactiveWindowTitle,
    MovingWindowBorder1,
    MovingWindowBorder2,
    MovingWindowTitle,
    HighlightWindowBorder1,
    HighlightWindowBorder2,
    HighlightWindowTitle,
    MenuStripe,
    MenuBase,
    MenuSelection,
    Window,
    WindowText,
    Button,
    ButtonText,
    Base,
    ThreedHighlight,
    ThreedShadow1,
    ThreedShadow2,
    HoverHighlight,

    DisabledText = ThreedShadow1,
};

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
        WarmGray,
        DarkCyan,
        DarkGreen,
        DarkBlue,
        DarkRed,
        MidCyan,
        MidGreen,
        MidRed,
        MidBlue,
        MidMagenta,
    };

    Color() {}
    Color(NamedColor);
    Color(SystemColor);
    Color(u8 r, u8 g, u8 b)
        : m_value(0xff000000 | (r << 16) | (g << 8) | b)
    {
    }
    Color(u8 r, u8 g, u8 b, u8 a)
        : m_value((a << 24) | (r << 16) | (g << 8) | b)
    {
    }

    static Color from_rgb(unsigned rgb) { return Color(rgb | 0xff000000); }
    static Color from_rgba(unsigned rgba) { return Color(rgba); }

    u8 red() const { return (m_value >> 16) & 0xff; }
    u8 green() const { return (m_value >> 8) & 0xff; }
    u8 blue() const { return m_value & 0xff; }
    u8 alpha() const { return (m_value >> 24) & 0xff; }

    void set_alpha(u8 value)
    {
        m_value &= 0x00ffffff;
        m_value |= value << 24;
    }

    void set_red(u8 value)
    {
        m_value &= 0xff00ffff;
        m_value |= value << 16;
    }

    void set_green(u8 value)
    {
        m_value &= 0xffff00ff;
        m_value |= value << 8;
    }

    void set_blue(u8 value)
    {
        m_value &= 0xffffff00;
        m_value |= value;
    }

    Color with_alpha(u8 alpha)
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
        u8 r = (red() * alpha() * (255 - source.alpha()) + 255 * source.alpha() * source.red()) / d;
        u8 g = (green() * alpha() * (255 - source.alpha()) + 255 * source.alpha() * source.green()) / d;
        u8 b = (blue() * alpha() * (255 - source.alpha()) + 255 * source.alpha() * source.blue()) / d;
        u8 a = d / 255;
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
    static Optional<Color> from_string(const StringView&);

private:
    explicit Color(RGBA32 rgba)
        : m_value(rgba)
    {
    }

    RGBA32 m_value { 0 };
};

inline const LogStream& operator<<(const LogStream& stream, Color value)
{
    return stream << value.to_string();
}
