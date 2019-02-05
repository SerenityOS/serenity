#include "GButton.h"
#include <SharedGraphics/Painter.h>

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

void GButton::paint_event(GPaintEvent&)
{
    Color button_color = Color::LightGray;
    Color highlight_color = Color::White;
    Color shadow_color = Color(96, 96, 96);

    Painter painter(*this);
    painter.draw_rect(rect(), Color::Black, true);

    if (m_being_pressed) {
        // Base
        painter.fill_rect(rect().shrunken(2, 2), button_color);

        // Sunken shadow
        painter.draw_line({ 1, 1 }, { width() - 2, 1 }, shadow_color);
        painter.draw_line({ 1, 2 }, {1, height() - 2 }, shadow_color);
    } else {
        // Base
        painter.fill_rect({ 3, 3, width() - 5, height() - 5 }, button_color);
        painter.fill_rect_with_gradient({ 3, 3, width() - 5, height() - 5 }, button_color, Color::White);

        // White highlight
        painter.draw_line({ 1, 1 }, { width() - 2, 1 }, highlight_color);
        painter.draw_line({ 1, 2 }, { width() - 3, 2 }, highlight_color);
        painter.draw_line({ 1, 3 }, { 1, height() - 2 }, highlight_color);
        painter.draw_line({ 2, 3 }, { 2, height() - 3 }, highlight_color);

        // Gray shadow 
        painter.draw_line({ width() - 2, 1 }, { width() - 2, height() - 4 }, shadow_color);
        painter.draw_line({ width() - 3, 2 }, { width() - 3, height() - 4 }, shadow_color);
        painter.draw_line({ 1, height() - 2 }, { width() - 2, height() - 2 }, shadow_color);
        painter.draw_line({ 2, height() - 3 }, { width() - 2, height() - 3 }, shadow_color);
    }

    if (!caption().is_empty()) {
        auto text_rect = rect();
        if (m_being_pressed)
            text_rect.move_by(1, 1);
        painter.draw_text(text_rect, caption(), Painter::TextAlignment::Center, Color::Black);
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

