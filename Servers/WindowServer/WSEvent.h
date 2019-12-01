#pragma once

#include <AK/String.h>
#include <AK/Types.h>
#include <Kernel/KeyCode.h>
#include <LibCore/CEvent.h>
#include <LibDraw/Point.h>
#include <LibDraw/Rect.h>
#include <WindowServer/WSCursor.h>
#include <WindowServer/WSAPITypes.h>
#include <WindowServer/WSWindowType.h>

class WSEvent : public CEvent {
public:
    enum Type {
        Invalid = 3000,
        WM_DeferredCompose,
        MouseMove,
        MouseDown,
        MouseDoubleClick,
        MouseUp,
        MouseWheel,
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
        WM_WindowRectChanged,
        WM_WindowIconBitmapChanged,

        __Begin_API_Client_Requests,
        APICreateMenubarRequest,
        APIDestroyMenubarRequest,
        APIAddMenuToMenubarRequest,
        APISetApplicationMenubarRequest,
        APICreateMenuRequest,
        APIDestroyMenuRequest,
        APIAddMenuItemRequest,
        APIAddMenuSeparatorRequest,
        APIUpdateMenuItemRequest,
        APICreateWindowRequest,
        APIDestroyWindowRequest,
        APISetWindowTitleRequest,
        APIGetWindowTitleRequest,
        APISetWindowRectRequest,
        APIGetWindowRectRequest,
        APISetWindowIconBitmapRequest,
        APIInvalidateRectRequest,
        APIDidFinishPaintingNotification,
        APISetGlobalCursorTrackingRequest,
        APISetWindowOpacityRequest,
        APISetWindowBackingStoreRequest,
        APISetClipboardContentsRequest,
        APIGetClipboardContentsRequest,
        APISetWallpaperRequest,
        APIGetWallpaperRequest,
        APISetResolutionRequest,
        APISetWindowOverrideCursorRequest,
        APISetWindowHasAlphaChannelRequest,
        APIMoveWindowToFrontRequest,
        APISetFullscreenRequest,
        WMAPISetActiveWindowRequest,
        WMAPISetWindowMinimizedRequest,
        WMAPIStartWindowResizeRequest,
        WMAPIPopupWindowMenuRequest,
        APIPopupMenuRequest,
        APIDismissMenuRequest,
        __End_API_Client_Requests,
    };

    WSEvent() {}
    explicit WSEvent(Type type)
        : CEvent(type)
    {
    }
    virtual ~WSEvent() {}

    bool is_client_request() const { return type() > __Begin_API_Client_Requests && type() < __End_API_Client_Requests; }
    bool is_mouse_event() const { return type() == MouseMove || type() == MouseDown || type() == MouseDoubleClick || type() == MouseUp || type() == MouseWheel; }
    bool is_key_event() const { return type() == KeyUp || type() == KeyDown; }
};

class WSAPIClientRequest : public WSEvent {
public:
    WSAPIClientRequest(Type type, int client_id)
        : WSEvent(type)
        , m_client_id(client_id)
    {
    }

    int client_id() const { return m_client_id; }

private:
    int m_client_id { 0 };
};

class WSWMAPIStartWindowResizeRequest : public WSAPIClientRequest {
public:
    WSWMAPIStartWindowResizeRequest(int client_id, int target_client_id, int target_window_id)
        : WSAPIClientRequest(WSEvent::WMAPIStartWindowResizeRequest, client_id)
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

class WSWMAPIPopupWindowMenuRequest : public WSAPIClientRequest {
public:
    WSWMAPIPopupWindowMenuRequest(int client_id, int target_client_id, int target_window_id, const Point& position)
        : WSAPIClientRequest(WSEvent::WMAPIPopupWindowMenuRequest, client_id)
        , m_target_client_id(target_client_id)
        , m_target_window_id(target_window_id)
        , m_position(position)
    {
    }

    int target_client_id() const { return m_target_client_id; }
    int target_window_id() const { return m_target_window_id; }
    Point position() const { return m_position; }

private:
    int m_target_client_id;
    int m_target_window_id;
    Point m_position;
};

class WSWMAPISetActiveWindowRequest : public WSAPIClientRequest {
public:
    WSWMAPISetActiveWindowRequest(int client_id, int target_client_id, int target_window_id)
        : WSAPIClientRequest(WSEvent::WMAPISetActiveWindowRequest, client_id)
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

class WSWMAPISetWindowMinimizedRequest : public WSAPIClientRequest {
public:
    WSWMAPISetWindowMinimizedRequest(int client_id, int target_client_id, int target_window_id, bool minimized)
        : WSAPIClientRequest(WSEvent::WMAPISetWindowMinimizedRequest, client_id)
        , m_target_client_id(target_client_id)
        , m_target_window_id(target_window_id)
        , m_minimized(minimized)
    {
    }

    int target_client_id() const { return m_target_client_id; }
    int target_window_id() const { return m_target_window_id; }
    bool is_minimized() const { return m_minimized; }

private:
    int m_target_client_id;
    int m_target_window_id;
    bool m_minimized;
};

class WSAPISetGlobalCursorTrackingRequest : public WSAPIClientRequest {
public:
    WSAPISetGlobalCursorTrackingRequest(int client_id, int window_id, bool value)
        : WSAPIClientRequest(WSEvent::APISetGlobalCursorTrackingRequest, client_id)
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
        : WSAPIClientRequest(WSEvent::APICreateMenubarRequest, client_id)
    {
    }
};

class WSAPIDestroyMenubarRequest : public WSAPIClientRequest {
public:
    WSAPIDestroyMenubarRequest(int client_id, int menubar_id)
        : WSAPIClientRequest(WSEvent::APIDestroyMenubarRequest, client_id)
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
        : WSAPIClientRequest(WSEvent::APISetApplicationMenubarRequest, client_id)
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
        : WSAPIClientRequest(WSEvent::APIAddMenuToMenubarRequest, client_id)
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

class WSAPIPopupMenuRequest : public WSAPIClientRequest {
public:
    WSAPIPopupMenuRequest(int client_id, int menu_id, const Point& position)
        : WSAPIClientRequest(WSEvent::APIPopupMenuRequest, client_id)
        , m_menu_id(menu_id)
        , m_position(position)
    {
    }

    int menu_id() const { return m_menu_id; }
    Point position() const { return m_position; }

private:
    int m_menu_id;
    Point m_position;
};

class WSAPIDismissMenuRequest : public WSAPIClientRequest {
public:
    WSAPIDismissMenuRequest(int client_id, int menu_id)
        : WSAPIClientRequest(WSEvent::APIDismissMenuRequest, client_id)
        , m_menu_id(menu_id)
    {
    }

    int menu_id() const { return m_menu_id; }

private:
    int m_menu_id;
};

class WSAPICreateMenuRequest : public WSAPIClientRequest {
public:
    WSAPICreateMenuRequest(int client_id, const String& text)
        : WSAPIClientRequest(WSEvent::APICreateMenuRequest, client_id)
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
        : WSAPIClientRequest(WSEvent::APIDestroyMenuRequest, client_id)
        , m_menu_id(menu_id)
    {
    }

    int menu_id() const { return m_menu_id; }

private:
    int m_menu_id { 0 };
};

class WSAPIAddMenuItemRequest : public WSAPIClientRequest {
public:
    WSAPIAddMenuItemRequest(int client_id, int menu_id, unsigned identifier, const String& text, const String& shortcut_text, bool enabled, bool checkable, bool checked, int icon_buffer_id, int submenu_id)
        : WSAPIClientRequest(WSEvent::APIAddMenuItemRequest, client_id)
        , m_menu_id(menu_id)
        , m_identifier(identifier)
        , m_text(text)
        , m_shortcut_text(shortcut_text)
        , m_enabled(enabled)
        , m_checkable(checkable)
        , m_checked(checked)
        , m_icon_buffer_id(icon_buffer_id)
        , m_submenu_id(submenu_id)
    {
    }

    int menu_id() const { return m_menu_id; }
    unsigned identifier() const { return m_identifier; }
    String text() const { return m_text; }
    String shortcut_text() const { return m_shortcut_text; }
    bool is_enabled() const { return m_enabled; }
    bool is_checkable() const { return m_checkable; }
    bool is_checked() const { return m_checked; }
    int icon_buffer_id() const { return m_icon_buffer_id; }
    int submenu_id() const { return m_submenu_id; }

private:
    int m_menu_id { 0 };
    unsigned m_identifier { 0 };
    String m_text;
    String m_shortcut_text;
    bool m_enabled;
    bool m_checkable;
    bool m_checked;
    int m_icon_buffer_id { 0 };
    int m_submenu_id { 0 };
};

class WSAPIUpdateMenuItemRequest : public WSAPIClientRequest {
public:
    WSAPIUpdateMenuItemRequest(int client_id, int menu_id, unsigned identifier, const String& text, const String& shortcut_text, bool enabled, bool checkable, bool checked)
        : WSAPIClientRequest(WSEvent::APIUpdateMenuItemRequest, client_id)
        , m_menu_id(menu_id)
        , m_identifier(identifier)
        , m_text(text)
        , m_shortcut_text(shortcut_text)
        , m_enabled(enabled)
        , m_checkable(checkable)
        , m_checked(checked)
    {
    }

    int menu_id() const { return m_menu_id; }
    unsigned identifier() const { return m_identifier; }
    String text() const { return m_text; }
    String shortcut_text() const { return m_shortcut_text; }
    bool is_enabled() const { return m_enabled; }
    bool is_checkable() const { return m_checkable; }
    bool is_checked() const { return m_checked; }

private:
    int m_menu_id { 0 };
    unsigned m_identifier { 0 };
    String m_text;
    String m_shortcut_text;
    bool m_enabled { true };
    bool m_checkable;
    bool m_checked;
};

class WSAPIAddMenuSeparatorRequest : public WSAPIClientRequest {
public:
    WSAPIAddMenuSeparatorRequest(int client_id, int menu_id)
        : WSAPIClientRequest(WSEvent::APIAddMenuSeparatorRequest, client_id)
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
        : WSAPIClientRequest(WSEvent::APISetWindowOverrideCursorRequest, client_id)
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

class WSAPISetWindowHasAlphaChannelRequest final : public WSAPIClientRequest {
public:
    explicit WSAPISetWindowHasAlphaChannelRequest(int client_id, int window_id, bool value)
        : WSAPIClientRequest(WSEvent::APISetWindowHasAlphaChannelRequest, client_id)
        , m_window_id(window_id)
        , m_value(value)
    {
    }

    int window_id() const { return m_window_id; }
    bool value() const { return m_value; }

private:
    int m_window_id { 0 };
    bool m_value { 0 };
};

class WSAPISetWallpaperRequest final : public WSAPIClientRequest {
public:
    explicit WSAPISetWallpaperRequest(int client_id, const String& wallpaper)
        : WSAPIClientRequest(WSEvent::APISetWallpaperRequest, client_id)
        , m_wallpaper(wallpaper)
    {
    }

    String wallpaper() const { return m_wallpaper; }

private:
    String m_wallpaper;
};

class WSAPIGetWallpaperRequest final : public WSAPIClientRequest {
public:
    explicit WSAPIGetWallpaperRequest(int client_id)
        : WSAPIClientRequest(WSEvent::APIGetWallpaperRequest, client_id)
    {
    }
};

class WSAPISetResolutionRequest final : public WSAPIClientRequest {
public:
    explicit WSAPISetResolutionRequest(int client_id, int width, int height)
        : WSAPIClientRequest(WSEvent::APISetResolutionRequest, client_id),
          m_resolution(width, height)
    {
    }

    Size resolution() const { return m_resolution; }

private:
    Size m_resolution;
};

class WSAPISetWindowTitleRequest final : public WSAPIClientRequest {
public:
    explicit WSAPISetWindowTitleRequest(int client_id, int window_id, const String& title)
        : WSAPIClientRequest(WSEvent::APISetWindowTitleRequest, client_id)
        , m_window_id(window_id)
        , m_title(title)
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
        : WSAPIClientRequest(WSEvent::APIGetWindowTitleRequest, client_id)
        , m_window_id(window_id)
    {
    }

    int window_id() const { return m_window_id; }

private:
    int m_window_id { 0 };
};

class WSAPIMoveWindowToFrontRequest final : public WSAPIClientRequest {
public:
    explicit WSAPIMoveWindowToFrontRequest(int client_id, int window_id)
        : WSAPIClientRequest(WSEvent::APIMoveWindowToFrontRequest, client_id)
        , m_window_id(window_id)
    {
    }

    int window_id() const { return m_window_id; }

private:
    int m_window_id { 0 };
};

class WSAPISetFullscreenRequest final : public WSAPIClientRequest {
public:
    explicit WSAPISetFullscreenRequest(int client_id, int window_id, bool fullscreen)
        : WSAPIClientRequest(WSEvent::APISetFullscreenRequest, client_id)
        , m_window_id(window_id)
        , m_fullscreen(fullscreen)
    {
    }

    int window_id() const { return m_window_id; }
    bool fullscreen() const { return m_fullscreen; }

private:
    int m_window_id { 0 };
    bool m_fullscreen;
};

class WSAPISetClipboardContentsRequest final : public WSAPIClientRequest {
public:
    explicit WSAPISetClipboardContentsRequest(int client_id, int shared_buffer_id, int size, const String& data_type)
        : WSAPIClientRequest(WSEvent::APISetClipboardContentsRequest, client_id)
        , m_shared_buffer_id(shared_buffer_id)
        , m_size(size)
        , m_data_type(data_type)
    {
    }

    int shared_buffer_id() const { return m_shared_buffer_id; }
    int size() const { return m_size; }
    const String& data_type() const { return m_data_type; }

private:
    int m_shared_buffer_id { 0 };
    int m_size { 0 };
    String m_data_type;
};

class WSAPIGetClipboardContentsRequest final : public WSAPIClientRequest {
public:
    explicit WSAPIGetClipboardContentsRequest(int client_id)
        : WSAPIClientRequest(WSEvent::APIGetClipboardContentsRequest, client_id)
    {
    }
};

class WSAPISetWindowOpacityRequest final : public WSAPIClientRequest {
public:
    explicit WSAPISetWindowOpacityRequest(int client_id, int window_id, float opacity)
        : WSAPIClientRequest(WSEvent::APISetWindowOpacityRequest, client_id)
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
        : WSAPIClientRequest(WSEvent::APISetWindowBackingStoreRequest, client_id)
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
        : WSAPIClientRequest(WSEvent::APISetWindowRectRequest, client_id)
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

class WSAPISetWindowIconBitmapRequest final : public WSAPIClientRequest {
public:
    explicit WSAPISetWindowIconBitmapRequest(int client_id, int window_id, int icon_buffer_id, const Size& icon_size)
        : WSAPIClientRequest(WSEvent::APISetWindowIconBitmapRequest, client_id)
        , m_window_id(window_id)
        , m_icon_buffer_id(icon_buffer_id)
        , m_icon_size(icon_size)
    {
    }

    int window_id() const { return m_window_id; }
    int icon_buffer_id() const { return m_icon_buffer_id; }
    const Size& icon_size() const { return m_icon_size; }

private:
    int m_window_id { 0 };
    int m_icon_buffer_id { 0 };
    Size m_icon_size;
};

class WSAPIGetWindowRectRequest final : public WSAPIClientRequest {
public:
    explicit WSAPIGetWindowRectRequest(int client_id, int window_id)
        : WSAPIClientRequest(WSEvent::APIGetWindowRectRequest, client_id)
        , m_window_id(window_id)
    {
    }

    int window_id() const { return m_window_id; }

private:
    int m_window_id { 0 };
};

class WSAPICreateWindowRequest : public WSAPIClientRequest {
public:
    WSAPICreateWindowRequest(int client_id, const Rect& rect, const String& title, bool has_alpha_channel, bool modal, bool resizable, bool fullscreen, bool show_titlebar, float opacity, const Size& base_size, const Size& size_increment, WSWindowType window_type, Color background_color)
        : WSAPIClientRequest(WSEvent::APICreateWindowRequest, client_id)
        , m_rect(rect)
        , m_title(title)
        , m_opacity(opacity)
        , m_has_alpha_channel(has_alpha_channel)
        , m_modal(modal)
        , m_resizable(resizable)
        , m_fullscreen(fullscreen)
        , m_show_titlebar(show_titlebar)
        , m_size_increment(size_increment)
        , m_base_size(base_size)
        , m_window_type(window_type)
        , m_background_color(background_color)
    {
    }

    Rect rect() const { return m_rect; }
    String title() const { return m_title; }
    bool has_alpha_channel() const { return m_has_alpha_channel; }
    bool is_modal() const { return m_modal; }
    bool is_resizable() const { return m_resizable; }
    bool is_fullscreen() const { return m_fullscreen; }
    bool show_titlebar() const { return m_show_titlebar; }
    float opacity() const { return m_opacity; }
    Size size_increment() const { return m_size_increment; }
    Size base_size() const { return m_base_size; }
    WSWindowType window_type() const { return m_window_type; }
    Color background_color() const { return m_background_color; }

private:
    Rect m_rect;
    String m_title;
    float m_opacity { 0 };
    bool m_has_alpha_channel { false };
    bool m_modal { false };
    bool m_resizable { false };
    bool m_fullscreen { false };
    bool m_show_titlebar { true };
    Size m_size_increment;
    Size m_base_size;
    WSWindowType m_window_type;
    Color m_background_color;
};

class WSAPIDestroyWindowRequest : public WSAPIClientRequest {
public:
    WSAPIDestroyWindowRequest(int client_id, int window_id)
        : WSAPIClientRequest(WSEvent::APIDestroyWindowRequest, client_id)
        , m_window_id(window_id)
    {
    }

    int window_id() const { return m_window_id; }

private:
    int m_window_id { 0 };
};

class WSAPIInvalidateRectRequest final : public WSAPIClientRequest {
public:
    explicit WSAPIInvalidateRectRequest(int client_id, int window_id, const Vector<Rect, 32>& rects)
        : WSAPIClientRequest(WSEvent::APIInvalidateRectRequest, client_id)
        , m_window_id(window_id)
        , m_rects(rects)
    {
    }

    int window_id() const { return m_window_id; }
    const Vector<Rect, 32>& rects() const { return m_rects; }

private:
    int m_window_id { 0 };
    Vector<Rect, 32> m_rects;
};

class WSAPIDidFinishPaintingNotification final : public WSAPIClientRequest {
public:
    explicit WSAPIDidFinishPaintingNotification(int client_id, int window_id, const Vector<Rect, 32>& rects)
        : WSAPIClientRequest(WSEvent::APIDidFinishPaintingNotification, client_id)
        , m_window_id(window_id)
        , m_rects(rects)
    {
    }

    int window_id() const { return m_window_id; }
    const Vector<Rect, 32>& rects() const { return m_rects; }

private:
    int m_window_id { 0 };
    Vector<Rect, 32> m_rects;
};

enum class MouseButton : u8 {
    None = 0,
    Left = 1,
    Right = 2,
    Middle = 4,
};

class WSKeyEvent final : public WSEvent {
public:
    WSKeyEvent(Type type, int key, char character, u8 modifiers)
        : WSEvent(type)
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
    u8 modifiers() const { return m_modifiers; }
    char character() const { return m_character; }

private:
    friend class WSEventLoop;
    friend class WSScreen;
    int m_key { 0 };
    char m_character { 0 };
    u8 m_modifiers { 0 };
};

class WSMouseEvent final : public WSEvent {
public:
    WSMouseEvent(Type type, const Point& position, unsigned buttons, MouseButton button, unsigned modifiers, int wheel_delta = 0)
        : WSEvent(type)
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
    MouseButton button() const { return m_button; }
    unsigned buttons() const { return m_buttons; }
    unsigned modifiers() const { return m_modifiers; }
    int wheel_delta() const { return m_wheel_delta; }

    WSMouseEvent translated(const Point& delta) const { return WSMouseEvent((Type)type(), m_position.translated(delta), m_buttons, m_button, m_modifiers, m_wheel_delta); }

private:
    Point m_position;
    unsigned m_buttons { 0 };
    MouseButton m_button { MouseButton::None };
    unsigned m_modifiers { 0 };
    int m_wheel_delta { 0 };
};

class WSResizeEvent final : public WSEvent {
public:
    WSResizeEvent(const Rect& old_rect, const Rect& rect)
        : WSEvent(WSEvent::WindowResized)
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

class WSWMEvent : public WSEvent {
public:
    WSWMEvent(Type type, int client_id, int window_id)
        : WSEvent(type)
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
        : WSWMEvent(WSEvent::WM_WindowRemoved, client_id, window_id)
    {
    }
};

class WSWMWindowStateChangedEvent : public WSWMEvent {
public:
    WSWMWindowStateChangedEvent(int client_id, int window_id, const String& title, const Rect& rect, bool is_active, WSWindowType window_type, bool is_minimized)
        : WSWMEvent(WSEvent::WM_WindowStateChanged, client_id, window_id)
        , m_title(title)
        , m_rect(rect)
        , m_active(is_active)
        , m_window_type(window_type)
        , m_minimized(is_minimized)
    {
    }

    String title() const { return m_title; }
    Rect rect() const { return m_rect; }
    bool is_active() const { return m_active; }
    WSWindowType window_type() const { return m_window_type; }
    bool is_minimized() const { return m_minimized; }

private:
    String m_title;
    Rect m_rect;
    bool m_active;
    WSWindowType m_window_type;
    bool m_minimized;
};

class WSWMWindowIconBitmapChangedEvent : public WSWMEvent {
public:
    WSWMWindowIconBitmapChangedEvent(int client_id, int window_id, int icon_buffer_id, const Size& icon_size)
        : WSWMEvent(WSEvent::WM_WindowIconBitmapChanged, client_id, window_id)
        , m_icon_buffer_id(icon_buffer_id)
        , m_icon_size(icon_size)
    {
    }

    int icon_buffer_id() const { return m_icon_buffer_id; }
    const Size icon_size() const { return m_icon_size; }

private:
    int m_icon_buffer_id;
    Size m_icon_size;
};

class WSWMWindowRectChangedEvent : public WSWMEvent {
public:
    WSWMWindowRectChangedEvent(int client_id, int window_id, const Rect& rect)
        : WSWMEvent(WSEvent::WM_WindowRectChanged, client_id, window_id)
        , m_rect(rect)
    {
    }

    Rect rect() const { return m_rect; }

private:
    Rect m_rect;
};
