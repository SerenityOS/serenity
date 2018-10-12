#include "Label.h"
#include "Painter.h"
#include <cstdio>

Label::Label(Widget* parent)
    : Widget(parent)
{
}

Label::~Label()
{
}

void Label::setText(String&& text)
{
    if (text == m_text)
        return;
    m_text = std::move(text);
    update();
}

void Label::onPaint(PaintEvent&)
{
    Painter painter(*this);
    painter.fillRect({ 0, 0, width(), height() }, backgroundColor());
    if (!text().isEmpty())
        painter.drawText({ 4, 4, width(), height() }, text(), Painter::TextAlignment::TopLeft, foregroundColor());
}

void Label::onMouseMove(MouseEvent& event)
{
    printf("Label::onMouseMove: x=%d, y=%d\n", event.x(), event.y());
    Widget::onMouseMove(event);
}

