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
#include <Kernel/KeyCode.h>
#include <LibCore/CEvent.h>
#include <LibDraw/Rect.h>
#include <WindowServer/WSCursor.h>
#include <WindowServer/WSWindowType.h>

class WSEvent : public Core::Event {
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
        WindowCloseRequest,
        WindowResized,
    };

    WSEvent() {}
    explicit WSEvent(Type type)
        : Core::Event(type)
    {
    }
    virtual ~WSEvent() {}

    bool is_mouse_event() const { return type() == MouseMove || type() == MouseDown || type() == MouseDoubleClick || type() == MouseUp || type() == MouseWheel; }
    bool is_key_event() const { return type() == KeyUp || type() == KeyDown; }
};

enum class MouseButton : u8 {
    None = 0,
    Left = 1,
    Right = 2,
    Middle = 4,
};

class WSKeyEvent final : public WSEvent {
public:
    WSKeyEvent(Type type, int key, char character, u8 modifiers)
        : WSEvent(type)
        , m_key(key)
        , m_character(character)
        , m_modifiers(modifiers)
    {
    }

    int key() const { return m_key; }
    bool ctrl() const { return m_modifiers & Mod_Ctrl; }
    bool alt() const { return m_modifiers & Mod_Alt; }
    bool shift() const { return m_modifiers & Mod_Shift; }
    bool logo() const { return m_modifiers & Mod_Logo; }
    u8 modifiers() const { return m_modifiers; }
    char character() const { return m_character; }

private:
    friend class WSEventLoop;
    friend class WSScreen;
    int m_key { 0 };
    char m_character { 0 };
    u8 m_modifiers { 0 };
};

class WSMouseEvent final : public WSEvent {
public:
    WSMouseEvent(Type type, const Point& position, unsigned buttons, MouseButton button, unsigned modifiers, int wheel_delta = 0)
        : WSEvent(type)
        , m_position(position)
        , m_buttons(buttons)
        , m_button(button)
        , m_modifiers(modifiers)
        , m_wheel_delta(wheel_delta)
    {
    }

    Point position() const { return m_position; }
    int x() const { return m_position.x(); }
    int y() const { return m_position.y(); }
    MouseButton button() const { return m_button; }
    unsigned buttons() const { return m_buttons; }
    unsigned modifiers() const { return m_modifiers; }
    int wheel_delta() const { return m_wheel_delta; }

    WSMouseEvent translated(const Point& delta) const { return WSMouseEvent((Type)type(), m_position.translated(delta), m_buttons, m_button, m_modifiers, m_wheel_delta); }

private:
    Point m_position;
    unsigned m_buttons { 0 };
    MouseButton m_button { MouseButton::None };
    unsigned m_modifiers { 0 };
    int m_wheel_delta { 0 };
};

class WSResizeEvent final : public WSEvent {
public:
    WSResizeEvent(const Rect& old_rect, const Rect& rect)
        : WSEvent(WSEvent::WindowResized)
        , m_old_rect(old_rect)
        , m_rect(rect)
    {
    }

    Rect old_rect() const { return m_old_rect; }
    Rect rect() const { return m_rect; }

private:
    Rect m_old_rect;
    Rect m_rect;
};
