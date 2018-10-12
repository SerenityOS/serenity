#pragma once

#include <AK/Types.h>

class Color {
public:
    enum NamedColor {
        Black,
        White,
        Red,
        Green,
        Blue,
    };

    Color() { }
    Color(NamedColor);
    Color(byte r, byte g, byte b);

    dword value() const { return m_value; }

private:
    dword m_value { 0 };
};
