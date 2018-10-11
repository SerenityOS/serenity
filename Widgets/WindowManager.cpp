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
    static const int windowTitleBarHeight = 16;

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

    static const Color windowBorderColor(0x00, 0x00, 0x80);
    static const Color windowTitleColor(0xff, 0xff, 0xff);

    Rect borderRect {
        topRect.x() - 1, 
        topRect.y() - 1,
        topRect.width() + 2,
        windowFrameWidth + windowTitleBarHeight + widget.height() + 4
    };
    p.drawRect(borderRect, Color(255, 255, 255));
    borderRect.inflate(2, 2);
    p.drawRect(borderRect, windowBorderColor);

    p.fillRect(topRect, windowBorderColor);
    p.fillRect(bottomRect, windowBorderColor);
    p.fillRect(leftRect, windowBorderColor);
    p.fillRect(rightRect, windowBorderColor);

    p.drawText(topRect, widget.windowTitle(), Painter::TextAlignment::Center, windowTitleColor);
}

void WindowManager::addWindow(Widget& widget)
{
    m_windows.set(&widget);
}

void WindowManager::notifyTitleChanged(Widget&)
{
    AbstractScreen::the().rootWidget()->update();
}

