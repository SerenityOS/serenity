/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <LibWeb/UIEvents/KeyboardEvent.h>

namespace Web::UIEvents {

// https://www.w3.org/TR/uievents/#determine-keydown-keyup-keyCode
static unsigned long determine_key_code(KeyCode platform_key, u32 code_point)
{
    // If input key when pressed without modifiers would insert a numerical character (0-9), return the ASCII code of that numerical character.
    if (is_ascii_digit(code_point))
        return code_point;

    // If input key when pressed without modifiers would insert a lower case character in the a-z alphabetical range, return the ASCII code of the upper case equivalent.
    if (is_ascii_lower_alpha(code_point))
        return to_ascii_uppercase(code_point);

    // If the key’s function, as determined in an implementation-specific way, corresponds to one of the keys in the §8.3.3 Fixed virtual key codes table, return the corresponding key code.
    // https://www.w3.org/TR/uievents/#fixed-virtual-key-codes
    switch (platform_key) {
    case KeyCode::Key_Backspace:
        return 8;
    case KeyCode::Key_Tab:
        return 9;
    case KeyCode::Key_Return:
        return 13;
    case KeyCode::Key_Shift:
        return 16;
    case KeyCode::Key_Control:
        return 17;
    case KeyCode::Key_Alt:
        return 18;
    case KeyCode::Key_CapsLock:
        return 20;
    case KeyCode::Key_Escape:
        return 27;
    case KeyCode::Key_Space:
        return 32;
    case KeyCode::Key_PageUp:
        return 33;
    case KeyCode::Key_PageDown:
        return 34;
    case KeyCode::Key_End:
        return 35;
    case KeyCode::Key_Home:
        return 36;
    case KeyCode::Key_Left:
        return 37;
    case KeyCode::Key_Up:
        return 38;
    case KeyCode::Key_Right:
        return 39;
    case KeyCode::Key_Down:
        return 40;
    default:
        break;
    }

    // Return the virtual key code from the operating system.
    return platform_key;
}

NonnullRefPtr<KeyboardEvent> KeyboardEvent::create_from_platform_event(FlyString const& event_name, KeyCode platform_key, unsigned modifiers, u32 code_point)
{
    // FIXME: Figure out what these should actually contain.
    String event_key = key_code_to_string(platform_key);
    String event_code = "FIXME";

    auto key_code = determine_key_code(platform_key, code_point);
    KeyboardEventInit event_init {};
    event_init.key = move(event_key);
    event_init.code = move(event_code);
    event_init.location = 0;
    event_init.ctrl_key = modifiers & Mod_Ctrl;
    event_init.shift_key = modifiers & Mod_Shift;
    event_init.alt_key = modifiers & Mod_Alt;
    event_init.meta_key = false;
    event_init.repeat = false;
    event_init.is_composing = false;
    event_init.key_code = key_code;
    event_init.char_code = code_point;
    event_init.bubbles = true;
    event_init.cancelable = true;
    event_init.composed = true;
    return KeyboardEvent::create(event_name, event_init);
}

bool KeyboardEvent::get_modifier_state(String const& key_arg)
{
    if (key_arg == "Alt")
        return m_alt_key;
    if (key_arg == "Control")
        return m_ctrl_key;
    if (key_arg == "Shift")
        return m_shift_key;
    if (key_arg == "Meta")
        return m_meta_key;
    return false;
}
}
