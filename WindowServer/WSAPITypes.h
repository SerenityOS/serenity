#pragma once

#include <SharedGraphics/Color.h>
#include <SharedGraphics/Rect.h>

// GUI system call API types.

struct GUI_WindowFlags { enum {
    Visible = 1 << 0,
}; };

typedef unsigned GUI_Color;

struct GUI_Point {
    int x;
    int y;
};

struct GUI_Size {
    int width;
    int height;
};

struct GUI_Rect {
    GUI_Point location;
    GUI_Size size;
};

struct GUI_WindowParameters {
    GUI_Rect rect;
    Color background_color;
    unsigned flags { 0 };
    char title[128];
};

struct GUI_WindowBackingStoreInfo {
    void* backing_store_id;
    GUI_Size size;
    size_t bpp;
    size_t pitch;
    RGBA32* pixels;
};

enum class GUI_MouseButton : unsigned char {
    NoButton = 0,
    Left     = 1,
    Right    = 2,
    Middle   = 4,
};

struct GUI_KeyModifiers { enum {
    Shift = 1 << 0,
    Alt   = 1 << 1,
    Ctrl  = 1 << 2,
}; };


struct GUI_ServerMessage {
    enum Type : unsigned {
        Invalid,
        Error,
        Paint,
        MouseMove,
        MouseDown,
        MouseUp,
        KeyDown,
        KeyUp,
        WindowActivated,
        WindowDeactivated,
        WindowCloseRequest,
        MenuItemActivated,
        DidCreateMenubar,
        DidDestroyMenubar,
        DidCreateMenu,
        DidDestroyMenu,
        DidAddMenuToMenubar,
        DidSetApplicationMenubar,
        DidAddMenuItem,
        DidAddMenuSeparator,
        DidCreateWindow,
        DidDestroyWindow,
        DidGetWindowTitle,
        DidGetWindowRect,
        DidGetWindowBackingStore,
    };
    Type type { Invalid };
    int window_id { -1 };
    size_t text_length;
    char text[256];

    union {
        struct {
            GUI_Rect rect;
        } window;
        struct {
            GUI_Rect rect;
        } paint;
        struct {
            GUI_Point position;
            GUI_MouseButton button;
            unsigned buttons;
        } mouse;
        struct {
            char character;
            byte key;
            byte modifiers;
            bool ctrl : 1;
            bool alt : 1;
            bool shift : 1;
        } key;
        struct {
            int menubar_id;
            int menu_id;
            unsigned identifier;
        } menu;
        struct {
            void* backing_store_id;
            GUI_Size size;
            size_t bpp;
            size_t pitch;
            RGBA32* pixels;
        } backing;
    };
};

struct GUI_ClientMessage {
    enum Type : unsigned {
        Invalid,
        CreateMenubar,
        DestroyMenubar,
        CreateMenu,
        DestroyMenu,
        AddMenuToMenubar,
        SetApplicationMenubar,
        AddMenuItem,
        AddMenuSeparator,
        CreateWindow,
        DestroyWindow,
        SetWindowTitle,
        GetWindowTitle,
        SetWindowRect,
        GetWindowRect,
        InvalidateRect,
        DidFinishPainting,
        GetWindowBackingStore,
        ReleaseWindowBackingStore,
        SetGlobalCursorTracking,
    };
    Type type { Invalid };
    int window_id { -1 };
    size_t text_length;
    char text[256];
    int value { 0 };

    union {
        struct {
            int menubar_id;
            int menu_id;
            unsigned identifier;
        } menu;
        struct {
            GUI_Rect rect;
        } window;
        struct {
            void* backing_store_id;
        } backing;
    };
};

inline Rect::Rect(const GUI_Rect& r) : Rect(r.location, r.size) { }
inline Point::Point(const GUI_Point& p) : Point(p.x, p.y) { }
inline Size::Size(const GUI_Size& s) : Size(s.width, s.height) { }
inline Rect::operator GUI_Rect() const { return { m_location, m_size }; }
inline Point::operator GUI_Point() const { return { m_x, m_y }; }
inline Size::operator GUI_Size() const { return { m_width, m_height }; }
