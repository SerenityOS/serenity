/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/String.h>
#include <Kernel/API/KeyCode.h>
#include <LibCore/Event.h>
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
        WindowInputEntered,
        WindowInputLeft,
        WindowCloseRequest,
        WindowResized,
    };

    Event() { }
    explicit Event(Type type)
        : Core::Event(type)
    {
    }
    virtual ~Event() { }

    bool is_mouse_event() const { return type() == MouseMove || type() == MouseDown || type() == MouseDoubleClick || type() == MouseUp || type() == MouseWheel; }
    bool is_key_event() const { return type() == KeyUp || type() == KeyDown; }
};

enum class MouseButton : u8 {
    None = 0,
    Left = 1,
    Right = 2,
    Middle = 4,
    Back = 8,
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
    bool logo() const { return m_modifiers & Mod_Logo; }
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
    MouseEvent(Type type, const Gfx::IntPoint& position, unsigned buttons, MouseButton button, unsigned modifiers, int wheel_delta = 0)
        : Event(type)
        , m_position(position)
        , m_buttons(buttons)
        , m_button(button)
        , m_modifiers(modifiers)
        , m_wheel_delta(wheel_delta)
    {
    }

    Gfx::IntPoint position() const { return m_position; }
    int x() const { return m_position.x(); }
    int y() const { return m_position.y(); }
    MouseButton button() const { return m_button; }
    unsigned buttons() const { return m_buttons; }
    unsigned modifiers() const { return m_modifiers; }
    int wheel_delta() const { return m_wheel_delta; }
    bool is_drag() const { return m_drag; }
    const String& drag_data_type() const { return m_drag_data_type; }

    void set_drag(bool b) { m_drag = b; }
    void set_drag_data_type(const String& drag_data_type) { m_drag_data_type = drag_data_type; }

    MouseEvent translated(const Gfx::IntPoint& delta) const { return MouseEvent((Type)type(), m_position.translated(delta), m_buttons, m_button, m_modifiers, m_wheel_delta); }

private:
    Gfx::IntPoint m_position;
    unsigned m_buttons { 0 };
    MouseButton m_button { MouseButton::None };
    unsigned m_modifiers { 0 };
    int m_wheel_delta { 0 };
    bool m_drag { false };
    String m_drag_data_type;
};

class ResizeEvent final : public Event {
public:
    ResizeEvent(const Gfx::IntRect& rect)
        : Event(Event::WindowResized)
        , m_rect(rect)
    {
    }

    Gfx::IntRect rect() const { return m_rect; }

private:
    Gfx::IntRect m_rect;
};

}
