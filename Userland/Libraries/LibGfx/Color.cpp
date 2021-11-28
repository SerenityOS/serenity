/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGfx/Color.h>
#include <LibGfx/SystemTheme.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>
#include <ctype.h>
#include <stdlib.h>

namespace Gfx {

String Color::to_string() const
{
    return String::formatted("#{:02x}{:02x}{:02x}{:02x}", red(), green(), blue(), alpha());
}

String Color::to_string_without_alpha() const
{
    return String::formatted("#{:02x}{:02x}{:02x}", red(), green(), blue());
}

static Optional<Color> parse_rgb_color(StringView string)
{
    VERIFY(string.starts_with("rgb(", CaseSensitivity::CaseInsensitive));
    VERIFY(string.ends_with(")"));

    auto substring = string.substring_view(4, string.length() - 5);
    auto parts = substring.split_view(',');

    if (parts.size() != 3)
        return {};

    auto r = parts[0].to_uint().value_or(256);
    auto g = parts[1].to_uint().value_or(256);
    auto b = parts[2].to_uint().value_or(256);

    if (r > 255 || g > 255 || b > 255)
        return {};

    return Color(r, g, b);
}

static Optional<Color> parse_rgba_color(StringView string)
{
    VERIFY(string.starts_with("rgba(", CaseSensitivity::CaseInsensitive));
    VERIFY(string.ends_with(")"));

    auto substring = string.substring_view(5, string.length() - 6);
    auto parts = substring.split_view(',');

    if (parts.size() != 4)
        return {};

    auto r = parts[0].to_int().value_or(256);
    auto g = parts[1].to_int().value_or(256);
    auto b = parts[2].to_int().value_or(256);

    double alpha = strtod(parts[3].to_string().characters(), nullptr);
    unsigned a = alpha * 255;

    if (r > 255 || g > 255 || b > 255 || a > 255)
        return {};

    return Color(r, g, b, a);
}

Optional<Color> Color::from_string(StringView string)
{
    if (string.is_empty())
        return {};

    struct ColorAndWebName {
        constexpr ColorAndWebName(RGBA32 c, char const* n)
            : color(c)
            , name(n)
        {
        }
        RGBA32 color;
        StringView name;
    };

    constexpr ColorAndWebName web_colors[] = {
        // CSS Level 1
        { 0x000000, "black" },
        { 0xc0c0c0, "silver" },
        { 0x808080, "gray" },
        { 0xffffff, "white" },
        { 0x800000, "maroon" },
        { 0xff0000, "red" },
        { 0x800080, "purple" },
        { 0xff00ff, "fuchsia" },
        { 0x008000, "green" },
        { 0x00ff00, "lime" },
        { 0x808000, "olive" },
        { 0xffff00, "yellow" },
        { 0x000080, "navy" },
        { 0x0000ff, "blue" },
        { 0x008080, "teal" },
        { 0x00ffff, "aqua" },
        // CSS Level 2 (Revision 1)
        { 0xffa500, "orange" },
        // CSS Color Module Level 3
        { 0xf0f8ff, "aliceblue" },
        { 0xfaebd7, "antiquewhite" },
        { 0x7fffd4, "aquamarine" },
        { 0xf0ffff, "azure" },
        { 0xf5f5dc, "beige" },
        { 0xffe4c4, "bisque" },
        { 0xffebcd, "blanchedalmond" },
        { 0x8a2be2, "blueviolet" },
        { 0xa52a2a, "brown" },
        { 0xdeb887, "burlywood" },
        { 0x5f9ea0, "cadetblue" },
        { 0x7fff00, "chartreuse" },
        { 0xd2691e, "chocolate" },
        { 0xff7f50, "coral" },
        { 0x6495ed, "cornflowerblue" },
        { 0xfff8dc, "cornsilk" },
        { 0xdc143c, "crimson" },
        { 0x00ffff, "cyan" },
        { 0x00008b, "darkblue" },
        { 0x008b8b, "darkcyan" },
        { 0xb8860b, "darkgoldenrod" },
        { 0xa9a9a9, "darkgray" },
        { 0x006400, "darkgreen" },
        { 0xa9a9a9, "darkgrey" },
        { 0xbdb76b, "darkkhaki" },
        { 0x8b008b, "darkmagenta" },
        { 0x556b2f, "darkolivegreen" },
        { 0xff8c00, "darkorange" },
        { 0x9932cc, "darkorchid" },
        { 0x8b0000, "darkred" },
        { 0xe9967a, "darksalmon" },
        { 0x8fbc8f, "darkseagreen" },
        { 0x483d8b, "darkslateblue" },
        { 0x2f4f4f, "darkslategray" },
        { 0x2f4f4f, "darkslategrey" },
        { 0x00ced1, "darkturquoise" },
        { 0x9400d3, "darkviolet" },
        { 0xff1493, "deeppink" },
        { 0x00bfff, "deepskyblue" },
        { 0x696969, "dimgray" },
        { 0x696969, "dimgrey" },
        { 0x1e90ff, "dodgerblue" },
        { 0xb22222, "firebrick" },
        { 0xfffaf0, "floralwhite" },
        { 0x228b22, "forestgreen" },
        { 0xdcdcdc, "gainsboro" },
        { 0xf8f8ff, "ghostwhite" },
        { 0xffd700, "gold" },
        { 0xdaa520, "goldenrod" },
        { 0xadff2f, "greenyellow" },
        { 0x808080, "grey" },
        { 0xf0fff0, "honeydew" },
        { 0xff69b4, "hotpink" },
        { 0xcd5c5c, "indianred" },
        { 0x4b0082, "indigo" },
        { 0xfffff0, "ivory" },
        { 0xf0e68c, "khaki" },
        { 0xe6e6fa, "lavender" },
        { 0xfff0f5, "lavenderblush" },
        { 0x7cfc00, "lawngreen" },
        { 0xfffacd, "lemonchiffon" },
        { 0xadd8e6, "lightblue" },
        { 0xf08080, "lightcoral" },
        { 0xe0ffff, "lightcyan" },
        { 0xfafad2, "lightgoldenrodyellow" },
        { 0xd3d3d3, "lightgray" },
        { 0x90ee90, "lightgreen" },
        { 0xd3d3d3, "lightgrey" },
        { 0xffb6c1, "lightpink" },
        { 0xffa07a, "lightsalmon" },
        { 0x20b2aa, "lightseagreen" },
        { 0x87cefa, "lightskyblue" },
        { 0x778899, "lightslategray" },
        { 0x778899, "lightslategrey" },
        { 0xb0c4de, "lightsteelblue" },
        { 0xffffe0, "lightyellow" },
        { 0x32cd32, "limegreen" },
        { 0xfaf0e6, "linen" },
        { 0xff00ff, "magenta" },
        { 0x66cdaa, "mediumaquamarine" },
        { 0x0000cd, "mediumblue" },
        { 0xba55d3, "mediumorchid" },
        { 0x9370db, "mediumpurple" },
        { 0x3cb371, "mediumseagreen" },
        { 0x7b68ee, "mediumslateblue" },
        { 0x00fa9a, "mediumspringgreen" },
        { 0x48d1cc, "mediumturquoise" },
        { 0xc71585, "mediumvioletred" },
        { 0x191970, "midnightblue" },
        { 0xf5fffa, "mintcream" },
        { 0xffe4e1, "mistyrose" },
        { 0xffe4b5, "moccasin" },
        { 0xffdead, "navajowhite" },
        { 0xfdf5e6, "oldlace" },
        { 0x6b8e23, "olivedrab" },
        { 0xff4500, "orangered" },
        { 0xda70d6, "orchid" },
        { 0xeee8aa, "palegoldenrod" },
        { 0x98fb98, "palegreen" },
        { 0xafeeee, "paleturquoise" },
        { 0xdb7093, "palevioletred" },
        { 0xffefd5, "papayawhip" },
        { 0xffdab9, "peachpuff" },
        { 0xcd853f, "peru" },
        { 0xffc0cb, "pink" },
        { 0xdda0dd, "plum" },
        { 0xb0e0e6, "powderblue" },
        { 0xbc8f8f, "rosybrown" },
        { 0x4169e1, "royalblue" },
        { 0x8b4513, "saddlebrown" },
        { 0xfa8072, "salmon" },
        { 0xf4a460, "sandybrown" },
        { 0x2e8b57, "seagreen" },
        { 0xfff5ee, "seashell" },
        { 0xa0522d, "sienna" },
        { 0x87ceeb, "skyblue" },
        { 0x6a5acd, "slateblue" },
        { 0x708090, "slategray" },
        { 0x708090, "slategrey" },
        { 0xfffafa, "snow" },
        { 0x00ff7f, "springgreen" },
        { 0x4682b4, "steelblue" },
        { 0xd2b48c, "tan" },
        { 0xd8bfd8, "thistle" },
        { 0xff6347, "tomato" },
        { 0x40e0d0, "turquoise" },
        { 0xee82ee, "violet" },
        { 0xf5deb3, "wheat" },
        { 0xf5f5f5, "whitesmoke" },
        { 0x9acd32, "yellowgreen" },
        // CSS Color Module Level 4
        { 0x663399, "rebeccapurple" },
        // (Fallback)
        { 0x000000, nullptr }
    };

    if (string.equals_ignoring_case("transparent"))
        return Color::from_rgba(0x00000000);

    for (size_t i = 0; !web_colors[i].name.is_null(); ++i) {
        if (string.equals_ignoring_case(web_colors[i].name))
            return Color::from_rgb(web_colors[i].color);
    }

    if (string.starts_with("rgb(", CaseSensitivity::CaseInsensitive) && string.ends_with(")"))
        return parse_rgb_color(string);

    if (string.starts_with("rgba(", CaseSensitivity::CaseInsensitive) && string.ends_with(")"))
        return parse_rgba_color(string);

    if (string[0] != '#')
        return {};

    auto hex_nibble_to_u8 = [](char nibble) -> Optional<u8> {
        if (!isxdigit(nibble))
            return {};
        if (nibble >= '0' && nibble <= '9')
            return nibble - '0';
        return 10 + (tolower(nibble) - 'a');
    };

    if (string.length() == 4) {
        Optional<u8> r = hex_nibble_to_u8(string[1]);
        Optional<u8> g = hex_nibble_to_u8(string[2]);
        Optional<u8> b = hex_nibble_to_u8(string[3]);
        if (!r.has_value() || !g.has_value() || !b.has_value())
            return {};
        return Color(r.value() * 17, g.value() * 17, b.value() * 17);
    }

    if (string.length() == 5) {
        Optional<u8> r = hex_nibble_to_u8(string[1]);
        Optional<u8> g = hex_nibble_to_u8(string[2]);
        Optional<u8> b = hex_nibble_to_u8(string[3]);
        Optional<u8> a = hex_nibble_to_u8(string[4]);
        if (!r.has_value() || !g.has_value() || !b.has_value() || !a.has_value())
            return {};
        return Color(r.value() * 17, g.value() * 17, b.value() * 17, a.value() * 17);
    }

    if (string.length() != 7 && string.length() != 9)
        return {};

    auto to_hex = [&](char c1, char c2) -> Optional<u8> {
        auto nib1 = hex_nibble_to_u8(c1);
        auto nib2 = hex_nibble_to_u8(c2);
        if (!nib1.has_value() || !nib2.has_value())
            return {};
        return nib1.value() << 4 | nib2.value();
    };

    Optional<u8> r = to_hex(string[1], string[2]);
    Optional<u8> g = to_hex(string[3], string[4]);
    Optional<u8> b = to_hex(string[5], string[6]);
    Optional<u8> a = string.length() == 9 ? to_hex(string[7], string[8]) : Optional<u8>(255);

    if (!r.has_value() || !g.has_value() || !b.has_value() || !a.has_value())
        return {};

    return Color(r.value(), g.value(), b.value(), a.value());
}

Vector<Color> Color::shades(u32 steps, float max) const
{
    float shade = 1.f;
    float step = max / steps;
    Vector<Color> shades;
    for (u32 i = 0; i < steps; i++) {
        shade -= step;
        shades.append(this->darkened(shade));
    }
    return shades;
}

Vector<Color> Color::tints(u32 steps, float max) const
{
    float shade = 1.f;
    float step = max / steps;
    Vector<Color> tints;
    for (u32 i = 0; i < steps; i++) {
        shade += step;
        tints.append(this->lightened(shade));
    }
    return tints;
}

}

bool IPC::encode(IPC::Encoder& encoder, Color const& color)
{
    encoder << color.value();
    return true;
}

ErrorOr<void> IPC::decode(IPC::Decoder& decoder, Color& color)
{
    u32 rgba;
    TRY(decoder.decode(rgba));
    color = Color::from_rgba(rgba);
    return {};
}

ErrorOr<void> AK::Formatter<Gfx::Color>::format(FormatBuilder& builder, Gfx::Color const& value)
{
    return Formatter<StringView>::format(builder, value.to_string());
}
