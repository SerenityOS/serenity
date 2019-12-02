#pragma once

#include <AK/String.h>
#include <Kernel/KeyCode.h>
#include <LibCore/CEvent.h>
#include <LibDraw/Rect.h>
#include <WindowServer/WSCursor.h>
#include <WindowServer/WSWindowType.h>

class WSEvent : public CEvent {
public:
    enum Type {
        Invalid = 3000,
        WM_DeferredCompose,
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

        WM_WindowRemoved,
        WM_WindowStateChanged,
        WM_WindowRectChanged,
        WM_WindowIconBitmapChanged,
    };

    WSEvent() {}
    explicit WSEvent(Type type)
        : CEvent(type)
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

class WSWMEvent : public WSEvent {
public:
    WSWMEvent(Type type, int client_id, int window_id)
        : WSEvent(type)
        , m_client_id(client_id)
        , m_window_id(window_id)
    {
    }

    int client_id() const { return m_client_id; }
    int window_id() const { return m_window_id; }

private:
    int m_client_id;
    int m_window_id;
};

class WSWMWindowRemovedEvent : public WSWMEvent {
public:
    WSWMWindowRemovedEvent(int client_id, int window_id)
        : WSWMEvent(WSEvent::WM_WindowRemoved, client_id, window_id)
    {
    }
};

class WSWMWindowStateChangedEvent : public WSWMEvent {
public:
    WSWMWindowStateChangedEvent(int client_id, int window_id, const String& title, const Rect& rect, bool is_active, WSWindowType window_type, bool is_minimized)
        : WSWMEvent(WSEvent::WM_WindowStateChanged, client_id, window_id)
        , m_title(title)
        , m_rect(rect)
        , m_active(is_active)
        , m_window_type(window_type)
        , m_minimized(is_minimized)
    {
    }

    String title() const { return m_title; }
    Rect rect() const { return m_rect; }
    bool is_active() const { return m_active; }
    WSWindowType window_type() const { return m_window_type; }
    bool is_minimized() const { return m_minimized; }

private:
    String m_title;
    Rect m_rect;
    bool m_active;
    WSWindowType m_window_type;
    bool m_minimized;
};

class WSWMWindowIconBitmapChangedEvent : public WSWMEvent {
public:
    WSWMWindowIconBitmapChangedEvent(int client_id, int window_id, int icon_buffer_id, const Size& icon_size)
        : WSWMEvent(WSEvent::WM_WindowIconBitmapChanged, client_id, window_id)
        , m_icon_buffer_id(icon_buffer_id)
        , m_icon_size(icon_size)
    {
    }

    int icon_buffer_id() const { return m_icon_buffer_id; }
    const Size icon_size() const { return m_icon_size; }

private:
    int m_icon_buffer_id;
    Size m_icon_size;
};

class WSWMWindowRectChangedEvent : public WSWMEvent {
public:
    WSWMWindowRectChangedEvent(int client_id, int window_id, const Rect& rect)
        : WSWMEvent(WSEvent::WM_WindowRectChanged, client_id, window_id)
        , m_rect(rect)
    {
    }

    Rect rect() const { return m_rect; }

private:
    Rect m_rect;
};
