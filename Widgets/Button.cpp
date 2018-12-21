#include "Button.h"
#include "Painter.h"
#include <cstdio>

Button::Button(Widget* parent)
    : Widget(parent)
{
}

Button::~Button()
{
}

void Button::setCaption(String&& caption)
{
    if (caption == m_caption)
        return;
    m_caption = std::move(caption);
    update();
}

void Button::paintEvent(PaintEvent&)
{
    Color buttonColor = Color::LightGray;
    Color highlightColor = Color::White;
    Color shadowColor = Color(96, 96, 96);

    Painter painter(*this);

    painter.drawPixel({ 0, 0 }, backgroundColor());
    painter.drawPixel({ width() - 1, 0 }, backgroundColor());
    painter.drawPixel({ 0, height() - 1 }, backgroundColor());
    painter.drawPixel({ width() - 1, height() - 1 }, backgroundColor());

    painter.drawLine({ 1, 0 }, { width() - 2, 0 }, Color::Black);
    painter.drawLine({ 1, height() - 1 }, { width() - 2, height() - 1}, Color::Black);
    painter.drawLine({ 0, 1 }, { 0, height() - 2 }, Color::Black);
    painter.drawLine({ width() - 1, 1 }, { width() - 1, height() - 2 }, Color::Black);

    if (m_beingPressed) {
        // Base
        painter.fillRect({ 1, 1, width() - 2, height() - 2 }, buttonColor);

        // Sunken shadow
        painter.drawLine({ 1, 1 }, { width() - 2, 1 }, shadowColor);
        painter.drawLine({ 1, 2 }, {1, height() - 2 }, shadowColor);
    } else {
        // Base
        painter.fillRect({ 3, 3, width() - 6, height() - 6 }, buttonColor);

        // White highlight
        painter.drawLine({ 1, 1 }, { width() - 2, 1 }, highlightColor);
        painter.drawLine({ 1, 2 }, { width() - 3, 2 }, highlightColor);
        painter.drawLine({ 1, 3 }, { 1, height() - 2 }, highlightColor);
        painter.drawLine({ 2, 3 }, { 2, height() - 3 }, highlightColor);

        // Gray shadow 
        painter.drawLine({ width() - 2, 1 }, { width() - 2, height() - 4 }, shadowColor);
        painter.drawLine({ width() - 3, 2 }, { width() - 3, height() - 4 }, shadowColor);
        painter.drawLine({ 1, height() - 2 }, { width() - 2, height() - 2 }, shadowColor);
        painter.drawLine({ 2, height() - 3 }, { width() - 2, height() - 3 }, shadowColor);
    }

    if (!caption().is_empty()) {
        auto textRect = rect();
        if (m_beingPressed)
            textRect.moveBy(1, 1);
        painter.drawText(textRect, caption(), Painter::TextAlignment::Center, Color::Black);
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

