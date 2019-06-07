#pragma once

enum class TextAlignment {
    TopLeft,
    CenterLeft,
    Center,
    CenterRight
};

inline bool is_right_text_alignment(TextAlignment alignment)
{
    switch (alignment) {
    case TextAlignment::CenterRight:
        return true;
    default:
        return false;
    }
}
