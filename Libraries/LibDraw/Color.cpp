#include "Color.h"
#include <AK/Assertions.h>

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
