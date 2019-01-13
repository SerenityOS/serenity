#include "WindowManager.h"
#include "Painter.h"
#include "Widget.h"
#include "Window.h"
#include "AbstractScreen.h"
#include "EventLoop.h"
#include "FrameBuffer.h"
#include "Process.h"
#include "MemoryManager.h"

//#define DEBUG_FLUSH_YELLOW

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

static const char* cursor_bitmap_inner_ascii = {
    " #          "
    " ##         "
    " ###        "
    " ####       "
    " #####      "
    " ######     "
    " #######    "
    " ########   "
    " #########  "
    " ########## "
    " ######     "
    " ##  ##     "
    " #    ##    "
    "      ##    "
    "       ##   "
    "       ##   "
    "            "
};

static const char* cursor_bitmap_outer_ascii = {
    "##          "
    "# #         "
    "#  #        "
    "#   #       "
    "#    #      "
    "#     #     "
    "#      #    "
    "#       #   "
    "#        #  "
    "#         # "
    "#      #### "
    "#  ##  #    "
    "# #  #  #   "
    "##   #  #   "
    "      #  #  "
    "      #  #  "
    "       ##   "
};

WindowManager::WindowManager()
    : m_framebuffer(FrameBuffer::the())
    , m_screen_rect(m_framebuffer.rect())
{
    auto size = m_screen_rect.size();
    m_front_bitmap = GraphicsBitmap::create_wrapper(size, m_framebuffer.scanline(0));
    auto* region = current->allocate_region(LinearAddress(), size.width() * size.height() * sizeof(RGBA32), "BackBitmap", true, true, true);
    m_back_bitmap = GraphicsBitmap::create_wrapper(m_screen_rect.size(), (RGBA32*)region->linearAddress.get());

    m_front_painter = make<Painter>(*m_front_bitmap);
    m_back_painter = make<Painter>(*m_back_bitmap);

    m_activeWindowBorderColor = Color(0, 64, 192);
    m_activeWindowTitleColor = Color::White;

    m_inactiveWindowBorderColor = Color(64, 64, 64);
    m_inactiveWindowTitleColor = Color::White;

    m_cursor_bitmap_inner = CharacterBitmap::createFromASCII(cursor_bitmap_inner_ascii, 12, 17);
    m_cursor_bitmap_outer = CharacterBitmap::createFromASCII(cursor_bitmap_outer_ascii, 12, 17);

    invalidate();
    compose();
}

WindowManager::~WindowManager()
{
}

void WindowManager::paintWindowFrame(Window& window)
{
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

    m_back_painter->draw_rect(borderRect, Color::MidGray);
    m_back_painter->draw_rect(outerRect, borderColor);
    m_back_painter->fill_rect(titleBarRect, borderColor);
    m_back_painter->draw_rect(inner_border_rect, borderColor);
    m_back_painter->draw_text(titleBarTitleRect, window.title(), Painter::TextAlignment::CenterLeft, titleColor);
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
    printf("[WM] compose #%u (%u rects)\n", ++m_recompose_count, m_invalidated_rects.size());

    dbgprintf("kmalloc stats: alloc:%u free:%u eternal:%u\n", sum_alloc, sum_free, kmalloc_sum_eternal);

    auto any_window_contains_rect = [this] (const Rect& r) {
        for (auto* window = m_windows_in_order.head(); window; window = window->next()) {
            if (outerRectForWindow(window->rect()).contains(r))
                return true;
        }
        return false;
    };

    for (auto& r : m_invalidated_rects) {
        if (any_window_contains_rect(r))
            continue;
        //dbgprintf("Repaint root %d,%d %dx%d\n", r.x(), r.y(), r.width(), r.height());
        m_back_painter->fill_rect(r, Color(0, 72, 96));
    }
    for (auto* window = m_windows_in_order.head(); window; window = window->next()) {
        if (!window->backing())
            continue;
        paintWindowFrame(*window);
        m_back_painter->blit(window->position(), *window->backing());
    }
    for (auto& r : m_invalidated_rects)
        flush(r);
    draw_cursor();
    m_invalidated_rects.clear_with_capacity();
}

void WindowManager::draw_cursor()
{
    auto cursor_location = m_framebuffer.cursor_location();
    Rect cursor_rect { cursor_location.x(), cursor_location.y(), (int)m_cursor_bitmap_inner->width(), (int)m_cursor_bitmap_inner->height() };
    flush(m_last_cursor_rect.united(cursor_rect));
    Color inner_color = Color::White;
    Color outer_color = Color::Black;
    if (m_framebuffer.left_mouse_button_pressed())
        swap(inner_color, outer_color);
    m_front_painter->draw_bitmap(cursor_location, *m_cursor_bitmap_inner, inner_color);
    m_front_painter->draw_bitmap(cursor_location, *m_cursor_bitmap_outer, outer_color);
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

    if (event.type() == Event::WM_Compose) {
        m_pending_compose_event = false;
        compose();
        return;
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
}

bool WindowManager::isVisible(Window& window) const
{
    return m_windows.contains(&window);
}

void WindowManager::invalidate()
{
    m_invalidated_rects.clear_with_capacity();
    m_invalidated_rects.append(m_screen_rect);
}

void WindowManager::invalidate(const Rect& a_rect)
{
    auto rect = Rect::intersection(a_rect, m_screen_rect);
    if (rect.is_empty())
        return;
    for (auto& r : m_invalidated_rects) {
        if (r.contains(rect))
            return;
        if (r.intersects(rect)) {
            // Unite with the existing dirty rect.
            // FIXME: It would be much nicer to compute the exact rects needing repaint.
            r = r.united(rect);
            return;
        }
    }

    m_invalidated_rects.append(rect);

    if (!m_pending_compose_event) {
        EventLoop::main().postEvent(this, make<Event>(Event::WM_Compose));
        m_pending_compose_event = true;
    }
}

void WindowManager::invalidate(const Window& window)
{
    invalidate(outerRectForWindow(window.rect()));
}

void WindowManager::flush(const Rect& a_rect)
{
    auto rect = Rect::intersection(a_rect, m_screen_rect);

    RGBA32* front_ptr = m_front_bitmap->scanline(rect.y()) + rect.x();
    const RGBA32* back_ptr = m_back_bitmap->scanline(rect.y()) + rect.x();
    size_t pitch = m_back_bitmap->pitch();

#ifdef DEBUG_FLUSH_YELLOW
    m_front_painter->fill_rect(rect, Color::Yellow);
#endif

    for (int y = 0; y < rect.height(); ++y) {
        fast_dword_copy(front_ptr, back_ptr, rect.width());
        front_ptr = (RGBA32*)((byte*)front_ptr + pitch);
        back_ptr = (const RGBA32*)((const byte*)back_ptr + pitch);
    }

    m_framebuffer.flush();
}
