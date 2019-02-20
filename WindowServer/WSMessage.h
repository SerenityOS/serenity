#pragma once

#include <SharedGraphics/Point.h>
#include <SharedGraphics/Rect.h>
#include <AK/AKString.h>
#include <AK/Types.h>

class WSMessage {
public:
    enum Type {
        Invalid = 0,
        WM_DeferredCompose,
        WM_ClientDisconnected,
        MouseMove,
        MouseDown,
        MouseUp,
        WindowEntered,
        WindowLeft,
        KeyDown,
        KeyUp,
        WindowActivated,
        WindowDeactivated,
        WindowCloseRequest,
        WindowResized,

        __Begin_API_Client_Requests,
        APICreateMenubarRequest,
        APIDestroyMenubarRequest,
        APIAddMenuToMenubarRequest,
        APISetApplicationMenubarRequest,
        APICreateMenuRequest,
        APIDestroyMenuRequest,
        APIAddMenuItemRequest,
        APIAddMenuSeparatorRequest,
        APICreateWindowRequest,
        APIDestroyWindowRequest,
        APISetWindowTitleRequest,
        APIGetWindowTitleRequest,
        APISetWindowRectRequest,
        APIGetWindowRectRequest,
        APIInvalidateRectRequest,
        APIDidFinishPaintingNotification,
        APIGetWindowBackingStoreRequest,
        APISetGlobalCursorTrackingRequest,
        APISetWindowOpacityRequest,
        APISetWindowBackingStoreRequest,
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

class WSClientDisconnectedNotification : public WSMessage {
public:
    explicit WSClientDisconnectedNotification(int client_id)
        : WSMessage(WM_ClientDisconnected)
        , m_client_id(client_id)
    {
    }

    int client_id() const { return m_client_id; }

private:
    int m_client_id { 0 };
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

class WSAPISetGlobalCursorTrackingRequest : public WSAPIClientRequest {
public:
    WSAPISetGlobalCursorTrackingRequest(int client_id, int window_id, bool value)
        : WSAPIClientRequest(WSMessage::APISetGlobalCursorTrackingRequest, client_id)
        , m_window_id(window_id)
        , m_value(value)
    {
    }

    int window_id() const { return m_window_id; }
    bool value() const { return m_value; }

private:
    int m_window_id { 0 };
    bool m_value { false };
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

class WSAPISetApplicationMenubarRequest : public WSAPIClientRequest {
public:
    WSAPISetApplicationMenubarRequest(int client_id, int menubar_id)
        : WSAPIClientRequest(WSMessage::APISetApplicationMenubarRequest, client_id)
        , m_menubar_id(menubar_id)
    {
    }

    int menubar_id() const { return m_menubar_id; }

private:
    int m_menubar_id { 0 };
};

class WSAPIAddMenuToMenubarRequest : public WSAPIClientRequest {
public:
    WSAPIAddMenuToMenubarRequest(int client_id, int menubar_id, int menu_id)
        : WSAPIClientRequest(WSMessage::APIAddMenuToMenubarRequest, client_id)
        , m_menubar_id(menubar_id)
        , m_menu_id(menu_id)
    {
    }

    int menubar_id() const { return m_menubar_id; }
    int menu_id() const { return m_menu_id; }

private:
    int m_menubar_id { 0 };
    int m_menu_id { 0 };
};

class WSAPICreateMenuRequest : public WSAPIClientRequest {
public:
    WSAPICreateMenuRequest(int client_id, const String& text)
        : WSAPIClientRequest(WSMessage::APICreateMenuRequest, client_id)
        , m_text(text)
    {
    }

    String text() const { return m_text; }

private:
    String m_text;
};

class WSAPIDestroyMenuRequest : public WSAPIClientRequest {
public:
    WSAPIDestroyMenuRequest(int client_id, int menu_id)
        : WSAPIClientRequest(WSMessage::APIDestroyMenuRequest, client_id)
        , m_menu_id(menu_id)
    {
    }

    int menu_id() const { return m_menu_id; }

private:
    int m_menu_id { 0 };
};

class WSAPIAddMenuItemRequest : public WSAPIClientRequest {
public:
    WSAPIAddMenuItemRequest(int client_id, int menu_id, unsigned identifier, const String& text)
        : WSAPIClientRequest(WSMessage::APIAddMenuItemRequest, client_id)
        , m_menu_id(menu_id)
        , m_identifier(identifier)
        , m_text(text)
    {
    }

    int menu_id() const { return m_menu_id; }
    unsigned identifier() const { return m_identifier; }
    String text() const { return m_text; }

private:
    int m_menu_id { 0 };
    unsigned m_identifier { 0 };
    String m_text;
};

class WSAPIAddMenuSeparatorRequest : public WSAPIClientRequest {
public:
    WSAPIAddMenuSeparatorRequest(int client_id, int menu_id)
        : WSAPIClientRequest(WSMessage::APIAddMenuSeparatorRequest, client_id)
        , m_menu_id(menu_id)
    {
    }

    int menu_id() const { return m_menu_id; }

private:
    int m_menu_id { 0 };
};

class WSAPISetWindowTitleRequest final : public WSAPIClientRequest {
public:
    explicit WSAPISetWindowTitleRequest(int client_id, int window_id, String&& title)
        : WSAPIClientRequest(WSMessage::APISetWindowTitleRequest, client_id)
        , m_client_id(client_id)
        , m_window_id(window_id)
        , m_title(move(title))
    {
    }

    int client_id() const { return m_client_id; }
    int window_id() const { return m_window_id; }
    String title() const { return m_title; }

private:
    int m_client_id { 0 };
    int m_window_id { 0 };
    String m_title;
};

class WSAPIGetWindowTitleRequest final : public WSAPIClientRequest {
public:
    explicit WSAPIGetWindowTitleRequest(int client_id, int window_id)
        : WSAPIClientRequest(WSMessage::APIGetWindowTitleRequest, client_id)
        , m_client_id(client_id)
        , m_window_id(window_id)
    {
    }

    int client_id() const { return m_client_id; }
    int window_id() const { return m_window_id; }

private:
    int m_client_id { 0 };
    int m_window_id { 0 };
};

class WSAPISetWindowOpacityRequest final : public WSAPIClientRequest {
public:
    explicit WSAPISetWindowOpacityRequest(int client_id, int window_id, float opacity)
        : WSAPIClientRequest(WSMessage::APISetWindowOpacityRequest, client_id)
        , m_client_id(client_id)
        , m_window_id(window_id)
        , m_opacity(opacity)
    {
    }

    int client_id() const { return m_client_id; }
    int window_id() const { return m_window_id; }
    float opacity() const { return m_opacity; }

private:
    int m_client_id { 0 };
    int m_window_id { 0 };
    float m_opacity { 0 };
};

class WSAPISetWindowBackingStoreRequest final : public WSAPIClientRequest {
public:
    explicit WSAPISetWindowBackingStoreRequest(int client_id, int window_id, int shared_buffer_id, const Size& size, size_t bpp, size_t pitch, bool has_alpha_channel)
        : WSAPIClientRequest(WSMessage::APISetWindowBackingStoreRequest, client_id)
        , m_client_id(client_id)
        , m_window_id(window_id)
        , m_shared_buffer_id(shared_buffer_id)
        , m_size(size)
        , m_bpp(bpp)
        , m_pitch(pitch)
        , m_has_alpha_channel(has_alpha_channel)
    {
    }

    int client_id() const { return m_client_id; }
    int window_id() const { return m_window_id; }
    int shared_buffer_id() const { return m_shared_buffer_id; }
    Size size() const { return m_size; }
    size_t bpp() const { return m_bpp; }
    size_t pitch() const { return m_pitch; }
    bool has_alpha_channel() const { return m_has_alpha_channel; }

private:
    int m_client_id { 0 };
    int m_window_id { 0 };
    int m_shared_buffer_id { 0 };
    Size m_size;
    size_t m_bpp;
    size_t m_pitch;
    bool m_has_alpha_channel;
};

class WSAPISetWindowRectRequest final : public WSAPIClientRequest {
public:
    explicit WSAPISetWindowRectRequest(int client_id, int window_id, const Rect& rect)
        : WSAPIClientRequest(WSMessage::APISetWindowRectRequest, client_id)
        , m_client_id(client_id)
        , m_window_id(window_id)
        , m_rect(rect)
    {
    }

    int client_id() const { return m_client_id; }
    int window_id() const { return m_window_id; }
    Rect rect() const { return m_rect; }

private:
    int m_client_id { 0 };
    int m_window_id { 0 };
    Rect m_rect;
};

class WSAPIGetWindowRectRequest final : public WSAPIClientRequest {
public:
    explicit WSAPIGetWindowRectRequest(int client_id, int window_id)
        : WSAPIClientRequest(WSMessage::APIGetWindowRectRequest, client_id)
        , m_client_id(client_id)
        , m_window_id(window_id)
    {
    }

    int client_id() const { return m_client_id; }
    int window_id() const { return m_window_id; }

private:
    int m_client_id { 0 };
    int m_window_id { 0 };
};

class WSAPICreateWindowRequest : public WSAPIClientRequest {
public:
    WSAPICreateWindowRequest(int client_id, const Rect& rect, const String& title, bool has_alpha_channel, float opacity)
        : WSAPIClientRequest(WSMessage::APICreateWindowRequest, client_id)
        , m_rect(rect)
        , m_title(title)
        , m_opacity(opacity)
        , m_has_alpha_channel(has_alpha_channel)
    {
    }

    Rect rect() const { return m_rect; }
    String title() const { return m_title; }
    bool has_alpha_channel() const { return m_has_alpha_channel; }
    float opacity() const { return m_opacity; }

private:
    Rect m_rect;
    String m_title;
    float m_opacity { 0 };
    bool m_has_alpha_channel { false };
};

class WSAPIDestroyWindowRequest : public WSAPIClientRequest {
public:
    WSAPIDestroyWindowRequest(int client_id, int window_id)
        : WSAPIClientRequest(WSMessage::APIDestroyWindowRequest, client_id)
        , m_window_id(window_id)
    {
    }

    int window_id() const { return m_window_id; }

private:
    int m_window_id { 0 };
};

class WSAPIInvalidateRectRequest final : public WSAPIClientRequest {
public:
    explicit WSAPIInvalidateRectRequest(int client_id, int window_id, const Rect& rect)
        : WSAPIClientRequest(WSMessage::APIInvalidateRectRequest, client_id)
        , m_window_id(window_id)
        , m_rect(rect)
    {
    }

    int window_id() const { return m_window_id; }
    Rect rect() const { return m_rect; }

private:
    int m_window_id { 0 };
    Rect m_rect;
};

class WSAPIGetWindowBackingStoreRequest final : public WSAPIClientRequest {
public:
    explicit WSAPIGetWindowBackingStoreRequest(int client_id, int window_id)
        : WSAPIClientRequest(WSMessage::APIGetWindowBackingStoreRequest, client_id)
        , m_window_id(window_id)
    {
    }

    int window_id() const { return m_window_id; }

private:
    int m_window_id { 0 };
};

class WSAPIDidFinishPaintingNotification final : public WSAPIClientRequest {
public:
    explicit WSAPIDidFinishPaintingNotification(int client_id, int window_id, const Rect& rect)
        : WSAPIClientRequest(WSMessage::APIDidFinishPaintingNotification, client_id)
        , m_window_id(window_id)
        , m_rect(rect)
    {
    }

    int window_id() const { return m_window_id; }
    Rect rect() const { return m_rect; }

private:
    int m_window_id { 0 };
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

class WSResizeEvent final : public WSMessage {
public:
    WSResizeEvent(const Rect& old_rect, const Rect& rect)
        : WSMessage(WSMessage::WindowResized)
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
