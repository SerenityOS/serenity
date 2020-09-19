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

#include <AK/Optional.h>
#include <AK/StringView.h>

namespace Gfx {

#define GFX_ENUMERATE_TEXT_ALIGNMENTS(M) \
    M(TopLeft)                           \
    M(CenterLeft)                        \
    M(Center)                            \
    M(CenterRight)                       \
    M(TopRight)                          \
    M(BottomRight)

enum class TextAlignment {
#define __ENUMERATE(x) x,
    GFX_ENUMERATE_TEXT_ALIGNMENTS(__ENUMERATE)
#undef __ENUMERATE
};

inline bool is_right_text_alignment(TextAlignment alignment)
{
    switch (alignment) {
    case TextAlignment::CenterRight:
    case TextAlignment::TopRight:
    case TextAlignment::BottomRight:
        return true;
    default:
        return false;
    }
}

inline bool is_vertically_centered_text_alignment(TextAlignment alignment)
{
    switch (alignment) {
    case TextAlignment::CenterLeft:
    case TextAlignment::CenterRight:
    case TextAlignment::Center:
        return true;
    default:
        return false;
    }
}

inline Optional<TextAlignment> text_alignment_from_string(const StringView& string)
{
#define __ENUMERATE(x) \
    if (string == #x)  \
        return TextAlignment::x;
    GFX_ENUMERATE_TEXT_ALIGNMENTS(__ENUMERATE)
#undef __ENUMERATE
    return {};
}

inline const char* to_string(TextAlignment text_alignment)
{
#define __ENUMERATE(x)                      \
    if (text_alignment == TextAlignment::x) \
        return #x;
    GFX_ENUMERATE_TEXT_ALIGNMENTS(__ENUMERATE)
#undef __ENUMERATE
    return {};
}

}
