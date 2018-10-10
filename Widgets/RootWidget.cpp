#include "RootWidget.h"
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
    Widget::onPaint(event);
}

void RootWidget::onMouseMove(MouseEvent& event)
{
    printf("RootWidget::onMouseMove: x=%d, y=%d\n", event.x(), event.y());
    Widget::onMouseMove(event);
}

