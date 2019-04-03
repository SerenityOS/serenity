#pragma once

#include <SharedGraphics/Point.h>
#include <SharedGraphics/Rect.h>
#include <AK/AKString.h>
#include <AK/Types.h>
#include <AK/WeakPtr.h>
#include <Kernel/KeyCode.h>

class GObject;

class GEvent {
public:
    enum Type {
        Invalid = 0,
        Quit,
        Show,
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
        Timer,
        DeferredDestroy,
        WindowEntered,
        WindowLeft,
        WindowBecameInactive,
        WindowBecameActive,
        FocusIn,
        FocusOut,
        WindowCloseRequest,
        ChildAdded,
        ChildRemoved,
        WM_WindowAdded,
        WM_WindowRemoved,
        WM_WindowStateChanged,
    };

    GEvent() { }
    explicit GEvent(Type type) : m_type(type) { }
    virtual ~GEvent() { }

    Type type() const { return m_type; }

    bool is_mouse_event() const { return m_type == MouseMove || m_type == MouseDown || m_type == MouseUp; }
    bool is_key_event() const { return m_type == KeyUp || m_type == KeyDown; }
    bool is_paint_event() const { return m_type == Paint; }

private:
    Type m_type { Invalid };
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

class GWMWindowAddedEvent : public GWMEvent {
public:
    GWMWindowAddedEvent(int client_id, int window_id, const String& title, const Rect& rect)
        : GWMEvent(GEvent::Type::WM_WindowAdded, client_id, window_id)
        , m_title(title)
        , m_rect(rect)
    {
    }

    String title() const { return m_title; }
    Rect rect() const { return m_rect; }

private:
    String m_title;
    Rect m_rect;
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
    GWMWindowStateChangedEvent(int client_id, int window_id, const String& title, const Rect& rect)
        : GWMEvent(GEvent::Type::WM_WindowStateChanged, client_id, window_id)
        , m_title(title)
        , m_rect(rect)
    {
    }

    String title() const { return m_title; }
    Rect rect() const { return m_rect; }

private:
    String m_title;
    Rect m_rect;
};

class QuitEvent final : public GEvent {
public:
    QuitEvent()
        : GEvent(GEvent::Quit)
    {
    }
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

class GTimerEvent final : public GEvent {
public:
    explicit GTimerEvent(int timer_id) : GEvent(GEvent::Timer), m_timer_id(timer_id) { }
    ~GTimerEvent() { }

    int timer_id() const { return m_timer_id; }

private:
    int m_timer_id;
};

class GChildEvent final : public GEvent {
public:
    GChildEvent(Type, GObject& child);
    ~GChildEvent();

    GObject* child() { return m_child.ptr(); }
    const GObject* child() const { return m_child.ptr(); }

private:
    WeakPtr<GObject> m_child;
};
