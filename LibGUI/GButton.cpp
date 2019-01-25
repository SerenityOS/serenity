#include "GButton.h"
#include <SharedGraphics/Painter.h>

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

    painter.draw_line({ 1, 0 }, { width() - 2, 0 }, Color::Black);
    painter.draw_line({ 1, height() - 1 }, { width() - 2, height() - 1}, Color::Black);
    painter.draw_line({ 0, 1 }, { 0, height() - 2 }, Color::Black);
    painter.draw_line({ width() - 1, 1 }, { width() - 1, height() - 2 }, Color::Black);

    if (m_being_pressed) {
        // Base
        painter.fill_rect({ 1, 1, width() - 2, height() - 2 }, button_color);

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
        auto textRect = rect();
        if (m_being_pressed)
            textRect.move_by(1, 1);
        painter.draw_text(textRect, caption(), Painter::TextAlignment::Center, Color::Black);
    }
}

void GButton::mousedown_event(GMouseEvent& event)
{
    dbgprintf("Button::mouseDownEvent: x=%d, y=%d, button=%u\n", event.x(), event.y(), (unsigned)event.button());

    m_being_pressed = true;

    update();
    GWidget::mousedown_event(event);
}

void GButton::mouseup_event(GMouseEvent& event)
{
    dbgprintf("Button::mouseUpEvent: x=%d, y=%d, button=%u\n", event.x(), event.y(), (unsigned)event.button());

    m_being_pressed = false;

    update();
    GWidget::mouseup_event(event);

    if (on_click)
        on_click(*this);
}

