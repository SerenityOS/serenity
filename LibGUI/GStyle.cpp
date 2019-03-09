#include <LibGUI/GStyle.h>
#include <SharedGraphics/Painter.h>

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

void GStyle::paint_button(Painter& painter, const Rect& rect, GButtonStyle button_style, bool pressed, bool hovered)
{
    Color button_color = Color::LightGray;
    Color highlight_color = Color::White;
    Color shadow_color = Color(96, 96, 96);

    if (button_style == GButtonStyle::Normal)
        painter.draw_rect(rect, Color::Black, true);

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
    } else if (button_style == GButtonStyle::Normal || (button_style == GButtonStyle::CoolBar && hovered)) {
        // Base
        painter.fill_rect({ 3, 3, rect.width() - 5, rect.height() - 5 }, button_color);

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
    painter.draw_line(rect.bottom_left(), rect.bottom_right(), Color::DarkGray);
    painter.draw_line(rect.top_left().translated(0, 1), rect.bottom_left().translated(0, -1), Color::White);
    painter.draw_line(rect.top_right(), rect.bottom_right().translated(0, -1), Color::DarkGray);
}
