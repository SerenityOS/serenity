#include "WindowManager.h"
#include "Painter.h"
#include "Widget.h"
#include "Window.h"
#include "AbstractScreen.h"
#include "TerminalWidget.h"
#include "EventLoop.h"

extern TerminalWidget* g_tw;

static const int windowFrameWidth = 2;
static const int windowTitleBarHeight = 16;

static inline Rect titleBarRectForWindow(const Window& window)
{
    return {
        window.x() - windowFrameWidth,
        window.y() - windowTitleBarHeight - windowFrameWidth,
        window.width() + windowFrameWidth * 2,
        windowTitleBarHeight + windowFrameWidth
    };
}

WindowManager& WindowManager::the()
{
    static WindowManager* s_the = new WindowManager;
    return *s_the;
}

WindowManager::WindowManager()
{
    m_windowBorderColor = Color(0x00, 0x00, 0x80);
    m_windowTitleColor = Color(0xff, 0xff, 0xff);
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

    //printf("[WM] paintWindowFrame {%p}, rect: %d,%d %dx%d\n", &window, window.rect().x(), window.rect().y(), window.rect().width(), window.rect().height());

    Rect topRect = titleBarRectForWindow(window);

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

    Rect borderRect {
        topRect.x() - 1, 
        topRect.y() - 1,
        topRect.width() + 2,
        windowFrameWidth + windowTitleBarHeight + window.rect().height() + 4
    };

    if (!m_lastDragRect.isEmpty()) {
        p.xorRect(m_lastDragRect, Color(255, 0, 0));
        m_lastDragRect = Rect();
    }

    if (m_dragWindow == &window) {
        borderRect.inflate(2, 2);
        p.xorRect(borderRect, Color(255, 0, 0));
        m_lastDragRect = borderRect;
        return;
    }

    p.drawRect(borderRect, Color(255, 255, 255));
    borderRect.inflate(2, 2);
    p.drawRect(borderRect, m_windowBorderColor);

    p.fillRect(topRect, m_windowBorderColor);
    p.fillRect(bottomRect, m_windowBorderColor);
    p.fillRect(leftRect, m_windowBorderColor);
    p.fillRect(rightRect, m_windowBorderColor);

    p.drawText(topRect, window.title(), Painter::TextAlignment::Center, m_windowTitleColor);
}

void WindowManager::addWindow(Window& window)
{
    m_windows.set(&window);
}

void WindowManager::notifyTitleChanged(Window& window)
{
    //printf("[WM] Window{%p} title set to '%s'\n", &window, window.title().characters());
}

void WindowManager::notifyRectChanged(Window& window, const Rect& oldRect, const Rect& newRect)
{
    //printf("[WM] Window %p rect changed (%d,%d %dx%d) -> (%d,%d %dx%d)\n", &window, oldRect.x(), oldRect.y(), oldRect.width(), oldRect.height(), newRect.x(), newRect.y(), newRect.width(), newRect.height());
}

void WindowManager::handleTitleBarMouseEvent(Window& window, MouseEvent& event)
{
    if (event.type() == Event::MouseDown) {
        printf("[WM] Begin dragging Window{%p}\n", &window);
        m_dragWindow = &window;
        m_dragOrigin = event.position();
        m_dragWindowOrigin = window.position();
        window.setIsBeingDragged(true);
        return;
    }
#if 0
    byte r = (((double)rand()) / (double)RAND_MAX) * 255.0;
    byte g = (((double)rand()) / (double)RAND_MAX) * 255.0;
    byte b = (((double)rand()) / (double)RAND_MAX) * 255.0;
    m_windowBorderColor = Color(r, g, b);
    paintWindowFrame(window);
#endif
}

void WindowManager::processMouseEvent(MouseEvent& event)
{
    if (event.type() == Event::MouseUp) {
        if (m_dragWindow) {
            printf("[WM] Finish dragging Window{%p}\n", m_dragWindow);
            m_dragWindow->setIsBeingDragged(false);
            m_dragWindow = nullptr;
            EventLoop::main().postEvent(this, make<PaintEvent>());
            return;
        }
    }

    if (event.type() == Event::MouseMove) {
        if (m_dragWindow) {
            Point pos = m_dragWindowOrigin;
            printf("[WM] Dragging [origin: %d,%d] now: %d,%d\n", m_dragOrigin.x(), m_dragOrigin.y(), event.x(), event.y());
            pos.moveBy(event.x() - m_dragOrigin.x(), event.y() - m_dragOrigin.y());
            m_dragWindow->setPosition(pos);
            paintWindowFrame(*m_dragWindow);
            return;
        }
    }

    // FIXME: Respect z-order of windows...
    for (auto* window : m_windows) {
        if (titleBarRectForWindow(*window).contains(event.position())) {
            handleTitleBarMouseEvent(*window, event);
            return;
        }

        if (window->rect().contains(event.position())) {
            // FIXME: Re-use the existing event instead of crafting a new one?
            auto localEvent = make<MouseEvent>(event.type(), event.x() - window->rect().x(), event.y() - window->rect().y(), event.button());
            window->event(*localEvent);
            return;
        }
    }

}

void WindowManager::handlePaintEvent(PaintEvent& event)
{
    //printf("[WM] paint event\n");
    m_rootWidget->event(event);
    paintWindowFrames();

    for (auto* window : m_windows) {
        window->event(event);
    }
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

    if (event.isPaintEvent())
        return handlePaintEvent(static_cast<PaintEvent&>(event));

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
