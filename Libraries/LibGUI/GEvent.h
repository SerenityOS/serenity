#pragma once

#include <Kernel/KeyCode.h>
#include <LibCore/CEvent.h>
#include <LibDraw/Point.h>
#include <LibDraw/Rect.h>
#include <LibGUI/GWindowType.h>

class CObject;

class GEvent : public CEvent {
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

    GEvent() {}
    explicit GEvent(Type type)
        : CEvent(type)
    {
    }
    virtual ~GEvent() {}

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
    GWMWindowStateChangedEvent(int client_id, int window_id, const StringView& title, const Rect& rect, bool is_active, GWindowType window_type, bool is_minimized)
        : GWMEvent(GEvent::Type::WM_WindowStateChanged, client_id, window_id)
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
    GWindowType window_type() const { return m_window_type; }
    bool is_minimized() const { return m_minimized; }

private:
    String m_title;
    Rect m_rect;
    GWindowType m_window_type;
    bool m_active;
    bool m_minimized;
};

class GWMWindowRectChangedEvent : public GWMEvent {
public:
    GWMWindowRectChangedEvent(int client_id, int window_id, const Rect& rect)
        : GWMEvent(GEvent::Type::WM_WindowRectChanged, client_id, window_id)
        , m_rect(rect)
    {
    }

    Rect rect() const { return m_rect; }

private:
    Rect m_rect;
};

class GWMWindowIconBitmapChangedEvent : public GWMEvent {
public:
    GWMWindowIconBitmapChangedEvent(int client_id, int window_id, int icon_buffer_id, const Size& icon_size)
        : GWMEvent(GEvent::Type::WM_WindowIconBitmapChanged, client_id, window_id)
        , m_icon_buffer_id(icon_buffer_id)
        , m_icon_size(icon_size)
    {
    }

    int icon_buffer_id() const { return m_icon_buffer_id; }
    const Size& icon_size() const { return m_icon_size; }

private:
    int m_icon_buffer_id;
    Size m_icon_size;
};

class GMultiPaintEvent final : public GEvent {
public:
    explicit GMultiPaintEvent(const Vector<Rect, 32>& rects, const Size& window_size)
        : GEvent(GEvent::MultiPaint)
        , m_rects(rects)
        , m_window_size(window_size)
    {
    }

    const Vector<Rect, 32>& rects() const { return m_rects; }
    Size window_size() const { return m_window_size; }

private:
    Vector<Rect, 32> m_rects;
    Size m_window_size;
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

class GContextMenuEvent final : public GEvent {
public:
    explicit GContextMenuEvent(const Point& position, const Point& screen_position)
        : GEvent(GEvent::ContextMenu)
        , m_position(position)
        , m_screen_position(screen_position)
    {
    }

    const Point& position() const { return m_position; }
    const Point& screen_position() const { return m_screen_position; }

private:
    Point m_position;
    Point m_screen_position;
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

enum GMouseButton : u8 {
    None = 0,
    Left = 1,
    Right = 2,
    Middle = 4,
};

class GKeyEvent final : public GEvent {
public:
    GKeyEvent(Type type, int key, u8 modifiers)
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
    u8 modifiers() const { return m_modifiers; }
    String text() const { return m_text; }

private:
    friend class GWindowServerConnection;
    int m_key { 0 };
    u8 m_modifiers { 0 };
    String m_text;
};

class GMouseEvent final : public GEvent {
public:
    GMouseEvent(Type type, const Point& position, unsigned buttons, GMouseButton button, unsigned modifiers, int wheel_delta)
        : GEvent(type)
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
    GMouseButton button() const { return m_button; }
    unsigned buttons() const { return m_buttons; }
    unsigned modifiers() const { return m_modifiers; }
    int wheel_delta() const { return m_wheel_delta; }

private:
    Point m_position;
    unsigned m_buttons { 0 };
    GMouseButton m_button { GMouseButton::None };
    unsigned m_modifiers { 0 };
    int m_wheel_delta { 0 };
};

class GDropEvent final : public GEvent {
public:
    GDropEvent(const Point& position, const String& text, const String& data_type, const String& data)
        : GEvent(GEvent::Drop)
        , m_position(position)
        , m_text(text)
        , m_data_type(data_type)
        , m_data(data)
    {
    }

    const Point& position() const { return m_position; }
    const String& text() const { return m_text; }
    const String& data_type() const { return m_data_type; }
    const String& data() const { return m_data; }

private:
    Point m_position;
    String m_text;
    String m_data_type;
    String m_data;
};
