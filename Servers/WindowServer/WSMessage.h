#pragma once

#include <SharedGraphics/Point.h>
#include <SharedGraphics/Rect.h>
#include <AK/AKString.h>
#include <AK/Types.h>
#include <Kernel/KeyCode.h>
#include <WindowServer/WSCursor.h>
#include <WindowServer/WSWindowType.h>

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

        WM_WindowRemoved,
        WM_WindowStateChanged,

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
        APISetClipboardContentsRequest,
        APIGetClipboardContentsRequest,
        APISetWallpaperRequest,
        APIGetWallpaperRequest,
        APISetWindowOverrideCursorRequest,
        WMAPISetActiveWindowRequest,
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

class WSWMAPISetActiveWindowRequest : public WSAPIClientRequest {
public:
    WSWMAPISetActiveWindowRequest(int client_id, int target_client_id, int target_window_id)
        : WSAPIClientRequest(WSMessage::WMAPISetActiveWindowRequest, client_id)
        , m_target_client_id(target_client_id)
        , m_target_window_id(target_window_id)
    {
    }

    int target_client_id() const { return m_target_client_id; }
    int target_window_id() const { return m_target_window_id; }

private:
    int m_target_client_id;
    int m_target_window_id;
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
    WSAPIAddMenuItemRequest(int client_id, int menu_id, unsigned identifier, const String& text, const String& shortcut_text)
        : WSAPIClientRequest(WSMessage::APIAddMenuItemRequest, client_id)
        , m_menu_id(menu_id)
        , m_identifier(identifier)
        , m_text(text)
        , m_shortcut_text(shortcut_text)
    {
    }

    int menu_id() const { return m_menu_id; }
    unsigned identifier() const { return m_identifier; }
    String text() const { return m_text; }
    String shortcut_text() const { return m_shortcut_text; }

private:
    int m_menu_id { 0 };
    unsigned m_identifier { 0 };
    String m_text;
    String m_shortcut_text;
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

class WSAPISetWindowOverrideCursorRequest final : public WSAPIClientRequest {
public:
    explicit WSAPISetWindowOverrideCursorRequest(int client_id, int window_id, WSStandardCursor cursor)
        : WSAPIClientRequest(WSMessage::APISetWindowOverrideCursorRequest, client_id)
        , m_window_id(window_id)
        , m_cursor(cursor)
    {
    }

    int window_id() const { return m_window_id; }
    WSStandardCursor cursor() const { return m_cursor; }

private:
    int m_window_id { 0 };
    WSStandardCursor m_cursor { WSStandardCursor::None };
};

class WSAPISetWallpaperRequest final : public WSAPIClientRequest {
public:
    explicit WSAPISetWallpaperRequest(int client_id, String&& wallpaper)
        : WSAPIClientRequest(WSMessage::APISetWallpaperRequest, client_id)
        , m_wallpaper(move(wallpaper))
    {
    }

    String wallpaper() const { return m_wallpaper; }

private:
    String m_wallpaper;
};

class WSAPIGetWallpaperRequest final : public WSAPIClientRequest {
public:
    explicit WSAPIGetWallpaperRequest(int client_id)
        : WSAPIClientRequest(WSMessage::APIGetWallpaperRequest, client_id)
    {
    }
};

class WSAPISetWindowTitleRequest final : public WSAPIClientRequest {
public:
    explicit WSAPISetWindowTitleRequest(int client_id, int window_id, String&& title)
        : WSAPIClientRequest(WSMessage::APISetWindowTitleRequest, client_id)
        , m_window_id(window_id)
        , m_title(move(title))
    {
    }

    int window_id() const { return m_window_id; }
    String title() const { return m_title; }

private:
    int m_window_id { 0 };
    String m_title;
};

class WSAPIGetWindowTitleRequest final : public WSAPIClientRequest {
public:
    explicit WSAPIGetWindowTitleRequest(int client_id, int window_id)
        : WSAPIClientRequest(WSMessage::APIGetWindowTitleRequest, client_id)
        , m_window_id(window_id)
    {
    }

    int window_id() const { return m_window_id; }

private:
    int m_window_id { 0 };
};

class WSAPISetClipboardContentsRequest final : public WSAPIClientRequest {
public:
    explicit WSAPISetClipboardContentsRequest(int client_id, int shared_buffer_id, int size)
        : WSAPIClientRequest(WSMessage::APISetClipboardContentsRequest, client_id)
        , m_shared_buffer_id(shared_buffer_id)
        , m_size(size)
    {
    }

    int shared_buffer_id() const { return m_shared_buffer_id; }
    int size() const { return m_size; }

private:
    int m_shared_buffer_id { 0 };
    int m_size { 0 };
};

class WSAPIGetClipboardContentsRequest final : public WSAPIClientRequest {
public:
    explicit WSAPIGetClipboardContentsRequest(int client_id)
        : WSAPIClientRequest(WSMessage::APIGetClipboardContentsRequest, client_id)
    {
    }
};

class WSAPISetWindowOpacityRequest final : public WSAPIClientRequest {
public:
    explicit WSAPISetWindowOpacityRequest(int client_id, int window_id, float opacity)
        : WSAPIClientRequest(WSMessage::APISetWindowOpacityRequest, client_id)
        , m_window_id(window_id)
        , m_opacity(opacity)
    {
    }

    int window_id() const { return m_window_id; }
    float opacity() const { return m_opacity; }

private:
    int m_window_id { 0 };
    float m_opacity { 0 };
};

class WSAPISetWindowBackingStoreRequest final : public WSAPIClientRequest {
public:
    explicit WSAPISetWindowBackingStoreRequest(int client_id, int window_id, int shared_buffer_id, const Size& size, size_t bpp, size_t pitch, bool has_alpha_channel, bool flush_immediately)
        : WSAPIClientRequest(WSMessage::APISetWindowBackingStoreRequest, client_id)
        , m_window_id(window_id)
        , m_shared_buffer_id(shared_buffer_id)
        , m_size(size)
        , m_bpp(bpp)
        , m_pitch(pitch)
        , m_has_alpha_channel(has_alpha_channel)
        , m_flush_immediately(flush_immediately)
    {
    }

    int window_id() const { return m_window_id; }
    int shared_buffer_id() const { return m_shared_buffer_id; }
    Size size() const { return m_size; }
    size_t bpp() const { return m_bpp; }
    size_t pitch() const { return m_pitch; }
    bool has_alpha_channel() const { return m_has_alpha_channel; }
    bool flush_immediately() const { return m_flush_immediately; }

private:
    int m_window_id { 0 };
    int m_shared_buffer_id { 0 };
    Size m_size;
    size_t m_bpp;
    size_t m_pitch;
    bool m_has_alpha_channel;
    bool m_flush_immediately;
};

class WSAPISetWindowRectRequest final : public WSAPIClientRequest {
public:
    explicit WSAPISetWindowRectRequest(int client_id, int window_id, const Rect& rect)
        : WSAPIClientRequest(WSMessage::APISetWindowRectRequest, client_id)
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

class WSAPIGetWindowRectRequest final : public WSAPIClientRequest {
public:
    explicit WSAPIGetWindowRectRequest(int client_id, int window_id)
        : WSAPIClientRequest(WSMessage::APIGetWindowRectRequest, client_id)
        , m_window_id(window_id)
    {
    }

    int window_id() const { return m_window_id; }

private:
    int m_window_id { 0 };
};

class WSAPICreateWindowRequest : public WSAPIClientRequest {
public:
    WSAPICreateWindowRequest(int client_id, const Rect& rect, const String& title, bool has_alpha_channel, bool modal, bool resizable, float opacity, const Size& base_size, const Size& size_increment, WSWindowType window_type)
        : WSAPIClientRequest(WSMessage::APICreateWindowRequest, client_id)
        , m_rect(rect)
        , m_title(title)
        , m_opacity(opacity)
        , m_has_alpha_channel(has_alpha_channel)
        , m_modal(modal)
        , m_resizable(resizable)
        , m_size_increment(size_increment)
        , m_base_size(base_size)
        , m_window_type(window_type)
    {
    }

    Rect rect() const { return m_rect; }
    String title() const { return m_title; }
    bool has_alpha_channel() const { return m_has_alpha_channel; }
    bool is_modal() const { return m_modal; }
    bool is_resizable() const { return m_resizable; }
    float opacity() const { return m_opacity; }
    Size size_increment() const { return m_size_increment; }
    Size base_size() const { return m_base_size; }
    WSWindowType window_type() const { return m_window_type; }

private:
    Rect m_rect;
    String m_title;
    float m_opacity { 0 };
    bool m_has_alpha_channel { false };
    bool m_modal { false };
    bool m_resizable { false };
    Size m_size_increment;
    Size m_base_size;
    WSWindowType m_window_type;
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
    WSKeyEvent(Type type, int key, char character, byte modifiers)
        : WSMessage(type)
        , m_key(key)
        , m_character(character)
        , m_modifiers(modifiers)
    {
    }

    int key() const { return m_key; }
    bool ctrl() const { return m_modifiers & Mod_Ctrl; }
    bool alt() const { return m_modifiers & Mod_Alt; }
    bool shift() const { return m_modifiers & Mod_Shift; }
    bool logo() const { return m_modifiers & Mod_Logo; }
    byte modifiers() const { return m_modifiers; }
    char character() const { return m_character; }

private:
    friend class WSMessageLoop;
    friend class WSScreen;
    int m_key { 0 };
    char m_character { 0 };
    byte m_modifiers { 0 };
};

class WSMouseEvent final : public WSMessage {
public:
    WSMouseEvent(Type type, const Point& position, unsigned buttons, MouseButton button, unsigned modifiers)
        : WSMessage(type)
        , m_position(position)
        , m_buttons(buttons)
        , m_button(button)
        , m_modifiers(modifiers)
    {
    }

    Point position() const { return m_position; }
    int x() const { return m_position.x(); }
    int y() const { return m_position.y(); }
    MouseButton button() const { return m_button; }
    unsigned buttons() const { return m_buttons; }
    unsigned modifiers() const { return m_modifiers; }

private:
    Point m_position;
    unsigned m_buttons { 0 };
    MouseButton m_button { MouseButton::None };
    unsigned m_modifiers { 0 };
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

class WSWMEvent : public WSMessage {
public:
    WSWMEvent(Type type, int client_id, int window_id)
        : WSMessage(type)
        , m_client_id(client_id)
        , m_window_id(window_id)
    {
    }

    int client_id() const { return m_client_id; }
    int window_id() const { return m_window_id; }

private:
    int m_client_id;
    int m_window_id;
};

class WSWMWindowRemovedEvent : public WSWMEvent {
public:
    WSWMWindowRemovedEvent(int client_id, int window_id)
        : WSWMEvent(WSMessage::WM_WindowRemoved, client_id, window_id)
    {
    }
};

class WSWMWindowStateChangedEvent : public WSWMEvent {
public:
    WSWMWindowStateChangedEvent(int client_id, int window_id, const String& title, const Rect& rect, bool is_active, WSWindowType window_type)
        : WSWMEvent(WSMessage::WM_WindowStateChanged, client_id, window_id)
        , m_title(title)
        , m_rect(rect)
        , m_active(is_active)
        , m_window_type(window_type)
    {
    }

    String title() const { return m_title; }
    Rect rect() const { return m_rect; }
    bool is_active() const { return m_active; }
    WSWindowType window_type() const { return m_window_type; }

private:
    String m_title;
    Rect m_rect;
    bool m_active;
    WSWindowType m_window_type;
};
