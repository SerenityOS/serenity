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

void Button::onPaint(PaintEvent&)
{
    Painter painter(*this);
    painter.fillRect(rect(), backgroundColor());
    if (!caption().isEmpty()) {
        painter.drawText(rect(), caption(), Painter::TextAlignment::Center, Color(0, 0, 0));
    }
}

void Button::onMouseDown(MouseEvent& event)
{
    printf("Button::onMouseDown: x=%d, y=%d, button=%u\n", event.x(), event.y(), (unsigned)event.button());

    setBackgroundColor(Color(0xff, 0xc0, 0xc0));
    update();
    Widget::onMouseDown(event);
}

