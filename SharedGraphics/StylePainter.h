#pragma once

class Painter;
class Rect;

enum class ButtonStyle { Normal, CoolBar, OldNormal };
enum class FrameShadow { Plain, Raised, Sunken };
enum class FrameShape { NoFrame, Box, Container, Panel, VerticalLine, HorizontalLine };

class StylePainter {
public:
    static void paint_button(Painter&, const Rect&, ButtonStyle, bool pressed, bool hovered = false, bool checked = false, bool enabled = true);
    static void paint_tab_button(Painter&, const Rect&, bool active, bool hovered, bool enabled);
    static void paint_surface(Painter&, const Rect&, bool paint_vertical_lines = true, bool paint_top_line = true);
    static void paint_frame(Painter&, const Rect&, FrameShape, FrameShadow, int thickness, bool skip_vertical_lines = false);
    static void paint_window_frame(Painter&, const Rect&);
};
