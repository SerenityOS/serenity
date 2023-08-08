/*
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/TypeCasts.h>
#include <Kernel/API/KeyCode.h>
#include <LibWeb/UIEvents/EventModifier.h>
#include <LibWeb/UIEvents/UIEvent.h>

namespace Web::UIEvents {

struct KeyboardEventInit : public EventModifierInit {
    String key;
    String code;
    u32 location { 0 };
    bool repeat { false };
    bool is_composing { false };
    u32 key_code { 0 };
    u32 char_code { 0 };
};

enum class DOMKeyLocation {
    Standard = 0,
    Left = 1,
    Right = 2,
    Numpad = 3,
};

// https://www.w3.org/TR/uievents/#interface-keyboardevent
class KeyboardEvent final : public UIEvent {
    WEB_PLATFORM_OBJECT(KeyboardEvent, UIEvent);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<KeyboardEvent>> create(JS::Realm&, FlyString const& event_name, KeyboardEventInit const& event_init = {});
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<KeyboardEvent>> construct_impl(JS::Realm&, FlyString const& event_name, KeyboardEventInit const& event_init);
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<KeyboardEvent>> create_from_platform_event(JS::Realm&, FlyString const& event_name, KeyCode, unsigned modifiers, u32 code_point);

    virtual ~KeyboardEvent() override;

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

    virtual u32 which() const override { return m_key_code; }

private:
    KeyboardEvent(JS::Realm&, FlyString const& event_name, KeyboardEventInit const& event_init);

    virtual void initialize(JS::Realm&) override;

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
