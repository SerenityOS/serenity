/*
 * Copyright (c) 2021-2021, Vinicius Sugimoto <vtmsugimoto@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/StringView.h>

namespace GUI {

#define GUI_ENUMERATE_WORD_WRAP_MODES(M) \
    M(Word)                              \
    M(Anywhere)

enum class WordWrapMode {
#define __ENUMERATE(x) x,
    GUI_ENUMERATE_WORD_WRAP_MODES(__ENUMERATE)
#undef __ENUMERATE
};

ALWAYS_INLINE Optional<WordWrapMode> word_wrap_mode_from_string(StringView const& string)
{
#define __ENUMERATE(x) \
    if (string == #x)  \
        return WordWrapMode::x;
    GUI_ENUMERATE_WORD_WRAP_MODES(__ENUMERATE)
#undef __ENUMERATE
    return {};
}

ALWAYS_INLINE const char* to_string(WordWrapMode mode)
{
#define __ENUMERATE(x)           \
    if (mode == WordWrapMode::x) \
        return #x;
    GUI_ENUMERATE_WORD_WRAP_MODES(__ENUMERATE)
#undef __ENUMERATE
    return {};
}

}
