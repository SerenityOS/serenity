/*
 * Copyright (c) 2024, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Variant.h>
#include <LibGfx/Point.h>
#include <LibWeb/PixelUnits.h>

// FIXME: These should not be included outside of Serenity. This FIXME appears in several locations across the Ladybird
//        chromes. The classes in this file provide a good opportunity to remove LibGUI and Kernel types from LibWeb.
#include <Kernel/API/KeyCode.h>
#include <LibGUI/Event.h>

namespace Web {

struct ChromeInputData {
    virtual ~ChromeInputData() = default;
};

struct KeyEvent {
public:
    enum class Type {
        KeyDown,
        KeyUp,
    };

    Type type;
    KeyCode key { KeyCode::Key_Invalid };
    KeyModifier modifiers { KeyModifier::Mod_None };
    u32 code_point { 0 };

    OwnPtr<ChromeInputData> chrome_data;
};

struct MouseEvent {
public:
    enum class Type {
        MouseDown,
        MouseUp,
        MouseMove,
        MouseWheel,
        DoubleClick,
    };

    Type type;
    Web::DevicePixelPoint position;
    Web::DevicePixelPoint screen_position;
    GUI::MouseButton button { GUI::MouseButton::None };
    GUI::MouseButton buttons { GUI::MouseButton::None };
    KeyModifier modifiers { KeyModifier::Mod_None };
    int wheel_delta_x { 0 };
    int wheel_delta_y { 0 };

    OwnPtr<ChromeInputData> chrome_data;
};

using InputEvent = Variant<KeyEvent, MouseEvent>;

}
