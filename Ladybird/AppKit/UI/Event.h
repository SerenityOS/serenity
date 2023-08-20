/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// FIXME: These should not be included outside of Serenity.
#include <Kernel/API/KeyCode.h>
#include <LibGUI/Event.h>

#import <System/Cocoa.h>

namespace Ladybird {

struct MouseEvent {
    Gfx::IntPoint position {};
    GUI::MouseButton button { GUI::MouseButton::Primary };
    KeyModifier modifiers { KeyModifier::Mod_None };
};
MouseEvent ns_event_to_mouse_event(NSEvent*, NSView*, GUI::MouseButton);

NSEvent* create_context_menu_mouse_event(NSView*, Gfx::IntPoint);
NSEvent* create_context_menu_mouse_event(NSView*, NSPoint);

struct KeyEvent {
    KeyCode key_code { KeyCode::Key_Invalid };
    KeyModifier modifiers { KeyModifier::Mod_None };
    u32 code_point { 0 };
};
KeyEvent ns_event_to_key_event(NSEvent*);

}
