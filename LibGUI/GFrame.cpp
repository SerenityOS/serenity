#include <LibGUI/GFrame.h>
#include <LibGUI/GStyle.h>
#include <LibGUI/GPainter.h>

GFrame::GFrame(GWidget* parent)
    : GWidget(parent)
{
}

GFrame::~GFrame()
{
}

void GFrame::paint_event(GPaintEvent& event)
{
    if (m_shape == Shape::NoFrame)
        return;

    GPainter painter(*this);
    painter.set_clip_rect(event.rect());

    auto rect = this->rect();

    Color top_left_color;
    Color bottom_right_color;

    Color dark_shade = Color::from_rgb(0x808080);
    Color light_shade = Color::from_rgb(0xffffff);

    if (m_shape == Shape::Container) {
        dark_shade = Color::from_rgb(0x404040);
    }

    if (m_shadow == Shadow::Raised) {
        top_left_color = light_shade;
        bottom_right_color = dark_shade;
    } else if (m_shadow == Shadow::Sunken) {
        top_left_color = dark_shade;
        bottom_right_color = light_shade;
    } else if (m_shadow == Shadow::Plain) {
        top_left_color = dark_shade;
        bottom_right_color = dark_shade;
    }

    painter.draw_line(rect.top_left(), rect.top_right(), top_left_color);
    painter.draw_line(rect.bottom_left(), rect.bottom_right(), bottom_right_color);

    painter.draw_line(rect.top_left().translated(0, 1), rect.bottom_left().translated(0, -1), top_left_color);
    painter.draw_line(rect.top_right(), rect.bottom_right().translated(0, -1), bottom_right_color);
}
