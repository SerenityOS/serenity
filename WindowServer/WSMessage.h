#pragma once

#include <SharedGraphics/Point.h>
#include <SharedGraphics/Rect.h>
#include <AK/AKString.h>
#include <AK/Types.h>

class WSMessage {
public:
    enum Type {
        Invalid = 0,
        WM_ClientWantsToPaint,
        WM_ClientFinishedPaint,
        WM_SetWindowTitle,
        WM_SetWindowRect,
        WM_DeferredCompose,
        WM_DestroyWindow,
        MouseMove,
        MouseDown,
        MouseUp,
        KeyDown,
        KeyUp,
        WindowActivated,
        WindowDeactivated,
        WindowCloseRequest,

        __Begin_API_Client_Requests,
        APICreateMenubarRequest,
        APIDestroyMenubarRequest,
        __End_API_Client_Requests,
    };

    WSMessage() { }
    explicit WSMessage(Type type) : m_type(type) { }
    virtual ~WSMessage() { }

    Type type() const { return m_type; }

    bool is_client_request() const { return m_type > __Begin_API_Client_Requests && m_type < __End_API_Client_Requests; }
    bool is_mouse_event() const { return m_type == MouseMove || m_type == MouseDown || m_type == MouseUp; }
    bool is_key_event() const { return m_type == KeyUp || m_type == KeyDown; }

private:
    Type m_type { Invalid };
};

class WSAPIClientRequest : public WSMessage {
public:
    WSAPIClientRequest(Type type, int client_id)
        : WSMessage(type)
        , m_client_id(client_id)
    {
    }

    int client_id() const { return m_client_id; }

private:
    int m_client_id { 0 };
};

class WSAPICreateMenubarRequest : public WSAPIClientRequest {
public:
    WSAPICreateMenubarRequest(int client_id)
        : WSAPIClientRequest(WSMessage::APICreateMenubarRequest, client_id)
    {
    }
};

class WSAPIDestroyMenubarRequest : public WSAPIClientRequest {
public:
    WSAPIDestroyMenubarRequest(int client_id, int menubar_id)
        : WSAPIClientRequest(WSMessage::APIDestroyMenubarRequest, client_id)
        , m_menubar_id(menubar_id)
    {
    }

    int menubar_id() const { return m_menubar_id; }

private:
    int m_menubar_id { 0 };
};

class WSClientFinishedPaintMessage final : public WSMessage {
public:
    explicit WSClientFinishedPaintMessage(const Rect& rect = Rect())
        : WSMessage(WSMessage::WM_ClientFinishedPaint)
        , m_rect(rect)
    {
    }

    const Rect& rect() const { return m_rect; }
private:
    Rect m_rect;
};

class WSSetWindowTitleMessage final : public WSMessage {
public:
    explicit WSSetWindowTitleMessage(String&& title)
        : WSMessage(WSMessage::WM_SetWindowTitle)
        , m_title(move(title))
    {
    }

    String title() const { return m_title; }

private:
    String m_title;
};

class WSSetWindowRectMessage final : public WSMessage {
public:
    explicit WSSetWindowRectMessage(const Rect& rect)
        : WSMessage(WSMessage::WM_SetWindowRect)
        , m_rect(rect)
    {
    }

    Rect rect() const { return m_rect; }

private:
    Rect m_rect;
};

class WSClientWantsToPaintMessage final : public WSMessage {
public:
    explicit WSClientWantsToPaintMessage(const Rect& rect = Rect())
        : WSMessage(WSMessage::WM_ClientWantsToPaint)
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

class WSKeyEvent final : public WSMessage {
public:
    WSKeyEvent(Type type, int key, char character)
        : WSMessage(type)
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
    friend class WSMessageLoop;
    friend class WSScreen;
    int m_key { 0 };
    bool m_ctrl { false };
    bool m_alt { false };
    bool m_shift { false };
    char m_character { 0 };
};

class WSMouseEvent final : public WSMessage {
public:
    WSMouseEvent(Type type, const Point& position, unsigned buttons, MouseButton button = MouseButton::None)
        : WSMessage(type)
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
