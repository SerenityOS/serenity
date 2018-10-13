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

void RootWidget::paintEvent(PaintEvent& event)
{
    Widget::paintEvent(event);

    printf("RootWidget::paintEvent: %d,%d %dx%d\n",
            event.rect().x(),
            event.rect().y(),
            event.rect().width(),
            event.rect().height());
    Painter painter(*this);
    painter.fillRect(event.rect(), Color(0, 72, 96));
}

void RootWidget::mouseMoveEvent(MouseEvent& event)
{
    //printf("RootWidget::mouseMoveEvent: x=%d, y=%d\n", event.x(), event.y());
    Widget::mouseMoveEvent(event);
}

