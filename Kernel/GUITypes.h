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


struct GUI_Event {
    enum Type : unsigned {
        Invalid,
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
    };
    Type type { Invalid };
    int window_id { -1 };

    union {
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
            int menu_id;
            unsigned identifier;
        } menu;
    };
};

inline Rect::Rect(const GUI_Rect& r) : Rect(r.location, r.size) { }
inline Point::Point(const GUI_Point& p) : Point(p.x, p.y) { }
inline Size::Size(const GUI_Size& s) : Size(s.width, s.height) { }
inline Rect::operator GUI_Rect() const { return { m_location, m_size }; }
inline Point::operator GUI_Point() const { return { m_x, m_y }; }
inline Size::operator GUI_Size() const { return { m_width, m_height }; }
