#include "WSCompositor.h"
#include "WSEventLoop.h"
#include "WSMenu.h"
#include "WSMenuBar.h"
#include "WSMenuItem.h"
#include "WSScreen.h"
#include "WSWindow.h"
#include "WSWindowManager.h"
#include <AK/StdLibExtras.h>
#include <AK/Vector.h>
#include <LibCore/CTimer.h>
#include <SharedGraphics/CharacterBitmap.h>
#include <SharedGraphics/Font.h>
#include <SharedGraphics/PNGLoader.h>
#include <SharedGraphics/Painter.h>
#include <SharedGraphics/StylePainter.h>
#include <WindowServer/WSAPITypes.h>
#include <WindowServer/WSButton.h>
#include <WindowServer/WSClientConnection.h>
#include <WindowServer/WSCursor.h>
#include <errno.h>
#include <stdio.h>
#include <time.h>
#include <unistd.h>

//#define DEBUG_COUNTERS
//#define RESIZE_DEBUG

static WSWindowManager* s_the;

WSWindowManager& WSWindowManager::the()
{
    ASSERT(s_the);
    return *s_the;
}

WSWindowManager::WSWindowManager()
{
    s_the = this;

    m_arrow_cursor = WSCursor::create(*GraphicsBitmap::load_from_file("/res/cursors/arrow.png"), { 2, 2 });
    m_resize_horizontally_cursor = WSCursor::create(*GraphicsBitmap::load_from_file("/res/cursors/resize-horizontal.png"));
    m_resize_vertically_cursor = WSCursor::create(*GraphicsBitmap::load_from_file("/res/cursors/resize-vertical.png"));
    m_resize_diagonally_tlbr_cursor = WSCursor::create(*GraphicsBitmap::load_from_file("/res/cursors/resize-diagonal-tlbr.png"));
    m_resize_diagonally_bltr_cursor = WSCursor::create(*GraphicsBitmap::load_from_file("/res/cursors/resize-diagonal-bltr.png"));
    m_i_beam_cursor = WSCursor::create(*GraphicsBitmap::load_from_file("/res/cursors/i-beam.png"));
    m_disallowed_cursor = WSCursor::create(*GraphicsBitmap::load_from_file("/res/cursors/disallowed.png"));
    m_move_cursor = WSCursor::create(*GraphicsBitmap::load_from_file("/res/cursors/move.png"));

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

    m_username = getlogin();

    m_menu_selection_color = Color::from_rgb(0x84351a);

    struct AppMenuItem {
        const char *binary_name;
        const char *description;
    };

    Vector<AppMenuItem> apps;
    apps.append({ "/bin/Terminal", "Open Terminal..." });
    apps.append({ "/bin/FileManager", "Open FileManager..." });
    apps.append({ "/bin/ProcessManager", "Open ProcessManager..." });

    {
        byte system_menu_name[] = { 0xf8, 0 };
        m_system_menu = make<WSMenu>(nullptr, -1, String((const char*)system_menu_name));

        int appIndex = 1;
        for (const auto& app : apps) {
            m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, appIndex++, app.description));
        }

        m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, WSMenuItem::Separator));
        m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, 100, "640x480"));
        m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, 101, "800x600"));
        m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, 102, "1024x768"));
        m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, 103, "1280x720"));
        m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, 104, "1440x900"));
        m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, 105, "1920x1080"));
        m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, 106, "2560x1440"));
        m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, WSMenuItem::Separator));
        m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, 200, "About..."));
        m_system_menu->on_item_activation = [this, apps] (WSMenuItem& item) {
            if (item.identifier() >= 1 && item.identifier() <= 1 + apps.size() - 1) {
                if (fork() == 0) {
                    const auto& bin = apps[item.identifier() -1].binary_name;
                    execl(bin, bin, nullptr);
                    ASSERT_NOT_REACHED();
                }
            }
            switch (item.identifier()) {
            case 100: set_resolution(640, 480); break;
            case 101: set_resolution(800, 600); break;
            case 102: set_resolution(1024, 768); break;
            case 103: set_resolution(1280, 720); break;
            case 104: set_resolution(1440, 900); break;
            case 105: set_resolution(1920, 1080); break;
            case 106: set_resolution(2560, 1440); break;
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

    new CTimer(300, [this] {
        static time_t last_update_time;
        time_t now = time(nullptr);
        if (now != last_update_time || m_cpu_monitor.is_dirty()) {
            tick_clock();
            last_update_time = now;
            m_cpu_monitor.set_dirty(false);
        }
    });

    invalidate();
    WSCompositor::the().compose();
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

void WSWindowManager::tick_clock()
{
    invalidate(menubar_rect());
}

void WSWindowManager::set_resolution(int width, int height)
{
    WSCompositor::the().set_resolution(width, height);
    WSClientConnection::for_each_client([&] (WSClientConnection& client) {
        client.notify_about_new_screen_rect(WSScreen::the().rect());
    });
}


int WSWindowManager::menubar_menu_margin() const
{
    return 16;
}

void WSWindowManager::set_current_menu(WSMenu* menu)
{
    if (m_current_menu == menu)
        return;
    if (m_current_menu)
        m_current_menu->close();
    if (menu)
        m_current_menu = menu->make_weak_ptr();
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

    if (window.is_fullscreen()) {
        WSEventLoop::the().post_event(window, make<WSResizeEvent>(window.rect(), WSScreen::the().rect()));
        window.set_rect(WSScreen::the().rect());
    }

    set_active_window(&window);
    if (m_switcher.is_visible() && window.type() != WSWindowType::WindowSwitcher)
        m_switcher.refresh();

    if (window.listens_to_wm_events()) {
        for_each_window([&] (WSWindow& other_window) {
            if (&window != &other_window) {
                tell_wm_listener_about_window(window, other_window);
                tell_wm_listener_about_window_icon(window, other_window);
            }
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
    if (window.is_active())
        pick_new_active_window();
    if (m_switcher.is_visible() && window.type() != WSWindowType::WindowSwitcher)
        m_switcher.refresh();

    for_each_window_listening_to_wm_events([&window] (WSWindow& listener) {
        if (!(listener.wm_event_mask() & WSAPI_WMEventMask::WindowRemovals))
            return IterationDecision::Continue;
        if (window.client())
            WSEventLoop::the().post_event(listener, make<WSWMWindowRemovedEvent>(window.client()->client_id(), window.window_id()));
        return IterationDecision::Continue;
    });
}

void WSWindowManager::tell_wm_listener_about_window(WSWindow& listener, WSWindow& window)
{
    if (!(listener.wm_event_mask() & WSAPI_WMEventMask::WindowStateChanges))
        return;
    if (window.client())
        WSEventLoop::the().post_event(listener, make<WSWMWindowStateChangedEvent>(window.client()->client_id(), window.window_id(), window.title(), window.rect(), window.is_active(), window.type(), window.is_minimized()));
}

void WSWindowManager::tell_wm_listener_about_window_rect(WSWindow& listener, WSWindow& window)
{
    if (!(listener.wm_event_mask() & WSAPI_WMEventMask::WindowRectChanges))
        return;
    if (window.client())
        WSEventLoop::the().post_event(listener, make<WSWMWindowRectChangedEvent>(window.client()->client_id(), window.window_id(), window.rect()));
}

void WSWindowManager::tell_wm_listener_about_window_icon(WSWindow& listener, WSWindow& window)
{
    if (!(listener.wm_event_mask() & WSAPI_WMEventMask::WindowIconChanges))
        return;
    if (window.client())
        WSEventLoop::the().post_event(listener, make<WSWMWindowIconChangedEvent>(window.client()->client_id(), window.window_id(), window.icon_path()));
}

void WSWindowManager::tell_wm_listeners_window_state_changed(WSWindow& window)
{
    for_each_window_listening_to_wm_events([&] (WSWindow& listener) {
        tell_wm_listener_about_window(listener, window);
        return IterationDecision::Continue;
    });
}

void WSWindowManager::tell_wm_listeners_window_icon_changed(WSWindow& window)
{
    for_each_window_listening_to_wm_events([&] (WSWindow& listener) {
        tell_wm_listener_about_window_icon(listener, window);
        return IterationDecision::Continue;
    });
}

void WSWindowManager::tell_wm_listeners_window_rect_changed(WSWindow& window)
{
    for_each_window_listening_to_wm_events([&] (WSWindow& listener) {
        tell_wm_listener_about_window_rect(listener, window);
        return IterationDecision::Continue;
    });
}

void WSWindowManager::notify_title_changed(WSWindow& window)
{
    if (window.type() != WSWindowType::Normal)
        return;
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
    tell_wm_listeners_window_rect_changed(window);
}

void WSWindowManager::notify_minimization_state_changed(WSWindow& window)
{
    tell_wm_listeners_window_state_changed(window);

    if (window.is_active() && window.is_minimized())
        pick_new_active_window();
}

void WSWindowManager::pick_new_active_window()
{
    for_each_visible_window_of_type_from_front_to_back(WSWindowType::Normal, [&] (WSWindow& candidate) {
        set_active_window(&candidate);
        return IterationDecision::Abort;
    });
}

void WSWindowManager::handle_menu_mouse_event(WSMenu& menu, const WSMouseEvent& event)
{
    bool is_hover_with_any_menu_open = event.type() == WSMouseEvent::MouseMove && m_current_menu && (m_current_menu->menubar() || m_current_menu == m_system_menu);
    bool is_mousedown_with_left_button = event.type() == WSMouseEvent::MouseDown && event.button() == MouseButton::Left;
    bool should_open_menu = &menu != current_menu() && (is_hover_with_any_menu_open || is_mousedown_with_left_button);

    if (should_open_menu) {
        if (current_menu() == &menu)
            return;
        close_current_menu();
        if (!menu.is_empty()) {
            auto& menu_window = menu.ensure_menu_window();
            menu_window.move_to({ menu.rect_in_menubar().x(), menu.rect_in_menubar().bottom() + 2 });
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

void WSWindowManager::start_window_resize(WSWindow& window, const Point& position, MouseButton button)
{
    move_to_front_and_make_active(window);
    constexpr ResizeDirection direction_for_hot_area[3][3] = {
        { ResizeDirection::UpLeft, ResizeDirection::Up, ResizeDirection::UpRight },
        { ResizeDirection::Left, ResizeDirection::None, ResizeDirection::Right },
        { ResizeDirection::DownLeft, ResizeDirection::Down, ResizeDirection::DownRight },
    };
    Rect outer_rect = window.frame().rect();
    ASSERT(outer_rect.contains(position));
    int window_relative_x = position.x() - outer_rect.x();
    int window_relative_y = position.y() - outer_rect.y();
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
    m_resizing_mouse_button = button;
    m_resize_window = window.make_weak_ptr();;
    m_resize_origin = position;
    m_resize_window_original_rect = window.rect();

    invalidate(window);
}

void WSWindowManager::start_window_resize(WSWindow& window, const WSMouseEvent& event)
{
    start_window_resize(window, event.position(), event.button());
}

bool WSWindowManager::process_ongoing_window_drag(WSMouseEvent& event, WSWindow*& hovered_window)
{
    if (!m_drag_window)
        return false;
    if (event.type() == WSEvent::MouseUp && event.button() == MouseButton::Left) {
#ifdef DRAG_DEBUG
        printf("[WM] Finish dragging WSWindow{%p}\n", m_drag_window.ptr());
#endif
        invalidate(*m_drag_window);
        if (m_drag_window->rect().contains(event.position()))
            hovered_window = m_drag_window;
        if (m_drag_window->is_resizable()) {
            process_event_for_doubleclick(*m_drag_window, event);
            if (event.type() == WSEvent::MouseDoubleClick) {
#if defined(DOUBLECLICK_DEBUG)
                dbgprintf("[WM] Click up became doubleclick!\n");
#endif
                if (m_drag_window->is_maximized()) {
                    m_drag_window->set_maximized(false);
                } else {
                    m_drag_window->set_maximized(true);
                }
            }
        }
        m_drag_window = nullptr;
        return true;
    }
    if (event.type() == WSEvent::MouseMove) {
#ifdef DRAG_DEBUG
        dbgprintf("[WM] Dragging [origin: %d,%d] now: %d,%d\n", m_drag_origin.x(), m_drag_origin.y(), event.x(), event.y());
#endif
        Point pos = m_drag_window_origin.translated(event.position() - m_drag_origin);
        m_drag_window->set_position_without_repaint(pos);
        if (m_drag_window->rect().contains(event.position()))
            hovered_window = m_drag_window;
        return true;
    }
    return false;
}

bool WSWindowManager::process_ongoing_window_resize(const WSMouseEvent& event, WSWindow*& hovered_window)
{
    if (!m_resize_window)
        return false;

    if (event.type() == WSEvent::MouseUp && event.button() == m_resizing_mouse_button) {
#ifdef RESIZE_DEBUG
        printf("[WM] Finish resizing WSWindow{%p}\n", m_resize_window.ptr());
#endif
        WSEventLoop::the().post_event(*m_resize_window, make<WSResizeEvent>(m_resize_window->rect(), m_resize_window->rect()));
        invalidate(*m_resize_window);
        if (m_resize_window->rect().contains(event.position()))
            hovered_window = m_resize_window;
        m_resize_window = nullptr;
        m_resizing_mouse_button = MouseButton::None;
        return true;
    }

    if (event.type() != WSEvent::MouseMove)
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

    if (new_rect.contains(event.position()))
        hovered_window = m_resize_window;

    if (m_resize_window->rect() == new_rect)
        return true;
#ifdef RESIZE_DEBUG
    dbgprintf("[WM] Resizing [original: %s] now: %s\n",
              m_resize_window_original_rect.to_string().characters(),
              new_rect.to_string().characters());
#endif
    m_resize_window->set_rect(new_rect);
    WSEventLoop::the().post_event(*m_resize_window, make<WSResizeEvent>(old_rect, new_rect));
    return true;
}

void WSWindowManager::set_cursor_tracking_button(WSButton* button)
{
    m_cursor_tracking_button = button ? button->make_weak_ptr() : nullptr;
}

CElapsedTimer& WSWindowManager::DoubleClickInfo::click_clock(MouseButton button)
{
    switch (button) {
    case MouseButton::Left: return m_left_click_clock;
    case MouseButton::Right: return m_right_click_clock;
    case MouseButton::Middle: return m_middle_click_clock;
    default:
        ASSERT_NOT_REACHED();
    }
}

// #define DOUBLECLICK_DEBUG

void WSWindowManager::process_event_for_doubleclick(WSWindow& window, WSMouseEvent& event)
{
    // We only care about button presses (because otherwise it's not a doubleclick, duh!)
    ASSERT(event.type() == WSEvent::MouseUp);

    if (&window != m_double_click_info.m_clicked_window) {
        // we either haven't clicked anywhere, or we haven't clicked on this
        // window. set the current click window, and reset the timers.
#if defined(DOUBLECLICK_DEBUG)
        dbgprintf("Initial mouseup on window %p (previous was %p)\n", &window, m_double_click_info.m_clicked_window);
#endif
        m_double_click_info.m_clicked_window = window.make_weak_ptr();
        m_double_click_info.reset();
    }

    auto& clock = m_double_click_info.click_clock(event.button());

    // if the clock is invalid, we haven't clicked with this button on this
    // window yet, so there's nothing to do.
    if (!clock.is_valid()) {
        clock.start();
    } else {
        int elapsed_since_last_click = clock.elapsed();
        clock.start();

        // FIXME: It might be a sensible idea to also add a distance travel check.
        // If the pointer moves too far, it's not a double click.
        if (elapsed_since_last_click < 250) {
#if defined(DOUBLECLICK_DEBUG)
            dbgprintf("Transforming MouseUp to MouseDoubleClick!\n");
#endif
            event = WSMouseEvent(WSEvent::MouseDoubleClick, event.position(), event.buttons(), event.button(), event.modifiers(), event.wheel_delta());
            // invalidate this now we've delivered a doubleclick, otherwise
            // tripleclick will deliver two doubleclick events (incorrectly).
            clock = CElapsedTimer();
        } else {
            // too slow; try again
            clock.start();
        }
    }
}

void WSWindowManager::deliver_mouse_event(WSWindow& window, WSMouseEvent& event)
{
    window.event(event);
    if (event.type() == WSEvent::MouseUp) {
        process_event_for_doubleclick(window, event);
        if (event.type() == WSEvent::MouseDoubleClick)
            window.event(event);
    }
}

void WSWindowManager::process_mouse_event(WSMouseEvent& event, WSWindow*& hovered_window)
{
    hovered_window = nullptr;

    if (process_ongoing_window_drag(event, hovered_window))
        return;

    if (process_ongoing_window_resize(event, hovered_window))
        return;

    if (m_cursor_tracking_button)
        return m_cursor_tracking_button->on_mouse_event(event.translated(-m_cursor_tracking_button->screen_rect().location()));

    // This is quite hackish, but it's how the WSButton hover effect is implemented.
    if (m_hovered_button && event.type() == WSEvent::MouseMove)
        m_hovered_button->on_mouse_event(event.translated(-m_hovered_button->screen_rect().location()));

    HashTable<WSWindow*> windows_who_received_mouse_event_due_to_cursor_tracking;

    for (auto* window = m_windows_in_order.tail(); window; window = window->prev()) {
        if (!window->global_cursor_tracking())
            continue;
        ASSERT(window->is_visible()); // Maybe this should be supported? Idk. Let's catch it and think about it later.
        ASSERT(!window->is_minimized()); // Maybe this should also be supported? Idk.
        windows_who_received_mouse_event_due_to_cursor_tracking.set(window);
        auto translated_event = event.translated(-window->position());
        deliver_mouse_event(*window, translated_event);
    }

    if (menubar_rect().contains(event.position())) {
        handle_menubar_mouse_event(event);
        return;
    }

    if (m_current_menu && m_current_menu->menu_window()) {
        auto& window = *m_current_menu->menu_window();
        bool event_is_inside_current_menu = window.rect().contains(event.position());
        if (!event_is_inside_current_menu) {
            if (m_current_menu->hovered_item())
                m_current_menu->clear_hovered_item();
            if (event.type() == WSEvent::MouseDown || event.type() == WSEvent::MouseUp)
                close_current_menu();
        } else {
            hovered_window = &window;
            auto translated_event = event.translated(-window.position());
            deliver_mouse_event(window, translated_event);
        }
        return;
    }

    WSWindow* event_window_with_frame = nullptr;

    for_each_visible_window_from_front_to_back([&] (WSWindow& window) {
        auto window_frame_rect = window.frame().rect();
        if (!window_frame_rect.contains(event.position()))
            return IterationDecision::Continue;

        if (&window != m_resize_candidate.ptr())
            clear_resize_candidate();

        // First check if we should initiate a drag or resize (Logo+LMB or Logo+RMB).
        // In those cases, the event is swallowed by the window manager.
        if (window.type() == WSWindowType::Normal) {
            if (!window.is_fullscreen() && m_keyboard_modifiers == Mod_Logo && event.type() == WSEvent::MouseDown && event.button() == MouseButton::Left) {
                hovered_window = &window;
                start_window_drag(window, event);
                return IterationDecision::Abort;
            }
            if (window.is_resizable() && m_keyboard_modifiers == Mod_Logo && event.type() == WSEvent::MouseDown && event.button() == MouseButton::Right && !window.is_blocked_by_modal_window()) {
                hovered_window = &window;
                start_window_resize(window, event);
                return IterationDecision::Abort;
            }
        }
        // Well okay, let's see if we're hitting the frame or the window inside the frame.
        if (window.rect().contains(event.position())) {
            if (window.type() == WSWindowType::Normal && event.type() == WSEvent::MouseDown)
                move_to_front_and_make_active(window);

            hovered_window = &window;
            if (!window.global_cursor_tracking() && !windows_who_received_mouse_event_due_to_cursor_tracking.contains(&window)) {
                auto translated_event = event.translated(-window.position());
                deliver_mouse_event(window, translated_event);
            }
            return IterationDecision::Abort;
        }

        // We are hitting the frame, pass the event along to WSWindowFrame.
        window.frame().on_mouse_event(event.translated(-window_frame_rect.location()));
        event_window_with_frame = &window;
        return IterationDecision::Abort;
    });

    if (event_window_with_frame != m_resize_candidate.ptr())
        clear_resize_candidate();
}

void WSWindowManager::clear_resize_candidate()
{
    if (m_resize_candidate)
        WSCompositor::the().invalidate_cursor();
    m_resize_candidate = nullptr;
}

bool WSWindowManager::any_opaque_window_contains_rect(const Rect& rect)
{
    bool found_containing_window = false;
    for_each_window([&] (WSWindow& window) {
        if (!window.is_visible())
            return IterationDecision::Continue;
        if (window.is_minimized())
            return IterationDecision::Continue;
        if (window.opacity() < 1.0f)
            return IterationDecision::Continue;
        if (window.has_alpha_channel()) {
            // FIXME: Just because the window has an alpha channel doesn't mean it's not opaque.
            //        Maybe there's some way we could know this?
            return IterationDecision::Continue;
        }
        if (window.frame().rect().contains(rect)) {
            found_containing_window = true;
            return IterationDecision::Abort;
        }
        return IterationDecision::Continue;
    });
    return found_containing_window;
};

bool WSWindowManager::any_opaque_window_above_this_one_contains_rect(const WSWindow& a_window, const Rect& rect)
{
    bool found_containing_window = false;
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
            found_containing_window = true;
            return IterationDecision::Abort;
        }
        return IterationDecision::Continue;
    });
    return found_containing_window;
};

Rect WSWindowManager::menubar_rect() const
{
    if (active_fullscreen_window())
        return { };
    return { 0, 0, WSScreen::the().rect().width(), 18 };
}

void WSWindowManager::draw_window_switcher()
{
    if (m_switcher.is_visible())
        m_switcher.draw();
}

void WSWindowManager::event(CEvent& event)
{
    if (static_cast<WSEvent&>(event).is_mouse_event()) {
        WSWindow* hovered_window = nullptr;
        process_mouse_event(static_cast<WSMouseEvent&>(event), hovered_window);
        set_hovered_window(hovered_window);
        return;
    }

    if (static_cast<WSEvent&>(event).is_key_event()) {
        auto& key_event = static_cast<const WSKeyEvent&>(event);
        m_keyboard_modifiers = key_event.modifiers();

        if (key_event.type() == WSEvent::KeyDown && key_event.modifiers() == Mod_Logo && key_event.key() == Key_Tab)
            m_switcher.show();
        if (m_switcher.is_visible()) {
            m_switcher.on_key_event(key_event);
            return;
        }
        if (m_active_window)
            return m_active_window->event(event);
        return;
    }

    CObject::event(event);
}

void WSWindowManager::set_highlight_window(WSWindow* window)
{
    if (window == m_highlight_window)
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

    if (window->type() != WSWindowType::Normal)
        return;

    if (window == m_active_window)
        return;

    auto* previously_active_window = m_active_window.ptr();
    if (previously_active_window) {
        WSEventLoop::the().post_event(*previously_active_window, make<WSEvent>(WSEvent::WindowDeactivated));
        invalidate(*previously_active_window);
    }
    m_active_window = window->make_weak_ptr();
    if (m_active_window) {
        WSEventLoop::the().post_event(*m_active_window, make<WSEvent>(WSEvent::WindowActivated));
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
    if (m_hovered_window == window)
        return;

    if (m_hovered_window)
        WSEventLoop::the().post_event(*m_hovered_window, make<WSEvent>(WSEvent::WindowLeft));

    m_hovered_window = window ? window->make_weak_ptr() : nullptr;

    if (m_hovered_window)
        WSEventLoop::the().post_event(*m_hovered_window, make<WSEvent>(WSEvent::WindowEntered));
}

void WSWindowManager::invalidate()
{
    WSCompositor::the().invalidate();
}

void WSWindowManager::invalidate(const Rect& rect)
{
    WSCompositor::the().invalidate(rect);
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

    if (m_resize_window || m_resize_candidate) {
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
            break;
        }
    }

    if (m_hovered_window && m_hovered_window->override_cursor())
        return *m_hovered_window->override_cursor();

    return *m_arrow_cursor;
}

void WSWindowManager::set_hovered_button(WSButton* button)
{
    m_hovered_button = button ? button->make_weak_ptr() : nullptr;
}

void WSWindowManager::set_resize_candidate(WSWindow& window, ResizeDirection direction)
{
    m_resize_candidate = window.make_weak_ptr();
    m_resize_direction = direction;
}

Rect WSWindowManager::maximized_window_rect(const WSWindow& window) const
{
    Rect rect = WSScreen::the().rect();

    // Subtract window title bar (leaving the border)
    rect.set_y(rect.y() + window.frame().title_bar_rect().height());
    rect.set_height(rect.height() - window.frame().title_bar_rect().height());

    // Subtract menu bar
    rect.set_y(rect.y() + menubar_rect().height());
    rect.set_height(rect.height() - menubar_rect().height());

    // Subtract taskbar window height if present
    const_cast<WSWindowManager*>(this)->for_each_visible_window_of_type_from_back_to_front(WSWindowType::Taskbar, [&rect] (WSWindow& taskbar_window) {
        rect.set_height(rect.height() - taskbar_window.height());
        return IterationDecision::Abort;
    });

    return rect;
}
