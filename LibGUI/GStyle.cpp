#include <LibGUI/GStyle.h>
#include <LibGUI/GPainter.h>

static GStyle* s_the;

GStyle& GStyle::the()
{
    if (!s_the)
        s_the = new GStyle;
    return *s_the;
}

GStyle::GStyle()
{
}

static void paint_button_new(Painter& painter, const Rect& rect, bool pressed)
{
    Color button_color = Color::from_rgb(0xc0c0c0);
    Color highlight_color1 = Color::from_rgb(0xffffff);
    Color highlight_color2 = Color::from_rgb(0xdfdfdf);
    Color shadow_color1 = Color::from_rgb(0x808080);
    Color shadow_color2 = Color::from_rgb(0x404040);

    PainterStateSaver saver(painter);
    painter.translate(rect.location());

    if (pressed) {
        // Base
        painter.fill_rect({ 1, 1, rect.width() - 2, rect.height() - 2 }, button_color);

        painter.draw_rect(rect, shadow_color2);

        // Sunken shadow
        painter.draw_line({ 1, 1 }, { rect.width() - 2, 1 }, shadow_color1);
        painter.draw_line({ 1, 2 }, {1, rect.height() - 2 }, shadow_color1);
    } else {
        // Base
        painter.fill_rect({ 2, 2, rect.width() - 4, rect.height() - 4 }, button_color);

        // Outer highlight
        painter.draw_line({ 0, 0 }, { rect.width() - 2, 0 }, highlight_color2);
        painter.draw_line({ 0, 1 }, { 0, rect.height() - 2 }, highlight_color2);

        // Inner highlight
        painter.draw_line({ 1, 1 }, { rect.width() - 3, 1 }, highlight_color1);
        painter.draw_line({ 1, 2 }, { 1, rect.height() - 3 }, highlight_color1);

        // Outer shadow
        painter.draw_line({ 0, rect.height() - 1 }, { rect.width() - 1, rect.height() - 1 }, shadow_color2);
        painter.draw_line({ rect.width() - 1, 0 }, { rect.width() - 1, rect.height() - 2 }, shadow_color2);

        // Inner shadow
        painter.draw_line({ 1, rect.height() - 2 }, { rect.width() - 2, rect.height() - 2 }, shadow_color1);
        painter.draw_line({ rect.width() - 2, 1 }, { rect.width() - 2, rect.height() - 3 }, shadow_color1);
    }
}

void GStyle::paint_button(Painter& painter, const Rect& rect, GButtonStyle button_style, bool pressed, bool hovered)
{
    if (button_style == GButtonStyle::Normal)
        return paint_button_new(painter, rect, pressed);

    Color button_color = Color::LightGray;
    Color highlight_color = Color::White;
    Color shadow_color = Color(96, 96, 96);

    if (button_style == GButtonStyle::OldNormal)
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
    } else if (button_style == GButtonStyle::OldNormal || (button_style == GButtonStyle::CoolBar && hovered)) {
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

void GStyle::paint_surface(Painter& painter, const Rect& rect)
{
    painter.fill_rect({ rect.x(), rect.y() + 1, rect.width(), rect.height() - 2 }, Color::LightGray);
    painter.draw_line(rect.top_left(), rect.top_right(), Color::White);
    painter.draw_line(rect.bottom_left(), rect.bottom_right(), Color::MidGray);
    painter.draw_line(rect.top_left().translated(0, 1), rect.bottom_left().translated(0, -1), Color::White);
    painter.draw_line(rect.top_right(), rect.bottom_right().translated(0, -1), Color::MidGray);
}
