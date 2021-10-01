/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/TypeCasts.h>
#include <Kernel/API/KeyCode.h>
#include <LibWeb/UIEvents/EventModifier.h>
#include <LibWeb/UIEvents/UIEvent.h>

namespace Web::UIEvents {

struct KeyboardEventInit : public EventModifierInit {
    String key { "" };
    String code { "" };
    u32 location { 0 };
    bool repeat { false };
    bool is_composing { false };
    u32 key_code { 0 };
    u32 char_code { 0 };
};

// https://www.w3.org/TR/uievents/#interface-keyboardevent
class KeyboardEvent final : public UIEvent {
public:
    using WrapperType = Bindings::KeyboardEventWrapper;

    static NonnullRefPtr<KeyboardEvent> create(FlyString const& event_name, KeyboardEventInit const& event_init = {})
    {
        return adopt_ref(*new KeyboardEvent(event_name, event_init));
    }
    static NonnullRefPtr<KeyboardEvent> create_with_global_object(Bindings::WindowObject&, FlyString const& event_name, KeyboardEventInit const& event_init)
    {
        return KeyboardEvent::create(event_name, event_init);
    }

    static NonnullRefPtr<KeyboardEvent> create_from_platform_event(FlyString const& event_name, KeyCode, unsigned modifiers, u32 code_point);

    virtual ~KeyboardEvent() override = default;

    u32 key_code() const { return m_key_code; }
    u32 char_code() const { return m_char_code; }

    String key() const { return m_key; }
    String code() const { return m_code; }
    u32 location() const { return m_location; }

    bool ctrl_key() const { return m_ctrl_key; }
    bool shift_key() const { return m_shift_key; }
    bool alt_key() const { return m_alt_key; }
    bool meta_key() const { return m_meta_key; }

    bool repeat() const { return m_repeat; }
    bool is_composing() const { return m_is_composing; }

    bool get_modifier_state(String const& key_arg);

private:
    KeyboardEvent(FlyString const& event_name, KeyboardEventInit const& event_init)
        : UIEvent(event_name, event_init)
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
        , m_char_code(event_init.char_code) {};

    String m_key;
    String m_code;
    u32 m_location { 0 };
    bool m_ctrl_key { false };
    bool m_shift_key { false };
    bool m_alt_key { false };
    bool m_meta_key { false };
    bool m_repeat { false };
    bool m_is_composing { false };
    u32 m_key_code { 0 };
    u32 m_char_code { 0 };
};

}
