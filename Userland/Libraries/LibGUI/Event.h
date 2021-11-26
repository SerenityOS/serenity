/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <Kernel/API/KeyCode.h>
#include <LibCore/Event.h>
#include <LibGUI/FocusSource.h>
#include <LibGUI/Forward.h>
#include <LibGUI/WindowType.h>
#include <LibGfx/Bitmap.h>
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
        DragEnter,
        DragLeave,
        DragMove,
        Drop,
        ThemeChange,
        FontsChange,
        ScreenRectsChange,
        ActionEnter,
        ActionLeave,
        AppletAreaRectChange,

        __Begin_WM_Events,
        WM_WindowRemoved,
        WM_WindowStateChanged,
        WM_WindowRectChanged,
        WM_WindowIconBitmapChanged,
        WM_AppletAreaSizeChanged,
        WM_SuperKeyPressed,
        WM_SuperSpaceKeyPressed,
        WM_WorkspaceChanged,
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

class WMSuperKeyPressedEvent : public WMEvent {
public:
    explicit WMSuperKeyPressedEvent(int client_id)
        : WMEvent(Event::Type::WM_SuperKeyPressed, client_id, 0)
    {
    }
};

class WMSuperSpaceKeyPressedEvent : public WMEvent {
public:
    explicit WMSuperSpaceKeyPressedEvent(int client_id)
        : WMEvent(Event::Type::WM_SuperSpaceKeyPressed, client_id, 0)
    {
    }
};

class WMAppletAreaSizeChangedEvent : public WMEvent {
public:
    explicit WMAppletAreaSizeChangedEvent(const Gfx::IntSize& size)
        : WMEvent(Event::Type::WM_AppletAreaSizeChanged, 0, 0)
        , m_size(size)
    {
    }

    const Gfx::IntSize& size() const { return m_size; }

private:
    Gfx::IntSize m_size;
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
    WMWindowStateChangedEvent(int client_id, int window_id, int parent_client_id, int parent_window_id, StringView title, const Gfx::IntRect& rect, unsigned workspace_row, unsigned workspace_column, bool is_active, bool is_modal, WindowType window_type, bool is_minimized, bool is_frameless, Optional<int> progress)
        : WMEvent(Event::Type::WM_WindowStateChanged, client_id, window_id)
        , m_parent_client_id(parent_client_id)
        , m_parent_window_id(parent_window_id)
        , m_title(title)
        , m_rect(rect)
        , m_window_type(window_type)
        , m_workspace_row(workspace_row)
        , m_workspace_column(workspace_column)
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
    Optional<int> progress() const { return m_progress; }
    unsigned workspace_row() const { return m_workspace_row; }
    unsigned workspace_column() const { return m_workspace_column; }

private:
    int m_parent_client_id;
    int m_parent_window_id;
    String m_title;
    Gfx::IntRect m_rect;
    WindowType m_window_type;
    unsigned m_workspace_row;
    unsigned m_workspace_column;
    bool m_active;
    bool m_modal;
    bool m_minimized;
    bool m_frameless;
    Optional<int> m_progress;
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
    WMWindowIconBitmapChangedEvent(int client_id, int window_id, const Gfx::Bitmap* bitmap)
        : WMEvent(Event::Type::WM_WindowIconBitmapChanged, client_id, window_id)
        , m_bitmap(move(bitmap))
    {
    }

    const Gfx::Bitmap* bitmap() const { return m_bitmap; }

private:
    RefPtr<Gfx::Bitmap> m_bitmap;
};

class WMWorkspaceChangedEvent : public WMEvent {
public:
    explicit WMWorkspaceChangedEvent(int client_id, unsigned current_row, unsigned current_column)
        : WMEvent(Event::Type::WM_WorkspaceChanged, client_id, 0)
        , m_current_row(current_row)
        , m_current_column(current_column)
    {
    }

    unsigned current_row() const { return m_current_row; }
    unsigned current_column() const { return m_current_column; }

private:
    const unsigned m_current_row;
    const unsigned m_current_column;
};

class MultiPaintEvent final : public Event {
public:
    explicit MultiPaintEvent(Vector<Gfx::IntRect, 32> rects, Gfx::IntSize const& window_size)
        : Event(Event::MultiPaint)
        , m_rects(move(rects))
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
    Primary = 1,
    Secondary = 2,
    Middle = 4,
    Backward = 8,
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
    bool super() const { return m_modifiers & Mod_Super; }
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

    bool is_arrow_key() const
    {
        switch (m_key) {
        case KeyCode::Key_Up:
        case KeyCode::Key_Down:
        case KeyCode::Key_Left:
        case KeyCode::Key_Right:
            return true;
        default:
            return false;
        }
    }

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
    bool super() const { return m_modifiers & Mod_Super; }
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
    DragEvent(Type type, const Gfx::IntPoint& position, Vector<String> mime_types)
        : Event(type)
        , m_position(position)
        , m_mime_types(move(mime_types))
    {
    }

    const Gfx::IntPoint& position() const { return m_position; }
    const Vector<String>& mime_types() const { return m_mime_types; }

private:
    Gfx::IntPoint m_position;
    Vector<String> m_mime_types;
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

class FontsChangeEvent final : public Event {
public:
    FontsChangeEvent()
        : Event(Type::FontsChange)
    {
    }
};

class ScreenRectsChangeEvent final : public Event {
public:
    explicit ScreenRectsChangeEvent(const Vector<Gfx::IntRect, 4>& rects, size_t main_screen_index)
        : Event(Type::ScreenRectsChange)
        , m_rects(rects)
        , m_main_screen_index(main_screen_index)
    {
    }

    const Vector<Gfx::IntRect, 4>& rects() const { return m_rects; }
    size_t main_screen_index() const { return m_main_screen_index; }

private:
    Vector<Gfx::IntRect, 4> m_rects;
    size_t m_main_screen_index;
};

class AppletAreaRectChangeEvent final : public Event {
public:
    explicit AppletAreaRectChangeEvent(Gfx::IntRect rect)
        : Event(Type::AppletAreaRectChange)
        , m_rect(rect)
    {
    }

    Gfx::IntRect rect() const { return m_rect; }

private:
    Gfx::IntRect const m_rect;
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

class ActionEvent final : public Event {
public:
    ActionEvent(Type, Action&);
    ~ActionEvent();

    Action const& action() const { return *m_action; }
    Action& action() { return *m_action; }

private:
    NonnullRefPtr<Action> m_action;
};

}
