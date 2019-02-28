#include "GButton.h"
#include <SharedGraphics/Painter.h>
#include <LibGUI/GStyle.h>

//#define GBUTTON_DEBUG

GButton::GButton(GWidget* parent)
    : GWidget(parent)
{
    set_fill_with_background_color(false);
}

GButton::~GButton()
{
}

void GButton::set_caption(String&& caption)
{
    if (caption == m_caption)
        return;
    m_caption = move(caption);
    update();
}

void GButton::paint_event(GPaintEvent& event)
{
    Painter painter(*this);
    painter.set_clip_rect(event.rect());

    GStyle::the().paint_button(painter, rect(), m_button_style, m_being_pressed, m_hovered);

    if (!caption().is_empty() || m_icon) {
        auto content_rect = rect();
        auto icon_location = m_icon ? content_rect.center().translated(-(m_icon->width() / 2), -(m_icon->height() / 2)) : Point();
        if (m_being_pressed) {
            content_rect.move_by(1, 1);
            icon_location.move_by(1, 1);
        }
        if (m_icon) {
            painter.blit(icon_location, *m_icon, m_icon->rect());
            painter.draw_text(content_rect, caption(), TextAlignment::Center, Color::Black);
        } else {
            painter.draw_text(content_rect, caption(), TextAlignment::Center, Color::Black);
        }
    }
}

void GButton::mousemove_event(GMouseEvent& event)
{
    if (m_tracking_cursor) {
        bool being_pressed = rect().contains(event.position());
        if (being_pressed != m_being_pressed) {
            m_being_pressed = being_pressed;
            update();
        }
    }
    GWidget::mousemove_event(event);
}

void GButton::mousedown_event(GMouseEvent& event)
{
#ifdef GBUTTON_DEBUG
    dbgprintf("GButton::mouse_down_event: x=%d, y=%d, button=%u\n", event.x(), event.y(), (unsigned)event.button());
#endif
    if (event.button() == GMouseButton::Left) {
        m_being_pressed = true;
        m_tracking_cursor = true;
        set_global_cursor_tracking(true);
        update();
    }
    GWidget::mousedown_event(event);
}

void GButton::mouseup_event(GMouseEvent& event)
{
#ifdef GBUTTON_DEBUG
    dbgprintf("GButton::mouse_up_event: x=%d, y=%d, button=%u\n", event.x(), event.y(), (unsigned)event.button());
#endif
    if (event.button() == GMouseButton::Left) {
        bool was_being_pressed = m_being_pressed;
        m_being_pressed = false;
        m_tracking_cursor = false;
        set_global_cursor_tracking(false);
        update();
        if (was_being_pressed) {
            if (on_click)
                on_click(*this);
        }
    }
    GWidget::mouseup_event(event);
}

void GButton::enter_event(GEvent&)
{
    m_hovered = true;
    update();
}

void GButton::leave_event(GEvent&)
{
    m_hovered = false;
    update();
}
