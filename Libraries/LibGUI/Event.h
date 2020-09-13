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

#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <Kernel/API/KeyCode.h>
#include <LibCore/Event.h>
#include <LibGUI/FocusSource.h>
#include <LibGUI/WindowType.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>

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
        WindowInputEntered,
        WindowInputLeft,
        FocusIn,
        FocusOut,
        WindowCloseRequest,
        ContextMenu,
        EnabledChange,
        DragMove,
        Drop,
        ThemeChange,

        __Begin_WM_Events,
        WM_WindowRemoved,
        WM_WindowStateChanged,
        WM_WindowRectChanged,
        WM_WindowIconBitmapChanged,
        __End_WM_Events,
    };

    Event() { }
    explicit Event(Type type)
        : Core::Event(type)
    {
    }
    virtual ~Event() { }

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
    WMWindowStateChangedEvent(int client_id, int window_id, int parent_client_id, int parent_window_id, const StringView& title, const Gfx::IntRect& rect, bool is_active, bool is_modal, WindowType window_type, bool is_minimized, bool is_frameless, int progress)
        : WMEvent(Event::Type::WM_WindowStateChanged, client_id, window_id)
        , m_parent_client_id(parent_client_id)
        , m_parent_window_id(parent_window_id)
        , m_title(title)
        , m_rect(rect)
        , m_window_type(window_type)
        , m_active(is_active)
        , m_modal(is_modal)
        , m_minimized(is_minimized)
        , m_frameless(is_frameless)
        , m_progress(progress)
    {
    }

    int parent_client_id() const { return m_parent_client_id; }
    int parent_window_id() const { return m_parent_window_id; }
    const String& title() const { return m_title; }
    const Gfx::IntRect& rect() const { return m_rect; }
    bool is_active() const { return m_active; }
    bool is_modal() const { return m_modal; }
    WindowType window_type() const { return m_window_type; }
    bool is_minimized() const { return m_minimized; }
    bool is_frameless() const { return m_frameless; }
    int progress() const { return m_progress; }

private:
    int m_parent_client_id;
    int m_parent_window_id;
    String m_title;
    Gfx::IntRect m_rect;
    WindowType m_window_type;
    bool m_active;
    bool m_modal;
    bool m_minimized;
    bool m_frameless;
    int m_progress;
};

class WMWindowRectChangedEvent : public WMEvent {
public:
    WMWindowRectChangedEvent(int client_id, int window_id, const Gfx::IntRect& rect)
        : WMEvent(Event::Type::WM_WindowRectChanged, client_id, window_id)
        , m_rect(rect)
    {
    }

    const Gfx::IntRect& rect() const { return m_rect; }

private:
    Gfx::IntRect m_rect;
};

class WMWindowIconBitmapChangedEvent : public WMEvent {
public:
    WMWindowIconBitmapChangedEvent(int client_id, int window_id, int icon_buffer_id, const Gfx::IntSize& icon_size)
        : WMEvent(Event::Type::WM_WindowIconBitmapChanged, client_id, window_id)
        , m_icon_buffer_id(icon_buffer_id)
        , m_icon_size(icon_size)
    {
    }

    int icon_buffer_id() const { return m_icon_buffer_id; }
    const Gfx::IntSize& icon_size() const { return m_icon_size; }

private:
    int m_icon_buffer_id;
    Gfx::IntSize m_icon_size;
};

class MultiPaintEvent final : public Event {
public:
    explicit MultiPaintEvent(const Vector<Gfx::IntRect, 32>& rects, const Gfx::IntSize& window_size)
        : Event(Event::MultiPaint)
        , m_rects(rects)
        , m_window_size(window_size)
    {
    }

    const Vector<Gfx::IntRect, 32>& rects() const { return m_rects; }
    const Gfx::IntSize& window_size() const { return m_window_size; }

private:
    Vector<Gfx::IntRect, 32> m_rects;
    Gfx::IntSize m_window_size;
};

class PaintEvent final : public Event {
public:
    explicit PaintEvent(const Gfx::IntRect& rect, const Gfx::IntSize& window_size = {})
        : Event(Event::Paint)
        , m_rect(rect)
        , m_window_size(window_size)
    {
    }

    const Gfx::IntRect& rect() const { return m_rect; }
    const Gfx::IntSize& window_size() const { return m_window_size; }

private:
    Gfx::IntRect m_rect;
    Gfx::IntSize m_window_size;
};

class ResizeEvent final : public Event {
public:
    explicit ResizeEvent(const Gfx::IntSize& size)
        : Event(Event::Resize)
        , m_size(size)
    {
    }

    const Gfx::IntSize& size() const { return m_size; }

private:
    Gfx::IntSize m_size;
};

class ContextMenuEvent final : public Event {
public:
    explicit ContextMenuEvent(const Gfx::IntPoint& position, const Gfx::IntPoint& screen_position)
        : Event(Event::ContextMenu)
        , m_position(position)
        , m_screen_position(screen_position)
    {
    }

    const Gfx::IntPoint& position() const { return m_position; }
    const Gfx::IntPoint& screen_position() const { return m_screen_position; }

private:
    Gfx::IntPoint m_position;
    Gfx::IntPoint m_screen_position;
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
    Back = 8,
    Forward = 16,
};

class KeyEvent final : public Event {
public:
    KeyEvent(Type type, KeyCode key, u8 modifiers, u32 code_point, u32 scancode)
        : Event(type)
        , m_key(key)
        , m_modifiers(modifiers)
        , m_code_point(code_point)
        , m_scancode(scancode)
    {
    }

    KeyCode key() const { return m_key; }
    bool ctrl() const { return m_modifiers & Mod_Ctrl; }
    bool alt() const { return m_modifiers & Mod_Alt; }
    bool shift() const { return m_modifiers & Mod_Shift; }
    bool logo() const { return m_modifiers & Mod_Logo; }
    u8 modifiers() const { return m_modifiers; }
    u32 code_point() const { return m_code_point; }
    String text() const
    {
        StringBuilder sb;
        sb.append_code_point(m_code_point);
        return sb.to_string();
    }
    u32 scancode() const { return m_scancode; }

    String to_string() const;

private:
    friend class WindowServerConnection;
    KeyCode m_key { KeyCode::Key_Invalid };
    u8 m_modifiers { 0 };
    u32 m_code_point { 0 };
    u32 m_scancode { 0 };
};

class MouseEvent final : public Event {
public:
    MouseEvent(Type type, const Gfx::IntPoint& position, unsigned buttons, MouseButton button, unsigned modifiers, int wheel_delta)
        : Event(type)
        , m_position(position)
        , m_buttons(buttons)
        , m_button(button)
        , m_modifiers(modifiers)
        , m_wheel_delta(wheel_delta)
    {
    }

    const Gfx::IntPoint& position() const { return m_position; }
    int x() const { return m_position.x(); }
    int y() const { return m_position.y(); }
    MouseButton button() const { return m_button; }
    unsigned buttons() const { return m_buttons; }
    bool ctrl() const { return m_modifiers & Mod_Ctrl; }
    bool alt() const { return m_modifiers & Mod_Alt; }
    bool shift() const { return m_modifiers & Mod_Shift; }
    bool logo() const { return m_modifiers & Mod_Logo; }
    unsigned modifiers() const { return m_modifiers; }
    int wheel_delta() const { return m_wheel_delta; }

private:
    Gfx::IntPoint m_position;
    unsigned m_buttons { 0 };
    MouseButton m_button { MouseButton::None };
    unsigned m_modifiers { 0 };
    int m_wheel_delta { 0 };
};

class DragEvent final : public Event {
public:
    DragEvent(Type type, const Gfx::IntPoint& position, const String& data_type)
        : Event(type)
        , m_position(position)
        , m_data_type(data_type)
    {
    }

    const Gfx::IntPoint& position() const { return m_position; }
    const String& data_type() const { return m_data_type; }

private:
    Gfx::IntPoint m_position;
    String m_data_type;
};

class DropEvent final : public Event {
public:
    DropEvent(const Gfx::IntPoint&, const String& text, NonnullRefPtr<Core::MimeData> mime_data);

    ~DropEvent();

    const Gfx::IntPoint& position() const { return m_position; }
    const String& text() const { return m_text; }
    const Core::MimeData& mime_data() const { return m_mime_data; }

private:
    Gfx::IntPoint m_position;
    String m_text;
    NonnullRefPtr<Core::MimeData> m_mime_data;
};

class ThemeChangeEvent final : public Event {
public:
    ThemeChangeEvent()
        : Event(Type::ThemeChange)
    {
    }
};

class FocusEvent final : public Event {
public:
    explicit FocusEvent(Type type, FocusSource source)
        : Event(type)
        , m_source(source)
    {
    }

    FocusSource source() const { return m_source; }

private:
    FocusSource m_source { FocusSource::Programmatic };
};

}
