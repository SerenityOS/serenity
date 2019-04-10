#pragma once

#include <LibCore/CEvent.h>
#include <SharedGraphics/Point.h>
#include <SharedGraphics/Rect.h>
#include <Kernel/KeyCode.h>
#include <LibGUI/GWindowType.h>

class GObject;

class GEvent : public CEvent{
public:
    enum Type {
        Show = 1000,
        Hide,
        Paint,
        Resize,
        MouseMove,
        MouseDown,
        MouseUp,
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
        WM_WindowRemoved,
        WM_WindowStateChanged,
    };

    GEvent() { }
    explicit GEvent(Type type) : CEvent(type) { }
    virtual ~GEvent() { }

    bool is_mouse_event() const { return type() == MouseMove || type() == MouseDown || type() == MouseUp; }
    bool is_key_event() const { return type() == KeyUp || type() == KeyDown; }
    bool is_paint_event() const { return type() == Paint; }
};

class GWMEvent : public GEvent {
public:
    GWMEvent(Type type, int client_id, int window_id)
        : GEvent(type)
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

class GWMWindowRemovedEvent : public GWMEvent {
public:
    GWMWindowRemovedEvent(int client_id, int window_id)
        : GWMEvent(GEvent::Type::WM_WindowRemoved, client_id, window_id)
    {
    }
};

class GWMWindowStateChangedEvent : public GWMEvent {
public:
    GWMWindowStateChangedEvent(int client_id, int window_id, const String& title, const Rect& rect, bool is_active, GWindowType window_type, bool is_minimized)
        : GWMEvent(GEvent::Type::WM_WindowStateChanged, client_id, window_id)
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
    GWindowType window_type() const { return m_window_type; }
    bool is_minimized() const { return m_minimized; }

private:
    String m_title;
    Rect m_rect;
    bool m_active;
    GWindowType m_window_type;
    bool m_minimized;
};

class GPaintEvent final : public GEvent {
public:
    explicit GPaintEvent(const Rect& rect, const Size& window_size = Size())
        : GEvent(GEvent::Paint)
        , m_rect(rect)
        , m_window_size(window_size)
    {
    }

    Rect rect() const { return m_rect; }
    Size window_size() const { return m_window_size; }

private:
    Rect m_rect;
    Size m_window_size;
};

class GResizeEvent final : public GEvent {
public:
    explicit GResizeEvent(const Size& old_size, const Size& size)
        : GEvent(GEvent::Resize)
        , m_old_size(old_size)
        , m_size(size)
    {
    }

    const Size& old_size() const { return m_old_size; }
    const Size& size() const { return m_size; }
private:
    Size m_old_size;
    Size m_size;
};

class GShowEvent final : public GEvent {
public:
    GShowEvent()
        : GEvent(GEvent::Show)
    {
    }
};

class GHideEvent final : public GEvent {
public:
    GHideEvent()
        : GEvent(GEvent::Hide)
    {
    }
};

enum GMouseButton : byte {
    None = 0,
    Left = 1,
    Right = 2,
    Middle = 4,
};

class GKeyEvent final : public GEvent {
public:
    GKeyEvent(Type type, int key, byte modifiers)
        : GEvent(type)
        , m_key(key)
        , m_modifiers(modifiers)
    {
    }

    int key() const { return m_key; }
    bool ctrl() const { return m_modifiers & Mod_Ctrl; }
    bool alt() const { return m_modifiers & Mod_Alt; }
    bool shift() const { return m_modifiers & Mod_Shift; }
    bool logo() const { return m_modifiers & Mod_Logo; }
    byte modifiers() const { return m_modifiers; }
    String text() const { return m_text; }

private:
    friend class GEventLoop;
    int m_key { 0 };
    byte m_modifiers { 0 };
    String m_text;
};

class GMouseEvent final : public GEvent {
public:
    GMouseEvent(Type type, const Point& position, unsigned buttons, GMouseButton button, unsigned modifiers)
        : GEvent(type)
        , m_position(position)
        , m_buttons(buttons)
        , m_button(button)
        , m_modifiers(modifiers)
    {
    }

    Point position() const { return m_position; }
    int x() const { return m_position.x(); }
    int y() const { return m_position.y(); }
    GMouseButton button() const { return m_button; }
    unsigned buttons() const { return m_buttons; }
    unsigned modifiers() const { return m_modifiers; }

private:
    Point m_position;
    unsigned m_buttons { 0 };
    GMouseButton m_button { GMouseButton::None };
    unsigned m_modifiers { 0 };
};
