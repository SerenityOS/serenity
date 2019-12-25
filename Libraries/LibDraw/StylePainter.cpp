#include <LibDraw/Painter.h>
#include <LibDraw/Palette.h>
#include <LibDraw/StylePainter.h>

void StylePainter::paint_tab_button(Painter& painter, const Rect& rect, const Palette& palette, bool active, bool hovered, bool enabled)
{
    Color base_color = palette.button();
    Color highlight_color2 = palette.threed_highlight();
    Color shadow_color1 = palette.threed_shadow1();
    Color shadow_color2 = palette.threed_shadow2();

    if (hovered && enabled && !active)
        base_color = palette.hover_highlight();

    PainterStateSaver saver(painter);
    painter.translate(rect.location());

    // Base
    painter.fill_rect({ 1, 1, rect.width() - 2, rect.height() - 1 }, base_color);

    // Top line
    painter.draw_line({ 2, 0 }, { rect.width() - 3, 0 }, highlight_color2);

    // Left side
    painter.draw_line({ 0, 2 }, { 0, rect.height() - 1 }, highlight_color2);
    painter.set_pixel({ 1, 1 }, highlight_color2);

    // Right side
    painter.draw_line({
                          rect.width() - 1,
                          2,
                      },
        { rect.width() - 1, rect.height() - 1 }, shadow_color2);
    painter.draw_line({
                          rect.width() - 2,
                          2,
                      },
        { rect.width() - 2, rect.height() - 1 }, shadow_color1);
    painter.set_pixel({
                          rect.width() - 2,
                          1,
                      },
        shadow_color2);
}

static void paint_button_new(Painter& painter, const Rect& rect, const Palette& palette, bool pressed, bool checked, bool hovered, bool enabled)
{
    Color button_color = palette.button();
    Color highlight_color2 = palette.threed_highlight();
    Color shadow_color1 = palette.threed_shadow1();
    Color shadow_color2 = palette.threed_shadow2();

    if (checked && enabled) {
        if (hovered)
            button_color = palette.hover_highlight();
        else
            button_color = palette.button();
    } else if (hovered && enabled)
        button_color = palette.hover_highlight();

    PainterStateSaver saver(painter);
    painter.translate(rect.location());

    if (pressed || checked) {
        // Base
        painter.fill_rect({ 1, 1, rect.width() - 2, rect.height() - 2 }, button_color);

        painter.draw_rect({ {}, rect.size() }, shadow_color2);

        // Sunken shadow
        painter.draw_line({ 1, 1 }, { rect.width() - 2, 1 }, shadow_color1);
        painter.draw_line({ 1, 2 }, { 1, rect.height() - 2 }, shadow_color1);
    } else {
        // Base
        painter.fill_rect({ 1, 1, rect.width() - 3, rect.height() - 3 }, button_color);

        // Outer highlight
        painter.draw_line({ 0, 0 }, { rect.width() - 2, 0 }, highlight_color2);
        painter.draw_line({ 0, 1 }, { 0, rect.height() - 2 }, highlight_color2);

        // Outer shadow
        painter.draw_line({ 0, rect.height() - 1 }, { rect.width() - 1, rect.height() - 1 }, shadow_color2);
        painter.draw_line({ rect.width() - 1, 0 }, { rect.width() - 1, rect.height() - 2 }, shadow_color2);

        // Inner shadow
        painter.draw_line({ 1, rect.height() - 2 }, { rect.width() - 2, rect.height() - 2 }, shadow_color1);
        painter.draw_line({ rect.width() - 2, 1 }, { rect.width() - 2, rect.height() - 3 }, shadow_color1);
    }
}

void StylePainter::paint_button(Painter& painter, const Rect& rect, const Palette& palette, ButtonStyle button_style, bool pressed, bool hovered, bool checked, bool enabled)
{
    if (button_style == ButtonStyle::Normal)
        return paint_button_new(painter, rect, palette, pressed, checked, hovered, enabled);

    Color button_color = palette.button();
    Color highlight_color = palette.threed_highlight();
    Color shadow_color = palette.threed_shadow1();

    if (button_style == ButtonStyle::CoolBar && !enabled)
        return;

    PainterStateSaver saver(painter);
    painter.translate(rect.location());

    if (pressed || checked) {
        // Base
        painter.fill_rect({ 1, 1, rect.width() - 2, rect.height() - 2 }, button_color);

        // Sunken shadow
        painter.draw_line({ 1, 1 }, { rect.width() - 2, 1 }, shadow_color);
        painter.draw_line({ 1, 2 }, { 1, rect.height() - 2 }, shadow_color);

        // Bottom highlight
        painter.draw_line({ rect.width() - 2, 1 }, { rect.width() - 2, rect.height() - 3 }, highlight_color);
        painter.draw_line({ 1, rect.height() - 2 }, { rect.width() - 2, rect.height() - 2 }, highlight_color);
    } else if (button_style == ButtonStyle::CoolBar && hovered) {
        // Base
        painter.fill_rect({ 1, 1, rect.width() - 2, rect.height() - 2 }, button_color);

        // White highlight
        painter.draw_line({ 1, 1 }, { rect.width() - 2, 1 }, highlight_color);
        painter.draw_line({ 1, 2 }, { 1, rect.height() - 2 }, highlight_color);

        // Gray shadow
        painter.draw_line({ rect.width() - 2, 1 }, { rect.width() - 2, rect.height() - 3 }, shadow_color);
        painter.draw_line({ 1, rect.height() - 2 }, { rect.width() - 2, rect.height() - 2 }, shadow_color);
    }
}

void StylePainter::paint_surface(Painter& painter, const Rect& rect, const Palette& palette, bool paint_vertical_lines, bool paint_top_line)
{
    painter.fill_rect({ rect.x(), rect.y() + 1, rect.width(), rect.height() - 2 }, palette.button());
    painter.draw_line(rect.top_left(), rect.top_right(), paint_top_line ? palette.threed_highlight() : palette.button());
    painter.draw_line(rect.bottom_left(), rect.bottom_right(), palette.threed_shadow1());
    if (paint_vertical_lines) {
        painter.draw_line(rect.top_left().translated(0, 1), rect.bottom_left().translated(0, -1), palette.threed_highlight());
        painter.draw_line(rect.top_right(), rect.bottom_right().translated(0, -1), palette.threed_shadow1());
    }
}

void StylePainter::paint_frame(Painter& painter, const Rect& rect, const Palette& palette, FrameShape shape, FrameShadow shadow, int thickness, bool skip_vertical_lines)
{
    Color top_left_color;
    Color bottom_right_color;
    Color dark_shade = palette.threed_shadow1();
    Color light_shade = palette.threed_highlight();

    if (shape == FrameShape::Container && thickness >= 2) {
        if (shadow == FrameShadow::Raised) {
            dark_shade = palette.threed_shadow2();
        }
    }

    if (shadow == FrameShadow::Raised) {
        top_left_color = light_shade;
        bottom_right_color = dark_shade;
    } else if (shadow == FrameShadow::Sunken) {
        top_left_color = dark_shade;
        bottom_right_color = light_shade;
    } else if (shadow == FrameShadow::Plain) {
        top_left_color = dark_shade;
        bottom_right_color = dark_shade;
    }

    if (thickness >= 1) {
        painter.draw_line(rect.top_left(), rect.top_right(), top_left_color);
        painter.draw_line(rect.bottom_left(), rect.bottom_right(), bottom_right_color);

        if (shape != FrameShape::Panel || !skip_vertical_lines) {
            painter.draw_line(rect.top_left().translated(0, 1), rect.bottom_left().translated(0, -1), top_left_color);
            painter.draw_line(rect.top_right(), rect.bottom_right().translated(0, -1), bottom_right_color);
        }
    }

    if (shape == FrameShape::Container && thickness >= 2) {
        Color top_left_color;
        Color bottom_right_color;
        Color dark_shade = palette.threed_shadow2();
        Color light_shade = palette.button();
        if (shadow == FrameShadow::Raised) {
            dark_shade = palette.threed_shadow1();
            top_left_color = light_shade;
            bottom_right_color = dark_shade;
        } else if (shadow == FrameShadow::Sunken) {
            top_left_color = dark_shade;
            bottom_right_color = light_shade;
        } else if (shadow == FrameShadow::Plain) {
            top_left_color = dark_shade;
            bottom_right_color = dark_shade;
        }
        Rect inner_container_frame_rect = rect.shrunken(2, 2);
        painter.draw_line(inner_container_frame_rect.top_left(), inner_container_frame_rect.top_right(), top_left_color);
        painter.draw_line(inner_container_frame_rect.bottom_left(), inner_container_frame_rect.bottom_right(), bottom_right_color);
        painter.draw_line(inner_container_frame_rect.top_left().translated(0, 1), inner_container_frame_rect.bottom_left().translated(0, -1), top_left_color);
        painter.draw_line(inner_container_frame_rect.top_right(), inner_container_frame_rect.bottom_right().translated(0, -1), bottom_right_color);
    }

    if (shape == FrameShape::Box && thickness >= 2) {
        swap(top_left_color, bottom_right_color);
        Rect inner_rect = rect.shrunken(2, 2);
        painter.draw_line(inner_rect.top_left(), inner_rect.top_right(), top_left_color);
        painter.draw_line(inner_rect.bottom_left(), inner_rect.bottom_right(), bottom_right_color);
        painter.draw_line(inner_rect.top_left().translated(0, 1), inner_rect.bottom_left().translated(0, -1), top_left_color);
        painter.draw_line(inner_rect.top_right(), inner_rect.bottom_right().translated(0, -1), bottom_right_color);
    }
}

void StylePainter::paint_window_frame(Painter& painter, const Rect& rect, const Palette& palette)
{
    Color base_color = palette.button();
    Color dark_shade = palette.threed_shadow2();
    Color mid_shade = palette.threed_shadow1();
    Color light_shade = palette.threed_highlight();

    painter.draw_line(rect.top_left(), rect.top_right(), base_color);
    painter.draw_line(rect.top_left().translated(0, 1), rect.bottom_left(), base_color);
    painter.draw_line(rect.top_left().translated(1, 1), rect.top_right().translated(-1, 1), light_shade);
    painter.draw_line(rect.top_left().translated(1, 1), rect.bottom_left().translated(1, -1), light_shade);
    painter.draw_line(rect.top_left().translated(2, 2), rect.top_right().translated(-2, 2), base_color);
    painter.draw_line(rect.top_left().translated(2, 2), rect.bottom_left().translated(2, -2), base_color);

    painter.draw_line(rect.top_right(), rect.bottom_right(), dark_shade);
    painter.draw_line(rect.top_right().translated(-1, 1), rect.bottom_right().translated(-1, -1), mid_shade);
    painter.draw_line(rect.top_right().translated(-2, 2), rect.bottom_right().translated(-2, -2), base_color);
    painter.draw_line(rect.bottom_left(), rect.bottom_right(), dark_shade);
    painter.draw_line(rect.bottom_left().translated(1, -1), rect.bottom_right().translated(-1, -1), mid_shade);
    painter.draw_line(rect.bottom_left().translated(2, -2), rect.bottom_right().translated(-2, -2), base_color);
}

void StylePainter::paint_progress_bar(Painter& painter, const Rect& rect, const Palette& palette, int min, int max, int value, const StringView& text)
{
    // First we fill the entire widget with the gradient. This incurs a bit of
    // overdraw but ensures a consistent look throughout the progression.
    Color start_color = palette.active_window_border1();
    Color end_color = palette.active_window_border2();
    painter.fill_rect_with_gradient(rect, start_color, end_color);

    if (!text.is_null()) {
        painter.draw_text(rect.translated(1, 1), text, TextAlignment::Center, Color::Black);
        painter.draw_text(rect, text, TextAlignment::Center, palette.base_text());
    }

    float range_size = max - min;
    float progress = (value - min) / range_size;

    // Then we carve out a hole in the remaining part of the widget.
    // We draw the text a third time, clipped and inverse, for sharp contrast.
    float progress_width = progress * rect.width();
    Rect hole_rect { (int)progress_width, 0, (int)(rect.width() - progress_width), rect.height() };
    hole_rect.move_by(rect.location());
    hole_rect.set_right_without_resize(rect.right());
    PainterStateSaver saver(painter);
    painter.fill_rect(hole_rect, palette.base());

    painter.add_clip_rect(hole_rect);
    if (!text.is_null())
        painter.draw_text(rect.translated(0, 0), text, TextAlignment::Center, palette.base_text());
}
