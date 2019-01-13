#pragma once

// GUI system call API types.

struct GUI_Rect {
    int x;
    int y;
    int width;
    int height;
};

struct GUI_WindowFlags { enum {
    Visible = 1 << 0,
}; };

typedef unsigned GUI_Color;

struct GUI_CreateWindowParameters {
    GUI_Rect rect;
    GUI_Color background_color;
    unsigned flags;
    char title[128];
};

enum class GUI_WidgetType : unsigned {
    Label,
    Button,
};

struct GUI_CreateWidgetParameters {
    GUI_WidgetType type;
    GUI_Rect rect;
    GUI_Color background_color;
    bool opaque;
    unsigned flags;
    char text[256];
};
