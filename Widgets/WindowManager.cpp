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

static inline Rect borderRectForWindow(const Window& window)
{
    auto titleBarRect = titleBarRectForWindow(window);
    return { titleBarRect.x() - 1, 
        titleBarRect.y() - 1,
        titleBarRect.width() + 2,
        windowFrameWidth + windowTitleBarHeight + window.rect().height() + 4
    };
}

static inline Rect outerRectForWindow(const Window& window)
{
    auto rect = borderRectForWindow(window);
    rect.inflate(2, 2);
    return rect;
}

WindowManager& WindowManager::the()
{
    static WindowManager* s_the = new WindowManager;
    return *s_the;
}

WindowManager::WindowManager()
{
    m_activeWindowBorderColor = Color(0, 64, 192);
    m_activeWindowTitleColor = Color::White;

    m_inactiveWindowBorderColor = Color(64, 64, 64);
    m_inactiveWindowTitleColor = Color::White;
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

    auto titleBarRect = titleBarRectForWindow(window);
    auto outerRect = outerRectForWindow(window);
    auto borderRect = borderRectForWindow(window);

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


    if (!m_lastDragRect.isEmpty()) {
        p.xorRect(m_lastDragRect, Color::Red);
        m_lastDragRect = Rect();
    }

    if (m_dragWindow.ptr() == &window) {
        p.xorRect(outerRect, Color::Red);
        m_lastDragRect = outerRect;
        return;
    }

    auto titleColor = &window == activeWindow() ? m_activeWindowTitleColor : m_inactiveWindowTitleColor;
    auto borderColor = &window == activeWindow() ? m_activeWindowBorderColor : m_inactiveWindowBorderColor;

    p.drawRect(borderRect, Color::MidGray);
    p.drawRect(outerRect, borderColor);

    p.fillRect(titleBarRect, borderColor);
    p.fillRect(bottomRect, borderColor);
    p.fillRect(leftRect, borderColor);
    p.fillRect(rightRect, borderColor);

    p.drawText(titleBarRect, window.title(), Painter::TextAlignment::Center, titleColor);
}

void WindowManager::addWindow(Window& window)
{
    m_windows.set(&window);
    if (!activeWindow())
        setActiveWindow(&window);
}

void WindowManager::repaint()
{
    handlePaintEvent(*make<PaintEvent>());
}

void WindowManager::removeWindow(Window& window)
{
    if (!m_windows.contains(&window))
        return;

    m_windows.remove(&window);
    if (!activeWindow() && !m_windows.isEmpty())
        setActiveWindow(*m_windows.begin());

    repaint();
}

void WindowManager::notifyTitleChanged(Window& window)
{
    printf("[WM] Window{%p} title set to '%s'\n", &window, window.title().characters());
}

void WindowManager::notifyRectChanged(Window& window, const Rect& oldRect, const Rect& newRect)
{
    printf("[WM] Window %p rect changed (%d,%d %dx%d) -> (%d,%d %dx%d)\n", &window, oldRect.x(), oldRect.y(), oldRect.width(), oldRect.height(), newRect.x(), newRect.y(), newRect.width(), newRect.height());
    repaintAfterMove(oldRect, newRect);
}

void WindowManager::handleTitleBarMouseEvent(Window& window, MouseEvent& event)
{
    if (event.type() == Event::MouseDown) {
        printf("[WM] Begin dragging Window{%p}\n", &window);
        m_dragWindow = window.makeWeakPtr();;
        m_dragOrigin = event.position();
        m_dragWindowOrigin = window.position();
        m_dragStartRect = outerRectForWindow(window);
        window.setIsBeingDragged(true);
        return;
    }
#if 0
    byte r = (((double)rand()) / (double)RAND_MAX) * 255.0;
    byte g = (((double)rand()) / (double)RAND_MAX) * 255.0;
    byte b = (((double)rand()) / (double)RAND_MAX) * 255.0;
    m_activeWindowBorderColor = Color(r, g, b);
    paintWindowFrame(window);
#endif
}

void WindowManager::repaintAfterMove(const Rect& oldRect, const Rect& newRect)
{
    printf("[WM] repaint: [%d,%d %dx%d] -> [%d,%d %dx%d]\n",
            oldRect.x(),
            oldRect.y(),
            oldRect.width(),
            oldRect.height(),
            newRect.x(),
            newRect.y(),
            newRect.width(),
            newRect.height());

    m_rootWidget->repaint(oldRect);
    m_rootWidget->repaint(newRect);
    for (auto* window : m_windows) {
        if (outerRectForWindow(*window).intersects(oldRect) || outerRectForWindow(*window).intersects(newRect)) {
            paintWindowFrame(*window);
            window->repaint();
        }
    }
}

void WindowManager::processMouseEvent(MouseEvent& event)
{
    if (event.type() == Event::MouseUp) {
        if (m_dragWindow) {
            printf("[WM] Finish dragging Window{%p}\n", m_dragWindow.ptr());
            m_dragWindow->setIsBeingDragged(false);
            m_dragEndRect = outerRectForWindow(*m_dragWindow);
            m_dragWindow = nullptr;

            repaintAfterMove(m_dragStartRect, m_dragEndRect);
            return;
        }
    }

    if (event.type() == Event::MouseMove) {
        if (m_dragWindow) {
            Point pos = m_dragWindowOrigin;
            printf("[WM] Dragging [origin: %d,%d] now: %d,%d\n", m_dragOrigin.x(), m_dragOrigin.y(), event.x(), event.y());
            pos.moveBy(event.x() - m_dragOrigin.x(), event.y() - m_dragOrigin.y());
            m_dragWindow->setPositionWithoutRepaint(pos);
            paintWindowFrame(*m_dragWindow);
            return;
        }
    }

    // FIXME: Respect z-order of windows...
    for (auto* window : m_windows) {
        if (titleBarRectForWindow(*window).contains(event.position())) {
            if (event.type() == Event::MouseDown)
                setActiveWindow(window);
            handleTitleBarMouseEvent(*window, event);
            return;
        }

        if (window->rect().contains(event.position())) {
            if (event.type() == Event::MouseDown)
                setActiveWindow(window);
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
    if (event.rect().isEmpty()) {
        event.m_rect.setWidth(AbstractScreen::the().width());
        event.m_rect.setHeight(AbstractScreen::the().height());
    }

    m_rootWidget->event(event);
    paintWindowFrames();

    for (auto* window : m_windows)
        window->event(event);
}

void WindowManager::event(Event& event)
{
    if (event.isMouseEvent())
        return processMouseEvent(static_cast<MouseEvent&>(event));

    if (event.isKeyEvent()) {
        // FIXME: This is a good place to hook key events globally. :)
        if (m_activeWindow)
            return m_activeWindow->event(event);
        return Object::event(event);
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

void WindowManager::setActiveWindow(Window* window)
{
    if (window == m_activeWindow.ptr())
        return;

    auto* previouslyActiveWindow = m_activeWindow.ptr();

    m_activeWindow = window->makeWeakPtr();

    if (previouslyActiveWindow) {
        paintWindowFrame(*previouslyActiveWindow);
        previouslyActiveWindow->repaint();
    }

    if (m_activeWindow) {
        paintWindowFrame(*m_activeWindow);
        m_activeWindow->repaint();
    }
}

bool WindowManager::isVisible(Window& window) const
{
    return m_windows.contains(&window);
}

