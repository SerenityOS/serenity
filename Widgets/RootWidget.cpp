#include "RootWidget.h"
#include "Painter.h"
#include "WindowManager.h"
#include <cstdio>

RootWidget::RootWidget()
{
}

RootWidget::~RootWidget()
{
}

void RootWidget::onPaint(PaintEvent& event)
{
    Widget::onPaint(event);

    printf("RootWidget::onPaint: %d,%d %dx%d\n",
            event.rect().x(),
            event.rect().y(),
            event.rect().width(),
            event.rect().height());
    Painter painter(*this);
    painter.fillRect(event.rect(), Color(0x40, 0x40, 0x40));
}

void RootWidget::onMouseMove(MouseEvent& event)
{
    //printf("RootWidget::onMouseMove: x=%d, y=%d\n", event.x(), event.y());
    Widget::onMouseMove(event);
}

