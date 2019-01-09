#include "AbstractScreen.h"
#include "GraphicsBitmap.h"
#include "RootWidget.h"
#include "Painter.h"
#include "WindowManager.h"
#include "FrameBufferSDL.h"
#include <cstdio>

RootWidget::RootWidget()
{
    setWindowRelativeRect(FrameBufferSDL::the().rect());
    m_backing = GraphicsBitmap::create_wrapper(size(), (byte*)FrameBufferSDL::the().scanline(0));
}

RootWidget::~RootWidget()
{
}

void RootWidget::paintEvent(PaintEvent& event)
{
    Painter painter(*this);
    painter.fillRect(event.rect(), Color(0, 72, 96));
}

void RootWidget::mouseMoveEvent(MouseEvent& event)
{
    //printf("RootWidget::mouseMoveEvent: x=%d, y=%d\n", event.x(), event.y());
    Widget::mouseMoveEvent(event);
}

