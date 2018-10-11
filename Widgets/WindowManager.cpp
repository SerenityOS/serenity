#include "WindowManager.h"
#include "Painter.h"
#include "Widget.h"
#include "AbstractScreen.h"

WindowManager& WindowManager::the()
{
    static WindowManager* s_the = new WindowManager;
    return *s_the;
}

WindowManager::WindowManager()
{
}

WindowManager::~WindowManager()
{
}

void WindowManager::paintWindowFrames()
{
    for (auto* widget : m_windows) {
        paintWindowFrame(*widget);
    }
}

void WindowManager::paintWindowFrame(Widget& widget)
{
    Painter p(*AbstractScreen::the().rootWidget());

    printf("WM: paintWindowFrame %s{%p}, rect: %d,%d %dx%d\n", widget.className(), &widget, widget.rect().x(), widget.rect().y(), widget.rect().width(), widget.rect().height());

    static const int windowFrameWidth = 2;
    static const int windowTitleBarHeight = 14;

    Rect topRect {
        widget.x() - windowFrameWidth,
        widget.y() - windowTitleBarHeight - windowFrameWidth,
        widget.width() + windowFrameWidth * 2,
        windowTitleBarHeight + windowFrameWidth };

    Rect bottomRect {
        widget.x() - windowFrameWidth,
        widget.y() + widget.height(),
        widget.width() + windowFrameWidth * 2,
        windowFrameWidth };

    Rect leftRect {
        widget.x() - windowFrameWidth,
        widget.y(),
        windowFrameWidth,
        widget.height()
    };

    Rect rightRect {
        widget.x() + widget.width(),
        widget.y(),
        windowFrameWidth,
        widget.height()
    };

    p.fillRect(topRect, Color(0x40, 0x40, 0xc0));
    p.fillRect(bottomRect, Color(0x40, 0x40, 0xc0));
    p.fillRect(leftRect, Color(0x40, 0x40, 0xc0));
    p.fillRect(rightRect, Color(0x40, 0x40, 0xc0));

    p.drawText(topRect, widget.windowTitle(), Painter::TextAlignment::Center, Color(255, 255, 255));
}

void WindowManager::addWindow(Widget& widget)
{
    m_windows.set(&widget);
}

void WindowManager::notifyTitleChanged(Widget&)
{
    AbstractScreen::the().rootWidget()->update();
}

