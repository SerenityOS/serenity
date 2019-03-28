#pragma once

class Painter;
class Rect;

enum class ButtonStyle { Normal, CoolBar, OldNormal };

class StylePainter {
public:
    static StylePainter& the();

    void paint_button(Painter&, const Rect&, ButtonStyle, bool pressed, bool hovered = false);
    void paint_surface(Painter&, const Rect&);

private:
    StylePainter();
};
