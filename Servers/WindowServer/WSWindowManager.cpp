#include "WSWindowManager.h"
#include "WSCompositor.h"
#include "WSEventLoop.h"
#include "WSMenu.h"
#include "WSMenuBar.h"
#include "WSMenuItem.h"
#include "WSScreen.h"
#include "WSWindow.h"
#include <AK/FileSystemPath.h>
#include <AK/LogStream.h>
#include <AK/QuickSort.h>
#include <AK/StdLibExtras.h>
#include <AK/Vector.h>
#include <LibCore/CDirIterator.h>
#include <LibCore/CTimer.h>
#include <LibDraw/CharacterBitmap.h>
#include <LibDraw/Font.h>
#include <LibDraw/PNGLoader.h>
#include <LibDraw/Painter.h>
#include <LibDraw/StylePainter.h>
#include <LibDraw/SystemTheme.h>
#include <WindowServer/WSButton.h>
#include <WindowServer/WSClientConnection.h>
#include <WindowServer/WSCursor.h>
#include <WindowServer/WindowClientEndpoint.h>
#include <errno.h>
#include <stdio.h>
#include <time.h>
#include <unistd.h>

//#define DEBUG_COUNTERS
//#define DEBUG_MENUS
//#define RESIZE_DEBUG
//#define MOVE_DEBUG
//#define DOUBLECLICK_DEBUG

static WSWindowManager* s_the;

WSWindowManager& WSWindowManager::the()
{
    ASSERT(s_the);
    return *s_the;
}

WSWindowManager::WSWindowManager(const PaletteImpl& palette)
    : m_palette(palette)
{
    s_the = this;

    reload_config(false);

    HashTable<String> seen_app_categories;
    {
        CDirIterator dt("/res/apps", CDirIterator::SkipDots);
        while (dt.has_next()) {
            auto af_name = dt.next_path();
            auto af_path = String::format("/res/apps/%s", af_name.characters());
            auto af = CConfigFile::open(af_path);
            if (!af->has_key("App", "Name") || !af->has_key("App", "Executable"))
                continue;
            auto app_name = af->read_entry("App", "Name");
            auto app_executable = af->read_entry("App", "Executable");
            auto app_category = af->read_entry("App", "Category");
            auto app_icon_path = af->read_entry("Icons", "16x16");
            m_apps.append({ app_executable, app_name, app_icon_path, app_category });
            seen_app_categories.set(app_category);
        }
    }

    Vector<String> sorted_app_categories;
    for (auto& category : seen_app_categories)
        sorted_app_categories.append(category);
    quick_sort(sorted_app_categories.begin(), sorted_app_categories.end(), [](auto& a, auto& b) { return a < b; });

    u8 system_menu_name[] = { 0xc3, 0xb8, 0 };
    m_system_menu = WSMenu::construct(nullptr, -1, String((const char*)system_menu_name));

    // First we construct all the necessary app category submenus.
    for (const auto& category : sorted_app_categories) {

        if (m_app_category_menus.contains(category))
            continue;
        auto category_menu = WSMenu::construct(nullptr, 5000 + m_app_category_menus.size(), category);
        category_menu->on_item_activation = [this](auto& item) {
            if (item.identifier() >= 1 && item.identifier() <= 1u + m_apps.size() - 1) {
                if (fork() == 0) {
                    const auto& bin = m_apps[item.identifier() - 1].executable;
                    execl(bin.characters(), bin.characters(), nullptr);
                    ASSERT_NOT_REACHED();
                }
            }
        };
        auto item = make<WSMenuItem>(*m_system_menu, -1, category);
        item->set_submenu_id(category_menu->menu_id());
        m_system_menu->add_item(move(item));
        m_app_category_menus.set(category, move(category_menu));
    }

    // Then we create and insert all the app menu items into the right place.
    int app_identifier = 1;
    for (const auto& app : m_apps) {
        auto parent_menu = m_app_category_menus.get(app.category).value_or(*m_system_menu);
        parent_menu->add_item(make<WSMenuItem>(*m_system_menu, app_identifier++, app.name, String(), true, false, false, load_png(app.icon_path)));
    }

    m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, WSMenuItem::Separator));

    m_themes_menu = WSMenu::construct(nullptr, 9000, "Themes");

    auto themes_menu_item = make<WSMenuItem>(*m_system_menu, 100, "Themes");
    themes_menu_item->set_submenu_id(m_themes_menu->menu_id());
    m_system_menu->add_item(move(themes_menu_item));

    {
        CDirIterator dt("/res/themes", CDirIterator::SkipDots);
        while (dt.has_next()) {
            auto theme_name = dt.next_path();
            auto theme_path = String::format("/res/themes/%s", theme_name.characters());
            m_themes.append({ FileSystemPath(theme_name).title(), theme_path });
        }
        quick_sort(m_themes.begin(), m_themes.end(), [](auto& a, auto& b) { return a.name < b.name; });
    }

    {
        int theme_identifier = 9000;
        for (auto& theme : m_themes) {
            m_themes_menu->add_item(make<WSMenuItem>(*m_themes_menu, theme_identifier++, theme.name));
        }
    }

    m_themes_menu->on_item_activation = [this](WSMenuItem& item) {
        auto& theme = m_themes[(int)item.identifier() - 9000];
        auto new_theme = load_system_theme(theme.path);
        ASSERT(new_theme);
        set_system_theme(*new_theme);
        m_palette = PaletteImpl::create_with_shared_buffer(*new_theme);
        HashTable<WSClientConnection*> notified_clients;
        for_each_window([&](WSWindow& window) {
            if (window.client()) {
                if (!notified_clients.contains(window.client())) {
                    window.client()->post_message(WindowClient::UpdateSystemTheme(current_system_theme_buffer_id()));
                    notified_clients.set(window.client());
                }
            }
            return IterationDecision::Continue;
        });
        ++m_theme_index;
        auto wm_config = CConfigFile::get_for_app("WindowManager");
        wm_config->write_entry("Theme", "Name", theme.name);
        wm_config->sync();
        invalidate();
    };

    m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, WSMenuItem::Separator));
    m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, 100, "Reload WM Config File"));

    m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, WSMenuItem::Separator));
    m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, 200, "About...", String(), true, false, false, load_png("/res/icons/16x16/ladybug.png")));
    m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, WSMenuItem::Separator));
    m_system_menu->add_item(make<WSMenuItem>(*m_system_menu, 300, "Shutdown..."));
    m_system_menu->on_item_activation = [this](WSMenuItem& item) {
        if (item.identifier() >= 1 && item.identifier() <= 1u + m_apps.size() - 1) {
            if (fork() == 0) {
                const auto& bin = m_apps[item.identifier() - 1].executable;
                execl(bin.characters(), bin.characters(), nullptr);
                ASSERT_NOT_REACHED();
            }
        }
        switch (item.identifier()) {
        case 100:
            reload_config(true);
            break;
        case 200:
            if (fork() == 0) {
                execl("/bin/About", "/bin/About", nullptr);
                ASSERT_NOT_REACHED();
            }
            return;
        case 300:
            if (fork() == 0) {
                execl("/bin/SystemDialog", "/bin/SystemDialog", "--shutdown", nullptr);
                ASSERT_NOT_REACHED();
            }
            return;
        }
#ifdef DEBUG_MENUS
        dbg() << "WSMenu 1 item activated: " << item.text();
#endif
    };

    // NOTE: This ensures that the system menu has the correct dimensions.
    set_current_menubar(nullptr);

    m_menu_manager.setup();

    invalidate();
    WSCompositor::the().compose();
}

WSWindowManager::~WSWindowManager()
{
}

NonnullRefPtr<WSCursor> WSWindowManager::get_cursor(const String& name, const Point& hotspot)
{
    auto path = m_wm_config->read_entry("Cursor", name, "/res/cursors/arrow.png");
    auto gb = GraphicsBitmap::load_from_file(path);
    if (gb)
        return WSCursor::create(*gb, hotspot);
    return WSCursor::create(*GraphicsBitmap::load_from_file("/res/cursors/arrow.png"));
}

NonnullRefPtr<WSCursor> WSWindowManager::get_cursor(const String& name)
{
    auto path = m_wm_config->read_entry("Cursor", name, "/res/cursors/arrow.png");
    auto gb = GraphicsBitmap::load_from_file(path);

    if (gb)
        return WSCursor::create(*gb);
    return WSCursor::create(*GraphicsBitmap::load_from_file("/res/cursors/arrow.png"));
}

void WSWindowManager::reload_config(bool set_screen)
{
    m_wm_config = CConfigFile::get_for_app("WindowManager");

    m_double_click_speed = m_wm_config->read_num_entry("Input", "DoubleClickSpeed", 250);

    if (set_screen)
        set_resolution(m_wm_config->read_num_entry("Screen", "Width", 1920),
            m_wm_config->read_num_entry("Screen", "Height", 1080));

    m_arrow_cursor = get_cursor("Arrow", { 2, 2 });
    m_hand_cursor = get_cursor("Hand", { 8, 4 });
    m_resize_horizontally_cursor = get_cursor("ResizeH");
    m_resize_vertically_cursor = get_cursor("ResizeV");
    m_resize_diagonally_tlbr_cursor = get_cursor("ResizeDTLBR");
    m_resize_diagonally_bltr_cursor = get_cursor("ResizeDBLTR");
    m_i_beam_cursor = get_cursor("IBeam");
    m_disallowed_cursor = get_cursor("Disallowed");
    m_move_cursor = get_cursor("Move");
    m_drag_cursor = get_cursor("Drag");
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

void WSWindowManager::set_resolution(int width, int height)
{
    WSCompositor::the().set_resolution(width, height);
    m_menu_manager.set_needs_window_resize();
    WSClientConnection::for_each_client([&](WSClientConnection& client) {
        client.notify_about_new_screen_rect(WSScreen::the().rect());
    });
    if (m_wm_config) {
        dbg() << "Saving resolution: " << Size(width, height) << " to config file at " << m_wm_config->file_name();
        m_wm_config->write_num_entry("Screen", "Width", width);
        m_wm_config->write_num_entry("Screen", "Height", height);
        m_wm_config->sync();
    }
}

void WSWindowManager::set_current_menubar(WSMenuBar* menubar)
{
    if (menubar)
        m_current_menubar = menubar->make_weak_ptr();
    else
        m_current_menubar = nullptr;
#ifdef DEBUG_MENUS
    dbg() << "[WM] Current menubar is now " << menubar;
#endif
    Point next_menu_location { WSMenuManager::menubar_menu_margin() / 2, 0 };
    int index = 0;
    for_each_active_menubar_menu([&](WSMenu& menu) {
        int text_width = index == 1 ? Font::default_bold_font().width(menu.name()) : font().width(menu.name());
        menu.set_rect_in_menubar({ next_menu_location.x() - WSMenuManager::menubar_menu_margin() / 2, 0, text_width + WSMenuManager::menubar_menu_margin(), menubar_rect().height() - 1 });
        menu.set_text_rect_in_menubar({ next_menu_location, { text_width, menubar_rect().height() } });
        next_menu_location.move_by(menu.rect_in_menubar().width(), 0);
        ++index;
        return IterationDecision::Continue;
    });
    m_menu_manager.refresh();
}

void WSWindowManager::add_window(WSWindow& window)
{
    m_windows_in_order.append(&window);

    if (window.is_fullscreen()) {
        CEventLoop::current().post_event(window, make<WSResizeEvent>(window.rect(), WSScreen::the().rect()));
        window.set_rect(WSScreen::the().rect());
    }

    set_active_window(&window);
    if (m_switcher.is_visible() && window.type() != WSWindowType::WindowSwitcher)
        m_switcher.refresh();

    recompute_occlusions();

    if (window.listens_to_wm_events()) {
        for_each_window([&](WSWindow& other_window) {
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

    recompute_occlusions();

    set_active_window(&window);
}

void WSWindowManager::remove_window(WSWindow& window)
{
    invalidate(window);
    m_windows_in_order.remove(&window);
    if (window.is_active())
        pick_new_active_window();
    if (m_active_window.ptr() == &window)
        set_active_window(nullptr);
    if (m_switcher.is_visible() && window.type() != WSWindowType::WindowSwitcher)
        m_switcher.refresh();

    recompute_occlusions();

    for_each_window_listening_to_wm_events([&window](WSWindow& listener) {
        if (!(listener.wm_event_mask() & WSWMEventMask::WindowRemovals))
            return IterationDecision::Continue;
        if (window.client())
            CEventLoop::current().post_event(listener, make<WSWMWindowRemovedEvent>(window.client()->client_id(), window.window_id()));
        return IterationDecision::Continue;
    });
}

void WSWindowManager::tell_wm_listener_about_window(WSWindow& listener, WSWindow& window)
{
    if (!(listener.wm_event_mask() & WSWMEventMask::WindowStateChanges))
        return;
    if (window.client())
        CEventLoop::current().post_event(listener, make<WSWMWindowStateChangedEvent>(window.client()->client_id(), window.window_id(), window.title(), window.rect(), window.is_active(), window.type(), window.is_minimized()));
}

void WSWindowManager::tell_wm_listener_about_window_rect(WSWindow& listener, WSWindow& window)
{
    if (!(listener.wm_event_mask() & WSWMEventMask::WindowRectChanges))
        return;
    if (window.client())
        CEventLoop::current().post_event(listener, make<WSWMWindowRectChangedEvent>(window.client()->client_id(), window.window_id(), window.rect()));
}

void WSWindowManager::tell_wm_listener_about_window_icon(WSWindow& listener, WSWindow& window)
{
    if (!(listener.wm_event_mask() & WSWMEventMask::WindowIconChanges))
        return;
    if (window.client() && window.icon().shared_buffer_id() != -1)
        CEventLoop::current().post_event(listener, make<WSWMWindowIconBitmapChangedEvent>(window.client()->client_id(), window.window_id(), window.icon().shared_buffer_id(), window.icon().size()));
}

void WSWindowManager::tell_wm_listeners_window_state_changed(WSWindow& window)
{
    for_each_window_listening_to_wm_events([&](WSWindow& listener) {
        tell_wm_listener_about_window(listener, window);
        return IterationDecision::Continue;
    });
}

void WSWindowManager::tell_wm_listeners_window_icon_changed(WSWindow& window)
{
    for_each_window_listening_to_wm_events([&](WSWindow& listener) {
        tell_wm_listener_about_window_icon(listener, window);
        return IterationDecision::Continue;
    });
}

void WSWindowManager::tell_wm_listeners_window_rect_changed(WSWindow& window)
{
    for_each_window_listening_to_wm_events([&](WSWindow& listener) {
        tell_wm_listener_about_window_rect(listener, window);
        return IterationDecision::Continue;
    });
}

void WSWindowManager::notify_title_changed(WSWindow& window)
{
    if (window.type() != WSWindowType::Normal)
        return;
    dbg() << "[WM] WSWindow{" << &window << "} title set to \"" << window.title() << '"';
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
    dbg() << "[WM] WSWindow " << &window << " rect changed " << old_rect << " -> " << new_rect;
#endif
    if (m_switcher.is_visible() && window.type() != WSWindowType::WindowSwitcher)
        m_switcher.refresh();

    recompute_occlusions();

    tell_wm_listeners_window_rect_changed(window);

    m_menu_manager.refresh();
}

void WSWindowManager::recompute_occlusions()
{
    for_each_visible_window_from_back_to_front([&](WSWindow& window) {
        if (m_switcher.is_visible()) {
            window.set_occluded(false);
        } else {
            if (any_opaque_window_above_this_one_contains_rect(window, window.frame().rect()))
                window.set_occluded(true);
            else
                window.set_occluded(false);
        }
        return IterationDecision::Continue;
    });
}

void WSWindowManager::notify_opacity_changed(WSWindow&)
{
    recompute_occlusions();
}

void WSWindowManager::notify_minimization_state_changed(WSWindow& window)
{
    tell_wm_listeners_window_state_changed(window);

    if (window.client())
        window.client()->post_message(WindowClient::WindowStateChanged(window.window_id(), window.is_minimized(), window.is_occluded()));

    if (window.is_active() && window.is_minimized())
        pick_new_active_window();
}

void WSWindowManager::notify_occlusion_state_changed(WSWindow& window)
{
    if (window.client())
        window.client()->post_message(WindowClient::WindowStateChanged(window.window_id(), window.is_minimized(), window.is_occluded()));
}

void WSWindowManager::pick_new_active_window()
{
    for_each_visible_window_of_type_from_front_to_back(WSWindowType::Normal, [&](WSWindow& candidate) {
        set_active_window(&candidate);
        return IterationDecision::Break;
    });
}

void WSWindowManager::start_window_move(WSWindow& window, const WSMouseEvent& event)
{
#ifdef MOVE_DEBUG
    dbg() << "[WM] Begin moving WSWindow{" << &window << "}";
#endif
    move_to_front_and_make_active(window);
    m_move_window = window.make_weak_ptr();
    m_move_origin = event.position();
    m_move_window_origin = window.position();
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
    dbg() << "[WM] Begin resizing WSWindow{" << &window << "}";
#endif
    m_resizing_mouse_button = button;
    m_resize_window = window.make_weak_ptr();
    ;
    m_resize_origin = position;
    m_resize_window_original_rect = window.rect();

    invalidate(window);
}

void WSWindowManager::start_window_resize(WSWindow& window, const WSMouseEvent& event)
{
    start_window_resize(window, event.position(), event.button());
}

bool WSWindowManager::process_ongoing_window_move(WSMouseEvent& event, WSWindow*& hovered_window)
{
    if (!m_move_window)
        return false;
    if (event.type() == WSEvent::MouseUp && event.button() == MouseButton::Left) {
#ifdef MOVE_DEBUG
        dbg() << "[WM] Finish moving WSWindow{" << m_move_window << "}";
#endif
        invalidate(*m_move_window);
        if (m_move_window->rect().contains(event.position()))
            hovered_window = m_move_window;
        if (m_move_window->is_resizable()) {
            process_event_for_doubleclick(*m_move_window, event);
            if (event.type() == WSEvent::MouseDoubleClick) {
#if defined(DOUBLECLICK_DEBUG)
                dbg() << "[WM] Click up became doubleclick!";
#endif
                m_move_window->set_maximized(!m_move_window->is_maximized());
            }
        }
        m_move_window = nullptr;
        return true;
    }
    if (event.type() == WSEvent::MouseMove) {

#ifdef MOVE_DEBUG
        dbg() << "[WM] Moving, origin: " << m_move_origin << ", now: " << event.position();
        if (m_move_window->is_maximized()) {
            dbg() << "  [!] The window is still maximized. Not moving yet.";
        }
#endif
        if (m_move_window->is_maximized()) {
            auto pixels_moved_from_start = event.position().pixels_moved(m_move_origin);
            // dbg() << "[WM] " << pixels_moved_from_start << " moved since start of window move";
            if (pixels_moved_from_start > 5) {
                // dbg() << "[WM] de-maximizing window";
                m_move_origin = event.position();
                auto width_before_resize = m_move_window->width();
                m_move_window->set_maximized(false);
                m_move_window->move_to(m_move_origin.x() - (m_move_window->width() * ((float)m_move_origin.x() / width_before_resize)), m_move_origin.y());
                m_move_window_origin = m_move_window->position();
            }
        } else {
            Point pos = m_move_window_origin.translated(event.position() - m_move_origin);
            m_move_window->set_position_without_repaint(pos);
            if (m_move_window->rect().contains(event.position()))
                hovered_window = m_move_window;
            return true;
        }
    }
    return false;
}

bool WSWindowManager::process_ongoing_window_resize(const WSMouseEvent& event, WSWindow*& hovered_window)
{
    if (!m_resize_window)
        return false;

    if (event.type() == WSEvent::MouseUp && event.button() == m_resizing_mouse_button) {
#ifdef RESIZE_DEBUG
        dbg() << "[WM] Finish resizing WSWindow{" << m_resize_window << "}";
#endif
        CEventLoop::current().post_event(*m_resize_window, make<WSResizeEvent>(m_resize_window->rect(), m_resize_window->rect()));
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
        change_h = -diff_y;
        break;
    case ResizeDirection::Up:
        change_h = -diff_y;
        break;
    case ResizeDirection::UpLeft:
        change_w = -diff_x;
        change_h = -diff_y;
        break;
    case ResizeDirection::Left:
        change_w = -diff_x;
        break;
    case ResizeDirection::DownLeft:
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

    // First, size the new rect.
    Size minimum_size { 50, 50 };

    new_rect.set_width(max(minimum_size.width(), new_rect.width() + change_w));
    new_rect.set_height(max(minimum_size.height(), new_rect.height() + change_h));

    if (!m_resize_window->size_increment().is_null()) {
        int horizontal_incs = (new_rect.width() - m_resize_window->base_size().width()) / m_resize_window->size_increment().width();
        new_rect.set_width(m_resize_window->base_size().width() + horizontal_incs * m_resize_window->size_increment().width());
        int vertical_incs = (new_rect.height() - m_resize_window->base_size().height()) / m_resize_window->size_increment().height();
        new_rect.set_height(m_resize_window->base_size().height() + vertical_incs * m_resize_window->size_increment().height());
    }

    // Second, set its position so that the sides of the window
    // that end up moving are the same ones as the user is dragging,
    // no matter which part of the logic above caused us to decide
    // to resize by this much.
    switch (m_resize_direction) {
    case ResizeDirection::DownRight:
    case ResizeDirection::Right:
    case ResizeDirection::Down:
        break;
    case ResizeDirection::Left:
    case ResizeDirection::Up:
    case ResizeDirection::UpLeft:
        new_rect.set_right_without_resize(m_resize_window_original_rect.right());
        new_rect.set_bottom_without_resize(m_resize_window_original_rect.bottom());
        break;
    case ResizeDirection::UpRight:
        new_rect.set_bottom_without_resize(m_resize_window_original_rect.bottom());
        break;
    case ResizeDirection::DownLeft:
        new_rect.set_right_without_resize(m_resize_window_original_rect.right());
        break;
    default:
        ASSERT_NOT_REACHED();
    }

    if (new_rect.contains(event.position()))
        hovered_window = m_resize_window;

    if (m_resize_window->rect() == new_rect)
        return true;
#ifdef RESIZE_DEBUG
    dbg() << "[WM] Resizing, original: " << m_resize_window_original_rect << ", now: " << new_rect;
#endif
    m_resize_window->set_rect(new_rect);
    CEventLoop::current().post_event(*m_resize_window, make<WSResizeEvent>(old_rect, new_rect));
    return true;
}

bool WSWindowManager::process_ongoing_drag(WSMouseEvent& event, WSWindow*& hovered_window)
{
    if (!m_dnd_client)
        return false;
    if (!(event.type() == WSEvent::MouseUp && event.button() == MouseButton::Left))
        return true;

    hovered_window = nullptr;
    for_each_visible_window_from_front_to_back([&](auto& window) {
        if (window.frame().rect().contains(event.position())) {
            hovered_window = &window;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    if (hovered_window) {
        m_dnd_client->post_message(WindowClient::DragAccepted());
        if (hovered_window->client()) {
            auto translated_event = event.translated(-hovered_window->position());
            hovered_window->client()->post_message(WindowClient::DragDropped(hovered_window->window_id(), translated_event.position(), m_dnd_text, m_dnd_data_type, m_dnd_data));
        }
    } else {
        m_dnd_client->post_message(WindowClient::DragCancelled());
    }

    end_dnd_drag();
    return true;
}

void WSWindowManager::set_cursor_tracking_button(WSButton* button)
{
    m_cursor_tracking_button = button ? button->make_weak_ptr() : nullptr;
}

auto WSWindowManager::DoubleClickInfo::metadata_for_button(MouseButton button) -> ClickMetadata&
{
    switch (button) {
    case MouseButton::Left:
        return m_left;
    case MouseButton::Right:
        return m_right;
    case MouseButton::Middle:
        return m_middle;
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
        dbg() << "Initial mouseup on window " << &window << " (previous was " << m_double_click_info.m_clicked_window << ')';
#endif
        m_double_click_info.m_clicked_window = window.make_weak_ptr();
        m_double_click_info.reset();
    }

    auto& metadata = m_double_click_info.metadata_for_button(event.button());

    // if the clock is invalid, we haven't clicked with this button on this
    // window yet, so there's nothing to do.
    if (!metadata.clock.is_valid()) {
        metadata.clock.start();
    } else {
        int elapsed_since_last_click = metadata.clock.elapsed();
        metadata.clock.start();
        if (elapsed_since_last_click < m_double_click_speed) {
            auto diff = event.position() - metadata.last_position;
            auto distance_travelled_squared = diff.x() * diff.x() + diff.y() * diff.y();
            if (distance_travelled_squared > (m_max_distance_for_double_click * m_max_distance_for_double_click)) {
                // too far; try again
                metadata.clock.start();
            } else {
#if defined(DOUBLECLICK_DEBUG)
                dbg() << "Transforming MouseUp to MouseDoubleClick (" << elapsed_since_last_click << " < " << m_double_click_speed << ")!";
#endif
                event = WSMouseEvent(WSEvent::MouseDoubleClick, event.position(), event.buttons(), event.button(), event.modifiers(), event.wheel_delta());
                // invalidate this now we've delivered a doubleclick, otherwise
                // tripleclick will deliver two doubleclick events (incorrectly).
                metadata.clock = {};
            }
        } else {
            // too slow; try again
            metadata.clock.start();
        }
    }

    metadata.last_position = event.position();
}

void WSWindowManager::deliver_mouse_event(WSWindow& window, WSMouseEvent& event)
{
    window.dispatch_event(event);
    if (event.type() == WSEvent::MouseUp) {
        process_event_for_doubleclick(window, event);
        if (event.type() == WSEvent::MouseDoubleClick)
            window.dispatch_event(event);
    }
}

void WSWindowManager::process_mouse_event(WSMouseEvent& event, WSWindow*& hovered_window)
{
    hovered_window = nullptr;

    if (process_ongoing_drag(event, hovered_window))
        return;

    if (process_ongoing_window_move(event, hovered_window))
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
        ASSERT(window->is_visible());    // Maybe this should be supported? Idk. Let's catch it and think about it later.
        ASSERT(!window->is_minimized()); // Maybe this should also be supported? Idk.
        windows_who_received_mouse_event_due_to_cursor_tracking.set(window);
        auto translated_event = event.translated(-window->position());
        deliver_mouse_event(*window, translated_event);
    }

    // FIXME: Now that the menubar has a dedicated window, is this special-casing really necessary?
    if (!active_window_is_modal() && menubar_rect().contains(event.position())) {
        m_menu_manager.dispatch_event(event);
        return;
    }

    if (!menu_manager().open_menu_stack().is_empty()) {
        auto* topmost_menu = menu_manager().open_menu_stack().last().ptr();
        ASSERT(topmost_menu);
        auto* window = topmost_menu->menu_window();
        ASSERT(window);
        bool event_is_inside_current_menu = window->rect().contains(event.position());
        if (!event_is_inside_current_menu) {
            if (topmost_menu->hovered_item())
                topmost_menu->clear_hovered_item();
            if (event.type() == WSEvent::MouseDown || event.type() == WSEvent::MouseUp)
                m_menu_manager.close_bar();
            if (event.type() == WSEvent::MouseMove) {
                for (auto& menu : m_menu_manager.open_menu_stack()) {
                    if (!menu)
                        continue;
                    if (!menu->menu_window()->rect().contains(event.position()))
                        continue;
                    hovered_window = menu->menu_window();
                    auto translated_event = event.translated(-menu->menu_window()->position());
                    deliver_mouse_event(*menu->menu_window(), translated_event);
                    break;
                }
            }
        } else {
            hovered_window = window;
            auto translated_event = event.translated(-window->position());
            deliver_mouse_event(*window, translated_event);
        }
        return;
    }

    WSWindow* event_window_with_frame = nullptr;

    if (m_active_input_window) {
        // At this point, we have delivered the start of an input sequence to a
        // client application. We must keep delivering to that client
        // application until the input sequence is done.
        //
        // This prevents e.g. moving on one window out of the bounds starting
        // a move in that other unrelated window, and other silly shenanigans.
        if (!windows_who_received_mouse_event_due_to_cursor_tracking.contains(m_active_input_window)) {
            auto translated_event = event.translated(-m_active_input_window->position());
            deliver_mouse_event(*m_active_input_window, translated_event);
            windows_who_received_mouse_event_due_to_cursor_tracking.set(m_active_input_window.ptr());
        }
        if (event.type() == WSEvent::MouseUp && event.buttons() == 0) {
            m_active_input_window = nullptr;
        }

        for_each_visible_window_from_front_to_back([&](auto& window) {
            if (window.frame().rect().contains(event.position())) {
                hovered_window = &window;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
    } else {
        for_each_visible_window_from_front_to_back([&](WSWindow& window) {
            auto window_frame_rect = window.frame().rect();
            if (!window_frame_rect.contains(event.position()))
                return IterationDecision::Continue;

            if (&window != m_resize_candidate.ptr())
                clear_resize_candidate();

            // First check if we should initiate a move or resize (Logo+LMB or Logo+RMB).
            // In those cases, the event is swallowed by the window manager.
            if (window.is_movable()) {
                if (!window.is_fullscreen() && m_keyboard_modifiers == Mod_Logo && event.type() == WSEvent::MouseDown && event.button() == MouseButton::Left) {
                    hovered_window = &window;
                    start_window_move(window, event);
                    return IterationDecision::Break;
                }
                if (window.is_resizable() && m_keyboard_modifiers == Mod_Logo && event.type() == WSEvent::MouseDown && event.button() == MouseButton::Right && !window.is_blocked_by_modal_window()) {
                    hovered_window = &window;
                    start_window_resize(window, event);
                    return IterationDecision::Break;
                }
            }

            if (m_keyboard_modifiers == Mod_Logo && event.type() == WSEvent::MouseWheel) {
                float opacity_change = -event.wheel_delta() * 0.05f;
                float new_opacity = window.opacity() + opacity_change;
                if (new_opacity < 0.05f)
                    new_opacity = 0.05f;
                if (new_opacity > 1.0f)
                    new_opacity = 1.0f;
                window.set_opacity(new_opacity);
                window.invalidate();
                return IterationDecision::Break;
            }

            // Well okay, let's see if we're hitting the frame or the window inside the frame.
            if (window.rect().contains(event.position())) {
                if (window.type() == WSWindowType::Normal && event.type() == WSEvent::MouseDown)
                    move_to_front_and_make_active(window);

                hovered_window = &window;
                if (!window.global_cursor_tracking() && !windows_who_received_mouse_event_due_to_cursor_tracking.contains(&window)) {
                    auto translated_event = event.translated(-window.position());
                    deliver_mouse_event(window, translated_event);
                    if (event.type() == WSEvent::MouseDown) {
                        m_active_input_window = window.make_weak_ptr();
                    }
                }
                return IterationDecision::Break;
            }

            // We are hitting the frame, pass the event along to WSWindowFrame.
            window.frame().on_mouse_event(event.translated(-window_frame_rect.location()));
            event_window_with_frame = &window;
            return IterationDecision::Break;
        });

        // Clicked outside of any window
        if (!hovered_window && !event_window_with_frame && event.type() == WSEvent::MouseDown)
            set_active_window(nullptr);

    }

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
    for_each_window([&](WSWindow& window) {
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
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return found_containing_window;
};

bool WSWindowManager::any_opaque_window_above_this_one_contains_rect(const WSWindow& a_window, const Rect& rect)
{
    bool found_containing_window = false;
    bool checking = false;
    for_each_visible_window_from_back_to_front([&](WSWindow& window) {
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
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return found_containing_window;
};

Rect WSWindowManager::menubar_rect() const
{
    if (active_fullscreen_window())
        return {};
    return m_menu_manager.menubar_rect();
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

        if (key_event.type() == WSEvent::KeyDown && key_event.key() == Key_Escape && m_dnd_client) {
            m_dnd_client->post_message(WindowClient::DragCancelled());
            end_dnd_drag();
            return;
        }

        if (key_event.type() == WSEvent::KeyDown && key_event.modifiers() == Mod_Logo && key_event.key() == Key_Tab)
            m_switcher.show();
        if (m_switcher.is_visible()) {
            m_switcher.on_key_event(key_event);
            return;
        }
        if (m_active_window)
            return m_active_window->dispatch_event(event);
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

    if (window && window->type() != WSWindowType::Normal)
        return;

    if (window == m_active_window)
        return;

    auto* previously_active_window = m_active_window.ptr();

    WSClientConnection* previously_active_client = nullptr;
    WSClientConnection* active_client = nullptr;

    if (previously_active_window) {
        previously_active_client = previously_active_window->client();
        CEventLoop::current().post_event(*previously_active_window, make<WSEvent>(WSEvent::WindowDeactivated));
        invalidate(*previously_active_window);
        m_active_window = nullptr;
        tell_wm_listeners_window_state_changed(*previously_active_window);
    }

    if (window) {
        m_active_window = window->make_weak_ptr();
        active_client = m_active_window->client();
        CEventLoop::current().post_event(*m_active_window, make<WSEvent>(WSEvent::WindowActivated));
        invalidate(*m_active_window);

        auto* client = window->client();
        ASSERT(client);
        set_current_menubar(client->app_menubar());
        tell_wm_listeners_window_state_changed(*m_active_window);
    } else {
        set_current_menubar(nullptr);
    }

    if (active_client != previously_active_client) {
        if (previously_active_client)
            previously_active_client->deboost();
        if (active_client)
            active_client->boost();
    }
}

void WSWindowManager::set_hovered_window(WSWindow* window)
{
    if (m_hovered_window == window)
        return;

    if (m_hovered_window)
        CEventLoop::current().post_event(*m_hovered_window, make<WSEvent>(WSEvent::WindowLeft));

    m_hovered_window = window ? window->make_weak_ptr() : nullptr;

    if (m_hovered_window)
        CEventLoop::current().post_event(*m_hovered_window, make<WSEvent>(WSEvent::WindowEntered));
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
    if (window.type() == WSWindowType::MenuApplet) {
        menu_manager().invalidate_applet(window, rect);
        return;
    }

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
    m_menu_manager.refresh();
}

const WSCursor& WSWindowManager::active_cursor() const
{
    if (m_dnd_client)
        return *m_drag_cursor;

    if (m_move_window)
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

ResizeDirection WSWindowManager::resize_direction_of_window(const WSWindow& window)
{
    if (&window != m_resize_window)
        return ResizeDirection::None;
    return m_resize_direction;
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
    const_cast<WSWindowManager*>(this)->for_each_visible_window_of_type_from_back_to_front(WSWindowType::Taskbar, [&rect](WSWindow& taskbar_window) {
        rect.set_height(rect.height() - taskbar_window.height());
        return IterationDecision::Break;
    });

    return rect;
}

WSMenu* WSWindowManager::find_internal_menu_by_id(int menu_id)
{
    if (m_themes_menu->menu_id() == menu_id)
        return m_themes_menu.ptr();
    for (auto& it : m_app_category_menus) {
        if (menu_id == it.value->menu_id())
            return it.value;
    }
    return nullptr;
}

void WSWindowManager::start_dnd_drag(WSClientConnection& client, const String& text, GraphicsBitmap* bitmap, const String& data_type, const String& data)
{
    ASSERT(!m_dnd_client);
    m_dnd_client = client.make_weak_ptr();
    m_dnd_text = text;
    m_dnd_bitmap = bitmap;
    m_dnd_data_type = data_type;
    m_dnd_data = data;
    WSCompositor::the().invalidate_cursor();
    m_active_input_window = nullptr;
}

void WSWindowManager::end_dnd_drag()
{
    ASSERT(m_dnd_client);
    WSCompositor::the().invalidate_cursor();
    m_dnd_client = nullptr;
    m_dnd_text = {};
    m_dnd_bitmap = nullptr;
}

Rect WSWindowManager::dnd_rect() const
{
    int bitmap_width = m_dnd_bitmap ? m_dnd_bitmap->width() : 0;
    int bitmap_height = m_dnd_bitmap ? m_dnd_bitmap->height() : 0;
    int width = font().width(m_dnd_text) + bitmap_width;
    int height = max((int)font().glyph_height(), bitmap_height);
    auto location = WSCompositor::the().current_cursor_rect().center().translated(8, 8);
    return Rect(location, { width, height }).inflated(4, 4);
}
