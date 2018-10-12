#pragma once

#include <AK/Types.h>
#include "Point.h"

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

class QuitEvent final : public Event {
public:
    QuitEvent()
        : Event(Event::Quit)
    {
    }
};

class PaintEvent final : public Event {
public:
    PaintEvent()
        : Event(Event::Paint)
    {
    }
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

class KeyEvent final : public Event {
public:
    KeyEvent(Type type, int key)
        : Event(type)
        , m_key(key)
    {
    }

    int key() const { return m_key; }

private:
    int m_key { 0 };
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

