#include <LibGUI/GFrame.h>
#include <SharedGraphics/StylePainter.h>
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

    Color top_left_color;
    Color bottom_right_color;
    Color dark_shade = Color::from_rgb(0x808080);
    Color light_shade = Color::from_rgb(0xffffff);

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

    if (m_thickness >= 1) {
        painter.draw_line(rect().top_left(), rect().top_right(), top_left_color);
        painter.draw_line(rect().bottom_left(), rect().bottom_right(), bottom_right_color);

        if (m_shape != Shape::Panel || !spans_entire_window_horizontally()) {
            painter.draw_line(rect().top_left().translated(0, 1), rect().bottom_left().translated(0, -1), top_left_color);
            painter.draw_line(rect().top_right(), rect().bottom_right().translated(0, -1), bottom_right_color);
        }
    }

    if (m_shape == Shape::Container && m_thickness >= 2) {
        Color top_left_color;
        Color bottom_right_color;
        Color dark_shade = Color::from_rgb(0x404040);
        Color light_shade = Color::from_rgb(0xc0c0c0);
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
        Rect inner_container_frame_rect = rect().shrunken(2, 2);
        painter.draw_line(inner_container_frame_rect.top_left(), inner_container_frame_rect.top_right(), top_left_color);
        painter.draw_line(inner_container_frame_rect.bottom_left(), inner_container_frame_rect.bottom_right(), bottom_right_color);
        painter.draw_line(inner_container_frame_rect.top_left().translated(0, 1), inner_container_frame_rect.bottom_left().translated(0, -1), top_left_color);
        painter.draw_line(inner_container_frame_rect.top_right(), inner_container_frame_rect.bottom_right().translated(0, -1), bottom_right_color);
    }
}
