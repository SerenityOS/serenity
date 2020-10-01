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

#include <AK/Forward.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <LibGfx/Color.h>

namespace Gfx {

#define ENUMERATE_COLOR_ROLES(C)   \
    C(ActiveLink)                  \
    C(ActiveWindowBorder1)         \
    C(ActiveWindowBorder2)         \
    C(ActiveWindowTitle)           \
    C(ActiveWindowTitleShadow)     \
    C(ActiveWindowTitleStripes)    \
    C(Base)                        \
    C(BaseText)                    \
    C(Button)                      \
    C(ButtonText)                  \
    C(DesktopBackground)           \
    C(FocusOutline)                \
    C(HighlightWindowBorder1)      \
    C(HighlightWindowBorder2)      \
    C(HighlightWindowTitle)        \
    C(HighlightWindowTitleShadow)  \
    C(HighlightWindowTitleStripes) \
    C(HoverHighlight)              \
    C(InactiveSelection)           \
    C(InactiveSelectionText)       \
    C(InactiveWindowBorder1)       \
    C(InactiveWindowBorder2)       \
    C(InactiveWindowTitle)         \
    C(InactiveWindowTitleShadow)   \
    C(InactiveWindowTitleStripes)  \
    C(Link)                        \
    C(MenuBase)                    \
    C(MenuBaseText)                \
    C(MenuSelection)               \
    C(MenuSelectionText)           \
    C(MenuStripe)                  \
    C(MovingWindowBorder1)         \
    C(MovingWindowBorder2)         \
    C(MovingWindowTitle)           \
    C(MovingWindowTitleShadow)     \
    C(MovingWindowTitleStripes)    \
    C(PlaceholderText)             \
    C(RubberBandBorder)            \
    C(RubberBandFill)              \
    C(Ruler)                       \
    C(RulerActiveText)             \
    C(RulerBorder)                 \
    C(RulerInactiveText)           \
    C(Selection)                   \
    C(SelectionText)               \
    C(SyntaxComment)               \
    C(SyntaxControlKeyword)        \
    C(SyntaxIdentifier)            \
    C(SyntaxKeyword)               \
    C(SyntaxNumber)                \
    C(SyntaxOperator)              \
    C(SyntaxPreprocessorStatement) \
    C(SyntaxPreprocessorValue)     \
    C(SyntaxPunctuation)           \
    C(SyntaxString)                \
    C(SyntaxType)                  \
    C(TextCursor)                  \
    C(ThreedHighlight)             \
    C(ThreedShadow1)               \
    C(ThreedShadow2)               \
    C(VisitedLink)                 \
    C(Window)                      \
    C(WindowText)

enum class ColorRole {
    NoRole,

#undef __ENUMERATE_COLOR_ROLE
#define __ENUMERATE_COLOR_ROLE(role) role,
    ENUMERATE_COLOR_ROLES(__ENUMERATE_COLOR_ROLE)
#undef __ENUMERATE_COLOR_ROLE

        __Count,

    Background = Window,
    DisabledText = ThreedShadow1,
};

inline const char* to_string(ColorRole role)
{
    switch (role) {
    case ColorRole::NoRole:
        return "NoRole";
#undef __ENUMERATE_COLOR_ROLE
#define __ENUMERATE_COLOR_ROLE(role) \
    case ColorRole::role:            \
        return #role;
        ENUMERATE_COLOR_ROLES(__ENUMERATE_COLOR_ROLE)
#undef __ENUMERATE_COLOR_ROLE
    default:
        ASSERT_NOT_REACHED();
    }
}

enum class MetricRole {
    NoRole,
    TitleHeight,
    TitleButtonWidth,
    TitleButtonHeight,
    __Count,
};

enum class PathRole {
    NoRole,
    TitleButtonIcons,
    __Count,
};

struct SystemTheme {
    RGBA32 color[(int)ColorRole::__Count];
    int metric[(int)MetricRole::__Count];
    char path[(int)PathRole::__Count][256]; // TODO: PATH_MAX?
};

const SystemTheme& current_system_theme();
int current_system_theme_buffer_id();
void set_system_theme(SharedBuffer&);
RefPtr<SharedBuffer> load_system_theme(const String& path);

}

using Gfx::ColorRole;
