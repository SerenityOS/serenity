#pragma once

class Painter;
class Rect;

class GStyle {
public:
    static GStyle& the();

    void paint_button(Painter& painter, const Rect& rect, bool pressed);

private:
    GStyle();
};
