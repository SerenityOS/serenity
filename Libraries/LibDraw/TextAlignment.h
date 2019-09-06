#pragma once

enum class TextAlignment {
    TopLeft,
    CenterLeft,
    Center,
    CenterRight,
    TopRight,
};

inline bool is_right_text_alignment(TextAlignment alignment)
{
    switch (alignment) {
    case TextAlignment::CenterRight:
    case TextAlignment::TopRight:
        return true;
    default:
        return false;
    }
}
