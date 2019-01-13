#include "Label.h"
#include "Painter.h"

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
    m_text = move(text);
    update();
}

void Label::paintEvent(PaintEvent&)
{
    Painter painter(*this);
    if (fillWithBackgroundColor())
        painter.fill_rect({ 0, 0, width(), height() }, backgroundColor());
    if (!text().is_empty())
        painter.draw_text({ 4, 4, width(), height() }, text(), Painter::TextAlignment::TopLeft, foregroundColor());
}

void Label::mouseMoveEvent(MouseEvent& event)
{
    printf("Label::mouseMoveEvent: x=%d, y=%d\n", event.x(), event.y());
    Widget::mouseMoveEvent(event);
}

