#include "WSWindowManager.h"
#include "WSWindow.h"
#include "WSScreen.h"
#include "WSMessageLoop.h"
#include <SharedGraphics/Font.h>
#include <SharedGraphics/Painter.h>
#include <SharedGraphics/CharacterBitmap.h>
#include <AK/StdLibExtras.h>
#include <errno.h>
#include "WSMenu.h"
#include "WSMenuBar.h"
#include "WSMenuItem.h"
#include <WindowServer/WSClientConnection.h>
#include <unistd.h>
#include <stdio.h>
#include <time.h>
#include <SharedGraphics/StylePainter.h>
#include <SharedGraphics/PNGLoader.h>
#include <WindowServer/WSCursor.h>
#include <WindowServer/WSButton.h>

//#define DEBUG_COUNTERS
//#define RESIZE_DEBUG

static void get_cpu_usage(unsigned& busy, unsigned& idle);

static WSWindowManager* s_the;

WSWindowManager& WSWindowManager::the()
{
    ASSERT(s_the);
    return *s_the;
}

void WSWindowManager::flip_buffers()
{
    swap(m_front_bitmap, m_back_bitmap);
    swap(m_front_painter, m_back_painter);
    int new_y_offset = m_buffers_are_flipped ? 0 : m_screen_rect.height();
    WSScreen::the().set_y_offset(new_y_offset);
    m_buffers_are_flipped = !m_buffers_are_flipped;
}

WSWindowManager::WSWindowManager()
    : m_screen(WSScreen::the())
    , m_screen_rect(m_screen.rect())
    , m_flash_flush(false)
{
    s_the = this;

#ifndef DEBUG_COUNTERS
    (void)m_compose_count;
    (void)m_flush_count;
#endif
    auto size = m_screen_rect.size();
    m_front_bitmap = GraphicsBitmap::create_wrapper(GraphicsBitmap::Format::RGB32, size, m_screen.scanline(0));
    m_back_bitmap = GraphicsBitmap::create_wrapper(GraphicsBitmap::Format::RGB32, size, m_screen.scanline(size.height()));

    m_front_painter = make<Painter>(*m_front_bitmap);
    m_back_painter = make<Painter>(*m_back_bitmap);

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
    m_highlight_window_border_color = Color::from_rgb(0xa10d0d);
    m_highlight_window_border_color2 = Color::from_rgb(0xfabbbb);
    m_highlight_window_title_color = Color::White;

    m_arrow_cursor = WSCursor::create(*GraphicsBitmap::load_from_file("/res/cursors/arrow.png"), { 2, 2 });
    m_resize_horizontally_cursor = WSCursor::create(*GraphicsBitmap::load_from_file("/res/cursors/resize-horizontal.png"));
    m_resize_vertically_cursor = WSCursor::create(*GraphicsBitmap::load_from_file("/res/cursors/resize-vertical.png"));
    m_resize_diagonally_tlbr_cursor = WSCursor::create(*GraphicsBitmap::load_from_file("/res/cursors/resize-diagonal-tlbr.png"));
    m_resize_diagonally_bltr_cursor = WSCursor::create(*GraphicsBitmap::load_from_file("/res/cursors/resize-diagonal-bltr.png"));
    m_i_beam_cursor = WSCursor::create(*GraphicsBitmap::load_from_file("/res/cursors/i-beam.png"));
    m_disallowed_cursor = WSCursor::create(*GraphicsBitmap::load_from_file("/res/cursors/disallowed.png"));
    m_move_cursor = WSCursor::create(*GraphicsBitmap::load_from_file("/res/cursors/move.png"));

    m_wallpaper_path = "/res/wallpapers/retro.rgb";
    m_wallpaper = GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, m_wallpaper_path, { 1024, 768 });

    m_username = getlogin();

    m_menu_selection_color = Color::from_rgb(0x84351a);

    {
        byte system_menu_name[] = { 0xf8, 0 };
        m_system_menu = make<WSMenu>(nullptr, -1, String((const char*)system_menu_name));
        m_system_menu->add_item(make<WSMenuItem>(0, "Open Terminal..."));
        m_system_menu->add_item(make<WSMenuItem>(1, "Open ProcessManager..."));
        m_system_menu->add_item(make<WSMenuItem>(WSMenuItem::Separator));
        m_system_menu->add_item(make<WSMenuItem>(100, "640x480"));
        m_system_menu->add_item(make<WSMenuItem>(101, "800x600"));
        m_system_menu->add_item(make<WSMenuItem>(102, "1024x768"));
        m_system_menu->add_item(make<WSMenuItem>(103, "1920x1080"));
        m_system_menu->add_item(make<WSMenuItem>(WSMenuItem::Separator));
        m_system_menu->add_item(make<WSMenuItem>(200, "About..."));
        m_system_menu->on_item_activation = [this] (WSMenuItem& item) {
            if (item.identifier() == 0) {
                if (fork() == 0) {
                    execl("/bin/Terminal", "/bin/Terminal", nullptr);
                    ASSERT_NOT_REACHED();
                }
                return;
            }
            if (item.identifier() == 1) {
                if (fork() == 0) {
                    execl("/bin/ProcessManager", "/bin/ProcessManager", nullptr);
                    ASSERT_NOT_REACHED();
                }
                return;
            }
            switch (item.identifier()) {
            case 100: set_resolution(640, 480); break;
            case 101: set_resolution(800, 600); break;
            case 102: set_resolution(1024, 768); break;
            case 103: set_resolution(1920, 1080); break;
            }
            if (item.identifier() == 200) {
                if (fork() == 0) {
                    execl("/bin/About", "/bin/About", nullptr);
                    ASSERT_NOT_REACHED();
                }
                return;
            }
#ifdef DEBUG_MENUS
            dbgprintf("WSMenu 1 item activated: '%s'\n", item.text().characters());
#endif
        };
    }

    // NOTE: This ensures that the system menu has the correct dimensions.
    set_current_menubar(nullptr);

    create_thread([] (void* context) -> int {
        auto& wm = *(WSWindowManager*)context;
        for (;;) {
            static unsigned last_busy;
            static unsigned last_idle;
            unsigned busy;
            unsigned idle;
            get_cpu_usage(busy, idle);
            unsigned busy_diff = busy - last_busy;
            unsigned idle_diff = idle - last_idle;
            last_busy = busy;
            last_idle = idle;
            float cpu = (float)busy_diff / (float)(busy_diff + idle_diff);
            wm.m_cpu_history.enqueue(cpu);
            sleep(1);
        }
    }, this);

    WSMessageLoop::the().start_timer(300, [this] {
        static time_t last_update_time;
        static int last_cpu_history_size = 0;
        static int last_cpu_history_head_index = 0;
        time_t now = time(nullptr);
        if (now != last_update_time || m_cpu_history.size() != last_cpu_history_size || m_cpu_history.head_index() != last_cpu_history_head_index) {
            tick_clock();
            last_update_time = now;
            last_cpu_history_head_index = m_cpu_history.head_index();
            last_cpu_history_size = m_cpu_history.size();
        }
    });

    invalidate();
    compose();
}

WSWindowManager::~WSWindowManager()
{
}

const Font& WSWindowManager::font() const
{
    return Font::default_font();
}

const Font& WSWindowManager::window_title_font() const
{
    return Font::default_bold_font();
}

const Font& WSWindowManager::menu_font() const
{
    return Font::default_font();
}

const Font& WSWindowManager::app_menu_font() const
{
    return Font::default_bold_font();
}

void get_cpu_usage(unsigned& busy, unsigned& idle)
{
    busy = 0;
    idle = 0;

    FILE* fp = fopen("/proc/all", "r");
    if (!fp) {
        perror("failed to open /proc/all");
        ASSERT_NOT_REACHED();
    }
    for (;;) {
        char buf[BUFSIZ];
        char* ptr = fgets(buf, sizeof(buf), fp);
        if (!ptr)
            break;
        auto parts = String(buf, Chomp).split(',');
        if (parts.size() < 17)
            break;
        bool ok;
        pid_t pid = parts[0].to_uint(ok);
        ASSERT(ok);
        unsigned nsched = parts[1].to_uint(ok);
        ASSERT(ok);

        if (pid == 0)
            idle += nsched;
        else
            busy += nsched;
    }
    int rc = fclose(fp);
    ASSERT(rc == 0);
}

void WSWindowManager::tick_clock()
{
    invalidate(menubar_rect());
}

bool WSWindowManager::set_wallpaper(const String& path)
{
    auto bitmap = load_png(path);
    if (!bitmap)
        return false;

    m_wallpaper_path = path;
    m_wallpaper = move(bitmap);
    invalidate();
    return true;
}

void WSWindowManager::set_resolution(int width, int height)
{
    if (m_screen_rect.width() == width && m_screen_rect.height() == height)
        return;
    m_wallpaper_path = { };
    m_wallpaper = nullptr;
    m_screen.set_resolution(width, height);
    m_screen_rect = m_screen.rect();
    m_front_bitmap = GraphicsBitmap::create_wrapper(GraphicsBitmap::Format::RGB32, { width, height }, m_screen.scanline(0));
    m_back_bitmap = GraphicsBitmap::create_wrapper(GraphicsBitmap::Format::RGB32, { width, height }, m_screen.scanline(height));
    m_front_painter = make<Painter>(*m_front_bitmap);
    m_back_painter = make<Painter>(*m_back_bitmap);
    m_buffers_are_flipped = false;
    invalidate();
    compose();

    WSClientConnection::for_each_client([&] (WSClientConnection& client) {
        client.notify_about_new_screen_rect(m_screen_rect);
    });
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
    if (menubar)
        m_current_menubar = menubar->make_weak_ptr();
    else
        m_current_menubar = nullptr;
#ifdef DEBUG_MENUS
    dbgprintf("[WM] Current menubar is now %p\n", menubar);
#endif
    Point next_menu_location { menubar_menu_margin() / 2, 0 };
    int index = 0;
    for_each_active_menubar_menu([&] (WSMenu& menu) {
        int text_width = index == 1 ? Font::default_bold_font().width(menu.name()) : font().width(menu.name());
        menu.set_rect_in_menubar({ next_menu_location.x() - menubar_menu_margin() / 2, 0, text_width + menubar_menu_margin(), menubar_rect().height() - 1 });
        menu.set_text_rect_in_menubar({ next_menu_location, { text_width, menubar_rect().height() } });
        next_menu_location.move_by(menu.rect_in_menubar().width(), 0);
        ++index;
        return true;
    });
    invalidate(menubar_rect());
}

void WSWindowManager::add_window(WSWindow& window)
{
    m_windows.set(&window);
    m_windows_in_order.append(&window);
    if (!active_window() || active_window()->client() == window.client())
        set_active_window(&window);
    if (m_switcher.is_visible() && window.type() != WSWindowType::WindowSwitcher)
        m_switcher.refresh();

    if (window.listens_to_wm_events()) {
        for_each_window([&] (WSWindow& other_window) {
            if (&window != &other_window)
                tell_wm_listener_about_window(window, other_window);
            return IterationDecision::Continue;
        });
    }

    tell_wm_listeners_window_state_changed(window);
}

void WSWindowManager::move_to_front_and_make_active(WSWindow& window)
{
    if (window.is_blocked_by_modal_window())
        return;

    if (m_windows_in_order.tail() != &window)
        invalidate(window);
    m_windows_in_order.remove(&window);
    m_windows_in_order.append(&window);

    set_active_window(&window);
}

void WSWindowManager::remove_window(WSWindow& window)
{
    if (!m_windows.contains(&window))
        return;

    invalidate(window);
    m_windows.remove(&window);
    m_windows_in_order.remove(&window);
    if (!active_window() && !m_windows.is_empty())
        set_active_window(*m_windows.begin());
    if (m_switcher.is_visible() && window.type() != WSWindowType::WindowSwitcher)
        m_switcher.refresh();

    for_each_window_listening_to_wm_events([&window] (WSWindow& listener) {
        if (window.client())
            WSMessageLoop::the().post_message(listener, make<WSWMWindowRemovedEvent>(window.client()->client_id(), window.window_id()));
        return IterationDecision::Continue;
    });
}

void WSWindowManager::tell_wm_listener_about_window(WSWindow& listener, WSWindow& window)
{
    if (window.client())
        WSMessageLoop::the().post_message(listener, make<WSWMWindowStateChangedEvent>(window.client()->client_id(), window.window_id(), window.title(), window.rect(), window.is_active(), window.type()));
}

void WSWindowManager::tell_wm_listeners_window_state_changed(WSWindow& window)
{
    for_each_window_listening_to_wm_events([&] (WSWindow& listener) {
        tell_wm_listener_about_window(listener, window);
        return IterationDecision::Continue;
    });
}

void WSWindowManager::notify_title_changed(WSWindow& window)
{
    dbgprintf("[WM] WSWindow{%p} title set to '%s'\n", &window, window.title().characters());
    invalidate(window.frame().rect());
    if (m_switcher.is_visible())
        m_switcher.refresh();

    tell_wm_listeners_window_state_changed(window);
}

void WSWindowManager::notify_rect_changed(WSWindow& window, const Rect& old_rect, const Rect& new_rect)
{
    UNUSED_PARAM(old_rect);
    UNUSED_PARAM(new_rect);
#ifdef RESIZE_DEBUG
    dbgprintf("[WM] WSWindow %p rect changed (%d,%d %dx%d) -> (%d,%d %dx%d)\n", &window, old_rect.x(), old_rect.y(), old_rect.width(), old_rect.height(), new_rect.x(), new_rect.y(), new_rect.width(), new_rect.height());
#endif
    if (m_switcher.is_visible() && window.type() != WSWindowType::WindowSwitcher)
        m_switcher.refresh();
    tell_wm_listeners_window_state_changed(window);
}

void WSWindowManager::handle_menu_mouse_event(WSMenu& menu, const WSMouseEvent& event)
{
    bool is_hover_with_any_menu_open = event.type() == WSMouseEvent::MouseMove && m_current_menu;
    bool is_mousedown_with_left_button = event.type() == WSMouseEvent::MouseDown && event.button() == MouseButton::Left;
    bool should_open_menu = &menu != current_menu() && (is_hover_with_any_menu_open || is_mousedown_with_left_button);

    if (should_open_menu) {
        if (current_menu() == &menu)
            return;
        close_current_menu();
        if (!menu.is_empty()) {
            auto& menu_window = menu.ensure_menu_window();
            menu_window.move_to({ menu.rect_in_menubar().x(), menu.rect_in_menubar().bottom() });
            menu_window.set_visible(true);
        }
        m_current_menu = menu.make_weak_ptr();
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

void WSWindowManager::handle_menubar_mouse_event(const WSMouseEvent& event)
{
    for_each_active_menubar_menu([&] (WSMenu& menu) {
        if (menu.rect_in_menubar().contains(event.position())) {
            handle_menu_mouse_event(menu, event);
            return false;
        }
        return true;
    });
}

void WSWindowManager::start_window_drag(WSWindow& window, const WSMouseEvent& event)
{
#ifdef DRAG_DEBUG
    printf("[WM] Begin dragging WSWindow{%p}\n", &window);
#endif
    move_to_front_and_make_active(window);
    m_drag_window = window.make_weak_ptr();;
    m_drag_origin = event.position();
    m_drag_window_origin = window.position();
    invalidate(window);
}

void WSWindowManager::start_window_resize(WSWindow& window, const WSMouseEvent& event)
{
    move_to_front_and_make_active(window);
    constexpr ResizeDirection direction_for_hot_area[3][3] = {
        { ResizeDirection::UpLeft, ResizeDirection::Up, ResizeDirection::UpRight },
        { ResizeDirection::Left, ResizeDirection::None, ResizeDirection::Right },
        { ResizeDirection::DownLeft, ResizeDirection::Down, ResizeDirection::DownRight },
    };
    Rect outer_rect = window.frame().rect();
    ASSERT(outer_rect.contains(event.position()));
    int window_relative_x = event.x() - outer_rect.x();
    int window_relative_y = event.y() - outer_rect.y();
    int hot_area_row = min(2, window_relative_y / (outer_rect.height() / 3));
    int hot_area_column = min(2, window_relative_x / (outer_rect.width() / 3));
    m_resize_direction = direction_for_hot_area[hot_area_row][hot_area_column];
    if (m_resize_direction == ResizeDirection::None) {
        ASSERT(!m_resize_window);
        return;
    }

#ifdef RESIZE_DEBUG
    printf("[WM] Begin resizing WSWindow{%p}\n", &window);
#endif
    m_resize_window = window.make_weak_ptr();;
    m_resize_origin = event.position();
    m_resize_window_original_rect = window.rect();
    m_resize_window->set_has_painted_since_last_resize(true);

    invalidate(window);
}

bool WSWindowManager::process_ongoing_window_drag(const WSMouseEvent& event, WSWindow*&)
{
    if (!m_drag_window)
        return false;
    if (event.type() == WSMessage::MouseUp && event.button() == MouseButton::Left) {
#ifdef DRAG_DEBUG
        printf("[WM] Finish dragging WSWindow{%p}\n", m_drag_window.ptr());
#endif
        invalidate(*m_drag_window);
        m_drag_window = nullptr;
        return true;
    }
    if (event.type() == WSMessage::MouseMove) {
        Point pos = m_drag_window_origin;
#ifdef DRAG_DEBUG
        dbgprintf("[WM] Dragging [origin: %d,%d] now: %d,%d\n", m_drag_origin.x(), m_drag_origin.y(), event.x(), event.y());
#endif
        pos.move_by(event.x() - m_drag_origin.x(), event.y() - m_drag_origin.y());
        m_drag_window->set_position_without_repaint(pos);
        return true;
    }
    return false;
}

bool WSWindowManager::process_ongoing_window_resize(const WSMouseEvent& event, WSWindow*&)
{
    if (!m_resize_window)
        return false;

    if (event.type() == WSMessage::MouseUp && event.button() == MouseButton::Right) {
#ifdef RESIZE_DEBUG
        printf("[WM] Finish resizing WSWindow{%p}\n", m_resize_window.ptr());
#endif
        WSMessageLoop::the().post_message(*m_resize_window, make<WSResizeEvent>(m_resize_window->rect(), m_resize_window->rect()));
        invalidate(*m_resize_window);
        m_resize_window = nullptr;
        return true;
    }

    if (event.type() != WSMessage::MouseMove)
        return false;

    auto old_rect = m_resize_window->rect();

    int diff_x = event.x() - m_resize_origin.x();
    int diff_y = event.y() - m_resize_origin.y();

    int change_x = 0;
    int change_y = 0;
    int change_w = 0;
    int change_h = 0;

    switch (m_resize_direction) {
    case ResizeDirection::DownRight:
        change_w = diff_x;
        change_h = diff_y;
        break;
    case ResizeDirection::Right:
        change_w = diff_x;
        break;
    case ResizeDirection::UpRight:
        change_w = diff_x;
        change_y = diff_y;
        change_h = -diff_y;
        break;
    case ResizeDirection::Up:
        change_y = diff_y;
        change_h = -diff_y;
        break;
    case ResizeDirection::UpLeft:
        change_x = diff_x;
        change_w = -diff_x;
        change_y = diff_y;
        change_h = -diff_y;
        break;
    case ResizeDirection::Left:
        change_x = diff_x;
        change_w = -diff_x;
        break;
    case ResizeDirection::DownLeft:
        change_x = diff_x;
        change_w = -diff_x;
        change_h = diff_y;
        break;
    case ResizeDirection::Down:
        change_h = diff_y;
        break;
    default:
        ASSERT_NOT_REACHED();
    }

    auto new_rect = m_resize_window_original_rect;
    Size minimum_size { 50, 50 };

    new_rect.set_x(new_rect.x() + change_x);
    new_rect.set_y(new_rect.y() + change_y);
    new_rect.set_width(max(minimum_size.width(), new_rect.width() + change_w));
    new_rect.set_height(max(minimum_size.height(), new_rect.height() + change_h));

    if (!m_resize_window->size_increment().is_null()) {
        int horizontal_incs = (new_rect.width() - m_resize_window->base_size().width()) / m_resize_window->size_increment().width();
        new_rect.set_width(m_resize_window->base_size().width() + horizontal_incs * m_resize_window->size_increment().width());
        int vertical_incs = (new_rect.height() - m_resize_window->base_size().height()) / m_resize_window->size_increment().height();
        new_rect.set_height(m_resize_window->base_size().height() + vertical_incs * m_resize_window->size_increment().height());
    }

    if (m_resize_window->rect() == new_rect)
        return true;
#ifdef RESIZE_DEBUG
    dbgprintf("[WM] Resizing [original: %s] now: %s\n",
              m_resize_window_original_rect.to_string().characters(),
              new_rect.to_string().characters());
#endif
    m_resize_window->set_rect(new_rect);
    if (m_resize_window->has_painted_since_last_resize()) {
        m_resize_window->set_has_painted_since_last_resize(false);
#ifdef RESIZE_DEBUG
        dbgprintf("[WM] I'm gonna wait for %s\n", new_rect.to_string().characters());
#endif
        m_resize_window->set_last_lazy_resize_rect(new_rect);
        WSMessageLoop::the().post_message(*m_resize_window, make<WSResizeEvent>(old_rect, new_rect));
    }
    return true;
}

void WSWindowManager::process_mouse_event(const WSMouseEvent& event, WSWindow*& event_window)
{
    event_window = nullptr;

    if (process_ongoing_window_drag(event, event_window))
        return;

    if (process_ongoing_window_resize(event, event_window))
        return;

    if (m_cursor_tracking_button)
        return m_cursor_tracking_button->on_mouse_event(event.translated(-m_cursor_tracking_button->screen_rect().location()));

    HashTable<WSWindow*> windows_who_received_mouse_event_due_to_cursor_tracking;

    for (auto* window = m_windows_in_order.tail(); window; window = window->prev()) {
        if (!window->global_cursor_tracking())
            continue;
        ASSERT(window->is_visible()); // Maybe this should be supported? Idk. Let's catch it and think about it later.
        ASSERT(!window->is_minimized()); // Maybe this should also be supported? Idk.
        windows_who_received_mouse_event_due_to_cursor_tracking.set(window);
        window->on_message(event.translated(-window->position()));
    }

    if (menubar_rect().contains(event.position())) {
        handle_menubar_mouse_event(event);
        return;
    }

    if (m_current_menu && m_current_menu->menu_window()) {
        bool event_is_inside_current_menu = m_current_menu->menu_window()->rect().contains(event.position());
        if (!event_is_inside_current_menu) {
            if (m_current_menu->hovered_item())
                m_current_menu->clear_hovered_item();
            if (event.type() == WSMessage::MouseDown || event.type() == WSMessage::MouseUp)
                close_current_menu();
        }
    }

    for_each_visible_window_from_front_to_back([&] (WSWindow& window) {
        auto window_frame_rect = window.frame().rect();
        if (!window_frame_rect.contains(event.position()))
            return IterationDecision::Continue;
        // First check if we should initiate a drag or resize (Logo+LMB or Logo+RMB).
        // In those cases, the event is swallowed by the window manager.
        if (window.type() == WSWindowType::Normal) {
            if (m_keyboard_modifiers == Mod_Logo && event.type() == WSMessage::MouseDown && event.button() == MouseButton::Left) {
                start_window_drag(window, event);
                return IterationDecision::Abort;
            }
            if (m_keyboard_modifiers == Mod_Logo && event.type() == WSMessage::MouseDown && event.button() == MouseButton::Right && !window.is_blocked_by_modal_window()) {
                start_window_resize(window, event);
                return IterationDecision::Abort;
            }
        }
        // Well okay, let's see if we're hitting the frame or the window inside the frame.
        if (window.rect().contains(event.position())) {
            if (window.type() == WSWindowType::Normal && event.type() == WSMessage::MouseDown)
                move_to_front_and_make_active(window);
            event_window = &window;
            if (!window.global_cursor_tracking() && !windows_who_received_mouse_event_due_to_cursor_tracking.contains(&window))
                window.on_message(event.translated(-window.position()));
            return IterationDecision::Abort;
        }

        // We are hitting the frame, pass the event along to WSWindowFrame.
        window.frame().on_mouse_event(event.translated(-window_frame_rect.location()));
        return IterationDecision::Abort;
    });
}

void WSWindowManager::compose()
{
    auto dirty_rects = move(m_dirty_rects);
    dirty_rects.add(Rect::intersection(m_last_cursor_rect, m_screen_rect));
    dirty_rects.add(Rect::intersection(current_cursor_rect(), m_screen_rect));
#ifdef DEBUG_COUNTERS
    dbgprintf("[WM] compose #%u (%u rects)\n", ++m_compose_count, dirty_rects.rects().size());
#endif

    auto any_opaque_window_contains_rect = [this] (const Rect& r) {
        for (auto* window = m_windows_in_order.head(); window; window = window->next()) {
            if (!window->is_visible())
                continue;
            if (window->is_minimized())
                continue;
            if (window->opacity() < 1.0f)
                continue;
            if (window->has_alpha_channel()) {
                // FIXME: Just because the window has an alpha channel doesn't mean it's not opaque.
                //        Maybe there's some way we could know this?
                continue;
            }
            if (window->frame().rect().contains(r))
                return true;
        }
        return false;
    };

    auto any_opaque_window_above_this_one_contains_rect = [this] (const WSWindow& a_window, const Rect& rect) -> bool {
        bool found = false;
        bool checking = false;
        for_each_visible_window_from_back_to_front([&] (WSWindow& window) {
            if (&window == &a_window) {
                checking = true;
                return IterationDecision::Continue;
            }
            if (!checking)
                return IterationDecision::Continue;
            if (!window.is_visible())
                return IterationDecision::Continue;
            if (window.is_minimized())
                return IterationDecision::Continue;
            if (window.opacity() < 1.0f)
                return IterationDecision::Continue;
            if (window.has_alpha_channel())
                return IterationDecision::Continue;
            if (window.frame().rect().contains(rect)) {
                found = true;
                return IterationDecision::Abort;
            }
            return IterationDecision::Continue;
        });
        return found;
    };

    auto any_dirty_rect_intersects_window = [&dirty_rects] (const WSWindow& window) {
        auto window_frame_rect = window.frame().rect();
        for (auto& dirty_rect : dirty_rects.rects()) {
            if (dirty_rect.intersects(window_frame_rect))
                return true;
        }
        return false;
    };

    for (auto& dirty_rect : dirty_rects.rects()) {
        if (any_opaque_window_contains_rect(dirty_rect))
            continue;
        if (!m_wallpaper)
            m_back_painter->fill_rect(dirty_rect, m_background_color);
        else
            m_back_painter->blit(dirty_rect.location(), *m_wallpaper, dirty_rect);
    }

    for_each_visible_window_from_back_to_front([&] (WSWindow& window) {
        RetainPtr<GraphicsBitmap> backing_store = window.backing_store();
        if (!any_dirty_rect_intersects_window(window))
            return IterationDecision::Continue;
        PainterStateSaver saver(*m_back_painter);
        m_back_painter->add_clip_rect(window.frame().rect());
        for (auto& dirty_rect : dirty_rects.rects()) {
            if (any_opaque_window_above_this_one_contains_rect(window, dirty_rect))
                continue;
            PainterStateSaver saver(*m_back_painter);
            m_back_painter->add_clip_rect(dirty_rect);
            window.frame().paint(*m_back_painter);
            if (!backing_store)
                continue;
            Rect dirty_rect_in_window_coordinates = Rect::intersection(dirty_rect, window.rect());
            if (dirty_rect_in_window_coordinates.is_empty())
                continue;
            dirty_rect_in_window_coordinates.move_by(-window.position());
            auto dst = window.position();
            dst.move_by(dirty_rect_in_window_coordinates.location());
            if (window.opacity() == 1.0f)
                m_back_painter->blit(dst, *backing_store, dirty_rect_in_window_coordinates);
            else
                m_back_painter->blit_with_opacity(dst, *backing_store, dirty_rect_in_window_coordinates, window.opacity());
        }
        return IterationDecision::Continue;
    });

    draw_menubar();
    draw_cursor();

    if (m_flash_flush) {
        for (auto& rect : dirty_rects.rects())
            m_front_painter->fill_rect(rect, Color::Yellow);
    }

    flip_buffers();
    for (auto& r : dirty_rects.rects())
        flush(r);
}

Rect WSWindowManager::current_cursor_rect() const
{
    return { m_screen.cursor_location().translated(-active_cursor().hotspot()), active_cursor().size() };
}

void WSWindowManager::invalidate_cursor()
{
    invalidate(current_cursor_rect());
}

Rect WSWindowManager::menubar_rect() const
{
    return { 0, 0, m_screen_rect.width(), 18 };
}

void WSWindowManager::draw_menubar()
{
    auto menubar_rect = this->menubar_rect();

    m_back_painter->fill_rect(menubar_rect, Color::LightGray);
    m_back_painter->draw_line({ 0, menubar_rect.bottom() }, { menubar_rect.right(), menubar_rect.bottom() }, Color::White);
    int index = 0;
    for_each_active_menubar_menu([&] (WSMenu& menu) {
        Color text_color = Color::Black;
        if (&menu == current_menu()) {
            m_back_painter->fill_rect(menu.rect_in_menubar(), menu_selection_color());
            text_color = Color::White;
        }
        m_back_painter->draw_text(
            menu.text_rect_in_menubar(),
            menu.name(),
            index == 1 ? app_menu_font() : menu_font(),
            TextAlignment::CenterLeft,
            text_color
        );
        ++index;
        return true;
    });

    int username_width = Font::default_bold_font().width(m_username);
    Rect username_rect {
        menubar_rect.right() - menubar_menu_margin() / 2 - Font::default_bold_font().width(m_username),
        menubar_rect.y(),
        username_width,
        menubar_rect.height()
    };
    m_back_painter->draw_text(username_rect, m_username, Font::default_bold_font(), TextAlignment::CenterRight, Color::Black);

    time_t now = time(nullptr);
    auto* tm = localtime(&now);
    auto time_text = String::format("%4u-%02u-%02u %02u:%02u:%02u",
        tm->tm_year + 1900,
        tm->tm_mon + 1,
        tm->tm_mday,
        tm->tm_hour,
        tm->tm_min,
        tm->tm_sec);
    int time_width = font().width(time_text);
    Rect time_rect {
        username_rect.left() - menubar_menu_margin() / 2 - time_width,
        menubar_rect.y(),
        time_width,
        menubar_rect.height()
    };

    m_back_painter->draw_text(time_rect, time_text, font(), TextAlignment::CenterRight, Color::Black);

    Rect cpu_rect { time_rect.right() - font().width(time_text) - (int)m_cpu_history.capacity() - 10, time_rect.y() + 1, (int)m_cpu_history.capacity(), time_rect.height() - 2 };
    m_back_painter->fill_rect(cpu_rect, Color::Black);
    int i = m_cpu_history.capacity() - m_cpu_history.size();
    for (auto cpu_usage : m_cpu_history) {
        m_back_painter->draw_line(
            { cpu_rect.x() + i, cpu_rect.bottom() },
            { cpu_rect.x() + i, (int)(cpu_rect.y() + (cpu_rect.height() - (cpu_usage * (float)cpu_rect.height()))) },
            Color::from_rgb(0xaa6d4b)
        );
        ++i;
    }
}

void WSWindowManager::draw_window_switcher()
{
    if (m_switcher.is_visible())
        m_switcher.draw();
}

void WSWindowManager::draw_cursor()
{
    Rect cursor_rect = current_cursor_rect();
    Color inner_color = Color::White;
    Color outer_color = Color::Black;
    if (m_screen.mouse_button_state() & (unsigned)MouseButton::Left)
        swap(inner_color, outer_color);
    m_back_painter->blit(cursor_rect.location(), active_cursor().bitmap(), active_cursor().rect());
    m_last_cursor_rect = cursor_rect;
}

void WSWindowManager::on_message(const WSMessage& message)
{
    if (message.is_mouse_event()) {
        WSWindow* event_window = nullptr;
        process_mouse_event(static_cast<const WSMouseEvent&>(message), event_window);
        set_hovered_window(event_window);
        return;
    }

    if (message.is_key_event()) {
        auto& key_event = static_cast<const WSKeyEvent&>(message);
        m_keyboard_modifiers = key_event.modifiers();

        if (key_event.type() == WSMessage::KeyDown && key_event.modifiers() == Mod_Logo && key_event.key() == Key_Tab)
            m_switcher.show();
        if (m_switcher.is_visible()) {
            m_switcher.on_key_event(key_event);
            return;
        }
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

void WSWindowManager::set_highlight_window(WSWindow* window)
{
    if (window == m_highlight_window.ptr())
        return;
    if (auto* previous_highlight_window = m_highlight_window.ptr())
        invalidate(*previous_highlight_window);
    m_highlight_window = window ? window->make_weak_ptr() : nullptr;
    if (m_highlight_window)
        invalidate(*m_highlight_window);
}

void WSWindowManager::set_active_window(WSWindow* window)
{
    if (window && window->is_blocked_by_modal_window())
        return;

    if (window->type() != WSWindowType::Normal) {
        dbgprintf("WSWindowManager: Attempted to make a non-normal window active.\n");
        return;
    }

    if (window == m_active_window.ptr())
        return;

    auto* previously_active_window = m_active_window.ptr();
    if (previously_active_window) {
        WSMessageLoop::the().post_message(*previously_active_window, make<WSMessage>(WSMessage::WindowDeactivated));
        invalidate(*previously_active_window);
    }
    m_active_window = window->make_weak_ptr();
    if (m_active_window) {
        WSMessageLoop::the().post_message(*m_active_window, make<WSMessage>(WSMessage::WindowActivated));
        invalidate(*m_active_window);

        auto* client = window->client();
        ASSERT(client);
        set_current_menubar(client->app_menubar());
        if (previously_active_window)
            tell_wm_listeners_window_state_changed(*previously_active_window);
        tell_wm_listeners_window_state_changed(*m_active_window);
    }
}

void WSWindowManager::set_hovered_window(WSWindow* window)
{
    if (m_hovered_window.ptr() == window)
        return;

    if (m_hovered_window)
        WSMessageLoop::the().post_message(*m_hovered_window, make<WSMessage>(WSMessage::WindowLeft));

    m_hovered_window = window ? window->make_weak_ptr() : nullptr;

    if (m_hovered_window)
        WSMessageLoop::the().post_message(*m_hovered_window, make<WSMessage>(WSMessage::WindowEntered));
}

void WSWindowManager::invalidate()
{
    m_dirty_rects.clear_with_capacity();
    invalidate(m_screen_rect);
}

void WSWindowManager::recompose_immediately()
{
    m_dirty_rects.clear_with_capacity();
    invalidate(m_screen_rect, false);
}

void WSWindowManager::invalidate(const Rect& a_rect, bool should_schedule_compose_event)
{
    auto rect = Rect::intersection(a_rect, m_screen_rect);
    if (rect.is_empty())
        return;

    m_dirty_rects.add(rect);

    if (should_schedule_compose_event && !m_pending_compose_event) {
        WSMessageLoop::the().post_message(*this, make<WSMessage>(WSMessage::WM_DeferredCompose));
        m_pending_compose_event = true;
    }
}

void WSWindowManager::invalidate(const WSWindow& window)
{
    invalidate(window.frame().rect());
}

void WSWindowManager::invalidate(const WSWindow& window, const Rect& rect)
{
    if (rect.is_empty()) {
        invalidate(window);
        return;
    }
    auto outer_rect = window.frame().rect();
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

    const RGBA32* front_ptr = m_front_bitmap->scanline(rect.y()) + rect.x();
    RGBA32* back_ptr = m_back_bitmap->scanline(rect.y()) + rect.x();
    size_t pitch = m_back_bitmap->pitch();

    for (int y = 0; y < rect.height(); ++y) {
        fast_dword_copy(back_ptr, front_ptr, rect.width());
        front_ptr = (const RGBA32*)((const byte*)front_ptr + pitch);
        back_ptr = (RGBA32*)((byte*)back_ptr + pitch);
    }
}

void WSWindowManager::close_menu(WSMenu& menu)
{
    if (current_menu() == &menu)
        close_current_menu();
}

void WSWindowManager::close_menubar(WSMenuBar& menubar)
{
    if (current_menubar() == &menubar)
        set_current_menubar(nullptr);
}

const WSClientConnection* WSWindowManager::active_client() const
{
    if (m_active_window)
        return m_active_window->client();
    return nullptr;
}

void WSWindowManager::notify_client_changed_app_menubar(WSClientConnection& client)
{
    if (active_client() == &client)
        set_current_menubar(client.app_menubar());
    invalidate(menubar_rect());
}

const WSCursor& WSWindowManager::active_cursor() const
{
    if (m_drag_window)
        return *m_move_cursor;

    if (m_resize_window) {
        switch (m_resize_direction) {
        case ResizeDirection::Up:
        case ResizeDirection::Down:
            return *m_resize_vertically_cursor;
        case ResizeDirection::Left:
        case ResizeDirection::Right:
            return *m_resize_horizontally_cursor;
        case ResizeDirection::UpLeft:
        case ResizeDirection::DownRight:
            return *m_resize_diagonally_tlbr_cursor;
        case ResizeDirection::UpRight:
        case ResizeDirection::DownLeft:
            return *m_resize_diagonally_bltr_cursor;
        case ResizeDirection::None:
            ASSERT_NOT_REACHED();
        }
    }

    if (m_hovered_window && m_hovered_window->override_cursor())
        return *m_hovered_window->override_cursor();

    return *m_arrow_cursor;
}
