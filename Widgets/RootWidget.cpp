#include "RootWidget.h"
#include "Painter.h"
#include <cstdio>

RootWidget::RootWidget()
{
}

RootWidget::~RootWidget()
{
}

void RootWidget::onPaint(PaintEvent& event)
{
    printf("RootWidget::onPaint\n");
    Painter painter(*this);
    painter.fillRect(Rect(0, 0, 800, 600), Color(0x80, 0x80, 0x80));
    Widget::onPaint(event);
}

void RootWidget::onMouseMove(MouseEvent& event)
{
    printf("RootWidget::onMouseMove: x=%d, y=%d\n", event.x(), event.y());
    Widget::onMouseMove(event);
}

