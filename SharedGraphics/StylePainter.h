#pragma once

class Painter;
class Rect;

enum class ButtonStyle { Normal, CoolBar, OldNormal };

class StylePainter {
public:
    static void paint_button(Painter&, const Rect&, ButtonStyle, bool pressed, bool hovered = false);
    static void paint_surface(Painter&, const Rect&, bool paint_vertical_lines = true);
};
