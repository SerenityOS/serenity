#include "WSWindowManager.h"
#include "WSWindow.h"
#include "WSScreen.h"
#include "WSMessageLoop.h"
#include "Process.h"
#include "MemoryManager.h"
#include <Kernel/ProcFS.h>
#include <SharedGraphics/Font.h>
#include <SharedGraphics/Painter.h>
#include <SharedGraphics/CharacterBitmap.h>
#include <AK/StdLibExtras.h>
#include <Kernel/BochsVGADevice.h>

//#define DEBUG_COUNTERS
//#define DEBUG_WID_IN_TITLE_BAR

static const int window_titlebar_height = 16;

static inline Rect title_bar_rect(const Rect& window)
{
    return {
        window.x() - 1,
        window.y() - window_titlebar_height,
        window.width() + 2,
        window_titlebar_height
    };
}

static inline Rect title_bar_text_rect(const Rect& window)
{
    auto titlebar_rect = title_bar_rect(window);
    return {
        titlebar_rect.x() + 2,
        titlebar_rect.y(),
        titlebar_rect.width() - 4,
        titlebar_rect.height()
    };
}

static inline Rect close_button_rect_for_window(const Rect& window_rect)
{
    auto titlebar_inner_rect = title_bar_text_rect(window_rect);
    int close_button_margin = 1;
    int close_button_size = titlebar_inner_rect.height() - close_button_margin * 2;
    return Rect {
        titlebar_inner_rect.right() - close_button_size + 1,
        titlebar_inner_rect.top() + close_button_margin,
        close_button_size,
        close_button_size - 1
    };
}

static inline Rect border_window_rect(const Rect& window)
{
    auto titlebar_rect = title_bar_rect(window);
    return { titlebar_rect.x() - 1,
        titlebar_rect.y() - 1,
        titlebar_rect.width() + 2,
        window_titlebar_height + window.height() + 3
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

void WSWindowManager::flip_buffers()
{
    swap(m_front_bitmap, m_back_bitmap);
    swap(m_front_painter, m_back_painter);
    if (m_buffers_are_flipped)
        BochsVGADevice::the().set_y_offset(0);
    else
        BochsVGADevice::the().set_y_offset(m_screen_rect.height());
    m_buffers_are_flipped = !m_buffers_are_flipped;
}

WSWindowManager::WSWindowManager()
    : m_screen(WSScreen::the())
    , m_screen_rect(m_screen.rect())
    , m_lock("WSWindowManager")
{
#ifndef DEBUG_COUNTERS
    (void)m_compose_count;
    (void)m_flush_count;
#endif
    auto size = m_screen_rect.size();
    m_front_bitmap = GraphicsBitmap::create_wrapper(size, m_screen.scanline(0));
    m_back_bitmap = GraphicsBitmap::create_wrapper(size, m_screen.scanline(size.height()));

    m_front_painter = make<Painter>(*m_front_bitmap);
    m_back_painter = make<Painter>(*m_back_bitmap);

    m_font = Font::default_font();

    m_front_painter->set_font(font());
    m_back_painter->set_font(font());

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

    {
        LOCKER(m_wallpaper_path.lock());
        m_wallpaper_path.resource() = "/res/wallpapers/gray-wood.rgb";
        m_wallpaper = GraphicsBitmap::load_from_file(m_wallpaper_path.resource(), m_screen_rect.size());
    }

    ProcFS::the().add_sys_bool("wm_flash_flush", &m_flash_flush);
    ProcFS::the().add_sys_string("wm_wallpaper", m_wallpaper_path, [this] {
        LOCKER(m_wallpaper_path.lock());
        m_wallpaper = GraphicsBitmap::load_from_file(m_wallpaper_path.resource(), m_screen_rect.size());
        invalidate(m_screen_rect);
    });

    invalidate();
    compose();
}

WSWindowManager::~WSWindowManager()
{
}

static const char* s_close_button_bitmap_data = {
    "##    ##"
    "###  ###"
    " ###### "
    "  ####  "
    "   ##   "
    "  ####  "
    " ###### "
    "###  ###"
    "##    ##"
};

static CharacterBitmap* s_close_button_bitmap;
static const int s_close_button_bitmap_width = 8;
static const int s_close_button_bitmap_height = 9;

void WSWindowManager::paint_window_frame(WSWindow& window)
{
    LOCKER(m_lock);
    //printf("[WM] paint_window_frame {%p}, rect: %d,%d %dx%d\n", &window, window.rect().x(), window.rect().y(), window.rect().width(), window.rect().height());

    auto titlebar_rect = title_bar_rect(window.rect());
    auto titlebar_inner_rect = title_bar_text_rect(window.rect());
    auto outer_rect = outer_window_rect(window.rect());
    auto border_rect = border_window_rect(window.rect());
    auto close_button_rect = close_button_rect_for_window(window.rect());

    auto titlebar_title_rect = titlebar_inner_rect;
    titlebar_title_rect.set_width(font().glyph_width() * window.title().length());

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

    m_back_painter->fill_rect_with_gradient(titlebar_rect, border_color, border_color2);
    for (int i = 2; i <= titlebar_inner_rect.height() - 4; i += 2) {
        m_back_painter->draw_line({ titlebar_title_rect.right() + 4, titlebar_inner_rect.y() + i }, { close_button_rect.left() - 3, titlebar_inner_rect.y() + i }, border_color);
    }
    m_back_painter->draw_rect(border_rect, middle_border_color);
    m_back_painter->draw_rect(outer_rect, border_color);
    m_back_painter->draw_rect(inner_border_rect, border_color);
    m_back_painter->draw_text(titlebar_title_rect, window.title(), Painter::TextAlignment::CenterLeft, title_color);

    if (!s_close_button_bitmap)
        s_close_button_bitmap = CharacterBitmap::create_from_ascii(s_close_button_bitmap_data, s_close_button_bitmap_width, s_close_button_bitmap_height).leak_ref();

    m_back_painter->fill_rect_with_gradient(close_button_rect.shrunken(2, 2), Color::LightGray, Color::White);

    m_back_painter->draw_rect(close_button_rect, Color::DarkGray, true);
    auto x_location = close_button_rect.center();
    x_location.move_by(-(s_close_button_bitmap_width / 2), -(s_close_button_bitmap_height / 2));
    m_back_painter->draw_bitmap(x_location, *s_close_button_bitmap, Color::Black);

#ifdef DEBUG_WID_IN_TITLE_BAR
    Color metadata_color(96, 96, 96);
    m_back_painter->draw_text(
        titlebar_inner_rect,
        String::format("%d:%d", window.pid(), window.window_id()),
        Painter::TextAlignment::CenterRight,
        metadata_color
    );
#endif
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
        m_drag_window = window.make_weak_ptr();;
        m_drag_origin = event.position();
        m_drag_window_origin = window.position();
        m_drag_start_rect = outer_window_rect(window.rect());
        window.set_is_being_dragged(true);
        invalidate(window);
        return;
    }
}

void WSWindowManager::handle_close_button_mouse_event(WSWindow& window, WSMouseEvent& event)
{
    if (event.type() == WSMessage::MouseDown && event.button() == MouseButton::Left) {
        WSMessage message(WSMessage::WindowCloseRequest);
        window.on_message(message);
        return;
    }
}

void WSWindowManager::process_mouse_event(WSMouseEvent& event)
{
    if (event.type() == WSMessage::MouseUp && event.button() == MouseButton::Left) {
        if (m_drag_window) {
#ifdef DRAG_DEBUG
            printf("[WM] Finish dragging WSWindow{%p}\n", m_drag_window.ptr());
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
            dbgprintf("[WM] Dragging [origin: %d,%d] now: %d,%d\n", m_drag_origin.x(), m_drag_origin.y(), event.x(), event.y());
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
            if (close_button_rect_for_window(window->rect()).contains(event.position())) {
                handle_close_button_mouse_event(*window, event);
                return;
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
    auto cursor_location = m_screen.cursor_location();
    dirty_rects.append(m_last_cursor_rect);
    dirty_rects.append({ cursor_location.x(), cursor_location.y(), (int)m_cursor_bitmap_inner->width(), (int)m_cursor_bitmap_inner->height() });
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
        LOCKER(m_wallpaper_path.lock());
        if (!m_wallpaper)
            m_back_painter->fill_rect(dirty_rect, m_background_color);
        else
            m_back_painter->blit(dirty_rect.location(), *m_wallpaper, dirty_rect);
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
    draw_cursor();

    if (m_flash_flush) {
        for (auto& rect : dirty_rects)
            m_front_painter->fill_rect(rect, Color::Yellow);
    }

    flip_buffers();
    for (auto& r : dirty_rects)
        flush(r);
}

void WSWindowManager::invalidate_cursor()
{
    LOCKER(m_lock);
    auto cursor_location = m_screen.cursor_location();
    Rect cursor_rect { cursor_location.x(), cursor_location.y(), (int)m_cursor_bitmap_inner->width(), (int)m_cursor_bitmap_inner->height() };
    invalidate(cursor_rect);
}

void WSWindowManager::draw_cursor()
{
    ASSERT_INTERRUPTS_ENABLED();
    LOCKER(m_lock);
    auto cursor_location = m_screen.cursor_location();
    Rect cursor_rect { cursor_location.x(), cursor_location.y(), (int)m_cursor_bitmap_inner->width(), (int)m_cursor_bitmap_inner->height() };
    Color inner_color = Color::White;
    Color outer_color = Color::Black;
    if (m_screen.left_mouse_button_pressed())
        swap(inner_color, outer_color);
    m_back_painter->draw_bitmap(cursor_location, *m_cursor_bitmap_inner, inner_color);
    m_back_painter->draw_bitmap(cursor_location, *m_cursor_bitmap_outer, outer_color);
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
    m_active_window = window->make_weak_ptr();
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
    RGBA32* back_ptr = m_back_bitmap->scanline(rect.y()) + rect.x();
    size_t pitch = m_back_bitmap->pitch();

    for (int y = 0; y < rect.height(); ++y) {
        fast_dword_copy(back_ptr, front_ptr, rect.width());
        front_ptr = (RGBA32*)((byte*)front_ptr + pitch);
        back_ptr = (RGBA32*)((byte*)back_ptr + pitch);
    }
}
