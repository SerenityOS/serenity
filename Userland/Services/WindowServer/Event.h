/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <Kernel/API/KeyCode.h>
#include <LibCore/Event.h>
#include <LibCore/MimeData.h>
#include <LibGfx/Rect.h>
#include <WindowServer/Cursor.h>
#include <WindowServer/WindowType.h>

namespace WindowServer {

class Event : public Core::Event {
public:
    enum Type {
        Invalid = 3000,
        MouseMove,
        MouseDown,
        MouseDoubleClick,
        MouseUp,
        MouseWheel,
        WindowEntered,
        WindowLeft,
        KeyDown,
        KeyUp,
        WindowActivated,
        WindowDeactivated,
        WindowInputPreempted,
        WindowInputRestored,
        WindowCloseRequest,
        WindowResized,
        WindowMoved,
    };

    Event() = default;
    explicit Event(Type type)
        : Core::Event(type)
    {
    }
    virtual ~Event() = default;

    bool is_mouse_event() const { return type() == MouseMove || type() == MouseDown || type() == MouseDoubleClick || type() == MouseUp || type() == MouseWheel; }
    bool is_key_event() const { return type() == KeyUp || type() == KeyDown; }
};

enum MouseButton : u8 {
    None = 0,
    Primary = 1,
    Secondary = 2,
    Middle = 4,
    Backward = 8,
    Forward = 16,
};

class KeyEvent final : public Event {
public:
    KeyEvent(Type type, int key, u32 code_point, u8 modifiers, u32 scancode)
        : Event(type)
        , m_key(key)
        , m_code_point(code_point)
        , m_modifiers(modifiers)
        , m_scancode(scancode)
    {
    }

    int key() const { return m_key; }
    bool ctrl() const { return m_modifiers & Mod_Ctrl; }
    bool alt() const { return m_modifiers & Mod_Alt; }
    bool shift() const { return m_modifiers & Mod_Shift; }
    bool super() const { return m_modifiers & Mod_Super; }
    u8 modifiers() const { return m_modifiers; }
    u32 code_point() const { return m_code_point; }
    u32 scancode() const { return m_scancode; }

private:
    friend class EventLoop;
    friend class Screen;
    int m_key { 0 };
    u32 m_code_point { 0 };
    u8 m_modifiers { 0 };
    u32 m_scancode { 0 };
};

class MouseEvent final : public Event {
public:
    MouseEvent(Type type, Gfx::IntPoint position, unsigned buttons, MouseButton button, unsigned modifiers, int wheel_delta_x = 0, int wheel_delta_y = 0, int wheel_raw_delta_x = 0, int wheel_raw_delta_y = 0)
        : Event(type)
        , m_position(position)
        , m_buttons(buttons)
        , m_button(button)
        , m_modifiers(modifiers)
        , m_wheel_delta_x(wheel_delta_x)
        , m_wheel_delta_y(wheel_delta_y)
        , m_wheel_raw_delta_x(wheel_raw_delta_x)
        , m_wheel_raw_delta_y(wheel_raw_delta_y)
    {
    }

    Gfx::IntPoint position() const { return m_position; }
    int x() const { return m_position.x(); }
    int y() const { return m_position.y(); }
    MouseButton button() const { return m_button; }
    unsigned buttons() const { return m_buttons; }
    unsigned modifiers() const { return m_modifiers; }
    int wheel_delta_x() const { return m_wheel_delta_x; }
    int wheel_delta_y() const { return m_wheel_delta_y; }
    int wheel_raw_delta_x() const { return m_wheel_raw_delta_x; }
    int wheel_raw_delta_y() const { return m_wheel_raw_delta_y; }
    bool is_drag() const { return m_drag; }

    Vector<String> mime_types() const
    {
        if (!m_mime_data)
            return {};
        return m_mime_data->formats();
    }

    void set_drag(bool b) { m_drag = b; }
    void set_mime_data(Core::MimeData const& mime_data) { m_mime_data = mime_data; }

    MouseEvent translated(Gfx::IntPoint delta) const
    {
        MouseEvent event = *this;
        event.m_position = m_position.translated(delta);
        return event;
    }

private:
    Gfx::IntPoint m_position;
    unsigned m_buttons { 0 };
    MouseButton m_button { MouseButton::None };
    unsigned m_modifiers { 0 };
    int m_wheel_delta_x { 0 };
    int m_wheel_delta_y { 0 };
    int m_wheel_raw_delta_x { 0 };
    int m_wheel_raw_delta_y { 0 };
    bool m_drag { false };
    RefPtr<const Core::MimeData> m_mime_data;
};

class ResizeEvent final : public Event {
public:
    ResizeEvent(Gfx::IntRect const& rect)
        : Event(Event::WindowResized)
        , m_rect(rect)
    {
    }

    Gfx::IntRect const& rect() const { return m_rect; }

private:
    Gfx::IntRect m_rect;
};

class MoveEvent final : public Event {
public:
    MoveEvent(Gfx::IntRect const& rect)
        : Event(Event::WindowMoved)
        , m_rect(rect)
    {
    }

    Gfx::IntRect const& rect() const { return m_rect; }

private:
    Gfx::IntRect m_rect;
};

}
