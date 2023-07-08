/*
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <LibWeb/Bindings/Intrinsics.h>
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

static ErrorOr<String> get_event_key(KeyCode platform_key, u32 code_point)
{
    auto event_key = String::from_deprecated_string(key_code_to_string(platform_key));
    if (event_key.is_error()) {
        return event_key;
    }
    // Original case should be preserved for the key value of the event.
    // https://www.w3.org/TR/uievents-key/#key-attr-values
    if (is_ascii_lower_alpha(code_point)) {
        event_key = event_key.release_value().to_lowercase();
    }
    return event_key;
}

// 3. Keyboard Event code Value Tables, https://www.w3.org/TR/uievents-code/#code-value-tables
static ErrorOr<String> get_event_code(KeyCode platform_key, unsigned modifiers)
{
    // 3.4. Numpad Section, https://www.w3.org/TR/uievents-code/#key-numpad-section
    if ((modifiers & Mod_Keypad) != 0) {
        switch (platform_key) {
        case KeyCode::Key_0:
            return "Numpad0"_string;
        case KeyCode::Key_1:
            return "Numpad1"_string;
        case KeyCode::Key_2:
            return "Numpad2"_string;
        case KeyCode::Key_3:
            return "Numpad3"_string;
        case KeyCode::Key_4:
            return "Numpad4"_string;
        case KeyCode::Key_5:
            return "Numpad5"_string;
        case KeyCode::Key_6:
            return "Numpad6"_string;
        case KeyCode::Key_7:
            return "Numpad7"_string;
        case KeyCode::Key_8:
            return "Numpad8"_string;
        case KeyCode::Key_9:
            return "Numpad9"_string;
        case KeyCode::Key_Plus:
            return "NumpadAdd"_string;
        case KeyCode::Key_Period:
        case KeyCode::Key_Delete:
            return "NumpadDecimal"_string;
        case KeyCode::Key_Slash:
            return "NumpadDivide"_string;
        case KeyCode::Key_Return:
            return "NumpadEnter"_string;
        case KeyCode::Key_Asterisk:
            return "NumpadAsterisk"_string;
        case KeyCode::Key_Minus:
            return "NumpadSubtract"_string;
        default:
            break;
        }
    }

    switch (platform_key) {
    // 3.1.1. Writing System Keys, https://www.w3.org/TR/uievents-code/#key-alphanumeric-writing-system
    case KeyCode::Key_Backtick:
    case KeyCode::Key_Tilde:
        return "Backquote"_string;
    case KeyCode::Key_Backslash:
    case KeyCode::Key_Pipe:
        return "Backslash"_string;
    case KeyCode::Key_LeftBrace:
    case KeyCode::Key_LeftBracket:
        return "BracketLeft"_string;
    case KeyCode::Key_RightBrace:
    case KeyCode::Key_RightBracket:
        return "BracketRight"_string;
    case KeyCode::Key_Comma:
    case KeyCode::Key_LessThan:
        return "Comma"_string;
    case KeyCode::Key_0:
    case KeyCode::Key_RightParen:
        return "Digit0"_string;
    case KeyCode::Key_1:
    case KeyCode::Key_ExclamationPoint:
        return "Digit1"_string;
    case KeyCode::Key_2:
    case KeyCode::Key_AtSign:
        return "Digit2"_string;
    case KeyCode::Key_3:
    case KeyCode::Key_Hashtag:
        return "Digit3"_string;
    case KeyCode::Key_4:
    case KeyCode::Key_Dollar:
        return "Digit4"_string;
    case KeyCode::Key_5:
    case KeyCode::Key_Percent:
        return "Digit5"_string;
    case KeyCode::Key_6:
    case KeyCode::Key_Circumflex:
        return "Digit6"_string;
    case KeyCode::Key_7:
    case KeyCode::Key_Ampersand:
        return "Digit7"_string;
    case KeyCode::Key_8:
    case KeyCode::Key_Asterisk:
        return "Digit8"_string;
    case KeyCode::Key_9:
    case KeyCode::Key_LeftParen:
        return "Digit9"_string;
    case KeyCode::Key_Equal:
    case KeyCode::Key_Plus:
        return "Equal"_string;
    // FIXME: IntlBackslash
    // FIXME: IntlRo
    // FIXME: IntlYen
    case KeyCode::Key_A:
        return "KeyA"_string;
    case KeyCode::Key_B:
        return "KeyB"_string;
    case KeyCode::Key_C:
        return "KeyC"_string;
    case KeyCode::Key_D:
        return "KeyD"_string;
    case KeyCode::Key_E:
        return "KeyE"_string;
    case KeyCode::Key_F:
        return "KeyF"_string;
    case KeyCode::Key_G:
        return "KeyG"_string;
    case KeyCode::Key_H:
        return "KeyH"_string;
    case KeyCode::Key_I:
        return "KeyI"_string;
    case KeyCode::Key_J:
        return "KeyJ"_string;
    case KeyCode::Key_K:
        return "KeyK"_string;
    case KeyCode::Key_L:
        return "KeyL"_string;
    case KeyCode::Key_M:
        return "KeyM"_string;
    case KeyCode::Key_N:
        return "KeyN"_string;
    case KeyCode::Key_O:
        return "KeyO"_string;
    case KeyCode::Key_P:
        return "KeyP"_string;
    case KeyCode::Key_Q:
        return "KeyQ"_string;
    case KeyCode::Key_R:
        return "KeyR"_string;
    case KeyCode::Key_S:
        return "KeyS"_string;
    case KeyCode::Key_T:
        return "KeyT"_string;
    case KeyCode::Key_U:
        return "KeyU"_string;
    case KeyCode::Key_V:
        return "KeyV"_string;
    case KeyCode::Key_W:
        return "KeyW"_string;
    case KeyCode::Key_X:
        return "KeyX"_string;
    case KeyCode::Key_Y:
        return "KeyY"_string;
    case KeyCode::Key_Z:
        return "KeyZ"_string;
    case KeyCode::Key_Minus:
    case KeyCode::Key_Underscore:
        return "Minus"_string;
    case KeyCode::Key_Period:
    case KeyCode::Key_GreaterThan:
        return "Period"_string;
    case KeyCode::Key_Apostrophe:
    case KeyCode::Key_DoubleQuote:
        return "Quote"_string;
    case KeyCode::Key_Semicolon:
    case KeyCode::Key_Colon:
        return "Semicolon"_string;
    case KeyCode::Key_Slash:
    case KeyCode::Key_QuestionMark:
        return "Slash"_string;

    // 3.1.2. Functional Keys, https://www.w3.org/TR/uievents-code/#key-alphanumeric-functional
    case KeyCode::Key_Alt:
        return "Alt"_string; // FIXME: Detect left vs. right key.
    case KeyCode::Key_Backspace:
        return "Backspace"_string;
    case KeyCode::Key_CapsLock:
        return "CapsLock"_string;
    case KeyCode::Key_Menu:
        return "ContextMenu"_string;
    case KeyCode::Key_Control:
        return "Control"_string; // FIXME: Detect left vs. right key.
    case KeyCode::Key_Return:
        return "Enter"_string;
    case KeyCode::Key_Super:
        return "Meta"_string; // FIXME: Detect left vs. right key.
    case KeyCode::Key_LeftShift:
        return "ShiftLeft"_string;
    case KeyCode::Key_RightShift:
        return "ShiftRight"_string;
    case KeyCode::Key_Space:
        return "Space"_string;
    case KeyCode::Key_Tab:
        return "Tab"_string;

    // 3.2. Control Pad Section, https://www.w3.org/TR/uievents-code/#key-controlpad-section
    case KeyCode::Key_Delete:
        return "Delete"_string;
    case KeyCode::Key_End:
        return "End"_string;
    // FIXME: Help
    case KeyCode::Key_Home:
        return "Home"_string;
    case KeyCode::Key_Insert:
        return "Insert"_string;
    case KeyCode::Key_PageDown:
        return "PageDown"_string;
    case KeyCode::Key_PageUp:
        return "PageUp"_string;

    // 3.3. Arrow Pad Section, https://www.w3.org/TR/uievents-code/#key-arrowpad-section
    case KeyCode::Key_Down:
        return "ArrowDown"_string;
    case KeyCode::Key_Left:
        return "ArrowLeft"_string;
    case KeyCode::Key_Right:
        return "ArrowRight"_string;
    case KeyCode::Key_Up:
        return "ArrowUp"_string;

    // 3.4. Numpad Section, https://www.w3.org/TR/uievents-code/#key-numpad-section
    case KeyCode::Key_NumLock:
        return "NumLock"_string;

    // 3.5. Function Section, https://www.w3.org/TR/uievents-code/#key-function-section
    case KeyCode::Key_Escape:
        return "Escape"_string;
    case KeyCode::Key_F1:
        return "F1"_string;
    case KeyCode::Key_F2:
        return "F2"_string;
    case KeyCode::Key_F3:
        return "F3"_string;
    case KeyCode::Key_F4:
        return "F4"_string;
    case KeyCode::Key_F5:
        return "F5"_string;
    case KeyCode::Key_F6:
        return "F6"_string;
    case KeyCode::Key_F7:
        return "F7"_string;
    case KeyCode::Key_F8:
        return "F8"_string;
    case KeyCode::Key_F9:
        return "F9"_string;
    case KeyCode::Key_F10:
        return "F10"_string;
    case KeyCode::Key_F11:
        return "F11"_string;
    case KeyCode::Key_F12:
        return "F12"_string;
    case KeyCode::Key_PrintScreen:
    case KeyCode::Key_SysRq:
        return "PrintScreen"_string;
    case KeyCode::Key_ScrollLock:
        return "ScrollLock"_string;
    // FIXME: Pause

    // 3.7. Legacy, Non-Standard and Special Keys, https://www.w3.org/TR/uievents-code/#key-legacy
    case KeyCode::Key_Invalid:
        return "Unidentified"_string;
    }

    VERIFY_NOT_REACHED();
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<KeyboardEvent>> KeyboardEvent::create_from_platform_event(JS::Realm& realm, FlyString const& event_name, KeyCode platform_key, unsigned modifiers, u32 code_point)
{
    auto& vm = realm.vm();

    // FIXME: Figure out what these should actually contain.
    auto event_key = TRY_OR_THROW_OOM(vm, get_event_key(platform_key, code_point));
    auto event_code = TRY_OR_THROW_OOM(vm, get_event_code(platform_key, modifiers));

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
    return KeyboardEvent::create(realm, event_name, event_init);
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

WebIDL::ExceptionOr<JS::NonnullGCPtr<KeyboardEvent>> KeyboardEvent::create(JS::Realm& realm, FlyString const& event_name, KeyboardEventInit const& event_init)
{
    return MUST_OR_THROW_OOM(realm.heap().allocate<KeyboardEvent>(realm, realm, event_name, event_init));
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<KeyboardEvent>> KeyboardEvent::construct_impl(JS::Realm& realm, FlyString const& event_name, KeyboardEventInit const& event_init)
{
    return create(realm, event_name, event_init);
}

KeyboardEvent::KeyboardEvent(JS::Realm& realm, FlyString const& event_name, KeyboardEventInit const& event_init)
    : UIEvent(realm, event_name, event_init)
    , m_key(event_init.key)
    , m_code(event_init.code)
    , m_location(event_init.location)
    , m_ctrl_key(event_init.ctrl_key)
    , m_shift_key(event_init.shift_key)
    , m_alt_key(event_init.alt_key)
    , m_meta_key(event_init.meta_key)
    , m_repeat(event_init.repeat)
    , m_is_composing(event_init.is_composing)
    , m_key_code(event_init.key_code)
    , m_char_code(event_init.char_code)
{
}

KeyboardEvent::~KeyboardEvent() = default;

JS::ThrowCompletionOr<void> KeyboardEvent::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::KeyboardEventPrototype>(realm, "KeyboardEvent"));

    return {};
}

}
