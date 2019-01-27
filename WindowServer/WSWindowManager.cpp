#include "WSWindowManager.h"
#include "WSWindow.h"
#include "WSScreen.h"
#include "WSMessageLoop.h"
#include "Process.h"
#include "MemoryManager.h"
#include <Kernel/ProcFileSystem.h>
#include <SharedGraphics/Painter.h>
#include <SharedGraphics/CharacterBitmap.h>
#include <AK/StdLibExtras.h>

//#define DEBUG_COUNTERS

static const int windowTitleBarHeight = 16;

static inline Rect title_bar_rect(const Rect& window)
{
    return {
        window.x() - 1,
        window.y() - windowTitleBarHeight,
        window.width() + 2,
        windowTitleBarHeight
    };
}

static inline Rect title_bar_text_rect(const Rect& window)
{
    auto titleBarRect = title_bar_rect(window);
    return {
        titleBarRect.x() + 2,
        titleBarRect.y(),
        titleBarRect.width() - 4,
        titleBarRect.height()
    };
}

static inline Rect border_window_rect(const Rect& window)
{
    auto titleBarRect = title_bar_rect(window);
    return { titleBarRect.x() - 1,
        titleBarRect.y() - 1,
        titleBarRect.width() + 2,
        windowTitleBarHeight + window.height() + 3
    };
}

static inline Rect outer_window_rect(const Rect& window)
{
    auto rect = border_window_rect(window);
    rect.inflate(2, 2);
    return rect;
}

static WSWindowManager* s_the;

WSWindowManager& WSWindowManager::the()
{
    if (!s_the)
        s_the = new WSWindowManager;
    return *s_the;
}

void WSWindowManager::initialize()
{
    s_the = nullptr;
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
    : m_screen(WSScreen::the())
    , m_screen_rect(m_screen.rect())
{
#ifndef DEBUG_COUNTERS
    (void)m_compose_count;
    (void)m_flush_count;
#endif
    auto size = m_screen_rect.size();
    m_front_bitmap = GraphicsBitmap::create_wrapper(size, m_screen.scanline(0));
    auto* region = current->allocate_region(LinearAddress(), size.width() * size.height() * sizeof(RGBA32), "BackBitmap", true, true, true);
    m_back_bitmap = GraphicsBitmap::create_wrapper(m_screen_rect.size(), (RGBA32*)region->laddr().get());

    m_front_painter = make<Painter>(*m_front_bitmap);
    m_back_painter = make<Painter>(*m_back_bitmap);

    m_background_color = Color(50, 50, 50);
    m_active_window_border_color = Color(110, 34, 9);
    m_active_window_border_color2 = Color(244, 202, 158);
    m_active_window_title_color = Color::White;
    m_inactive_window_border_color = Color(128, 128, 128);
    m_inactive_window_border_color2 = Color(192, 192, 192);
    m_inactive_window_title_color = Color(213, 208, 199);
    m_dragging_window_border_color = Color(161, 50, 13);
    m_dragging_window_border_color2 = Color(250, 220, 187);
    m_dragging_window_title_color = Color::White;

    m_cursor_bitmap_inner = CharacterBitmap::create_from_ascii(cursor_bitmap_inner_ascii, 12, 17);
    m_cursor_bitmap_outer = CharacterBitmap::create_from_ascii(cursor_bitmap_outer_ascii, 12, 17);

    ProcFS::the().add_sys_bool("wm_flash_flush", &m_flash_flush);

    invalidate();
    compose();
}

WSWindowManager::~WSWindowManager()
{
}

void WSWindowManager::paint_window_frame(WSWindow& window)
{
    LOCKER(m_lock);
    //printf("[WM] paintWindowFrame {%p}, rect: %d,%d %dx%d\n", &window, window.rect().x(), window.rect().y(), window.rect().width(), window.rect().height());

    auto titleBarRect = title_bar_rect(window.rect());
    auto titleBarTitleRect = title_bar_text_rect(window.rect());
    auto outerRect = outer_window_rect(window.rect());
    auto borderRect = border_window_rect(window.rect());

    Rect inner_border_rect {
        window.x() - 1,
        window.y() - 1,
        window.width() + 2,
        window.height() + 2
    };

    Color title_color;
    Color border_color;
    Color border_color2;
    Color middle_border_color;

    if (&window == m_drag_window.ptr()) {
        border_color = m_dragging_window_border_color;
        border_color2 = m_dragging_window_border_color2;
        title_color = m_dragging_window_title_color;
        middle_border_color = Color::White;
    } else if (&window == m_active_window.ptr()) {
        border_color = m_active_window_border_color;
        border_color2 = m_active_window_border_color2;
        title_color = m_active_window_title_color;
        middle_border_color = Color::MidGray;
    } else {
        border_color = m_inactive_window_border_color;
        border_color2 = m_inactive_window_border_color2;
        title_color = m_inactive_window_title_color;
        middle_border_color = Color::MidGray;
    }

    m_back_painter->fill_rect_with_gradient(titleBarRect, border_color, border_color2);
    m_back_painter->draw_rect(borderRect, middle_border_color);
    m_back_painter->draw_rect(outerRect, border_color);
    m_back_painter->draw_rect(inner_border_rect, border_color);
    m_back_painter->draw_text(titleBarTitleRect, window.title(), Painter::TextAlignment::CenterLeft, title_color);

    Color metadata_color(96, 96, 96);
    char buffer[64];
    ksprintf(buffer, "%d:%d", window.pid(), window.window_id());
    m_back_painter->draw_text(titleBarTitleRect, buffer, Painter::TextAlignment::CenterRight, metadata_color);
}

void WSWindowManager::add_window(WSWindow& window)
{
    LOCKER(m_lock);
    m_windows.set(&window);
    m_windows_in_order.append(&window);
    if (!active_window())
        set_active_window(&window);
}

void WSWindowManager::move_to_front(WSWindow& window)
{
    LOCKER(m_lock);
    if (m_windows_in_order.tail() != &window)
        invalidate(window);
    m_windows_in_order.remove(&window);
    m_windows_in_order.append(&window);
}

void WSWindowManager::remove_window(WSWindow& window)
{
    LOCKER(m_lock);
    if (!m_windows.contains(&window))
        return;

    invalidate(window);
    m_windows.remove(&window);
    m_windows_in_order.remove(&window);
    if (!active_window() && !m_windows.is_empty())
        set_active_window(*m_windows.begin());
}

void WSWindowManager::notify_title_changed(WSWindow& window)
{
    printf("[WM] WSWindow{%p} title set to '%s'\n", &window, window.title().characters());
    invalidate(outer_window_rect(window.rect()));
}

void WSWindowManager::notify_rect_changed(WSWindow& window, const Rect& old_rect, const Rect& new_rect)
{
    printf("[WM] WSWindow %p rect changed (%d,%d %dx%d) -> (%d,%d %dx%d)\n", &window, old_rect.x(), old_rect.y(), old_rect.width(), old_rect.height(), new_rect.x(), new_rect.y(), new_rect.width(), new_rect.height());
    ASSERT_INTERRUPTS_ENABLED();
    invalidate(outer_window_rect(old_rect));
    invalidate(outer_window_rect(new_rect));
}

void WSWindowManager::handle_titlebar_mouse_event(WSWindow& window, WSMouseEvent& event)
{
    if (event.type() == WSMessage::MouseDown && event.button() == MouseButton::Left) {
#ifdef DRAG_DEBUG
        printf("[WM] Begin dragging WSWindow{%p}\n", &window);
#endif
        m_drag_window = window.makeWeakPtr();;
        m_drag_origin = event.position();
        m_drag_window_origin = window.position();
        m_drag_start_rect = outer_window_rect(window.rect());
        window.set_is_being_dragged(true);
        invalidate(window);
        return;
    }
}

void WSWindowManager::process_mouse_event(WSMouseEvent& event)
{
    if (event.type() == WSMessage::MouseUp && event.button() == MouseButton::Left) {
        if (m_drag_window) {
#ifdef DRAG_DEBUG
            printf("[WM] Finish dragging WSWindow{%p}\n", m_dragWindow.ptr());
#endif
            invalidate(*m_drag_window);
            m_drag_window->set_is_being_dragged(false);
            m_drag_end_rect = outer_window_rect(m_drag_window->rect());
            m_drag_window = nullptr;
            return;
        }
    }

    if (event.type() == WSMessage::MouseMove) {
        if (m_drag_window) {
            auto old_window_rect = m_drag_window->rect();
            Point pos = m_drag_window_origin;
#ifdef DRAG_DEBUG
            dbgprintf("[WM] Dragging [origin: %d,%d] now: %d,%d\n", m_dragOrigin.x(), m_dragOrigin.y(), event.x(), event.y());
#endif
            pos.move_by(event.x() - m_drag_origin.x(), event.y() - m_drag_origin.y());
            m_drag_window->set_position_without_repaint(pos);
            invalidate(outer_window_rect(old_window_rect));
            invalidate(outer_window_rect(m_drag_window->rect()));
            return;
        }
    }

    for (auto* window = m_windows_in_order.tail(); window; window = window->prev()) {
        if (!window->global_cursor_tracking())
            continue;
        Point position { event.x() - window->rect().x(), event.y() - window->rect().y() };
        auto local_event = make<WSMouseEvent>(event.type(), position, event.buttons(), event.button());
        window->on_message(*local_event);
    }

    for (auto* window = m_windows_in_order.tail(); window; window = window->prev()) {
        if (title_bar_rect(window->rect()).contains(event.position())) {
            if (event.type() == WSMessage::MouseDown) {
                move_to_front(*window);
                set_active_window(window);
            }
            handle_titlebar_mouse_event(*window, event);
            return;
        }

        if (window->rect().contains(event.position())) {
            if (event.type() == WSMessage::MouseDown) {
                move_to_front(*window);
                set_active_window(window);
            }
            // FIXME: Should we just alter the coordinates of the existing MouseEvent and pass it through?
            Point position { event.x() - window->rect().x(), event.y() - window->rect().y() };
            auto local_event = make<WSMouseEvent>(event.type(), position, event.buttons(), event.button());
            window->on_message(*local_event);
            return;
        }
    }
}

void WSWindowManager::compose()
{
    LOCKER(m_lock);
    auto dirty_rects = move(m_dirty_rects);
#ifdef DEBUG_COUNTERS
    dbgprintf("[WM] compose #%u (%u rects)\n", ++m_compose_count, dirty_rects.size());
    dbgprintf("kmalloc stats: alloc:%u free:%u eternal:%u\n", sum_alloc, sum_free, kmalloc_sum_eternal);
#endif

    auto any_window_contains_rect = [this] (const Rect& r) {
        for (auto* window = m_windows_in_order.head(); window; window = window->next()) {
            if (outer_window_rect(window->rect()).contains(r))
                return true;
        }
        return false;
    };

    auto any_dirty_rect_intersects_window = [&dirty_rects] (const WSWindow& window) {
        auto window_rect = outer_window_rect(window.rect());
        for (auto& dirty_rect : dirty_rects) {
            if (dirty_rect.intersects(window_rect))
                return true;
        }
        return false;
    };

    for (auto& dirty_rect : dirty_rects) {
        if (any_window_contains_rect(dirty_rect)) {
            continue;
        }
        //dbgprintf("Repaint root %d,%d %dx%d\n", dirty_rect.x(), dirty_rect.y(), dirty_rect.width(), dirty_rect.height());
        m_back_painter->fill_rect(dirty_rect, m_background_color);
    }
    for (auto* window = m_windows_in_order.head(); window; window = window->next()) {
        WSWindowLocker locker(*window);
        RetainPtr<GraphicsBitmap> backing = window->backing();
        if (!backing)
            continue;
        if (!any_dirty_rect_intersects_window(*window))
            continue;
        for (auto& dirty_rect : dirty_rects) {
            m_back_painter->set_clip_rect(dirty_rect);
            paint_window_frame(*window);
            Rect dirty_rect_in_window_coordinates = Rect::intersection(dirty_rect, window->rect());
            if (dirty_rect_in_window_coordinates.is_empty())
                continue;
            dirty_rect_in_window_coordinates.set_x(dirty_rect_in_window_coordinates.x() - window->x());
            dirty_rect_in_window_coordinates.set_y(dirty_rect_in_window_coordinates.y() - window->y());
            auto dst = window->position();
            dst.move_by(dirty_rect_in_window_coordinates.location());
            m_back_painter->blit(dst, *backing, dirty_rect_in_window_coordinates);
            m_back_painter->clear_clip_rect();
        }
        m_back_painter->clear_clip_rect();
    }
    for (auto& r : dirty_rects)
        flush(r);
    draw_cursor();
}

void WSWindowManager::draw_cursor()
{
    ASSERT_INTERRUPTS_ENABLED();
    LOCKER(m_lock);
    auto cursor_location = m_screen.cursor_location();
    Rect cursor_rect { cursor_location.x(), cursor_location.y(), (int)m_cursor_bitmap_inner->width(), (int)m_cursor_bitmap_inner->height() };
    flush(m_last_cursor_rect.united(cursor_rect));
    Color inner_color = Color::White;
    Color outer_color = Color::Black;
    if (m_screen.left_mouse_button_pressed())
        swap(inner_color, outer_color);
    m_front_painter->draw_bitmap(cursor_location, *m_cursor_bitmap_inner, inner_color);
    m_front_painter->draw_bitmap(cursor_location, *m_cursor_bitmap_outer, outer_color);
    m_last_cursor_rect = cursor_rect;
}

void WSWindowManager::on_message(WSMessage& message)
{
    ASSERT_INTERRUPTS_ENABLED();
    LOCKER(m_lock);
    if (message.is_mouse_event())
        return process_mouse_event(static_cast<WSMouseEvent&>(message));

    if (message.is_key_event()) {
        // FIXME: This is a good place to hook key events globally. :)
        if (m_active_window)
            return m_active_window->on_message(message);
        return;
    }

    if (message.type() == WSMessage::WM_DeferredCompose) {
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
        WSMessageLoop::the().post_message(previously_active_window, make<WSMessage>(WSMessage::WindowDeactivated));
        invalidate(*previously_active_window);
    }
    m_active_window = window->makeWeakPtr();
    if (m_active_window) {
        WSMessageLoop::the().post_message(m_active_window.ptr(), make<WSMessage>(WSMessage::WindowActivated));
        invalidate(*m_active_window);
    }
}

void WSWindowManager::invalidate()
{
    LOCKER(m_lock);
    m_dirty_rects.clear_with_capacity();
    m_dirty_rects.append(m_screen_rect);
}

void WSWindowManager::invalidate(const Rect& a_rect)
{
    LOCKER(m_lock);
    auto rect = Rect::intersection(a_rect, m_screen_rect);
    if (rect.is_empty())
        return;

    for (auto& r : m_dirty_rects) {
        if (r.contains(rect))
            return;
        if (r.intersects(rect)) {
            // Unite with the existing dirty rect.
            // FIXME: It would be much nicer to compute the exact rects needing repaint.
            r = r.united(rect);
            return;
        }
    }

    m_dirty_rects.append(rect);

    if (!m_pending_compose_event) {
        ASSERT_INTERRUPTS_ENABLED();
        WSMessageLoop::the().post_message(this, make<WSMessage>(WSMessage::WM_DeferredCompose));
        m_pending_compose_event = true;
    }
}

void WSWindowManager::invalidate(const WSWindow& window)
{
    ASSERT_INTERRUPTS_ENABLED();
    LOCKER(m_lock);
    invalidate(outer_window_rect(window.rect()));
}

void WSWindowManager::invalidate(const WSWindow& window, const Rect& rect)
{
    if (rect.is_empty()) {
        invalidate(window);
        return;
    }
    ASSERT_INTERRUPTS_ENABLED();
    LOCKER(m_lock);
    auto outer_rect = outer_window_rect(window.rect());
    auto inner_rect = rect;
    inner_rect.move_by(window.position());
    // FIXME: This seems slightly wrong; the inner rect shouldn't intersect the border part of the outer rect.
    inner_rect.intersect(outer_rect);
    invalidate(inner_rect);
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

    if (m_flash_flush)
        m_front_painter->fill_rect(rect, Color::Yellow);

    for (int y = 0; y < rect.height(); ++y) {
        fast_dword_copy(front_ptr, back_ptr, rect.width());
        front_ptr = (RGBA32*)((byte*)front_ptr + pitch);
        back_ptr = (const RGBA32*)((const byte*)back_ptr + pitch);
    }
}
