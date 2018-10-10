#include "Label.h"
#include "Painter.h"
#include <cstdio>

Label::Label(Widget* parent)
    : Widget(parent)
{
    setRect(Rect(100, 100, 100, 100));
}

Label::~Label()
{
}

void Label::onPaint(PaintEvent&)
{
    Painter painter(*this);
    painter.fillRect({ 0, 0, width(), height() }, Color(0xc0, 0xc0, 0xc0));
}

void Label::onMouseMove(MouseEvent& event)
{
    printf("Label::onMouseMove: x=%d, y=%d\n", event.x(), event.y());
    Widget::onMouseMove(event);
}

