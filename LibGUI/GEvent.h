#pragma once

#include <SharedGraphics/Point.h>
#include <SharedGraphics/Rect.h>
#include <AK/AKString.h>
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

class GEvent {
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
        WindowBecameInactive,
        WindowBecameActive,
    };

    GEvent() { }
    explicit GEvent(Type type) : m_type(type) { }
    virtual ~GEvent() { }

    Type type() const { return m_type; }

    const char* name() const { return eventNames[(unsigned)m_type]; }

    bool isMouseEvent() const { return m_type == MouseMove || m_type == MouseDown || m_type == MouseUp; }
    bool isKeyEvent() const { return m_type == KeyUp || m_type == KeyDown; }
    bool isPaintEvent() const { return m_type == Paint; }

private:
    Type m_type { Invalid };
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
    explicit GPaintEvent(const Rect& rect = Rect())
        : GEvent(GEvent::Paint)
        , m_rect(rect)
    {
    }

    const Rect& rect() const { return m_rect; }
private:
    Rect m_rect;
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

enum class GMouseButton : byte {
    None = 0,
    Left,
    Middle,
    Right,
};

enum GKeyboardKey {
    Invalid,
    LeftArrow,
    RightArrow,
    UpArrow,
    DownArrow,
    Backspace,
    Return,
};

class GKeyEvent final : public GEvent {
public:
    GKeyEvent(Type type, int key)
        : GEvent(type)
        , m_key(key)
    {
    }

    int key() const { return m_key; }
    bool ctrl() const { return m_ctrl; }
    bool alt() const { return m_alt; }
    bool shift() const { return m_shift; }
    String text() const { return m_text; }

private:
    friend class GEventLoop;
    int m_key { 0 };
    bool m_ctrl { false };
    bool m_alt { false };
    bool m_shift { false };
    String m_text;
};

class GMouseEvent final : public GEvent {
public:
    GMouseEvent(Type type, const Point& position, GMouseButton button = GMouseButton::None)
        : GEvent(type)
        , m_position(position)
        , m_button(button)
    {
    }

    Point position() const { return m_position; }
    int x() const { return m_position.x(); }
    int y() const { return m_position.y(); }
    GMouseButton button() const { return m_button; }

private:
    Point m_position;
    GMouseButton m_button { GMouseButton::None };
};

class GTimerEvent final : public GEvent {
public:
    GTimerEvent() : GEvent(GEvent::Timer) { }
    ~GTimerEvent() { }
};

