#pragma once

#include "Point.h"
#include "Rect.h"
#include <AK/String.h>
#include <AK/Types.h>

static const char* eventNames[] = {
    "Invalid",
    "Quit",
    "Show",
    "Hide",
    "Paint",
    "MouseMove",
    "MouseDown",
    "MouseUp",
    "KeyDown",
    "KeyUp",
    "Timer",
    "DeferredDestroy",
};

class Event {
public:
    enum Type {
        Invalid = 0,
        Quit,
        Show,
        Hide,
        Paint,
        MouseMove,
        MouseDown,
        MouseUp,
        KeyDown,
        KeyUp,
        Timer,
        DeferredDestroy,
    };

    Event() { }
    ~Event() { }

    Type type() const { return m_type; }

    const char* name() const { return eventNames[(unsigned)m_type]; }

    bool isMouseEvent() const { return m_type == MouseMove || m_type == MouseDown || m_type == MouseUp; }
    bool isKeyEvent() const { return m_type == KeyUp || m_type == KeyDown; }
    bool isPaintEvent() const { return m_type == Paint; }

protected:
    explicit Event(Type type) : m_type(type) { }

private:
    Type m_type { Invalid };
};

class DeferredDestroyEvent final : public Event {
public:
    DeferredDestroyEvent()
        : Event(Event::DeferredDestroy)
    {
    }
};

class QuitEvent final : public Event {
public:
    QuitEvent()
        : Event(Event::Quit)
    {
    }
};

class PaintEvent final : public Event {
public:
    explicit PaintEvent(const Rect& rect = Rect())
        : Event(Event::Paint)
        , m_rect(rect)
    {
    }

    const Rect& rect() const { return m_rect; }
private:
    friend class WindowManager;
    Rect m_rect;
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

enum class MouseButton : byte {
    None = 0,
    Left,
    Middle,
    Right,
};

enum KeyboardKey {
    Invalid,
    LeftArrow,
    RightArrow,
    UpArrow,
    DownArrow,
    Backspace,
    Return,
};

class KeyEvent final : public Event {
public:
    KeyEvent(Type type, int key)
        : Event(type)
        , m_key(key)
    {
    }

    int key() const { return m_key; }
    bool ctrl() const { return m_ctrl; }
    bool alt() const { return m_alt; }
    bool shift() const { return m_shift; }
    String text() const { return m_text; }

private:
    friend class EventLoopSDL;
    int m_key { 0 };
    bool m_ctrl { false };
    bool m_alt { false };
    bool m_shift { false };
    String m_text;
};

class MouseEvent final : public Event {
public:
    MouseEvent(Type type, int x, int y, MouseButton button = MouseButton::None)
        : Event(type)
        , m_position(x, y)
        , m_button(button)
    {
    }

    Point position() const { return m_position; }
    int x() const { return m_position.x(); }
    int y() const { return m_position.y(); }
    MouseButton button() const { return m_button; }

private:
    Point m_position;
    MouseButton m_button { MouseButton::None };
};

class TimerEvent final : public Event {
public:
    TimerEvent() : Event(Event::Timer) { }
    ~TimerEvent() { }
};

