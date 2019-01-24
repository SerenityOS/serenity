#pragma once

#include <SharedGraphics/Point.h>
#include <SharedGraphics/Rect.h>
#include <AK/AKString.h>
#include <AK/Types.h>

static const char* WSEvent_names[] = {
    "Invalid",
    "Show",
    "Hide",
    "Paint",
    "MouseMove",
    "MouseDown",
    "MouseUp",
    "KeyDown",
    "KeyUp",
    "Timer",
    "WM_Compose",
    "WM_Invalidate",
    "WindowActivated",
    "WindowDeactivated",
};

class WSEvent {
public:
    enum Type {
        Invalid = 0,
        Show,
        Hide,
        Paint,
        MouseMove,
        MouseDown,
        MouseUp,
        KeyDown,
        KeyUp,
        Timer,
        WM_Compose,
        WM_Invalidate,
        WindowActivated,
        WindowDeactivated,
        WM_SetWindowTitle,
        WM_SetWindowRect,
    };

    WSEvent() { }
    explicit WSEvent(Type type) : m_type(type) { }
    virtual ~WSEvent() { }

    Type type() const { return m_type; }

    const char* name() const { return WSEvent_names[(unsigned)m_type]; }

    bool is_mouse_event() const { return m_type == MouseMove || m_type == MouseDown || m_type == MouseUp; }
    bool is_key_event() const { return m_type == KeyUp || m_type == KeyDown; }
    bool is_paint_event() const { return m_type == Paint; }

private:
    Type m_type { Invalid };
};

class WSWindowInvalidationEvent final : public WSEvent {
public:
    explicit WSWindowInvalidationEvent(const Rect& rect = Rect())
        : WSEvent(WSEvent::WM_Invalidate)
        , m_rect(rect)
    {
    }

    const Rect& rect() const { return m_rect; }
private:
    Rect m_rect;
};

class WSSetWindowTitle final : public WSEvent {
public:
    explicit WSSetWindowTitle(String&& title)
        : WSEvent(WSEvent::WM_SetWindowTitle)
        , m_title(move(title))
    {
    }

    String title() const { return m_title; }

private:
    String m_title;
};

class WSSetWindowRect final : public WSEvent {
public:
    explicit WSSetWindowRect(const Rect& rect)
        : WSEvent(WSEvent::WM_SetWindowRect)
        , m_rect(rect)
    {
    }

    Rect rect() const { return m_rect; }

private:
    Rect m_rect;
};

class WSPaintEvent final : public WSEvent {
public:
    explicit WSPaintEvent(const Rect& rect = Rect())
        : WSEvent(WSEvent::Paint)
        , m_rect(rect)
    {
    }

    const Rect& rect() const { return m_rect; }
private:
    friend class WSWindowManager;
    Rect m_rect;
};

enum class MouseButton : byte {
    None = 0,
    Left = 1,
    Right = 2,
    Middle = 4,
};

class WSKeyEvent final : public WSEvent {
public:
    WSKeyEvent(Type type, int key, char character)
        : WSEvent(type)
        , m_key(key)
        , m_character(character)
    {
    }

    int key() const { return m_key; }
    bool ctrl() const { return m_ctrl; }
    bool alt() const { return m_alt; }
    bool shift() const { return m_shift; }
    char character() const { return m_character; }

private:
    friend class WSEventLoop;
    friend class WSScreen;
    int m_key { 0 };
    bool m_ctrl { false };
    bool m_alt { false };
    bool m_shift { false };
    char m_character { 0 };
};

class WSMouseEvent final : public WSEvent {
public:
    WSMouseEvent(Type type, const Point& position, unsigned buttons, MouseButton button = MouseButton::None)
        : WSEvent(type)
        , m_position(position)
        , m_buttons(buttons)
        , m_button(button)
    {
    }

    Point position() const { return m_position; }
    int x() const { return m_position.x(); }
    int y() const { return m_position.y(); }
    MouseButton button() const { return m_button; }
    unsigned buttons() const { return m_buttons; }

private:
    Point m_position;
    unsigned m_buttons { 0 };
    MouseButton m_button { MouseButton::None };
};
