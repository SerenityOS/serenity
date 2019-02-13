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
#include <LibC/errno_numbers.h>
#include "WSMenu.h"
#include "WSMenuBar.h"
#include "WSMenuItem.h"
#include <Kernel/RTC.h>

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
    , m_flash_flush(false)
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
        m_wallpaper_path.resource() = "/res/wallpapers/cool.rgb";
        m_wallpaper = GraphicsBitmap::load_from_file(m_wallpaper_path.resource(), m_screen_rect.size());
    }

    ProcFS::the().add_sys_bool("wm_flash_flush", m_flash_flush);
    ProcFS::the().add_sys_string("wm_wallpaper", m_wallpaper_path, [this] {
        LOCKER(m_wallpaper_path.lock());
        m_wallpaper = GraphicsBitmap::load_from_file(m_wallpaper_path.resource(), m_screen_rect.size());
        invalidate(m_screen_rect);
    });

    m_menu_selection_color = Color(0x84351a);

    {
        byte system_menu_name[] = { 0xf8, 0 };
        m_system_menu = make<WSMenu>(*current, -1, String((const char*)system_menu_name));
        m_system_menu->add_item(make<WSMenuItem>(0, "Launch Terminal"));
        m_system_menu->add_item(make<WSMenuItem>(WSMenuItem::Separator));
        m_system_menu->add_item(make<WSMenuItem>(1, "Hello again"));
        m_system_menu->add_item(make<WSMenuItem>(2, "To all my friends"));
        m_system_menu->add_item(make<WSMenuItem>(3, "Together we can play some rock&roll"));
        m_system_menu->add_item(make<WSMenuItem>(WSMenuItem::Separator));
        m_system_menu->add_item(make<WSMenuItem>(4, "About..."));
        m_system_menu->on_item_activation = [] (WSMenuItem& item) {
            if (item.identifier() == 0) {
                int error;
                Process::create_user_process("/bin/Terminal", 100, 100, 0, error);
                return;
            }
            if (item.identifier() == 4) {
                int error;
                Process::create_user_process("/bin/About", 100, 100, 0, error);
                return;
            }
            kprintf("WSMenu 1 item activated: '%s'\n", item.text().characters());
        };
    }

    // NOTE: This ensures that the system menu has the correct dimensions.
    set_current_menubar(nullptr);

    WSMessageLoop::the().start_timer(300, [this] {
        invalidate(menubar_rect());
    });

    invalidate();
    compose();
}

WSWindowManager::~WSWindowManager()
{
}

template<typename Callback>
void WSWindowManager::for_each_active_menubar_menu(Callback callback)
{
    callback(*m_system_menu);
    if (m_current_menubar)
        m_current_menubar->for_each_menu(callback);
}

int WSWindowManager::menubar_menu_margin() const
{
    return 16;
}

void WSWindowManager::set_current_menubar(WSMenuBar* menubar)
{
    LOCKER(m_lock);
    m_current_menubar = menubar;
    dbgprintf("[WM] Current menubar is now %p\n", menubar);
    Point next_menu_location { menubar_menu_margin() / 2, 3 };
    for_each_active_menubar_menu([&] (WSMenu& menu) {
        int text_width = font().width(menu.name());
        menu.set_rect_in_menubar({ next_menu_location.x() - menubar_menu_margin() / 2, 0, text_width + menubar_menu_margin(), menubar_rect().height() - 1 });
        menu.set_text_rect_in_menubar({ next_menu_location, { text_width, font().glyph_height() } });
        next_menu_location.move_by(menu.rect_in_menubar().width(), 0);
        return true;
    });
    invalidate();
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

    if (window.type() == WSWindowType::Menu) {
        m_back_painter->draw_rect(window.rect().inflated(2, 2), Color::LightGray);
        return;
    }

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
    m_back_painter->draw_text(titlebar_title_rect, window.title(), TextAlignment::CenterLeft, title_color);

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
        TextAlignment::CenterRight,
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

void WSWindowManager::handle_menu_mouse_event(WSMenu& menu, WSMouseEvent& event)
{
    bool is_hover_with_any_menu_open = event.type() == WSMouseEvent::MouseMove && m_current_menu;
    bool is_mousedown_with_left_button = event.type() == WSMouseEvent::MouseDown && event.button() == MouseButton::Left;
    bool should_open_menu = &menu != m_current_menu && (is_hover_with_any_menu_open || is_mousedown_with_left_button);

    if (should_open_menu) {
        if (m_current_menu == &menu)
            return;
        close_current_menu();
        if (!menu.is_empty()) {
            auto& menu_window = menu.ensure_menu_window();
            menu_window.move_to({ menu.rect_in_menubar().x(), menu.rect_in_menubar().bottom() });
            menu_window.set_visible(true);
        }
        m_current_menu = &menu;
        return;
    }
    if (event.type() == WSMouseEvent::MouseDown && event.button() == MouseButton::Left) {
        close_current_menu();
        return;
    }
}

void WSWindowManager::close_current_menu()
{
    if (m_current_menu && m_current_menu->menu_window())
        m_current_menu->menu_window()->set_visible(false);
    m_current_menu = nullptr;
}

void WSWindowManager::handle_menubar_mouse_event(WSMenuBar&, WSMouseEvent& event)
{
    for_each_active_menubar_menu([&] (WSMenu& menu) {
        if (menu.rect_in_menubar().contains(event.position())) {
            handle_menu_mouse_event(menu, event);
            return false;
        }
        return true;
    });
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
    LOCKER(m_lock);
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
        ASSERT(window->is_visible()); // Maybe this should be supported? Idk. Let's catch it and think about it later.
        Point position { event.x() - window->rect().x(), event.y() - window->rect().y() };
        auto local_event = make<WSMouseEvent>(event.type(), position, event.buttons(), event.button());
        window->on_message(*local_event);
    }

    if (menubar_rect().contains(event.position())) {
        handle_menubar_mouse_event(*m_current_menubar, event);
        return;
    }

    if (m_current_menu) {
        bool event_is_inside_current_menu = m_current_menu->menu_window()->rect().contains(event.position());
        if (!event_is_inside_current_menu) {
            if (m_current_menu->hovered_item())
                m_current_menu->clear_hovered_item();
            if (event.type() == WSMessage::MouseDown || event.type() == WSMessage::MouseUp)
                close_current_menu();
        }
    }

    for_each_visible_window_from_front_to_back([&] (WSWindow& window) {
        if (window.type() != WSWindowType::Menu && title_bar_rect(window.rect()).contains(event.position())) {
            if (event.type() == WSMessage::MouseDown) {
                move_to_front(window);
                set_active_window(&window);
            }
            if (close_button_rect_for_window(window.rect()).contains(event.position())) {
                handle_close_button_mouse_event(window, event);
                return IterationDecision::Abort;
            }
            handle_titlebar_mouse_event(window, event);
            return IterationDecision::Abort;
        }

        if (window.rect().contains(event.position())) {
            if (window.type() != WSWindowType::Menu && event.type() == WSMessage::MouseDown) {
                move_to_front(window);
                set_active_window(&window);
            }
            // FIXME: Should we just alter the coordinates of the existing MouseEvent and pass it through?
            Point position { event.x() - window.rect().x(), event.y() - window.rect().y() };
            auto local_event = make<WSMouseEvent>(event.type(), position, event.buttons(), event.button());
            window.on_message(*local_event);
            return IterationDecision::Abort;
        }
        return IterationDecision::Continue;
    });
}

template<typename Callback>
IterationDecision WSWindowManager::for_each_visible_window_of_type_from_back_to_front(WSWindowType type, Callback callback)
{
    for (auto* window = m_windows_in_order.head(); window; window = window->next()) {
        if (!window->is_visible())
            continue;
        if (window->type() != type)
            continue;
        if (callback(*window) == IterationDecision::Abort)
            return IterationDecision::Abort;
    }
    return IterationDecision::Continue;
}

template<typename Callback>
IterationDecision WSWindowManager::for_each_visible_window_from_back_to_front(Callback callback)
{
    if (for_each_visible_window_of_type_from_back_to_front(WSWindowType::Normal, callback) == IterationDecision::Abort)
        return IterationDecision::Abort;
    return for_each_visible_window_of_type_from_back_to_front(WSWindowType::Menu, callback);
}

template<typename Callback>
IterationDecision WSWindowManager::for_each_visible_window_of_type_from_front_to_back(WSWindowType type, Callback callback)
{
    for (auto* window = m_windows_in_order.tail(); window; window = window->prev()) {
        if (!window->is_visible())
            continue;
        if (window->type() != type)
            continue;
        if (callback(*window) == IterationDecision::Abort)
            return IterationDecision::Abort;
    }
    return IterationDecision::Continue;
}

template<typename Callback>
IterationDecision WSWindowManager::for_each_visible_window_from_front_to_back(Callback callback)
{
    if (for_each_visible_window_of_type_from_front_to_back(WSWindowType::Menu, callback) == IterationDecision::Abort)
        return IterationDecision::Abort;
    return for_each_visible_window_of_type_from_front_to_back(WSWindowType::Normal, callback);
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
            if (!window->is_visible())
                continue;
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
        LOCKER(m_wallpaper_path.lock());
        if (!m_wallpaper)
            m_back_painter->fill_rect(dirty_rect, m_background_color);
        else
            m_back_painter->blit(dirty_rect.location(), *m_wallpaper, dirty_rect);
    }

    for_each_visible_window_from_back_to_front([&] (WSWindow& window) {
        WSWindowLocker locker(window);
        RetainPtr<GraphicsBitmap> backing = window.backing();
        if (!backing)
            return IterationDecision::Continue;
        if (!any_dirty_rect_intersects_window(window))
            return IterationDecision::Continue;
        for (auto& dirty_rect : dirty_rects) {
            m_back_painter->set_clip_rect(dirty_rect);
            paint_window_frame(window);
            Rect dirty_rect_in_window_coordinates = Rect::intersection(dirty_rect, window.rect());
            if (dirty_rect_in_window_coordinates.is_empty())
                continue;
            dirty_rect_in_window_coordinates.set_x(dirty_rect_in_window_coordinates.x() - window.x());
            dirty_rect_in_window_coordinates.set_y(dirty_rect_in_window_coordinates.y() - window.y());
            auto dst = window.position();
            dst.move_by(dirty_rect_in_window_coordinates.location());
            m_back_painter->blit(dst, *backing, dirty_rect_in_window_coordinates);
            m_back_painter->clear_clip_rect();
        }
        m_back_painter->clear_clip_rect();
        return IterationDecision::Continue;
    });

    draw_menubar();
    draw_cursor();

    if (m_flash_flush.lock_and_copy()) {
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

Rect WSWindowManager::menubar_rect() const
{
    return { 0, 0, m_screen_rect.width(), 16 };
}

void WSWindowManager::draw_menubar()
{
    m_back_painter->fill_rect(menubar_rect(), Color::LightGray);
    m_back_painter->draw_line({ 0, menubar_rect().bottom() }, { menubar_rect().right(), menubar_rect().bottom() }, Color::White);
    for_each_active_menubar_menu([&] (WSMenu& menu) {
        Color text_color = Color::Black;
        if (&menu == m_current_menu) {
            m_back_painter->fill_rect(menu.rect_in_menubar(), menu_selection_color());
            text_color = Color::White;
        }
        m_back_painter->draw_text(menu.text_rect_in_menubar(), menu.name(), TextAlignment::CenterLeft, text_color);
        return true;
    });

    unsigned year, month, day, hour, minute, second;
    RTC::read_registers(year, month, day, hour, minute, second);
    auto time_text = String::format("%04u-%02u-%02u  %02u:%02u:%02u", year, month, day, hour, minute, second);
    auto time_rect = menubar_rect().translated(-(menubar_menu_margin() / 2), 0);
    m_back_painter->draw_text(time_rect, time_text, TextAlignment::CenterRight, Color::Black);
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

    if (message.is_client_request())
        handle_client_request(static_cast<WSAPIClientRequest&>(message));
}

void WSWindowManager::handle_client_request(WSAPIClientRequest& request)
{
    switch (request.type()) {
    case WSMessage::APICreateMenubarRequest: {
        int menubar_id = m_next_menubar_id++;
        auto menubar = make<WSMenuBar>(menubar_id, *WSMessageLoop::process_from_client_id(request.client_id()));
        m_menubars.set(menubar_id, move(menubar));
        GUI_ServerMessage response;
        response.type = GUI_ServerMessage::Type::DidCreateMenubar;
        response.menu.menubar_id = menubar_id;
        WSMessageLoop::the().post_message_to_client(request.client_id(), response);
        break;
    }
    case WSMessage::APIDestroyMenubarRequest: {
        int menubar_id = static_cast<WSAPIDestroyMenubarRequest&>(request).menubar_id();
        auto it = m_menubars.find(menubar_id);
        if (it == m_menubars.end()) {
            ASSERT_NOT_REACHED();
            // FIXME: Send an error.
            return;
        }
        auto& menubar = *(*it).value;
        if (&menubar == m_current_menubar)
            set_current_menubar(nullptr);
        m_menubars.remove(it);
        GUI_ServerMessage response;
        response.type = GUI_ServerMessage::Type::DidDestroyMenubar;
        response.menu.menubar_id = menubar_id;
        WSMessageLoop::the().post_message_to_client(request.client_id(), response);
        break;
    }
    case WSMessage::APICreateMenuRequest: {
        int menu_id = m_next_menu_id++;
        auto menu = make<WSMenu>(*WSMessageLoop::process_from_client_id(request.client_id()), menu_id, static_cast<WSAPICreateMenuRequest&>(request).text());
        m_menus.set(menu_id, move(menu));
        GUI_ServerMessage response;
        response.type = GUI_ServerMessage::Type::DidCreateMenu;
        response.menu.menu_id = menu_id;
        WSMessageLoop::the().post_message_to_client(request.client_id(), response);
        break;
    }
    case WSMessage::APIDestroyMenuRequest: {
        int menu_id = static_cast<WSAPIDestroyMenuRequest&>(request).menu_id();
        auto it = m_menus.find(menu_id);
        if (it == m_menus.end()) {
            ASSERT_NOT_REACHED();
            // FIXME: Send an error.
            return;
        }
        auto& menu = *(*it).value;
        close_menu(menu);
        m_menus.remove(it);
        GUI_ServerMessage response;
        response.type = GUI_ServerMessage::Type::DidDestroyMenu;
        response.menu.menu_id = menu_id;
        WSMessageLoop::the().post_message_to_client(request.client_id(), response);
        break;
    }
    case WSMessage::APISetApplicationMenubarRequest: {
        int menubar_id = static_cast<WSAPISetApplicationMenubarRequest&>(request).menubar_id();
        auto it = m_menubars.find(menubar_id);
        if (it == m_menubars.end()) {
            ASSERT_NOT_REACHED();
            // FIXME: Send an error.
            return;
        }
        auto& menubar = *(*it).value;
        m_app_menubars.set(request.client_id(), &menubar);
        if (active_client_id() == request.client_id())
            set_current_menubar(&menubar);
        invalidate();
        GUI_ServerMessage response;
        response.type = GUI_ServerMessage::Type::DidSetApplicationMenubar;
        response.menu.menubar_id = menubar_id;
        WSMessageLoop::the().post_message_to_client(request.client_id(), response);
        break;
    }
    case WSMessage::APIAddMenuToMenubarRequest: {
        int menubar_id = static_cast<WSAPIAddMenuToMenubarRequest&>(request).menubar_id();
        int menu_id = static_cast<WSAPIAddMenuToMenubarRequest&>(request).menu_id();
        auto it = m_menubars.find(menubar_id);
        auto jt = m_menus.find(menu_id);
        if (it == m_menubars.end() || jt == m_menus.end()) {
            ASSERT_NOT_REACHED();
            // FIXME: Send an error.
            return;
        }
        auto& menubar = *(*it).value;
        auto& menu = *(*jt).value;
        menubar.add_menu(&menu);
        GUI_ServerMessage response;
        response.type = GUI_ServerMessage::Type::DidAddMenuToMenubar;
        response.menu.menubar_id = menubar_id;
        response.menu.menu_id = menu_id;
        WSMessageLoop::the().post_message_to_client(request.client_id(), response);
        break;
    }
    default:
        break;
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

        ASSERT(window->process());
        auto it = m_app_menubars.find(window->process()->gui_client_id());
        if (it != m_app_menubars.end()) {
            auto* menubar = (*it).value;
            if (menubar != m_current_menubar)
                set_current_menubar(menubar);
        } else {
            set_current_menubar(nullptr);
        }
    }
}

void WSWindowManager::invalidate()
{
    LOCKER(m_lock);
    m_dirty_rects.clear_with_capacity();
    invalidate(m_screen_rect);
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

void WSWindowManager::close_menu(WSMenu& menu)
{
    LOCKER(m_lock);
    if (m_current_menu == &menu)
        close_current_menu();
}

int WSWindowManager::api$menu_add_separator(int menu_id)
{
    LOCKER(m_lock);
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end())
        return -EBADMENU;
    auto& menu = *(*it).value;
    menu.add_item(make<WSMenuItem>(WSMenuItem::Separator));
    return 0;
}

int WSWindowManager::api$menu_add_item(int menu_id, unsigned identifier, String&& text)
{
    LOCKER(m_lock);
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end())
        return -EBADMENU;
    auto& menu = *(*it).value;
    menu.add_item(make<WSMenuItem>(identifier, move(text)));
    return 0;
}

int WSWindowManager::active_client_id() const
{
    if (m_active_window)
        return m_active_window->process()->gui_client_id();
    return 0;
}

void WSWindowManager::destroy_all_menus(Process& process)
{
    LOCKER(m_lock);
    Vector<int> menu_ids;
    bool should_close_current_menu = false;
    for (auto& it : m_menus) {
        if (it.value->process() == &process)
            menu_ids.append(it.value->menu_id());
        if (m_current_menu == it.value.ptr())
            should_close_current_menu = true;
    }
    if (should_close_current_menu)
        close_current_menu();
    for (int menu_id : menu_ids)
        m_menus.remove(menu_id);

    Vector<int> menubar_ids;
    bool should_close_current_menubar = false;
    for (auto& it : m_menubars) {
        if (it.value->process() == &process)
            menubar_ids.append(it.value->menubar_id());
        if (m_current_menubar == it.value.ptr())
            should_close_current_menubar = true;
    }
    if (should_close_current_menubar)
        set_current_menubar(nullptr);
    for (int menubar_id : menubar_ids)
        m_menubars.remove(menubar_id);
    m_app_menubars.remove(process.gui_client_id());
}
