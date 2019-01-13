#include "Button.h"
#include "Painter.h"

Button::Button(Widget* parent)
    : Widget(parent)
{
    setFillWithBackgroundColor(false);
}

Button::~Button()
{
}

void Button::setCaption(String&& caption)
{
    if (caption == m_caption)
        return;
    m_caption = move(caption);
    update();
}

void Button::paintEvent(PaintEvent&)
{
    Color buttonColor = Color::LightGray;
    Color highlightColor = Color::White;
    Color shadowColor = Color(96, 96, 96);

    Painter painter(*this);

    painter.draw_line({ 1, 0 }, { width() - 2, 0 }, Color::Black);
    painter.draw_line({ 1, height() - 1 }, { width() - 2, height() - 1}, Color::Black);
    painter.draw_line({ 0, 1 }, { 0, height() - 2 }, Color::Black);
    painter.draw_line({ width() - 1, 1 }, { width() - 1, height() - 2 }, Color::Black);

    if (m_beingPressed) {
        // Base
        painter.fill_rect({ 1, 1, width() - 2, height() - 2 }, buttonColor);

        // Sunken shadow
        painter.draw_line({ 1, 1 }, { width() - 2, 1 }, shadowColor);
        painter.draw_line({ 1, 2 }, {1, height() - 2 }, shadowColor);
    } else {
        // Base
        painter.fill_rect({ 3, 3, width() - 5, height() - 5 }, buttonColor);

        // White highlight
        painter.draw_line({ 1, 1 }, { width() - 2, 1 }, highlightColor);
        painter.draw_line({ 1, 2 }, { width() - 3, 2 }, highlightColor);
        painter.draw_line({ 1, 3 }, { 1, height() - 2 }, highlightColor);
        painter.draw_line({ 2, 3 }, { 2, height() - 3 }, highlightColor);

        // Gray shadow 
        painter.draw_line({ width() - 2, 1 }, { width() - 2, height() - 4 }, shadowColor);
        painter.draw_line({ width() - 3, 2 }, { width() - 3, height() - 4 }, shadowColor);
        painter.draw_line({ 1, height() - 2 }, { width() - 2, height() - 2 }, shadowColor);
        painter.draw_line({ 2, height() - 3 }, { width() - 2, height() - 3 }, shadowColor);
    }

    if (!caption().is_empty()) {
        auto textRect = rect();
        if (m_beingPressed)
            textRect.moveBy(1, 1);
        painter.draw_text(textRect, caption(), Painter::TextAlignment::Center, Color::Black);
    }
}

void Button::mouseDownEvent(MouseEvent& event)
{
    printf("Button::mouseDownEvent: x=%d, y=%d, button=%u\n", event.x(), event.y(), (unsigned)event.button());

    m_beingPressed = true;

    update();
    Widget::mouseDownEvent(event);
}

void Button::mouseUpEvent(MouseEvent& event)
{
    printf("Button::mouseUpEvent: x=%d, y=%d, button=%u\n", event.x(), event.y(), (unsigned)event.button());

    m_beingPressed = false;

    update();
    Widget::mouseUpEvent(event);

    if (onClick)
        onClick(*this);
}

