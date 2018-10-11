#include "WindowManager.h"
#include "Painter.h"
#include "Widget.h"
#include "Window.h"
#include "AbstractScreen.h"
#include "TerminalWidget.h"
#include "EventLoop.h"

extern TerminalWidget* g_tw;

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
    for (auto* window : m_windows)
        paintWindowFrame(*window);
}

void WindowManager::paintWindowFrame(Window& window)
{
    Painter p(*m_rootWidget);

    printf("WM: paintWindowFrame {%p}, rect: %d,%d %dx%d\n", &window, window.rect().x(), window.rect().y(), window.rect().width(), window.rect().height());

    static const int windowFrameWidth = 2;
    static const int windowTitleBarHeight = 16;

    Rect topRect {
        window.x() - windowFrameWidth,
        window.y() - windowTitleBarHeight - windowFrameWidth,
        window.width() + windowFrameWidth * 2,
        windowTitleBarHeight + windowFrameWidth };

    Rect bottomRect {
        window.x() - windowFrameWidth,
        window.y() + window.height(),
        window.width() + windowFrameWidth * 2,
        windowFrameWidth };

    Rect leftRect {
        window.x() - windowFrameWidth,
        window.y(),
        windowFrameWidth,
        window.height()
    };

    Rect rightRect {
        window.x() + window.width(),
        window.y(),
        windowFrameWidth,
        window.height()
    };

    static const Color windowBorderColor(0x00, 0x00, 0x80);
    static const Color windowTitleColor(0xff, 0xff, 0xff);

    Rect borderRect {
        topRect.x() - 1, 
        topRect.y() - 1,
        topRect.width() + 2,
        windowFrameWidth + windowTitleBarHeight + window.rect().height() + 4
    };
    p.drawRect(borderRect, Color(255, 255, 255));
    borderRect.inflate(2, 2);
    p.drawRect(borderRect, windowBorderColor);

    p.fillRect(topRect, windowBorderColor);
    p.fillRect(bottomRect, windowBorderColor);
    p.fillRect(leftRect, windowBorderColor);
    p.fillRect(rightRect, windowBorderColor);

    p.drawText(topRect, window.title(), Painter::TextAlignment::Center, windowTitleColor);
}

void WindowManager::addWindow(Window& window)
{
    m_windows.set(&window);
}

void WindowManager::notifyTitleChanged(Window& window)
{
    printf("[WM] ]Window{%p} title set to '%s'\n", &window, window.title().characters());
}

void WindowManager::notifyRectChanged(Window& window, const Rect& oldRect, const Rect& newRect)
{
    printf("[WM] Window %p rect changed (%d,%d %dx%d) -> (%d,%d %dx%d)\n",
        &window,
        oldRect.x(),
        oldRect.y(),
        oldRect.width(),
        oldRect.height(),
        newRect.x(),
        newRect.y(),
        newRect.width(),
        newRect.height());
}

void WindowManager::processMouseEvent(MouseEvent& event)
{
    // First step: hit test windows.
    for (auto* window : m_windows) {
        // FIXME: z-order...
        if (window->rect().contains(event.position())) {
            // FIXME: Re-use the existing event instead of crafting a new one?
            auto localEvent = make<MouseEvent>(event.type(), event.x() - window->rect().x(), event.y() - window->rect().y(), event.button());
            window->event(*localEvent);
            return;
        }
    }

    // Otherwise: send it to root the root widget...
    auto result = m_rootWidget->hitTest(event.x(), event.y());
    //printf("hit test for %d,%d found: %s{%p} %d,%d\n", me.x(), me.y(), result.widget->className(), result.widget, result.localX, result.localY);
    // FIXME: Re-use the existing event instead of crafting a new one?
    auto localEvent = make<MouseEvent>(event.type(), result.localX, result.localY, event.button());
    result.widget->event(*localEvent);
}

void WindowManager::event(Event& event)
{
    if (event.isMouseEvent())
        return processMouseEvent(static_cast<MouseEvent&>(event));

    if (event.isKeyEvent()) {
        // FIXME: Implement proper focus.
        Widget* focusedWidget = g_tw;
        return focusedWidget->event(event);
    }

    if (event.type() == Event::Paint) {
        return m_rootWidget->event(event);
    }

    return Object::event(event);
}

void WindowManager::setRootWidget(Widget* widget)
{
    // FIXME: Should we support switching root widgets?
    ASSERT(!m_rootWidget);
    ASSERT(widget);
    
    m_rootWidget = widget;
    EventLoop::main().postEvent(m_rootWidget, make<ShowEvent>());
}
