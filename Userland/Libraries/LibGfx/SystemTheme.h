/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <LibCore/AnonymousBuffer.h>
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
    C(HighlightSearching)          \
    C(HighlightSearchingText)      \
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
    C(Tooltip)                     \
    C(TooltipText)                 \
    C(Tray)                        \
    C(TrayText)                    \
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
        VERIFY_NOT_REACHED();
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
    InactiveWindowShadow,
    ActiveWindowShadow,
    TaskbarShadow,
    MenuShadow,
    TooltipShadow,
    __Count,
};

struct SystemTheme {
    RGBA32 color[(int)ColorRole::__Count];
    int metric[(int)MetricRole::__Count];
    char path[(int)PathRole::__Count][256]; // TODO: PATH_MAX?
};

Core::AnonymousBuffer& current_system_theme_buffer();
void set_system_theme(Core::AnonymousBuffer);
Core::AnonymousBuffer load_system_theme(const String& path);

}

using Gfx::ColorRole;
