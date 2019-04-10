#pragma once

class Painter;
class Rect;

enum class ButtonStyle { Normal, CoolBar, OldNormal };
enum class FrameShadow { Plain, Raised, Sunken };
enum class FrameShape { NoFrame, Container, Panel, VerticalLine, HorizontalLine };

class StylePainter {
public:
    static void paint_button(Painter&, const Rect&, ButtonStyle, bool pressed, bool hovered = false, bool checked = false);
    static void paint_surface(Painter&, const Rect&, bool paint_vertical_lines = true);
    static void paint_frame(Painter&, const Rect&, FrameShape, FrameShadow, int thickness, bool skip_vertical_lines = false);
};
