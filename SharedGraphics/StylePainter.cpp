#include <SharedGraphics/StylePainter.h>
#include <LibGUI/GPainter.h>

static void paint_button_new(Painter& painter, const Rect& rect, bool pressed, bool checked, bool hovered)
{
    Color button_color = Color::from_rgb(0xc0c0c0);
    Color highlight_color2 = Color::from_rgb(0xdfdfdf);
    Color shadow_color1 = Color::from_rgb(0x808080);
    Color shadow_color2 = Color::from_rgb(0x404040);

    if (checked) {
        if (hovered)
            button_color = Color::from_rgb(0xe3dfdb);
        else
            button_color = Color::from_rgb(0xd6d2ce);
    } else if (hovered)
        button_color = Color::from_rgb(0xd4d4d4);

    PainterStateSaver saver(painter);
    painter.translate(rect.location());

    if (pressed || checked) {
        // Base
        painter.fill_rect({ 1, 1, rect.width() - 2, rect.height() - 2 }, button_color);

        painter.draw_rect(rect, shadow_color2);

        // Sunken shadow
        painter.draw_line({ 1, 1 }, { rect.width() - 2, 1 }, shadow_color1);
        painter.draw_line({ 1, 2 }, {1, rect.height() - 2 }, shadow_color1);
    } else {
        // Base
        painter.fill_rect({ 1, 1, rect.width() - 3, rect.height() - 3 }, button_color);

        // Outer highlight
        painter.draw_line({ 0, 0 }, { rect.width() - 2, 0 }, highlight_color2);
        painter.draw_line({ 0, 1 }, { 0, rect.height() - 2 }, highlight_color2);

#if 0
        // Inner highlight (this looks "too thick" to me right now..)
        Color highlight_color1 = Color::from_rgb(0xffffff);
        painter.draw_line({ 1, 1 }, { rect.width() - 3, 1 }, highlight_color1);
        painter.draw_line({ 1, 2 }, { 1, rect.height() - 3 }, highlight_color1);
#endif

        // Outer shadow
        painter.draw_line({ 0, rect.height() - 1 }, { rect.width() - 1, rect.height() - 1 }, shadow_color2);
        painter.draw_line({ rect.width() - 1, 0 }, { rect.width() - 1, rect.height() - 2 }, shadow_color2);

        // Inner shadow
        painter.draw_line({ 1, rect.height() - 2 }, { rect.width() - 2, rect.height() - 2 }, shadow_color1);
        painter.draw_line({ rect.width() - 2, 1 }, { rect.width() - 2, rect.height() - 3 }, shadow_color1);
    }
}

void StylePainter::paint_button(Painter& painter, const Rect& rect, ButtonStyle button_style, bool pressed, bool hovered, bool checked, bool enabled)
{
    if (button_style == ButtonStyle::Normal)
        return paint_button_new(painter, rect, pressed, checked, hovered);

    Color button_color = Color::LightGray;
    Color highlight_color = Color::White;
    Color shadow_color = Color(96, 96, 96);

    if (button_style == ButtonStyle::OldNormal)
        painter.draw_rect(rect, Color::Black);

    if (button_style == ButtonStyle::CoolBar && !enabled)
        return;

    PainterStateSaver saver(painter);
    painter.translate(rect.location());

    if (pressed) {
        // Base
        painter.fill_rect({ 1, 1, rect.width() - 2, rect.height() - 2 }, button_color);

        // Sunken shadow
        painter.draw_line({ 1, 1 }, { rect.width() - 2, 1 }, shadow_color);
        painter.draw_line({ 1, 2 }, {1, rect.height() - 2 }, shadow_color);

        // Bottom highlight
        painter.draw_line({ rect.width() - 2, 1 }, { rect.width() - 2, rect.height() - 3 }, highlight_color);
        painter.draw_line({ 1, rect.height() - 2 }, { rect.width() - 2, rect.height() - 2 }, highlight_color);
    } else if (button_style == ButtonStyle::OldNormal || (button_style == ButtonStyle::CoolBar && hovered)) {
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

void StylePainter::paint_surface(Painter& painter, const Rect& rect, bool paint_vertical_lines)
{
    painter.fill_rect({ rect.x(), rect.y() + 1, rect.width(), rect.height() - 2 }, Color::LightGray);
    painter.draw_line(rect.top_left(), rect.top_right(), Color::White);
    painter.draw_line(rect.bottom_left(), rect.bottom_right(), Color::MidGray);
    if (paint_vertical_lines) {
        painter.draw_line(rect.top_left().translated(0, 1), rect.bottom_left().translated(0, -1), Color::White);
        painter.draw_line(rect.top_right(), rect.bottom_right().translated(0, -1), Color::MidGray);
    }
}

void StylePainter::paint_frame(Painter& painter, const Rect& rect, FrameShape shape, FrameShadow shadow, int thickness, bool skip_vertical_lines)
{
    Color top_left_color;
    Color bottom_right_color;
    Color dark_shade = Color::from_rgb(0x808080);
    Color light_shade = Color::from_rgb(0xffffff);

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
        Color dark_shade = Color::from_rgb(0x404040);
        Color light_shade = Color::from_rgb(0xc0c0c0);
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
