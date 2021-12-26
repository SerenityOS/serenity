#pragma once

#include <AK/SharedBuffer.h>
#include <AK/Types.h>
#include <LibDraw/Color.h>

enum class ColorRole {
    NoRole,
    DesktopBackground,
    ActiveWindowBorder1,
    ActiveWindowBorder2,
    ActiveWindowTitle,
    InactiveWindowBorder1,
    InactiveWindowBorder2,
    InactiveWindowTitle,
    MovingWindowBorder1,
    MovingWindowBorder2,
    MovingWindowTitle,
    HighlightWindowBorder1,
    HighlightWindowBorder2,
    HighlightWindowTitle,
    MenuStripe,
    MenuBase,
    MenuBaseText,
    MenuSelection,
    MenuSelectionText,
    Window,
    WindowText,
    Button,
    ButtonText,
    Base,
    BaseText,
    ThreedHighlight,
    ThreedShadow1,
    ThreedShadow2,
    HoverHighlight,
    Selection,
    SelectionText,
    RubberBandFill,
    RubberBandBorder,

    __Count,

    Background = Window,
    DisabledText = ThreedShadow1,
};

struct SystemTheme {
    Color color[(int)ColorRole::__Count];
};

const SystemTheme& current_system_theme();
int current_system_theme_buffer_id();
void set_system_theme(SharedBuffer&);
RefPtr<SharedBuffer> load_system_theme(const String& path);
