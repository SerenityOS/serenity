#pragma once

#include <AK/Types.h>

class Color {
public:
    Color() { }
    Color(byte r, byte g, byte b);

    dword value() const { return m_value; }

private:
    dword m_value { 0 };
};
