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

#include <Kernel/KeyCode.h>
#include <LibCore/CEvent.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>
#include <LibGUI/GWindowType.h>

namespace GUI {

class Event : public Core::Event {
public:
    enum Type {
        Show = 1000,
        Hide,
        Paint,
        MultiPaint,
        Resize,
        MouseMove,
        MouseDown,
        MouseDoubleClick,
        MouseUp,
        MouseWheel,
        Enter,
        Leave,
        KeyDown,
        KeyUp,
        WindowEntered,
        WindowLeft,
        WindowBecameInactive,
        WindowBecameActive,
        FocusIn,
        FocusOut,
        WindowCloseRequest,
        ContextMenu,
        EnabledChange,
        Drop,

        __Begin_WM_Events,
        WM_WindowRemoved,
        WM_WindowStateChanged,
        WM_WindowRectChanged,
        WM_WindowIconBitmapChanged,
        __End_WM_Events,
    };

    Event() {}
    explicit Event(Type type)
        : Core::Event(type)
    {
    }
    virtual ~Event() {}

    bool is_key_event() const { return type() == KeyUp || type() == KeyDown; }
    bool is_paint_event() const { return type() == Paint; }
};

class WMEvent : public Event {
public:
    WMEvent(Type type, int client_id, int window_id)
        : Event(type)
        , m_client_id(client_id)
        , m_window_id(window_id)
    {
    }

    int client_id() const { return m_client_id; }
    int window_id() const { return m_window_id; }

private:
    int m_client_id { -1 };
    int m_window_id { -1 };
};

class WMWindowRemovedEvent : public WMEvent {
public:
    WMWindowRemovedEvent(int client_id, int window_id)
        : WMEvent(Event::Type::WM_WindowRemoved, client_id, window_id)
    {
    }
};

class WMWindowStateChangedEvent : public WMEvent {
public:
    WMWindowStateChangedEvent(int client_id, int window_id, const StringView& title, const Gfx::Rect& rect, bool is_active, WindowType window_type, bool is_minimized)
        : WMEvent(Event::Type::WM_WindowStateChanged, client_id, window_id)
        , m_title(title)
        , m_rect(rect)
        , m_window_type(window_type)
        , m_active(is_active)
        , m_minimized(is_minimized)
    {
    }

    String title() const { return m_title; }
    Rect rect() const { return m_rect; }
    bool is_active() const { return m_active; }
    WindowType window_type() const { return m_window_type; }
    bool is_minimized() const { return m_minimized; }

private:
    String m_title;
    Gfx::Rect m_rect;
    WindowType m_window_type;
    bool m_active;
    bool m_minimized;
};

class WMWindowRectChangedEvent : public WMEvent {
public:
    WMWindowRectChangedEvent(int client_id, int window_id, const Gfx::Rect& rect)
        : WMEvent(Event::Type::WM_WindowRectChanged, client_id, window_id)
        , m_rect(rect)
    {
    }

    Rect rect() const { return m_rect; }

private:
    Gfx::Rect m_rect;
};

class WMWindowIconBitmapChangedEvent : public WMEvent {
public:
    WMWindowIconBitmapChangedEvent(int client_id, int window_id, int icon_buffer_id, const Gfx::Size& icon_size)
        : WMEvent(Event::Type::WM_WindowIconBitmapChanged, client_id, window_id)
        , m_icon_buffer_id(icon_buffer_id)
        , m_icon_size(icon_size)
    {
    }

    int icon_buffer_id() const { return m_icon_buffer_id; }
    const Gfx::Size& icon_size() const { return m_icon_size; }

private:
    int m_icon_buffer_id;
    Gfx::Size m_icon_size;
};

class MultiPaintEvent final : public Event {
public:
    explicit MultiPaintEvent(const Vector<Rect, 32>& rects, const Gfx::Size& window_size)
        : Event(Event::MultiPaint)
        , m_rects(rects)
        , m_window_size(window_size)
    {
    }

    const Vector<Rect, 32>& rects() const { return m_rects; }
    Size window_size() const { return m_window_size; }

private:
    Vector<Rect, 32> m_rects;
    Gfx::Size m_window_size;
};

class PaintEvent final : public Event {
public:
    explicit PaintEvent(const Gfx::Rect& rect, const Gfx::Size& window_size = Size())
        : Event(Event::Paint)
        , m_rect(rect)
        , m_window_size(window_size)
    {
    }

    Rect rect() const { return m_rect; }
    Size window_size() const { return m_window_size; }

private:
    Gfx::Rect m_rect;
    Gfx::Size m_window_size;
};

class ResizeEvent final : public Event {
public:
    explicit ResizeEvent(const Gfx::Size& old_size, const Gfx::Size& size)
        : Event(Event::Resize)
        , m_old_size(old_size)
        , m_size(size)
    {
    }

    const Gfx::Size& old_size() const { return m_old_size; }
    const Gfx::Size& size() const { return m_size; }

private:
    Gfx::Size m_old_size;
    Gfx::Size m_size;
};

class ContextMenuEvent final : public Event {
public:
    explicit ContextMenuEvent(const Gfx::Point& position, const Gfx::Point& screen_position)
        : Event(Event::ContextMenu)
        , m_position(position)
        , m_screen_position(screen_position)
    {
    }

    const Gfx::Point& position() const { return m_position; }
    const Gfx::Point& screen_position() const { return m_screen_position; }

private:
    Gfx::Point m_position;
    Gfx::Point m_screen_position;
};

class ShowEvent final : public Event {
public:
    ShowEvent()
        : Event(Event::Show)
    {
    }
};

class HideEvent final : public Event {
public:
    HideEvent()
        : Event(Event::Hide)
    {
    }
};

enum MouseButton : u8 {
    None = 0,
    Left = 1,
    Right = 2,
    Middle = 4,
};

class KeyEvent final : public Event {
public:
    KeyEvent(Type type, int key, u8 modifiers)
        : Event(type)
        , m_key(key)
        , m_modifiers(modifiers)
    {
    }

    int key() const { return m_key; }
    bool ctrl() const { return m_modifiers & Mod_Ctrl; }
    bool alt() const { return m_modifiers & Mod_Alt; }
    bool shift() const { return m_modifiers & Mod_Shift; }
    bool logo() const { return m_modifiers & Mod_Logo; }
    u8 modifiers() const { return m_modifiers; }
    String text() const { return m_text; }

private:
    friend class WindowServerConnection;
    int m_key { 0 };
    u8 m_modifiers { 0 };
    String m_text;
};

class MouseEvent final : public Event {
public:
    MouseEvent(Type type, const Gfx::Point& position, unsigned buttons, MouseButton button, unsigned modifiers, int wheel_delta)
        : Event(type)
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

private:
    Gfx::Point m_position;
    unsigned m_buttons { 0 };
    MouseButton m_button { MouseButton::None };
    unsigned m_modifiers { 0 };
    int m_wheel_delta { 0 };
};

class DropEvent final : public Event {
public:
    DropEvent(const Gfx::Point& position, const String& text, const String& data_type, const String& data)
        : Event(Event::Drop)
        , m_position(position)
        , m_text(text)
        , m_data_type(data_type)
        , m_data(data)
    {
    }

    const Gfx::Point& position() const { return m_position; }
    const String& text() const { return m_text; }
    const String& data_type() const { return m_data_type; }
    const String& data() const { return m_data; }

private:
    Gfx::Point m_position;
    String m_text;
    String m_data_type;
    String m_data;
};

}
