/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/StringView.h>
#include <LibGfx/Alignment.h>

namespace Gfx {

#define GFX_ENUMERATE_ALIGNMENTS(M) \
    M(Center)                       \
    M(CenterTop)                    \
    M(CenterBottom)                 \
    M(CenterLeft)                   \
    M(CenterRight)                  \
    M(Left)                         \
    M(Right)                        \
    M(Bottom)                       \
    M(BottomRight)                  \
    M(BottomLeft)                   \
    M(Top)                          \
    M(TopLeft)                      \
    M(TopRight)

enum class Alignment {
#define __ENUMERATE(x) x,
    GFX_ENUMERATE_ALIGNMENTS(__ENUMERATE)
#undef __ENUMERATE
};

inline bool is_right_alignment(Alignment alignment)
{
    switch (alignment) {
    case Alignment::CenterRight:
    case Alignment::TopRight:
    case Alignment::BottomRight:
    case Alignment::Right:
        return true;
    default:
        return false;
    }
}

inline bool is_left_alignment(Alignment alignment)
{
    switch (alignment) {
    case Alignment::CenterLeft:
    case Alignment::TopLeft:
    case Alignment::BottomLeft:
    case Alignment::Left:
        return true;
    default:
        return false;
    }
}

inline bool is_vertically_centered_alignment(Alignment alignment)
{
    switch (alignment) {
    case Alignment::CenterLeft:
    case Alignment::CenterRight:
    case Alignment::Center:
        return true;
    default:
        return false;
    }
}

inline Optional<Alignment> alignment_from_string(StringView string)
{
#define __ENUMERATE(x) \
    if (string == #x)  \
        return Alignment::x;
    GFX_ENUMERATE_ALIGNMENTS(__ENUMERATE)
#undef __ENUMERATE
    return {};
}

inline const char* to_string(Alignment alignment)
{
#define __ENUMERATE(x)             \
    if (alignment == Alignment::x) \
        return #x;
    GFX_ENUMERATE_ALIGNMENTS(__ENUMERATE)
#undef __ENUMERATE
    return {};
}

}
