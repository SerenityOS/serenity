#include "WSWindowManager.h"
#include "WSWindow.h"
#include "WSScreen.h"
#include "WSEventLoop.h"
#include "WSFrameBuffer.h"
#include "Process.h"
#include "MemoryManager.h"
#include <Widgets/Painter.h>
#include <Widgets/CharacterBitmap.h>
#include <AK/StdLibExtras.h>

//#define DEBUG_FLUSH_YELLOW
//#define DEBUG_COUNTERS

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

static WSWindowManager* s_the_window_manager;

WSWindowManager& WSWindowManager::the()
{
    if (!s_the_window_manager)
        s_the_window_manager = new WSWindowManager;
    return *s_the_window_manager;
}

void WSWindowManager::initialize()
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

WSWindowManager::WSWindowManager()
    : m_framebuffer(WSFrameBuffer::the())
    , m_screen_rect(m_framebuffer.rect())
{
#ifndef DEBUG_COUNTERS
    (void)m_recompose_count;
    (void)m_flush_count;
#endif
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

    m_cursor_bitmap_inner = CharacterBitmap::create_from_ascii(cursor_bitmap_inner_ascii, 12, 17);
    m_cursor_bitmap_outer = CharacterBitmap::create_from_ascii(cursor_bitmap_outer_ascii, 12, 17);

    invalidate();
    compose();
}

WSWindowManager::~WSWindowManager()
{
}

void WSWindowManager::paintWindowFrame(WSWindow& window)
{
    LOCKER(m_lock);
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

    m_back_painter->fill_rect(titleBarRect, borderColor);
    m_back_painter->draw_rect(borderRect, Color::MidGray);
    m_back_painter->draw_rect(outerRect, borderColor);
    m_back_painter->draw_rect(inner_border_rect, borderColor);
    m_back_painter->draw_text(titleBarTitleRect, window.title(), Painter::TextAlignment::CenterLeft, titleColor);
}

void WSWindowManager::addWindow(WSWindow& window)
{
    LOCKER(m_lock);
    m_windows.set(&window);
    m_windows_in_order.append(&window);
    if (!activeWindow())
        set_active_window(&window);
}

void WSWindowManager::move_to_front(WSWindow& window)
{
    LOCKER(m_lock);
    m_windows_in_order.remove(&window);
    m_windows_in_order.append(&window);
}

void WSWindowManager::removeWindow(WSWindow& window)
{
    LOCKER(m_lock);
    if (!m_windows.contains(&window))
        return;

    invalidate(window);
    m_windows.remove(&window);
    m_windows_in_order.remove(&window);
    if (!activeWindow() && !m_windows.is_empty())
        set_active_window(*m_windows.begin());
}

void WSWindowManager::notifyTitleChanged(WSWindow& window)
{
    printf("[WM] WSWindow{%p} title set to '%s'\n", &window, window.title().characters());
}

void WSWindowManager::notifyRectChanged(WSWindow& window, const Rect& old_rect, const Rect& new_rect)
{
    printf("[WM] WSWindow %p rect changed (%d,%d %dx%d) -> (%d,%d %dx%d)\n", &window, old_rect.x(), old_rect.y(), old_rect.width(), old_rect.height(), new_rect.x(), new_rect.y(), new_rect.width(), new_rect.height());
    ASSERT_INTERRUPTS_ENABLED();
    LOCKER(m_lock);
    invalidate(outerRectForWindow(old_rect));
    invalidate(outerRectForWindow(new_rect));
}

void WSWindowManager::handleTitleBarMouseEvent(WSWindow& window, MouseEvent& event)
{
    if (event.type() == WSEvent::MouseDown && event.button() == MouseButton::Left) {
        printf("[WM] Begin dragging WSWindow{%p}\n", &window);
        m_dragWindow = window.makeWeakPtr();;
        m_dragOrigin = event.position();
        m_dragWindowOrigin = window.position();
        m_dragStartRect = outerRectForWindow(window.rect());
        window.set_is_being_dragged(true);
        return;
    }
}

void WSWindowManager::processMouseEvent(MouseEvent& event)
{
    if (event.type() == WSEvent::MouseUp && event.button() == MouseButton::Left) {
        if (m_dragWindow) {
            printf("[WM] Finish dragging WSWindow{%p}\n", m_dragWindow.ptr());
            invalidate(m_dragStartRect);
            invalidate(*m_dragWindow);
            m_dragWindow->set_is_being_dragged(false);
            m_dragEndRect = outerRectForWindow(m_dragWindow->rect());
            m_dragWindow = nullptr;
            return;
        }
    }

    if (event.type() == WSEvent::MouseMove) {
        if (m_dragWindow) {
            auto old_window_rect = m_dragWindow->rect();
            Point pos = m_dragWindowOrigin;
            printf("[WM] Dragging [origin: %d,%d] now: %d,%d\n", m_dragOrigin.x(), m_dragOrigin.y(), event.x(), event.y());
            pos.move_by(event.x() - m_dragOrigin.x(), event.y() - m_dragOrigin.y());
            m_dragWindow->set_position_without_repaint(pos);
            invalidate(outerRectForWindow(old_window_rect));
            invalidate(outerRectForWindow(m_dragWindow->rect()));
            return;
        }
    }

    for (auto* window = m_windows_in_order.tail(); window; window = window->prev()) {
        if (titleBarRectForWindow(window->rect()).contains(event.position())) {
            if (event.type() == WSEvent::MouseDown) {
                move_to_front(*window);
                set_active_window(window);
            }
            handleTitleBarMouseEvent(*window, event);
            return;
        }

        if (window->rect().contains(event.position())) {
            if (event.type() == WSEvent::MouseDown) {
                move_to_front(*window);
                set_active_window(window);
            }
            // FIXME: Re-use the existing event instead of crafting a new one?
            auto localEvent = make<MouseEvent>(event.type(), event.x() - window->rect().x(), event.y() - window->rect().y(), event.button());
            window->event(*localEvent);
            return;
        }
    }
}

void WSWindowManager::compose()
{
    LOCKER(m_lock);
    auto invalidated_rects = move(m_invalidated_rects);
#ifdef DEBUG_COUNTERS
    dbgprintf("[WM] compose #%u (%u rects)\n", ++m_recompose_count, invalidated_rects.size());
    dbgprintf("kmalloc stats: alloc:%u free:%u eternal:%u\n", sum_alloc, sum_free, kmalloc_sum_eternal);
#endif

    auto any_window_contains_rect = [this] (const Rect& r) {
        for (auto* window = m_windows_in_order.head(); window; window = window->next()) {
            if (outerRectForWindow(window->rect()).contains(r))
                return true;
        }
        return false;
    };

    auto any_dirty_rect_intersects_window = [&invalidated_rects] (const WSWindow& window) {
        auto window_rect = outerRectForWindow(window.rect());
        for (auto& dirty_rect : invalidated_rects) {
            if (dirty_rect.intersects(window_rect))
                return true;
        }
        return false;
    };

    for (auto& r : invalidated_rects) {
        if (any_window_contains_rect(r))
            continue;
        //dbgprintf("Repaint root %d,%d %dx%d\n", r.x(), r.y(), r.width(), r.height());
        m_back_painter->fill_rect(r, Color(0, 72, 96));
    }
    for (auto* window = m_windows_in_order.head(); window; window = window->next()) {
        if (!window->backing())
            continue;
        if (!any_dirty_rect_intersects_window(*window))
            continue;
        paintWindowFrame(*window);
        m_back_painter->blit(window->position(), *window->backing());
    }
    for (auto& r : invalidated_rects)
        flush(r);
    draw_cursor();
}

void WSWindowManager::draw_cursor()
{
    ASSERT_INTERRUPTS_ENABLED();
    LOCKER(m_lock);
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

void WSWindowManager::event(WSEvent& event)
{
    ASSERT_INTERRUPTS_ENABLED();
    LOCKER(m_lock);
    if (event.isMouseEvent())
        return processMouseEvent(static_cast<MouseEvent&>(event));

    if (event.isKeyEvent()) {
        // FIXME: This is a good place to hook key events globally. :)
        if (m_active_window)
            return m_active_window->event(event);
        return;
    }

    if (event.type() == WSEvent::WM_Compose) {
        m_pending_compose_event = false;
        compose();
        return;
    }
}

void WSWindowManager::set_active_window(WSWindow* window)
{
    LOCKER(m_lock);
    if (window == m_active_window.ptr())
        return;

    if (auto* previously_active_window = m_active_window.ptr()) {
        WSEventLoop::the().post_event(previously_active_window, make<WSEvent>(WSEvent::WindowDeactivated));
        invalidate(*previously_active_window);
    }
    m_active_window = window->makeWeakPtr();
    if (m_active_window) {
        WSEventLoop::the().post_event(m_active_window.ptr(), make<WSEvent>(WSEvent::WindowActivated));
        invalidate(*m_active_window);
    }
}

void WSWindowManager::invalidate()
{
    LOCKER(m_lock);
    m_invalidated_rects.clear_with_capacity();
    m_invalidated_rects.append(m_screen_rect);
}

void WSWindowManager::invalidate(const Rect& a_rect)
{
    LOCKER(m_lock);
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
        ASSERT_INTERRUPTS_ENABLED();
        WSEventLoop::the().post_event(this, make<WSEvent>(WSEvent::WM_Compose));
        m_pending_compose_event = true;
    }
}

void WSWindowManager::invalidate(const WSWindow& window)
{
    ASSERT_INTERRUPTS_ENABLED();
    LOCKER(m_lock);
    invalidate(outerRectForWindow(window.rect()));
}

void WSWindowManager::flush(const Rect& a_rect)
{
    auto rect = Rect::intersection(a_rect, m_screen_rect);

#ifdef DEBUG_COUNTERS
    dbgprintf("[WM] flush #%u (%d,%d %dx%d)\n", ++m_flush_count, rect.x(), rect.y(), rect.width(), rect.height());
#endif

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
}
