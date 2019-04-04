#include <SharedGraphics/StylePainter.h>
#include <LibGUI/GPainter.h>

static void paint_button_new(Painter& painter, const Rect& rect, bool pressed, bool checked)
{
    Color button_color = Color::from_rgb(0xc0c0c0);
    Color highlight_color2 = Color::from_rgb(0xdfdfdf);
    Color shadow_color1 = Color::from_rgb(0x808080);
    Color shadow_color2 = Color::from_rgb(0x404040);

    if (checked)
        button_color = Color::from_rgb(0xd6d2ce);

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

void StylePainter::paint_button(Painter& painter, const Rect& rect, ButtonStyle button_style, bool pressed, bool hovered, bool checked)
{
    if (button_style == ButtonStyle::Normal)
        return paint_button_new(painter, rect, pressed, checked);

    Color button_color = Color::LightGray;
    Color highlight_color = Color::White;
    Color shadow_color = Color(96, 96, 96);

    if (button_style == ButtonStyle::OldNormal)
        painter.draw_rect(rect, Color::Black);

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
