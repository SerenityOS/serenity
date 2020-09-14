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

enum class TextAlignment {
    TopLeft,
    CenterLeft,
    Center,
    CenterRight,
    TopRight,
    BottomRight,
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

inline Optional<TextAlignment> text_alignment_from_string(const StringView& string)
{
    if (string == "TopLeft")
        return TextAlignment::TopLeft;
    if (string == "CenterLeft")
        return TextAlignment::CenterLeft;
    if (string == "Center")
        return TextAlignment::Center;
    if (string == "CenterRight")
        return TextAlignment::CenterRight;
    if (string == "TopRight")
        return TextAlignment::TopRight;
    if (string == "BottomRight")
        return TextAlignment::BottomRight;
    return {};
}

}
