/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Forward.h>
#include <AK/StdLibExtras.h>

namespace Gfx {

enum class ColorRole;
typedef u32 RGBA32;

inline constexpr u32 make_rgb(u8 r, u8 g, u8 b)
{
    return ((r << 16) | (g << 8) | b);
}

struct HSV {
    double hue { 0 };
    double saturation { 0 };
    double value { 0 };
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

    Color darkened(float amount = 0.5f) const
    {
        return Color(red() * amount, green() * amount, blue() * amount, alpha());
    }

    Color lightened(float amount = 1.2f) const
    {
        return Color(min(255, (int)((float)red() * amount)), min(255, (int)((float)green() * amount)), min(255, (int)((float)blue() * amount)), alpha());
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

    HSV to_hsv() const
    {
        HSV hsv;
        double r = static_cast<double>(red()) / 255.0;
        double g = static_cast<double>(green()) / 255.0;
        double b = static_cast<double>(blue()) / 255.0;
        double max = AK::max(AK::max(r, g), b);
        double min = AK::min(AK::min(r, g), b);
        double chroma = max - min;

        if (!chroma)
            hsv.hue = 0.0;
        else if (max == r)
            hsv.hue = (60.0 * ((g - b) / chroma)) + 360.0;
        else if (max == g)
            hsv.hue = (60.0 * ((b - r) / chroma)) + 120.0;
        else
            hsv.hue = (60.0 * ((r - g) / chroma)) + 240.0;

        if (hsv.hue >= 360.0)
            hsv.hue -= 360.0;

        hsv.hue /= 360.0;

        if (!max)
            hsv.saturation = 0;
        else
            hsv.saturation = chroma / max;

        hsv.value = max;
        return hsv;
    }

    static Color from_hsv(double hue, double saturation, double value)
    {
        return from_hsv({ hue, saturation, value });
    }

    static Color from_hsv(const HSV& hsv)
    {
        double hue = hsv.hue * 2.0;
        double saturation = hsv.saturation / 255.0;
        double value = hsv.value / 255.0;

        int high = static_cast<int>(hue / 60.0) % 6;
        double f = (hue / 60.0) - high;
        double c1 = value * (1.0 - saturation);
        double c2 = value * (1.0 - saturation * f);
        double c3 = value * (1.0 - saturation * (1.0 - f));

        double r = 0;
        double g = 0;
        double b = 0;

        switch (high) {
        case 0:
            r = value;
            g = c3;
            b = c1;
            break;
        case 1:
            r = c2;
            g = value;
            b = c1;
            break;
        case 2:
            r = c1;
            g = value;
            b = c3;
            break;
        case 3:
            r = c1;
            g = c2;
            b = value;
            break;
        case 4:
            r = c3;
            g = c1;
            b = value;
            break;
        case 5:
            r = value;
            g = c1;
            b = c2;
            break;
        }

        u8 out_r = (u8)(r * 255);
        u8 out_g = (u8)(g * 255);
        u8 out_b = (u8)(b * 255);
        return Color(out_r, out_g, out_b);
    }

private:
    explicit Color(RGBA32 rgba)
        : m_value(rgba)
    {
    }

    RGBA32 m_value { 0 };
};

const LogStream& operator<<(const LogStream&, Color);

}

using Gfx::Color;

namespace IPC {
bool decode(BufferStream&, Gfx::Color&);
}
