#include "WindowManager.h"
#include "Painter.h"
#include "Widget.h"
#include "Window.h"
#include "AbstractScreen.h"
#include "EventLoop.h"
#include "FrameBuffer.h"
#include "Process.h"
#include "MemoryManager.h"

static const int windowTitleBarHeight = 16;

static inline Rect titleBarRectForWindow(const Rect& window)
{
    return {
        window.x() - 1,
        window.y() - windowTitleBarHeight,
        window.width() + 2,
        windowTitleBarHeight
    };
}

static inline Rect titleBarTitleRectForWindow(const Rect& window)
{
    auto titleBarRect = titleBarRectForWindow(window);
    return {
        titleBarRect.x() + 2,
        titleBarRect.y(),
        titleBarRect.width() - 4,
        titleBarRect.height()
    };
}

static inline Rect borderRectForWindow(const Rect& window)
{
    auto titleBarRect = titleBarRectForWindow(window);
    return { titleBarRect.x() - 1,
        titleBarRect.y() - 1,
        titleBarRect.width() + 2,
        windowTitleBarHeight + window.height() + 3
    };
}

static inline Rect outerRectForWindow(const Rect& window)
{
    auto rect = borderRectForWindow(window);
    rect.inflate(2, 2);
    return rect;
}

static WindowManager* s_the_window_manager;

WindowManager& WindowManager::the()
{
    if (!s_the_window_manager)
        s_the_window_manager = new WindowManager;
    return *s_the_window_manager;
}

void WindowManager::initialize()
{
    s_the_window_manager = nullptr;
}

WindowManager::WindowManager()
{
    auto size = FrameBuffer::the().rect().size();
    m_front_bitmap = GraphicsBitmap::create_wrapper(size, FrameBuffer::the().scanline(0));
    auto* region = current->allocate_region(LinearAddress(), size.width() * size.height() * 4, "back buffer", true, true, true);
    m_back_bitmap = GraphicsBitmap::create_wrapper(FrameBuffer::the().rect().size(), (RGBA32*)region->linearAddress.get());

    m_activeWindowBorderColor = Color(0, 64, 192);
    m_activeWindowTitleColor = Color::White;

    m_inactiveWindowBorderColor = Color(64, 64, 64);
    m_inactiveWindowTitleColor = Color::White;

    invalidate();
}

WindowManager::~WindowManager()
{
}

void WindowManager::paintWindowFrame(Window& window)
{
    Painter p(*m_back_bitmap);

    //printf("[WM] paintWindowFrame {%p}, rect: %d,%d %dx%d\n", &window, window.rect().x(), window.rect().y(), window.rect().width(), window.rect().height());

    auto titleBarRect = titleBarRectForWindow(window.rect());
    auto titleBarTitleRect = titleBarTitleRectForWindow(window.rect());
    auto outerRect = outerRectForWindow(window.rect());
    auto borderRect = borderRectForWindow(window.rect());

    Rect inner_border_rect {
        window.x() - 1,
        window.y() - 1,
        window.width() + 2,
        window.height() + 2
    };

    auto titleColor = &window == activeWindow() ? m_activeWindowTitleColor : m_inactiveWindowTitleColor;
    auto borderColor = &window == activeWindow() ? m_activeWindowBorderColor : m_inactiveWindowBorderColor;

    p.drawRect(borderRect, Color::MidGray);
    p.drawRect(outerRect, borderColor);

    p.fillRect(titleBarRect, borderColor);

    p.drawRect(inner_border_rect, borderColor);

    p.drawText(titleBarTitleRect, window.title(), Painter::TextAlignment::CenterLeft, titleColor);
}

void WindowManager::addWindow(Window& window)
{
    m_windows.set(&window);
    m_windows_in_order.append(&window);
    if (!activeWindow())
        setActiveWindow(&window);
}

void WindowManager::move_to_front(Window& window)
{
    m_windows_in_order.remove(&window);
    m_windows_in_order.append(&window);
}

void WindowManager::did_paint(Window& window)
{
    invalidate(window);
    compose();
}

void WindowManager::removeWindow(Window& window)
{
    if (!m_windows.contains(&window))
        return;

    invalidate(window);
    m_windows.remove(&window);
    m_windows_in_order.remove(&window);
    if (!activeWindow() && !m_windows.is_empty())
        setActiveWindow(*m_windows.begin());
    compose();
}

void WindowManager::notifyTitleChanged(Window& window)
{
    printf("[WM] Window{%p} title set to '%s'\n", &window, window.title().characters());
}

void WindowManager::notifyRectChanged(Window& window, const Rect& old_rect, const Rect& new_rect)
{
    printf("[WM] Window %p rect changed (%d,%d %dx%d) -> (%d,%d %dx%d)\n", &window, old_rect.x(), old_rect.y(), old_rect.width(), old_rect.height(), new_rect.x(), new_rect.y(), new_rect.width(), new_rect.height());
    invalidate(outerRectForWindow(old_rect));
    invalidate(outerRectForWindow(new_rect));
    compose();
}

void WindowManager::handleTitleBarMouseEvent(Window& window, MouseEvent& event)
{
    if (event.type() == Event::MouseDown && event.button() == MouseButton::Left) {
        printf("[WM] Begin dragging Window{%p}\n", &window);
        m_dragWindow = window.makeWeakPtr();;
        m_dragOrigin = event.position();
        m_dragWindowOrigin = window.position();
        m_dragStartRect = outerRectForWindow(window.rect());
        window.setIsBeingDragged(true);
        return;
    }
}

void WindowManager::processMouseEvent(MouseEvent& event)
{
    if (event.type() == Event::MouseUp && event.button() == MouseButton::Left) {
        if (m_dragWindow) {
            printf("[WM] Finish dragging Window{%p}\n", m_dragWindow.ptr());
            invalidate(m_dragStartRect);
            invalidate(*m_dragWindow);
            m_dragWindow->setIsBeingDragged(false);
            m_dragEndRect = outerRectForWindow(m_dragWindow->rect());
            m_dragWindow = nullptr;
            compose();
            return;
        }
    }

    if (event.type() == Event::MouseMove) {
        if (m_dragWindow) {
            auto old_window_rect = m_dragWindow->rect();
            Point pos = m_dragWindowOrigin;
            printf("[WM] Dragging [origin: %d,%d] now: %d,%d\n", m_dragOrigin.x(), m_dragOrigin.y(), event.x(), event.y());
            pos.moveBy(event.x() - m_dragOrigin.x(), event.y() - m_dragOrigin.y());
            m_dragWindow->setPositionWithoutRepaint(pos);
            invalidate(outerRectForWindow(old_window_rect));
            invalidate(outerRectForWindow(m_dragWindow->rect()));
            compose();
            return;
        }
    }

    for (auto* window = m_windows_in_order.tail(); window; window = window->prev()) {
        if (titleBarRectForWindow(window->rect()).contains(event.position())) {
            if (event.type() == Event::MouseDown) {
                move_to_front(*window);
                setActiveWindow(window);
            }
            handleTitleBarMouseEvent(*window, event);
            return;
        }

        if (window->rect().contains(event.position())) {
            if (event.type() == Event::MouseDown) {
                move_to_front(*window);
                setActiveWindow(window);
            }
            // FIXME: Re-use the existing event instead of crafting a new one?
            auto localEvent = make<MouseEvent>(event.type(), event.x() - window->rect().x(), event.y() - window->rect().y(), event.button());
            window->event(*localEvent);
            return;
        }
    }
}

void WindowManager::compose()
{
    printf("[WM] recompose_count: %u\n", ++m_recompose_count);
    auto any_window_contains_rect = [this] (const Rect& r) {
        for (auto* window = m_windows_in_order.head(); window; window = window->next()) {
            if (outerRectForWindow(window->rect()).contains(r))
                return true;
        }
        return false;
    };
    Painter painter(*m_back_bitmap);
    {
        for (auto& r : m_invalidated_rects) {
            if (any_window_contains_rect(r))
                continue;
            dbgprintf("Repaint root %d,%d %dx%d\n", r.x(), r.y(), r.width(), r.height());
            painter.fillRect(r, Color(0, 72, 96));
        }
    }
    for (auto* window = m_windows_in_order.head(); window; window = window->next()) {
        if (!window->backing())
            continue;
        paintWindowFrame(*window);
        painter.blit(window->position(), *window->backing());
    }
    redraw_cursor();
    for (auto& r : m_invalidated_rects) {
        flush(r);
    }
    m_invalidated_rects.clear();
}

void WindowManager::redraw_cursor()
{
    auto cursor_location = AbstractScreen::the().cursor_location();
    Painter painter(*m_front_bitmap);
    Rect cursor_rect { cursor_location.x() - 10, cursor_location.y() - 10, 21, 21 };
    flush(m_last_cursor_rect);
    flush(cursor_rect);
    auto draw_cross = [&painter] (const Point& p) {
        painter.drawLine({ p.x() - 10, p.y() }, { p.x() + 10, p.y() }, Color::Red);
        painter.drawLine({ p.x(), p.y() - 10 }, { p.x(), p.y() + 10 }, Color::Red);
    };
    draw_cross(cursor_location);
    m_last_cursor_rect = cursor_rect;
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

    return Object::event(event);
}

void WindowManager::setActiveWindow(Window* window)
{
    if (window == m_activeWindow.ptr())
        return;

    if (auto* previously_active_window = m_activeWindow.ptr()) {
        invalidate(*previously_active_window);
        EventLoop::main().postEvent(previously_active_window, make<Event>(Event::WindowBecameInactive));
    }
    m_activeWindow = window->makeWeakPtr();
    if (m_activeWindow) {
        invalidate(*m_activeWindow);
        EventLoop::main().postEvent(m_activeWindow.ptr(), make<Event>(Event::WindowBecameActive));
    }

    compose();
}

bool WindowManager::isVisible(Window& window) const
{
    return m_windows.contains(&window);
}

void WindowManager::invalidate()
{
    m_invalidated_rects.clear();
    m_invalidated_rects.append(AbstractScreen::the().rect());
}

void WindowManager::invalidate(const Rect& a_rect)
{
    // FIXME: This code is fugly.
    Rect rect(a_rect);
    auto screen_rect = AbstractScreen::the().rect();
    if (rect.left() < 0)
        rect.set_left(0);
    if (rect.top() < 0)
        rect.set_top(0);
    if (rect.right() > screen_rect.right())
        rect.set_right(screen_rect.right());
    if (rect.bottom() > screen_rect.bottom())
        rect.set_bottom(screen_rect.bottom());
    if (rect.is_empty())
        return;
    for (auto& r : m_invalidated_rects) {
        if (r.contains(rect))
            return;
    }

    m_invalidated_rects.append(rect);
}

void WindowManager::invalidate(const Window& window)
{
    invalidate(outerRectForWindow(window.rect()));
}

void WindowManager::flush(const Rect& rect)
{
    auto& framebuffer = FrameBuffer::the();

    for (int y = rect.top(); y <= rect.bottom(); ++y) {
        auto* front_scanline = m_front_bitmap->scanline(y);
        auto* back_scanline = m_back_bitmap->scanline(y);
        memcpy(front_scanline + rect.x(), back_scanline + rect.x(), rect.width() * sizeof(RGBA32));
    }

    framebuffer.flush();
}
