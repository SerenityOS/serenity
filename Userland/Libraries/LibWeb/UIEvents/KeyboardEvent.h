/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/TypeCasts.h>
#include <Kernel/API/KeyCode.h>
#include <LibWeb/UIEvents/UIEvent.h>

namespace Web::UIEvents {

// https://www.w3.org/TR/uievents/#interface-keyboardevent
class KeyboardEvent final : public UIEvent {
public:
    using WrapperType = Bindings::KeyboardEventWrapper;

    static NonnullRefPtr<KeyboardEvent> create(FlyString event_name, String key, String code, unsigned long location, bool ctrl_key, bool shift_key, bool alt_key, bool meta_key, bool repeat, bool is_composing, unsigned long key_code, unsigned long char_code)
    {
        return adopt_ref(*new KeyboardEvent(move(event_name), move(key), move(code), location, ctrl_key, shift_key, alt_key, meta_key, repeat, is_composing, key_code, char_code));
    }

    static NonnullRefPtr<KeyboardEvent> create_from_platform_event(FlyString event_name, KeyCode, unsigned modifiers, u32 code_point);

    virtual ~KeyboardEvent() override;

    unsigned long key_code() const { return m_key_code; }
    unsigned long char_code() const { return m_char_code; }

    String key() const { return m_key; }
    String code() const { return m_code; }
    unsigned long location() const { return m_location; }

    bool ctrl_key() const { return m_ctrl_key; }
    bool shift_key() const { return m_shift_key; }
    bool alt_key() const { return m_alt_key; }
    bool meta_key() const { return m_meta_key; }

    bool repeat() const { return m_repeat; }
    bool is_composing() const { return m_is_composing; }

    bool get_modifier_state(String const& key_arg);

private:
    KeyboardEvent(FlyString event_name, String key, String code, unsigned long location, bool ctrl_key, bool shift_key, bool alt_key, bool meta_key, bool repeat, bool is_composing, unsigned long key_code, unsigned long char_code);

    String m_key;
    String m_code;
    unsigned long m_location { 0 };
    bool m_ctrl_key { false };
    bool m_shift_key { false };
    bool m_alt_key { false };
    bool m_meta_key { false };
    bool m_repeat { false };
    bool m_is_composing { false };
    unsigned long m_key_code { 0 };
    unsigned long m_char_code { 0 };
};

}
