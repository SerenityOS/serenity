#include <AK/Assertions.h>
#include <LibDraw/Color.h>
#include <ctype.h>
#include <stdio.h>

Color::Color(NamedColor named)
{
    struct {
        u8 r;
        u8 g;
        u8 b;
    } rgb;

    switch (named) {
    case Black:
        rgb = { 0, 0, 0 };
        break;
    case White:
        rgb = { 255, 255, 255 };
        break;
    case Red:
        rgb = { 255, 0, 0 };
        break;
    case Green:
        rgb = { 0, 255, 0 };
        break;
    case Cyan:
        rgb = { 0, 255, 255 };
        break;
    case DarkCyan:
        rgb = { 0, 127, 127 };
        break;
    case MidCyan:
        rgb = { 0, 192, 192 };
        break;
    case Blue:
        rgb = { 0, 0, 255 };
        break;
    case Yellow:
        rgb = { 255, 255, 0 };
        break;
    case Magenta:
        rgb = { 255, 0, 255 };
        break;
    case DarkGray:
        rgb = { 64, 64, 64 };
        break;
    case MidGray:
        rgb = { 127, 127, 127 };
        break;
    case LightGray:
        rgb = { 192, 192, 192 };
        break;
    case MidGreen:
        rgb = { 0, 192, 0 };
        break;
    case MidBlue:
        rgb = { 0, 0, 192 };
        break;
    case MidRed:
        rgb = { 192, 0, 0 };
        break;
    case MidMagenta:
        rgb = { 192, 0, 192 };
        break;
    case DarkGreen:
        rgb = { 0, 128, 0 };
        break;
    case DarkBlue:
        rgb = { 0, 0, 128 };
        break;
    case DarkRed:
        rgb = { 128, 0, 0 };
        break;
    case WarmGray:
        rgb = { 212, 208, 200 };
        break;
    default:
        ASSERT_NOT_REACHED();
        break;
    }

    m_value = 0xff000000 | (rgb.r << 16) | (rgb.g << 8) | rgb.b;
}

String Color::to_string() const
{
    return String::format("#%b%b%b%b", red(), green(), blue(), alpha());
}

Optional<Color> Color::from_string(const StringView& string)
{
    if (string.is_empty())
        return {};

    struct ColorAndWebName {
        RGBA32 color;
        const char* name;
    };

    const ColorAndWebName web_colors[] = {
        { 0x800000, "maroon", },
        { 0xff0000, "red", },
        { 0xffa500, "orange" },
        { 0xffff00, "yellow" },
        { 0x808000, "olive" },
        { 0x800080, "purple" },
        { 0xff00ff, "fuchsia" },
        { 0xffffff, "white" },
        { 0x00ff00, "lime" },
        { 0x008000, "green" },
        { 0x000080, "navy" },
        { 0x0000ff, "blue" },
        { 0x00ffff, "aqua" },
        { 0x008080, "teal" },
        { 0x000000, "black" },
        { 0xc0c0c0, "silver" },
        { 0x808080, "gray" },
        { 0x000000, nullptr }
    };

    for (size_t i = 0; web_colors[i].name; ++i) {
        if (string == web_colors[i].name)
            return Color::from_rgb(web_colors[i].color);
    }

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
