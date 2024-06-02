/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/KeyCode.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/MouseEventPrototype.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/UIEvents/EventNames.h>
#include <LibWeb/UIEvents/MouseButton.h>
#include <LibWeb/UIEvents/MouseEvent.h>

namespace Web::UIEvents {

JS_DEFINE_ALLOCATOR(MouseEvent);

MouseEvent::MouseEvent(JS::Realm& realm, FlyString const& event_name, MouseEventInit const& event_init, double page_x, double page_y, double offset_x, double offset_y)
    : UIEvent(realm, event_name, event_init)
    , m_screen_x(event_init.screen_x)
    , m_screen_y(event_init.screen_y)
    , m_page_x(page_x)
    , m_page_y(page_y)
    , m_client_x(event_init.client_x)
    , m_client_y(event_init.client_y)
    , m_offset_x(offset_x)
    , m_offset_y(offset_y)
    , m_ctrl_key(event_init.ctrl_key)
    , m_shift_key(event_init.shift_key)
    , m_alt_key(event_init.alt_key)
    , m_meta_key(event_init.meta_key)
    , m_modifier_alt_graph(event_init.modifier_alt_graph)
    , m_modifier_caps_lock(event_init.modifier_caps_lock)
    , m_modifier_fn(event_init.modifier_fn)
    , m_modifier_fn_lock(event_init.modifier_fn_lock)
    , m_modifier_hyper(event_init.modifier_hyper)
    , m_modifier_num_lock(event_init.modifier_num_lock)
    , m_modifier_scroll_lock(event_init.modifier_scroll_lock)
    , m_modifier_super(event_init.modifier_super)
    , m_modifier_symbol(event_init.modifier_symbol)
    , m_modifier_symbol_lock(event_init.modifier_symbol_lock)
    , m_movement_x(event_init.movement_x)
    , m_movement_y(event_init.movement_y)
    , m_button(event_init.button)
    , m_buttons(event_init.buttons)
    , m_related_target(event_init.related_target)
{
    set_event_characteristics();
}

MouseEvent::~MouseEvent() = default;

void MouseEvent::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE(MouseEvent);
}

void MouseEvent::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_related_target);
}

bool MouseEvent::get_modifier_state(String const& key_arg) const
{
    if (key_arg == "Control")
        return m_ctrl_key;
    if (key_arg == "Shift")
        return m_shift_key;
    if (key_arg == "Alt")
        return m_alt_key;
    if (key_arg == "Meta")
        return m_meta_key;
    if (key_arg == "AltGraph")
        return m_modifier_alt_graph;
    if (key_arg == "CapsLock")
        return m_modifier_caps_lock;
    if (key_arg == "Fn")
        return m_modifier_fn;
    if (key_arg == "FnLock")
        return m_modifier_fn_lock;
    if (key_arg == "Hyper")
        return m_modifier_hyper;
    if (key_arg == "NumLock")
        return m_modifier_num_lock;
    if (key_arg == "ScrollLock")
        return m_modifier_scroll_lock;
    if (key_arg == "Super")
        return m_modifier_super;
    if (key_arg == "Symbol")
        return m_modifier_symbol;
    if (key_arg == "SymbolLock")
        return m_modifier_symbol_lock;
    return false;
}

// https://www.w3.org/TR/uievents/#dom-mouseevent-button
static i16 determine_button(unsigned mouse_button)
{
    switch (mouse_button) {
    case MouseButton::Primary:
        return 0;
    case MouseButton::Middle:
        return 1;
    case MouseButton::Secondary:
        return 2;
    case MouseButton::Backward:
        return 3;
    case MouseButton::Forward:
        return 4;
    default:
        VERIFY_NOT_REACHED();
    }
}

JS::NonnullGCPtr<MouseEvent> MouseEvent::create(JS::Realm& realm, FlyString const& event_name, MouseEventInit const& event_init, double page_x, double page_y, double offset_x, double offset_y)
{
    return realm.heap().allocate<MouseEvent>(realm, realm, event_name, event_init, page_x, page_y, offset_x, offset_y);
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<MouseEvent>> MouseEvent::construct_impl(JS::Realm& realm, FlyString const& event_name, MouseEventInit const& event_init)
{
    return create(realm, event_name, event_init);
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<MouseEvent>> MouseEvent::create_from_platform_event(JS::Realm& realm, FlyString const& event_name, CSSPixelPoint screen, CSSPixelPoint page, CSSPixelPoint client, CSSPixelPoint offset, Optional<CSSPixelPoint> movement, unsigned button, unsigned buttons, unsigned modifiers)
{
    MouseEventInit event_init {};
    event_init.ctrl_key = modifiers & Mod_Ctrl;
    event_init.shift_key = modifiers & Mod_Shift;
    event_init.alt_key = modifiers & Mod_Alt;
    event_init.meta_key = modifiers & Mod_Super;
    event_init.screen_x = screen.x().to_double();
    event_init.screen_y = screen.y().to_double();
    event_init.client_x = client.x().to_double();
    event_init.client_y = client.y().to_double();
    if (movement.has_value()) {
        event_init.movement_x = movement.value().x().to_double();
        event_init.movement_y = movement.value().y().to_double();
    }
    event_init.button = determine_button(button);
    event_init.buttons = buttons;
    auto event = MouseEvent::create(realm, event_name, event_init, page.x().to_double(), page.y().to_double(), offset.x().to_double(), offset.y().to_double());
    event->set_is_trusted(true);
    return event;
}

void MouseEvent::set_event_characteristics()
{
    if (type().is_one_of(EventNames::mousedown, EventNames::mousemove, EventNames::mouseout, EventNames::mouseover, EventNames::mouseup, HTML::EventNames::click, EventNames::dblclick, EventNames::contextmenu)) {
        set_bubbles(true);
        set_cancelable(true);
        set_composed(true);
    }
}

}
