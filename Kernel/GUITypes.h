#pragma once

#include <Widgets/Color.h>
#include <Widgets/Rect.h>

// GUI system call API types.

struct GUI_WindowFlags { enum {
    Visible = 1 << 0,
}; };

typedef unsigned GUI_Color;

struct GUI_CreateWindowParameters {
    Rect rect;
    Color background_color;
    unsigned flags { 0 };
    char title[128];
};

enum class GUI_WidgetType : unsigned {
    Label,
    Button,
};

struct GUI_CreateWidgetParameters {
    GUI_WidgetType type;
    Rect rect;
    Color background_color;
    bool opaque { true };
    unsigned flags { 0 };
    char text[256];
};
