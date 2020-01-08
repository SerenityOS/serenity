#pragma once

#include <LibDraw/Color.h>

class Painter;
class Palette;
class Rect;

enum class ButtonStyle {
    Normal,
    CoolBar
};
enum class FrameShadow {
    Plain,
    Raised,
    Sunken
};
enum class FrameShape {
    NoFrame,
    Box,
    Container,
    Panel,
    VerticalLine,
    HorizontalLine
};

class StylePainter {
public:
    static void paint_button(Painter&, const Rect&, const Palette&, ButtonStyle, bool pressed, bool hovered = false, bool checked = false, bool enabled = true);
    static void paint_tab_button(Painter&, const Rect&, const Palette&, bool active, bool hovered, bool enabled);
    static void paint_surface(Painter&, const Rect&, const Palette&, bool paint_vertical_lines = true, bool paint_top_line = true);
    static void paint_frame(Painter&, const Rect&, const Palette&, FrameShape, FrameShadow, int thickness, bool skip_vertical_lines = false);
    static void paint_window_frame(Painter&, const Rect&, const Palette&);
    static void paint_progress_bar(Painter&, const Rect&, const Palette&, int min, int max, int value, const StringView& text = {});
    static void paint_radio_button(Painter&, const Rect&, const Palette&, bool is_checked, bool is_being_pressed);
};
