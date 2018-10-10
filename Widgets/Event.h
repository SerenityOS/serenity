#pragma once

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
    };

    Event() { }
    ~Event() { }

    Type type() const { return m_type; }

    const char* name() const { return eventNames[(unsigned)m_type]; }

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
    Right,
};

class KeyEvent : public Event {
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

class MouseEvent : public Event {
public:
    MouseEvent(Type type, int x, int y, MouseButton button = MouseButton::None)
        : Event(type)
        , m_x(x)
        , m_y(y)
        , m_button(button)
    {
    }

    int x() const { return m_x; }
    int y() const { return m_y; }
    MouseButton button() const { return m_button; }

private:
    int m_x { 0 };
    int m_y { 0 };
    MouseButton m_button { MouseButton::None };
};

