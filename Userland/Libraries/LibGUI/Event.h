/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/String.h>
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
        Move,
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
        WindowInputPreempted,
        WindowInputRestored,
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
        WM_SuperDKeyPressed,
        WM_SuperDigitKeyPressed,
        WM_WorkspaceChanged,
        WM_KeymapChanged,
        WM_AddToQuickLaunch,
        __End_WM_Events,
    };

    Event() = default;
    explicit Event(Type type)
        : Core::Event(type)
    {
    }
    virtual ~Event() = default;

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

class WMSuperDKeyPressedEvent : public WMEvent {
public:
    explicit WMSuperDKeyPressedEvent(int client_id)
        : WMEvent(Event::Type::WM_SuperDKeyPressed, client_id, 0)
    {
    }
};

class WMSuperDigitKeyPressedEvent : public WMEvent {
public:
    WMSuperDigitKeyPressedEvent(int client_id, u8 digit)
        : WMEvent(Event::Type::WM_SuperDigitKeyPressed, client_id, 0)
        , m_digit(digit)
    {
    }

    u8 digit() const { return m_digit; }

private:
    u8 m_digit { 0 };
};

class WMAppletAreaSizeChangedEvent : public WMEvent {
public:
    explicit WMAppletAreaSizeChangedEvent(Gfx::IntSize size)
        : WMEvent(Event::Type::WM_AppletAreaSizeChanged, 0, 0)
        , m_size(size)
    {
    }

    Gfx::IntSize size() const { return m_size; }

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
    WMWindowStateChangedEvent(int client_id, int window_id, StringView title, Gfx::IntRect const& rect, unsigned workspace_row, unsigned workspace_column, bool is_active, bool is_blocked, WindowType window_type, bool is_minimized, bool is_frameless, Optional<int> progress)
        : WMEvent(Event::Type::WM_WindowStateChanged, client_id, window_id)
        , m_title(title)
        , m_rect(rect)
        , m_window_type(window_type)
        , m_workspace_row(workspace_row)
        , m_workspace_column(workspace_column)
        , m_active(is_active)
        , m_blocked(is_blocked)
        , m_minimized(is_minimized)
        , m_frameless(is_frameless)
        , m_progress(progress)
    {
    }

    ByteString const& title() const { return m_title; }
    Gfx::IntRect const& rect() const { return m_rect; }
    bool is_active() const { return m_active; }
    bool is_blocked() const { return m_blocked; }
    WindowType window_type() const { return m_window_type; }
    bool is_minimized() const { return m_minimized; }
    bool is_frameless() const { return m_frameless; }
    Optional<int> progress() const { return m_progress; }
    unsigned workspace_row() const { return m_workspace_row; }
    unsigned workspace_column() const { return m_workspace_column; }

private:
    ByteString m_title;
    Gfx::IntRect m_rect;
    WindowType m_window_type;
    unsigned m_workspace_row;
    unsigned m_workspace_column;
    bool m_active;
    bool m_blocked;
    bool m_minimized;
    bool m_frameless;
    Optional<int> m_progress;
};

class WMWindowRectChangedEvent : public WMEvent {
public:
    WMWindowRectChangedEvent(int client_id, int window_id, Gfx::IntRect const& rect)
        : WMEvent(Event::Type::WM_WindowRectChanged, client_id, window_id)
        , m_rect(rect)
    {
    }

    Gfx::IntRect const& rect() const { return m_rect; }

private:
    Gfx::IntRect m_rect;
};

class WMWindowIconBitmapChangedEvent : public WMEvent {
public:
    WMWindowIconBitmapChangedEvent(int client_id, int window_id, Gfx::Bitmap const* bitmap)
        : WMEvent(Event::Type::WM_WindowIconBitmapChanged, client_id, window_id)
        , m_bitmap(move(bitmap))
    {
    }

    Gfx::Bitmap const* bitmap() const { return m_bitmap; }

private:
    RefPtr<Gfx::Bitmap const> m_bitmap;
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
    unsigned const m_current_row;
    unsigned const m_current_column;
};

class WMKeymapChangedEvent : public WMEvent {
public:
    explicit WMKeymapChangedEvent(int client_id, ByteString const& keymap)
        : WMEvent(Event::Type::WM_KeymapChanged, client_id, 0)
        , m_keymap(keymap)
    {
    }

    ByteString const& keymap() const { return m_keymap; }

private:
    ByteString const m_keymap;
};

class WMAddToQuickLaunchEvent : public WMEvent {
public:
    explicit WMAddToQuickLaunchEvent(int client_id, pid_t pid)
        : WMEvent(Event::Type::WM_AddToQuickLaunch, client_id, 0)
        , m_pid(pid)
    {
    }

    pid_t pid() const { return m_pid; }

private:
    pid_t m_pid;
};

class MultiPaintEvent final : public Event {
public:
    explicit MultiPaintEvent(Vector<Gfx::IntRect, 32> rects, Gfx::IntSize window_size)
        : Event(Event::MultiPaint)
        , m_rects(move(rects))
        , m_window_size(window_size)
    {
    }

    Vector<Gfx::IntRect, 32> const& rects() const { return m_rects; }
    Gfx::IntSize window_size() const { return m_window_size; }

private:
    Vector<Gfx::IntRect, 32> m_rects;
    Gfx::IntSize m_window_size;
};

class PaintEvent final : public Event {
public:
    explicit PaintEvent(Gfx::IntRect const& rect, Gfx::IntSize window_size = {})
        : Event(Event::Paint)
        , m_rect(rect)
        , m_window_size(window_size)
    {
    }

    Gfx::IntRect const& rect() const { return m_rect; }
    Gfx::IntSize window_size() const { return m_window_size; }

private:
    Gfx::IntRect m_rect;
    Gfx::IntSize m_window_size;
};

class ResizeEvent final : public Event {
public:
    explicit ResizeEvent(Gfx::IntSize size)
        : Event(Event::Resize)
        , m_size(size)
    {
    }

    Gfx::IntSize size() const { return m_size; }

private:
    Gfx::IntSize m_size;
};

class MoveEvent final : public Event {
public:
    explicit MoveEvent(Gfx::IntPoint size)
        : Event(Event::Move)
        , m_position(size)
    {
    }

    Gfx::IntPoint position() const { return m_position; }

private:
    Gfx::IntPoint m_position;
};

class ContextMenuEvent final : public Event {
public:
    explicit ContextMenuEvent(Gfx::IntPoint position, Gfx::IntPoint screen_position)
        : Event(Event::ContextMenu)
        , m_position(position)
        , m_screen_position(screen_position)
    {
    }

    Gfx::IntPoint position() const { return m_position; }
    Gfx::IntPoint screen_position() const { return m_screen_position; }

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

AK_ENUM_BITWISE_OPERATORS(MouseButton);

class KeyEvent final : public Event {
public:
    KeyEvent(Type type, KeyCode key, u8 map_entry_index, u8 modifiers, u32 code_point, u32 scancode)
        : Event(type)
        , m_key(key)
        , m_map_entry_index(map_entry_index)
        , m_modifiers(modifiers)
        , m_code_point(code_point)
        , m_scancode(scancode)
    {
    }

    KeyCode key() const { return m_key; }
    bool ctrl() const { return m_modifiers & Mod_Ctrl; }
    bool alt() const { return m_modifiers & Mod_Alt; }
    bool altgr() const { return m_modifiers & Mod_AltGr; }
    bool shift() const { return m_modifiers & Mod_Shift; }
    bool super() const { return m_modifiers & Mod_Super; }
    u8 modifiers() const { return m_modifiers; }
    u32 code_point() const { return m_code_point; }
    ByteString text() const
    {
        StringBuilder sb;
        sb.append_code_point(m_code_point);
        return sb.to_byte_string();
    }
    u32 scancode() const { return m_scancode; }

    u8 map_entry_index() const { return m_map_entry_index; }

    ByteString to_byte_string() const;

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
    friend class ConnectionToWindowServer;
    KeyCode m_key { KeyCode::Key_Invalid };
    u8 m_map_entry_index { 0 };
    u8 m_modifiers { 0 };
    u32 m_code_point { 0 };
    u32 m_scancode { 0 };
};

class MouseEvent final : public Event {
public:
    MouseEvent(Type type, Gfx::IntPoint position, unsigned buttons, MouseButton button, unsigned modifiers, int wheel_delta_x, int wheel_delta_y, int wheel_raw_delta_x, int wheel_raw_delta_y)
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
    bool ctrl() const { return m_modifiers & Mod_Ctrl; }
    bool alt() const { return m_modifiers & Mod_Alt; }
    bool shift() const { return m_modifiers & Mod_Shift; }
    bool super() const { return m_modifiers & Mod_Super; }
    unsigned modifiers() const { return m_modifiers; }
    int wheel_delta_x() const { return m_wheel_delta_x; }
    int wheel_delta_y() const { return m_wheel_delta_y; }
    int wheel_raw_delta_x() const { return m_wheel_raw_delta_x; }
    int wheel_raw_delta_y() const { return m_wheel_raw_delta_y; }

private:
    Gfx::IntPoint m_position;
    unsigned m_buttons { 0 };
    MouseButton m_button { MouseButton::None };
    unsigned m_modifiers { 0 };
    int m_wheel_delta_x { 0 };
    int m_wheel_delta_y { 0 };
    int m_wheel_raw_delta_x { 0 };
    int m_wheel_raw_delta_y { 0 };
};

class DropEvent : public Event {
public:
    DropEvent(Type type, Gfx::IntPoint, MouseButton button, u32 buttons, u32 modifiers, ByteString const& text, NonnullRefPtr<Core::MimeData const> mime_data);
    ~DropEvent();

    Gfx::IntPoint position() const { return m_position; }
    MouseButton button() const { return m_button; }
    unsigned buttons() const { return m_buttons; }
    unsigned modifiers() const { return m_modifiers; }
    ByteString const& text() const { return m_text; }
    Core::MimeData const& mime_data() const { return m_mime_data; }

private:
    Gfx::IntPoint m_position;
    MouseButton m_button { MouseButton::None };
    unsigned m_buttons { 0 };
    unsigned m_modifiers { 0 };
    ByteString m_text;
    NonnullRefPtr<Core::MimeData const> m_mime_data;
};

class DragEvent final : public DropEvent {
public:
    DragEvent(Type type, Gfx::IntPoint, MouseButton button, u32 buttons, u32 modifiers, ByteString const& text, NonnullRefPtr<Core::MimeData const> mime_data);
    ~DragEvent();
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
    explicit ScreenRectsChangeEvent(Vector<Gfx::IntRect, 4> const& rects, size_t main_screen_index)
        : Event(Type::ScreenRectsChange)
        , m_rects(rects)
        , m_main_screen_index(main_screen_index)
    {
    }

    Vector<Gfx::IntRect, 4> const& rects() const { return m_rects; }
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

inline StringView mouse_button_to_string(MouseButton key)
{
    switch (key) {
    case MouseButton::None:
        return "None"sv;
    case MouseButton::Primary:
        return "Primary"sv;
    case MouseButton::Secondary:
        return "Secondary"sv;
    case MouseButton::Middle:
        return "Middle"sv;
    case MouseButton::Backward:
        return "Backward"sv;
    case MouseButton::Forward:
        return "Forward"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

}
