#pragma once

class Painter;
class Rect;

enum class GButtonStyle { Normal, CoolBar };

class GStyle {
public:
    static GStyle& the();

    void paint_button(Painter&, const Rect&, GButtonStyle, bool pressed, bool hovered = false);
    void paint_surface(Painter&, const Rect&);

private:
    GStyle();
};
