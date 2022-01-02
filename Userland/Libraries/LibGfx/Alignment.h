/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/StringView.h>
#include <LibGfx/TextAlignment.h>

namespace Gfx {

#define GFX_ENUMERATE_ALIGNMENTS(M) \
    M(Center)                       \
    M(TopCenter)                    \
    M(BottomCenter)                 \
    M(Left)                         \
    M(Right)                        \
    M(Bottom)                       \
    M(Top)

enum class Alignment {
#define __ENUMERATE(x) x,
    GFX_ENUMERATE_ALIGNMENTS(__ENUMERATE)
#undef __ENUMERATE
};

inline Gfx::TextAlignment text_alignment_from_alignment(Alignment alignment)
{
    switch (alignment) {
    case Alignment::Center:
        return TextAlignment::Center;
    case Alignment::Left:
        return TextAlignment::CenterLeft;
    case Alignment::Right:
        return TextAlignment::CenterRight;
    case Alignment::Bottom:
    case Alignment::Top:
        VERIFY_NOT_REACHED();
    default:
        return {};
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
