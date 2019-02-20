#pragma once

class Painter;
class Rect;

enum class GButtonStyle { Normal, CoolBar };

class GStyle {
public:
    static GStyle& the();

    void paint_button(Painter& painter, const Rect& rect, GButtonStyle, bool pressed, bool hovered = false);

private:
    GStyle();
};
