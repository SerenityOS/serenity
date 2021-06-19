/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WindowManager.h"
#include "Compositor.h"
#include "EventLoop.h"
#include "Menu.h"
#include "Screen.h"
#include "Window.h"
#include <AK/Debug.h>
#include <AK/StdLibExtras.h>
#include <AK/Vector.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/StylePainter.h>
#include <LibGfx/SystemTheme.h>
#include <WindowServer/AppletManager.h>
#include <WindowServer/Button.h>
#include <WindowServer/ClientConnection.h>
#include <WindowServer/Cursor.h>
#include <WindowServer/WindowClientEndpoint.h>

namespace WindowServer {

static WindowManager* s_the;

WindowManager& WindowManager::the()
{
    VERIFY(s_the);
    return *s_the;
}

WindowManager::WindowManager(Gfx::PaletteImpl const& palette)
    : m_palette(palette)
{
    s_the = this;

    reload_config();

    Compositor::the().did_construct_window_manager({});
}

WindowManager::~WindowManager()
{
}

RefPtr<Cursor> WindowManager::get_cursor(String const& name)
{
    static auto const s_default_cursor_path = "/res/cursors/arrow.x2y2.png";
    return Cursor::create(m_config->read_entry("Cursor", name, s_default_cursor_path), s_default_cursor_path);
}

void WindowManager::reload_config()
{
    m_config = Core::ConfigFile::open("/etc/WindowServer.ini");

    m_double_click_speed = m_config->read_num_entry("Input", "DoubleClickSpeed", 250);

    auto* current_cursor = Compositor::the().current_cursor();
    auto reload_cursor = [&](RefPtr<Cursor>& cursor, const String& name) {
        bool is_current_cursor = current_cursor && current_cursor == cursor.ptr();
        cursor = get_cursor(name);
        if (is_current_cursor)
            Compositor::the().current_cursor_was_reloaded(cursor.ptr());
    };

    reload_cursor(m_hidden_cursor, "Hidden");
    reload_cursor(m_arrow_cursor, "Arrow");
    reload_cursor(m_hand_cursor, "Hand");
    reload_cursor(m_help_cursor, "Help");
    reload_cursor(m_resize_horizontally_cursor, "ResizeH");
    reload_cursor(m_resize_vertically_cursor, "ResizeV");
    reload_cursor(m_resize_diagonally_tlbr_cursor, "ResizeDTLBR");
    reload_cursor(m_resize_diagonally_bltr_cursor, "ResizeDBLTR");
    reload_cursor(m_resize_column_cursor, "ResizeColumn");
    reload_cursor(m_resize_row_cursor, "ResizeRow");
    reload_cursor(m_i_beam_cursor, "IBeam");
    reload_cursor(m_disallowed_cursor, "Disallowed");
    reload_cursor(m_move_cursor, "Move");
    reload_cursor(m_drag_cursor, "Drag");
    reload_cursor(m_wait_cursor, "Wait");
    reload_cursor(m_crosshair_cursor, "Crosshair");

    WindowFrame::reload_config();
}

Gfx::Font const& WindowManager::font() const
{
    return Gfx::FontDatabase::default_font();
}

Gfx::Font const& WindowManager::window_title_font() const
{
    return Gfx::FontDatabase::default_font().bold_variant();
}

bool WindowManager::set_screen_layout(ScreenLayout&& screen_layout, bool save, String& error_msg)
{
    if (!Screen::apply_layout(move(screen_layout), error_msg))
        return false;

    reload_icon_bitmaps_after_scale_change();

    Compositor::the().screen_resolution_changed();

    ClientConnection::for_each_client([&](ClientConnection& client) {
        client.notify_about_new_screen_rects(Screen::rects(), Screen::main().index());
    });

    m_window_stack.for_each_window([](Window& window) {
        window.screens().clear_with_capacity();
        window.recalculate_rect();
        return IterationDecision::Continue;
    });

    if (save)
        Screen::layout().save_config(*m_config);
    return true;
}

ScreenLayout WindowManager::get_screen_layout() const
{
    return Screen::layout();
}

bool WindowManager::save_screen_layout(String& error_msg)
{
    if (!Screen::layout().save_config(*m_config)) {
        error_msg = "Could not save";
        return false;
    }
    return true;
}

void WindowManager::set_acceleration_factor(double factor)
{
    ScreenInput::the().set_acceleration_factor(factor);
    dbgln("Saving acceleration factor {} to config file at {}", factor, m_config->filename());
    m_config->write_entry("Mouse", "AccelerationFactor", String::formatted("{}", factor));
    m_config->sync();
}

void WindowManager::set_scroll_step_size(unsigned step_size)
{
    ScreenInput::the().set_scroll_step_size(step_size);
    dbgln("Saving scroll step size {} to config file at {}", step_size, m_config->filename());
    m_config->write_entry("Mouse", "ScrollStepSize", String::number(step_size));
    m_config->sync();
}

void WindowManager::set_double_click_speed(int speed)
{
    VERIFY(speed >= double_click_speed_min && speed <= double_click_speed_max);
    m_double_click_speed = speed;
    dbgln("Saving double-click speed {} to config file at {}", speed, m_config->filename());
    m_config->write_entry("Input", "DoubleClickSpeed", String::number(speed));
    m_config->sync();
}

int WindowManager::double_click_speed() const
{
    return m_double_click_speed;
}

void WindowManager::add_window(Window& window)
{
    bool is_first_window = m_window_stack.is_empty();

    m_window_stack.add(window);

    if (window.is_fullscreen()) {
        auto& screen = Screen::main(); // TODO: support fullscreen windows on other screens!
        Core::EventLoop::current().post_event(window, make<ResizeEvent>(screen.rect()));
        window.set_rect(screen.rect());
    }

    if (window.type() != WindowType::Desktop || is_first_window)
        set_active_window(&window);

    if (m_switcher.is_visible() && window.type() != WindowType::WindowSwitcher)
        m_switcher.refresh();

    Compositor::the().invalidate_occlusions();

    window.invalidate(true, true);

    tell_wms_window_state_changed(window);
}

void WindowManager::move_to_front_and_make_active(Window& window)
{
    auto move_window_to_front = [&](Window& wnd, bool make_active, bool make_input) {
        if (wnd.is_accessory()) {
            auto* parent = wnd.parent_window();
            do_move_to_front(*parent, true, false);
            make_active = false;

            for (auto& accessory_window : parent->accessory_windows()) {
                if (accessory_window && accessory_window.ptr() != &wnd)
                    do_move_to_front(*accessory_window, false, false);
            }
        }

        do_move_to_front(wnd, make_active, make_input);
    };

    // If a window that is currently blocked by a modal child is being
    // brought to the front, bring the entire stack of modal windows
    // to the front and activate the modal window. Also set the
    // active input window to that same window (which would pull
    // active input from any accessory window)
    for_each_window_in_modal_stack(window, [&](auto& w, bool is_stack_top) {
        move_window_to_front(w, is_stack_top, is_stack_top);
        return IterationDecision::Continue;
    });

    Compositor::the().invalidate_occlusions();
}

void WindowManager::do_move_to_front(Window& window, bool make_active, bool make_input)
{
    m_window_stack.move_to_front(window);

    if (make_active)
        set_active_window(&window, make_input);

    if (m_switcher.is_visible()) {
        m_switcher.refresh();
        if (!window.is_accessory()) {
            m_switcher.select_window(window);
            set_highlight_window(&window);
        }
    }

    for (auto& child_window : window.child_windows()) {
        if (child_window)
            do_move_to_front(*child_window, make_active, make_input);
    }
}

void WindowManager::remove_window(Window& window)
{
    m_window_stack.remove(window);
    auto* active = active_window();
    auto* active_input = active_input_window();
    if (active == &window || active_input == &window || (active && window.is_descendant_of(*active)) || (active_input && active_input != active && window.is_descendant_of(*active_input)))
        pick_new_active_window(&window);

    Compositor::the().invalidate_screen(window.frame().render_rect());

    if (m_switcher.is_visible() && window.type() != WindowType::WindowSwitcher)
        m_switcher.refresh();

    Compositor::the().invalidate_occlusions();

    for_each_window_manager([&window](WMClientConnection& conn) {
        if (conn.window_id() < 0 || !(conn.event_mask() & WMEventMask::WindowRemovals))
            return IterationDecision::Continue;
        if (!window.is_internal() && !window.is_modal())
            conn.async_window_removed(conn.window_id(), window.client_id(), window.window_id());
        return IterationDecision::Continue;
    });
}

void WindowManager::greet_window_manager(WMClientConnection& conn)
{
    if (conn.window_id() < 0)
        return;

    m_window_stack.for_each_window([&](Window& other_window) {
        //if (conn.window_id() != other_window.window_id()) {
        tell_wm_about_window(conn, other_window);
        tell_wm_about_window_icon(conn, other_window);
        //}
        return IterationDecision::Continue;
    });
    if (auto* applet_area_window = AppletManager::the().window())
        tell_wms_applet_area_size_changed(applet_area_window->size());
}

void WindowManager::tell_wm_about_window(WMClientConnection& conn, Window& window)
{
    if (conn.window_id() < 0)
        return;
    if (!(conn.event_mask() & WMEventMask::WindowStateChanges))
        return;
    if (window.is_internal())
        return;
    auto* parent = window.parent_window();
    conn.async_window_state_changed(conn.window_id(), window.client_id(), window.window_id(), parent ? parent->client_id() : -1, parent ? parent->window_id() : -1, window.is_active(), window.is_minimized(), window.is_modal_dont_unparent(), window.is_frameless(), (i32)window.type(), window.computed_title(), window.rect(), window.progress());
}

void WindowManager::tell_wm_about_window_rect(WMClientConnection& conn, Window& window)
{
    if (conn.window_id() < 0)
        return;
    if (!(conn.event_mask() & WMEventMask::WindowRectChanges))
        return;
    if (window.is_internal())
        return;
    conn.async_window_rect_changed(conn.window_id(), window.client_id(), window.window_id(), window.rect());
}

void WindowManager::tell_wm_about_window_icon(WMClientConnection& conn, Window& window)
{
    if (conn.window_id() < 0)
        return;
    if (!(conn.event_mask() & WMEventMask::WindowIconChanges))
        return;
    if (window.is_internal())
        return;
    conn.async_window_icon_bitmap_changed(conn.window_id(), window.client_id(), window.window_id(), window.icon().to_shareable_bitmap());
}

void WindowManager::tell_wms_window_state_changed(Window& window)
{
    for_each_window_manager([&](WMClientConnection& conn) {
        tell_wm_about_window(conn, window);
        return IterationDecision::Continue;
    });
}

void WindowManager::tell_wms_window_icon_changed(Window& window)
{
    for_each_window_manager([&](WMClientConnection& conn) {
        tell_wm_about_window_icon(conn, window);
        return IterationDecision::Continue;
    });
}

void WindowManager::tell_wms_window_rect_changed(Window& window)
{
    for_each_window_manager([&](WMClientConnection& conn) {
        tell_wm_about_window_rect(conn, window);
        return IterationDecision::Continue;
    });
}

void WindowManager::tell_wms_applet_area_size_changed(Gfx::IntSize const& size)
{
    for_each_window_manager([&](WMClientConnection& conn) {
        if (conn.window_id() < 0)
            return IterationDecision::Continue;

        conn.async_applet_area_size_changed(conn.window_id(), size);
        return IterationDecision::Continue;
    });
}

void WindowManager::tell_wms_super_key_pressed()
{
    for_each_window_manager([](WMClientConnection& conn) {
        if (conn.window_id() < 0)
            return IterationDecision::Continue;

        conn.async_super_key_pressed(conn.window_id());
        return IterationDecision::Continue;
    });
}

static bool window_type_has_title(WindowType type)
{
    return type == WindowType::Normal || type == WindowType::ToolWindow;
}

void WindowManager::notify_modified_changed(Window& window)
{
    if (m_switcher.is_visible())
        m_switcher.refresh();

    tell_wms_window_state_changed(window);
}

void WindowManager::notify_title_changed(Window& window)
{
    if (!window_type_has_title(window.type()))
        return;

    dbgln_if(WINDOWMANAGER_DEBUG, "[WM] Window({}) title set to '{}'", &window, window.title());

    if (m_switcher.is_visible())
        m_switcher.refresh();

    tell_wms_window_state_changed(window);
}

void WindowManager::notify_modal_unparented(Window& window)
{
    if (window.type() != WindowType::Normal)
        return;

    dbgln_if(WINDOWMANAGER_DEBUG, "[WM] Window({}) was unparented", &window);

    if (m_switcher.is_visible())
        m_switcher.refresh();

    tell_wms_window_state_changed(window);
}

void WindowManager::notify_rect_changed(Window& window, Gfx::IntRect const& old_rect, Gfx::IntRect const& new_rect)
{
    dbgln_if(RESIZE_DEBUG, "[WM] Window({}) rect changed {} -> {}", &window, old_rect, new_rect);

    if (m_switcher.is_visible() && window.type() != WindowType::WindowSwitcher)
        m_switcher.refresh();

    tell_wms_window_rect_changed(window);

    if (window.type() == WindowType::Applet)
        AppletManager::the().relayout();

    MenuManager::the().refresh();
    reevaluate_hovered_window(&window);
}

void WindowManager::notify_opacity_changed(Window&)
{
    Compositor::the().invalidate_occlusions();
}

void WindowManager::notify_minimization_state_changed(Window& window)
{
    tell_wms_window_state_changed(window);

    if (window.client())
        window.client()->async_window_state_changed(window.window_id(), window.is_minimized(), window.is_occluded());

    if (window.is_active() && window.is_minimized())
        pick_new_active_window(&window);
}

void WindowManager::notify_occlusion_state_changed(Window& window)
{
    if (window.client())
        window.client()->async_window_state_changed(window.window_id(), window.is_minimized(), window.is_occluded());
}

void WindowManager::notify_progress_changed(Window& window)
{
    tell_wms_window_state_changed(window);
}

bool WindowManager::pick_new_active_window(Window* previous_active)
{
    bool new_window_picked = false;
    Window* first_candidate = nullptr;
    m_window_stack.for_each_visible_window_from_front_to_back([&](Window& candidate) {
        if (candidate.type() != WindowType::Normal && candidate.type() != WindowType::ToolWindow)
            return IterationDecision::Continue;
        if (candidate.is_destroyed())
            return IterationDecision::Continue;
        if (previous_active != first_candidate)
            first_candidate = &candidate;
        if ((!previous_active && !candidate.is_accessory()) || (previous_active && !candidate.is_accessory_of(*previous_active))) {
            set_active_window(&candidate);
            new_window_picked = true;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    if (!new_window_picked) {
        set_active_window(first_candidate);
        new_window_picked = first_candidate != nullptr;
    }
    return new_window_picked;
}

void WindowManager::start_window_move(Window& window, Gfx::IntPoint const& origin)
{
    MenuManager::the().close_everyone();

    dbgln_if(MOVE_DEBUG, "[WM] Begin moving Window({})", &window);

    move_to_front_and_make_active(window);
    m_move_window = window;
    m_move_window->set_default_positioned(false);
    m_move_origin = origin;
    m_move_window_origin = window.position();
    window.invalidate(true, true);
}

void WindowManager::start_window_move(Window& window, MouseEvent const& event)
{
    start_window_move(window, event.position());
}

void WindowManager::start_window_resize(Window& window, Gfx::IntPoint const& position, MouseButton button)
{
    MenuManager::the().close_everyone();

    move_to_front_and_make_active(window);
    constexpr ResizeDirection direction_for_hot_area[3][3] = {
        { ResizeDirection::UpLeft, ResizeDirection::Up, ResizeDirection::UpRight },
        { ResizeDirection::Left, ResizeDirection::None, ResizeDirection::Right },
        { ResizeDirection::DownLeft, ResizeDirection::Down, ResizeDirection::DownRight },
    };
    Gfx::IntRect outer_rect = window.frame().rect();
    if (!outer_rect.contains(position)) {
        // FIXME: This used to be an VERIFY but crashing WindowServer over this seems silly.
        dbgln("FIXME: !outer_rect.contains(position): outer_rect={}, position={}", outer_rect, position);
    }
    int window_relative_x = position.x() - outer_rect.x();
    int window_relative_y = position.y() - outer_rect.y();
    int hot_area_row = min(2, window_relative_y / (outer_rect.height() / 3));
    int hot_area_column = min(2, window_relative_x / (outer_rect.width() / 3));
    m_resize_direction = direction_for_hot_area[hot_area_row][hot_area_column];
    if (m_resize_direction == ResizeDirection::None) {
        VERIFY(!m_resize_window);
        return;
    }

    dbgln_if(RESIZE_DEBUG, "[WM] Begin resizing Window({})", &window);

    m_resizing_mouse_button = button;
    m_resize_window = window;
    m_resize_origin = position;
    m_resize_window_original_rect = window.rect();

    m_active_input_tracking_window = nullptr;

    window.invalidate(true, true);

    if (hot_area_row == 0 || hot_area_column == 0) {
        m_resize_window->set_default_positioned(false);
    }
}

void WindowManager::start_window_resize(Window& window, MouseEvent const& event)
{
    start_window_resize(window, event.position(), event.button());
}

bool WindowManager::process_ongoing_window_move(MouseEvent& event)
{
    if (!m_move_window)
        return false;
    if (event.type() == Event::MouseUp && event.button() == MouseButton::Left) {

        dbgln_if(MOVE_DEBUG, "[WM] Finish moving Window({})", m_move_window);

        m_move_window->invalidate(true, true);
        if (m_move_window->is_resizable()) {
            process_event_for_doubleclick(*m_move_window, event);
            if (event.type() == Event::MouseDoubleClick) {
                dbgln_if(DOUBLECLICK_DEBUG, "[WM] Click up became doubleclick!");
                m_move_window->set_maximized(!m_move_window->is_maximized());
            }
        }
        m_move_window = nullptr;
        return true;
    }
    if (event.type() == Event::MouseMove) {
        if constexpr (MOVE_DEBUG) {
            dbgln("[WM] Moving, origin: {}, now: {}", m_move_origin, event.position());
            if (m_move_window->is_maximized())
                dbgln("  [!] The window is still maximized. Not moving yet.");
        }

        const int tiling_deadzone = 10;
        const int secondary_deadzone = 2;
        auto& cursor_screen = Screen::closest_to_location(event.position());
        auto desktop = desktop_rect(cursor_screen);
        auto desktop_relative_to_screen = desktop.translated(-cursor_screen.rect().location());
        if (m_move_window->is_maximized()) {
            auto pixels_moved_from_start = event.position().pixels_moved(m_move_origin);
            if (pixels_moved_from_start > 5) {
                // dbgln("[WM] de-maximizing window");
                m_move_origin = event.position();
                if (m_move_origin.y() <= secondary_deadzone + desktop.top())
                    return true;
                m_move_window->set_maximized(false, event.position());
                m_move_window_origin = m_move_window->position();
            }
        } else {
            bool is_resizable = m_move_window->is_resizable();
            auto pixels_moved_from_start = event.position().pixels_moved(m_move_origin);

            auto event_location_relative_to_screen = event.position().translated(-cursor_screen.rect().location());
            if (is_resizable && event_location_relative_to_screen.x() <= tiling_deadzone) {
                if (event_location_relative_to_screen.y() <= tiling_deadzone + desktop_relative_to_screen.top())
                    m_move_window->set_tiled(&cursor_screen, WindowTileType::TopLeft);
                else if (event_location_relative_to_screen.y() >= desktop_relative_to_screen.height() - tiling_deadzone)
                    m_move_window->set_tiled(&cursor_screen, WindowTileType::BottomLeft);
                else
                    m_move_window->set_tiled(&cursor_screen, WindowTileType::Left);
            } else if (is_resizable && event_location_relative_to_screen.x() >= cursor_screen.width() - tiling_deadzone) {
                if (event_location_relative_to_screen.y() <= tiling_deadzone + desktop.top())
                    m_move_window->set_tiled(&cursor_screen, WindowTileType::TopRight);
                else if (event_location_relative_to_screen.y() >= desktop_relative_to_screen.height() - tiling_deadzone)
                    m_move_window->set_tiled(&cursor_screen, WindowTileType::BottomRight);
                else
                    m_move_window->set_tiled(&cursor_screen, WindowTileType::Right);
            } else if (is_resizable && event_location_relative_to_screen.y() <= secondary_deadzone + desktop_relative_to_screen.top()) {
                m_move_window->set_tiled(&cursor_screen, WindowTileType::Top);
            } else if (is_resizable && event_location_relative_to_screen.y() >= desktop_relative_to_screen.bottom() - secondary_deadzone) {
                m_move_window->set_tiled(&cursor_screen, WindowTileType::Bottom);
            } else if (m_move_window->tiled() == WindowTileType::None) {
                Gfx::IntPoint pos = m_move_window_origin.translated(event.position() - m_move_origin);
                m_move_window->set_position_without_repaint(pos);
                // "Bounce back" the window if it would end up too far outside the screen.
                // If the user has let go of Mod_Super, maybe they didn't intentionally press it to begin with. Therefore, refuse to go into a state where knowledge about super-drags is necessary.
                bool force_titlebar_visible = !(m_keyboard_modifiers & Mod_Super);
                m_move_window->nudge_into_desktop(&cursor_screen, force_titlebar_visible);
            } else if (pixels_moved_from_start > 5) {
                m_move_window->set_untiled(event.position());
                m_move_origin = event.position();
                m_move_window_origin = m_move_window->position();
            }
        }
    }
    return true;
}

bool WindowManager::process_ongoing_window_resize(MouseEvent const& event)
{
    if (!m_resize_window)
        return false;

    if (event.type() == Event::MouseUp && event.button() == m_resizing_mouse_button) {
        dbgln_if(RESIZE_DEBUG, "[WM] Finish resizing Window({})", m_resize_window);

        auto max_rect = maximized_window_rect(*m_resize_window);
        if (event.y() > max_rect.bottom()) {
            dbgln_if(RESIZE_DEBUG, "Should Maximize vertically");
            m_resize_window->set_vertically_maximized();
            m_resize_window = nullptr;
            m_resizing_mouse_button = MouseButton::None;
            return true;
        }

        Core::EventLoop::current().post_event(*m_resize_window, make<ResizeEvent>(m_resize_window->rect()));
        m_resize_window->invalidate(true, true);
        m_resize_window = nullptr;
        m_resizing_mouse_button = MouseButton::None;
        return true;
    }

    if (event.type() != Event::MouseMove)
        return true;

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
        VERIFY_NOT_REACHED();
    }

    auto new_rect = m_resize_window_original_rect;

    // First, size the new rect.
    new_rect.set_width(new_rect.width() + change_w);
    new_rect.set_height(new_rect.height() + change_h);
    m_resize_window->apply_minimum_size(new_rect);

    if (!m_resize_window->size_increment().is_null()) {
        int horizontal_incs = (new_rect.width() - m_resize_window->base_size().width()) / m_resize_window->size_increment().width();
        new_rect.set_width(m_resize_window->base_size().width() + horizontal_incs * m_resize_window->size_increment().width());
        int vertical_incs = (new_rect.height() - m_resize_window->base_size().height()) / m_resize_window->size_increment().height();
        new_rect.set_height(m_resize_window->base_size().height() + vertical_incs * m_resize_window->size_increment().height());
    }

    if (m_resize_window->resize_aspect_ratio().has_value()) {
        auto& ratio = m_resize_window->resize_aspect_ratio().value();
        auto base_size = m_resize_window->base_size();
        if (abs(change_w) > abs(change_h)) {
            new_rect.set_height(base_size.height() + (new_rect.width() - base_size.width()) * ratio.height() / ratio.width());
        } else {
            new_rect.set_width(base_size.width() + (new_rect.height() - base_size.height()) * ratio.width() / ratio.height());
        }
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
        VERIFY_NOT_REACHED();
    }

    if (m_resize_window->rect() == new_rect)
        return true;

    dbgln_if(RESIZE_DEBUG, "[WM] Resizing, original: {}, now: {}", m_resize_window_original_rect, new_rect);

    m_resize_window->set_rect(new_rect);
    Core::EventLoop::current().post_event(*m_resize_window, make<ResizeEvent>(new_rect));
    return true;
}

bool WindowManager::process_ongoing_drag(MouseEvent& event)
{
    if (!m_dnd_client)
        return false;

    if (event.type() == Event::MouseMove) {
        // We didn't let go of the drag yet, see if we should send some drag move events..
        m_window_stack.for_each_visible_window_from_front_to_back([&](Window& window) {
            if (!window.rect().contains(event.position()))
                return IterationDecision::Continue;
            event.set_drag(true);
            event.set_mime_data(*m_dnd_mime_data);
            deliver_mouse_event(window, event, false);
            return IterationDecision::Break;
        });
    }

    if (!(event.type() == Event::MouseUp && event.button() == MouseButton::Left))
        return true;

    if (auto* window = m_window_stack.window_at(event.position())) {
        m_dnd_client->async_drag_accepted();
        if (window->client()) {
            auto translated_event = event.translated(-window->position());
            window->client()->async_drag_dropped(window->window_id(), translated_event.position(), m_dnd_text, m_dnd_mime_data->all_data());
        }
    } else {
        m_dnd_client->async_drag_cancelled();
    }

    end_dnd_drag();
    return true;
}

void WindowManager::set_cursor_tracking_button(Button* button)
{
    m_cursor_tracking_button = button;
}

auto WindowManager::DoubleClickInfo::metadata_for_button(MouseButton button) const -> ClickMetadata const&
{
    switch (button) {
    case MouseButton::Left:
        return m_left;
    case MouseButton::Right:
        return m_right;
    case MouseButton::Middle:
        return m_middle;
    case MouseButton::Back:
        return m_back;
    case MouseButton::Forward:
        return m_forward;
    default:
        VERIFY_NOT_REACHED();
    }
}

auto WindowManager::DoubleClickInfo::metadata_for_button(MouseButton button) -> ClickMetadata&
{
    switch (button) {
    case MouseButton::Left:
        return m_left;
    case MouseButton::Right:
        return m_right;
    case MouseButton::Middle:
        return m_middle;
    case MouseButton::Back:
        return m_back;
    case MouseButton::Forward:
        return m_forward;
    default:
        VERIFY_NOT_REACHED();
    }
}

bool WindowManager::is_considered_doubleclick(MouseEvent const& event, DoubleClickInfo::ClickMetadata const& metadata) const
{
    int elapsed_since_last_click = metadata.clock.elapsed();
    if (elapsed_since_last_click < m_double_click_speed) {
        auto diff = event.position() - metadata.last_position;
        auto distance_travelled_squared = diff.x() * diff.x() + diff.y() * diff.y();
        if (distance_travelled_squared <= (m_max_distance_for_double_click * m_max_distance_for_double_click))
            return true;
    }
    return false;
}

void WindowManager::start_menu_doubleclick(Window& window, MouseEvent const& event)
{
    // This is a special case. Basically, we're trying to determine whether
    // double clicking on the window menu icon happened. In this case, the
    // WindowFrame only receives a MouseDown event, and since the window
    // menu popus up, it does not see the MouseUp event. But, if they subsequently
    // click there again, the menu is closed and we receive a MouseUp event.
    // So, in order to be able to detect a double click when a menu is being
    // opened by the MouseDown event, we need to consider the MouseDown event
    // as a potential double-click trigger
    VERIFY(event.type() == Event::MouseDown);

    auto& metadata = m_double_click_info.metadata_for_button(event.button());
    if (&window != m_double_click_info.m_clicked_window) {
        // we either haven't clicked anywhere, or we haven't clicked on this
        // window. set the current click window, and reset the timers.

        dbgln_if(DOUBLECLICK_DEBUG, "Initial mousedown on Window({}) for menus (previous was {})", &window, m_double_click_info.m_clicked_window);

        m_double_click_info.m_clicked_window = window;
        m_double_click_info.reset();
    }

    metadata.last_position = event.position();
    metadata.clock.start();
}

bool WindowManager::is_menu_doubleclick(Window& window, MouseEvent const& event) const
{
    VERIFY(event.type() == Event::MouseUp);

    if (&window != m_double_click_info.m_clicked_window)
        return false;

    auto& metadata = m_double_click_info.metadata_for_button(event.button());
    if (!metadata.clock.is_valid())
        return false;

    return is_considered_doubleclick(event, metadata);
}

void WindowManager::process_event_for_doubleclick(Window& window, MouseEvent& event)
{
    // We only care about button presses (because otherwise it's not a doubleclick, duh!)
    VERIFY(event.type() == Event::MouseUp);

    if (&window != m_double_click_info.m_clicked_window) {
        // we either haven't clicked anywhere, or we haven't clicked on this
        // window. set the current click window, and reset the timers.
        dbgln_if(DOUBLECLICK_DEBUG, "Initial mouseup on Window({}) for menus (previous was {})", &window, m_double_click_info.m_clicked_window);

        m_double_click_info.m_clicked_window = window;
        m_double_click_info.reset();
    }

    auto& metadata = m_double_click_info.metadata_for_button(event.button());

    if (!metadata.clock.is_valid() || !is_considered_doubleclick(event, metadata)) {
        // either the clock is invalid because we haven't clicked on this
        // button on this window yet, so there's nothing to do, or this
        // isn't considered to be a double click. either way, restart the
        // clock
        metadata.clock.start();
    } else {
        dbgln_if(DOUBLECLICK_DEBUG, "Transforming MouseUp to MouseDoubleClick ({} < {})!", metadata.clock.elapsed(), m_double_click_speed);

        event = MouseEvent(Event::MouseDoubleClick, event.position(), event.buttons(), event.button(), event.modifiers(), event.wheel_delta());
        // invalidate this now we've delivered a doubleclick, otherwise
        // tripleclick will deliver two doubleclick events (incorrectly).
        metadata.clock = {};
    }

    metadata.last_position = event.position();
}

void WindowManager::deliver_mouse_event(Window& window, MouseEvent const& event, bool process_double_click)
{
    auto translated_event = event.translated(-window.position());
    window.dispatch_event(translated_event);
    if (process_double_click && translated_event.type() == Event::MouseUp) {
        process_event_for_doubleclick(window, translated_event);
        if (translated_event.type() == Event::MouseDoubleClick)
            window.dispatch_event(translated_event);
    }
}

bool WindowManager::process_ongoing_active_input_mouse_event(MouseEvent const& event)
{
    if (!m_active_input_tracking_window)
        return false;

    // At this point, we have delivered the start of an input sequence to a
    // client application. We must keep delivering to that client
    // application until the input sequence is done.
    //
    // This prevents e.g. moving on one window out of the bounds starting
    // a move in that other unrelated window, and other silly shenanigans.
    deliver_mouse_event(*m_active_input_tracking_window, event, true);

    if (event.type() == Event::MouseUp && event.buttons() == 0) {
        m_active_input_tracking_window = nullptr;
    }

    return true;
}

bool WindowManager::process_mouse_event_for_titlebar_buttons(MouseEvent const& event)
{
    if (m_cursor_tracking_button) {
        m_cursor_tracking_button->on_mouse_event(event.translated(-m_cursor_tracking_button->screen_rect().location()));
        return true;
    }

    // This is quite hackish, but it's how the Button hover effect is implemented.
    if (m_hovered_button && event.type() == Event::MouseMove)
        m_hovered_button->on_mouse_event(event.translated(-m_hovered_button->screen_rect().location()));

    return false;
}

void WindowManager::process_mouse_event_for_window(HitTestResult& result, MouseEvent const& event)
{
    auto& window = *result.window;

    if (auto* blocking_modal_window = window.blocking_modal_window()) {
        if (event.type() == Event::Type::MouseDown) {
            // We're clicking on something that's blocked by a modal window.
            // Flash the modal window to let the user know about it.
            blocking_modal_window->frame().start_flash_animation();
        }
        // Don't send mouse events to windows blocked by a modal child.
        return;
    }

    // First check if we should initiate a move or resize (Super+LMB or Super+RMB).
    // In those cases, the event is swallowed by the window manager.
    if (window.is_movable()) {
        if (!window.is_fullscreen() && m_keyboard_modifiers == Mod_Super && event.type() == Event::MouseDown && event.button() == MouseButton::Left) {
            start_window_move(window, event);
            return;
        }
        if (window.is_resizable() && m_keyboard_modifiers == Mod_Super && event.type() == Event::MouseDown && event.button() == MouseButton::Right && !window.blocking_modal_window()) {
            start_window_resize(window, event);
            return;
        }
    }

    if (event.type() == Event::MouseDown) {
        if (window.type() == WindowType::Normal || window.type() == WindowType::ToolWindow)
            move_to_front_and_make_active(window);
        else if (window.type() == WindowType::Desktop)
            set_active_window(&window);
    }

    if (result.is_frame_hit) {
        // We are hitting the frame, pass the event along to WindowFrame.
        window.frame().handle_mouse_event(event.translated(-window.frame().rect().location()));
        return;
    }

    if (!window.global_cursor_tracking()) {
        deliver_mouse_event(window, event, true);
    }

    if (event.type() == Event::MouseDown)
        m_active_input_tracking_window = window;
}

void WindowManager::process_mouse_event(MouseEvent& event)
{
    // 0. Forget the resize candidate (window that we could initiate a resize of from the current cursor position.)
    //    A new resize candidate may be determined if we hit an appropriate part of a window.
    clear_resize_candidate();

    // 1. Process ongoing drag events. This is done first to avoid clashing with global cursor tracking.
    if (process_ongoing_drag(event))
        return;

    // 2. Send the mouse event to all windows with global cursor tracking enabled.
    m_window_stack.for_each_visible_window_from_front_to_back([&](Window& window) {
        if (window.global_cursor_tracking())
            deliver_mouse_event(window, event, false);
        return IterationDecision::Continue;
    });

    // 3. If there's an active input tracking window, all mouse events go there.
    //    Tracking ends after all mouse buttons have been released.
    if (process_ongoing_active_input_mouse_event(event))
        return;

    // 4. If there's a window being moved around, take care of that.
    if (process_ongoing_window_move(event))
        return;

    // 5. If there's a window being resized, take care of that.
    if (process_ongoing_window_resize(event))
        return;

    // 6. If the event is inside a titlebar button, WindowServer implements all
    //    the behavior for those buttons internally.
    if (process_mouse_event_for_titlebar_buttons(event))
        return;

    // 7. If there are menus open, deal with them now. (FIXME: This needs to be cleaned up & simplified!)
    bool hitting_menu_in_window_with_active_menu = [&] {
        if (!m_window_with_active_menu)
            return false;
        auto& frame = m_window_with_active_menu->frame();
        return frame.menubar_rect().contains(event.position().translated(-frame.rect().location()));
    }();

    // FIXME: This is quite hackish, we clear the hovered menu before potentially setting the same menu
    //        as hovered again. This makes sure that the hovered state doesn't linger after moving the
    //        cursor away from a hovered menu.
    MenuManager::the().set_hovered_menu(nullptr);

    if (MenuManager::the().has_open_menu()
        || hitting_menu_in_window_with_active_menu) {

        if (!hitting_menu_in_window_with_active_menu) {
            MenuManager::the().dispatch_event(event);
            return;
        }
    }

    // 8. Hit test the window stack to see what's under the cursor.
    auto result = m_window_stack.hit_test(event.position());

    if (!result.has_value()) {
        // No window is under the cursor.
        if (event.type() == Event::MouseDown) {
            // Clicked outside of any window -> no active window.
            // FIXME: Is this actually necessary? The desktop window should catch everything anyway.
            set_active_window(nullptr);
        }
        return;
    }

    process_mouse_event_for_window(result.value(), event);
}

void WindowManager::reevaluate_hovered_window(Window* updated_window)
{
    if (m_dnd_client || m_resize_window || m_move_window || m_cursor_tracking_button || MenuManager::the().has_open_menu())
        return;

    auto cursor_location = ScreenInput::the().cursor_location();
    auto* currently_hovered = hovered_window();
    if (updated_window) {
        if (!(updated_window == currently_hovered || updated_window->frame().rect().contains(cursor_location) || (currently_hovered && currently_hovered->frame().rect().contains(cursor_location))))
            return;
    }

    Window* hovered_window = nullptr;
    if (auto* fullscreen_window = active_fullscreen_window()) {
        if (fullscreen_window->hit_test(cursor_location).has_value())
            hovered_window = fullscreen_window;
    } else {
        hovered_window = m_window_stack.window_at(cursor_location);
    }

    if (set_hovered_window(hovered_window)) {
        if (currently_hovered && m_resize_candidate == currently_hovered)
            clear_resize_candidate();

        if (hovered_window) {
            // Send a fake MouseMove event. This allows the new hovering window
            // to determine which widget we're hovering, and also update the cursor
            // accordingly. We do this because this re-evaluation of the currently
            // hovered window wasn't triggered by a mouse move event, but rather
            // e.g. a hit-test result change due to a transparent window repaint.
            if (hovered_window->hit_test(cursor_location, false).has_value()) {
                MouseEvent event(Event::MouseMove, cursor_location.translated(-hovered_window->rect().location()), 0, MouseButton::None, 0);
                hovered_window->dispatch_event(event);
            } else if (!hovered_window->is_frameless()) {
                MouseEvent event(Event::MouseMove, cursor_location.translated(-hovered_window->frame().rect().location()), 0, MouseButton::None, 0);
                hovered_window->frame().handle_mouse_event(event);
            }
        }
    }
}

void WindowManager::clear_resize_candidate()
{
    if (m_resize_candidate)
        Compositor::the().invalidate_cursor();
    m_resize_candidate = nullptr;
}

Gfx::IntRect WindowManager::desktop_rect(Screen& screen) const
{
    if (active_fullscreen_window())
        return Screen::main().rect(); // TODO: we should support fullscreen windows on any screen
    auto screen_rect = screen.rect();
    if (screen.is_main_screen())
        screen_rect.set_height(screen.height() - 28);
    return screen_rect;
}

Gfx::IntRect WindowManager::arena_rect_for_type(Screen& screen, WindowType type) const
{
    switch (type) {
    case WindowType::Desktop:
        return Screen::bounding_rect();
    case WindowType::Normal:
    case WindowType::ToolWindow:
        return desktop_rect(screen);
    case WindowType::Menu:
    case WindowType::WindowSwitcher:
    case WindowType::Taskbar:
    case WindowType::Tooltip:
    case WindowType::Applet:
    case WindowType::Notification:
        return screen.rect();
    default:
        VERIFY_NOT_REACHED();
    }
}

void WindowManager::event(Core::Event& event)
{
    if (static_cast<Event&>(event).is_mouse_event()) {
        auto& mouse_event = static_cast<MouseEvent&>(event);
        if (mouse_event.type() != Event::MouseMove)
            m_previous_event_was_super_keydown = false;

        process_mouse_event(mouse_event);
        set_hovered_window(m_window_stack.window_at(mouse_event.position(), WindowStack::IncludeWindowFrame::No));
        return;
    }

    if (static_cast<Event&>(event).is_key_event()) {
        process_key_event(static_cast<KeyEvent&>(event));
        return;
    }

    Core::Object::event(event);
}

void WindowManager::process_key_event(KeyEvent& event)
{
    m_keyboard_modifiers = event.modifiers();

    // Escape key cancels an ongoing drag.
    if (event.type() == Event::KeyDown && event.key() == Key_Escape && m_dnd_client) {
        // Notify the drag-n-drop client that the drag was cancelled.
        m_dnd_client->async_drag_cancelled();

        // Also notify the currently hovered window (if any) that the ongoing drag was cancelled.
        if (m_hovered_window && m_hovered_window->client() && m_hovered_window->client() != m_dnd_client)
            m_hovered_window->client()->async_drag_cancelled();

        end_dnd_drag();
        return;
    }

    if (event.type() == Event::KeyDown && (event.modifiers() == (Mod_Ctrl | Mod_Super | Mod_Shift) && event.key() == Key_I)) {
        reload_icon_bitmaps_after_scale_change();
        Compositor::the().invalidate_screen();
        return;
    }

    if (event.type() == Event::KeyDown && event.key() == Key_Super) {
        m_previous_event_was_super_keydown = true;
    } else if (m_previous_event_was_super_keydown) {
        m_previous_event_was_super_keydown = false;
        if (!m_dnd_client && !m_active_input_tracking_window && event.type() == Event::KeyUp && event.key() == Key_Super) {
            tell_wms_super_key_pressed();
            return;
        }
    }

    if (MenuManager::the().current_menu() && event.key() != Key_Super) {
        MenuManager::the().dispatch_event(event);
        return;
    }

    if (event.type() == Event::KeyDown && ((event.modifiers() == Mod_Super && event.key() == Key_Tab) || (event.modifiers() == (Mod_Super | Mod_Shift) && event.key() == Key_Tab)))
        m_switcher.show();
    if (m_switcher.is_visible()) {
        m_switcher.on_key_event(event);
        return;
    }

    if (!m_active_input_window)
        return;

    if (event.type() == Event::KeyDown && event.modifiers() == Mod_Super && m_active_input_window->type() != WindowType::Desktop) {
        if (event.key() == Key_Down) {
            if (m_active_input_window->is_resizable() && m_active_input_window->is_maximized()) {
                maximize_windows(*m_active_input_window, false);
                return;
            }
            if (m_active_input_window->is_minimizable())
                minimize_windows(*m_active_input_window, true);
            return;
        }
        if (m_active_input_window->is_resizable()) {
            if (event.key() == Key_Up) {
                maximize_windows(*m_active_input_window, !m_active_input_window->is_maximized());
                return;
            }
            if (event.key() == Key_Left) {
                if (m_active_input_window->tiled() == WindowTileType::Left)
                    return;
                if (m_active_input_window->tiled() != WindowTileType::None) {
                    m_active_input_window->set_untiled();
                    return;
                }
                if (m_active_input_window->is_maximized())
                    maximize_windows(*m_active_input_window, false);
                m_active_input_window->set_tiled(nullptr, WindowTileType::Left);
                return;
            }
            if (event.key() == Key_Right) {
                if (m_active_input_window->tiled() == WindowTileType::Right)
                    return;
                if (m_active_input_window->tiled() != WindowTileType::None) {
                    m_active_input_window->set_untiled();
                    return;
                }
                if (m_active_input_window->is_maximized())
                    maximize_windows(*m_active_input_window, false);
                m_active_input_window->set_tiled(nullptr, WindowTileType::Right);
                return;
            }
        }
    }
    m_active_input_window->dispatch_event(event);
}

void WindowManager::set_highlight_window(Window* new_highlight_window)
{
    if (new_highlight_window == m_window_stack.highlight_window())
        return;
    auto* previous_highlight_window = m_window_stack.highlight_window();
    m_window_stack.set_highlight_window(new_highlight_window);
    if (previous_highlight_window) {
        previous_highlight_window->invalidate(true, true);
        Compositor::the().invalidate_screen(previous_highlight_window->frame().render_rect());
    }
    if (new_highlight_window) {
        new_highlight_window->invalidate(true, true);
        Compositor::the().invalidate_screen(new_highlight_window->frame().render_rect());
    }
    // Invalidate occlusions in case the state change changes geometry
    Compositor::the().invalidate_occlusions();
}

bool WindowManager::is_active_window_or_accessory(Window& window) const
{
    if (window.is_accessory())
        return window.parent_window()->is_active();

    return window.is_active();
}

static bool window_type_can_become_active(WindowType type)
{
    return type == WindowType::Normal || type == WindowType::ToolWindow || type == WindowType::Desktop;
}

void WindowManager::restore_active_input_window(Window* window)
{
    // If the previous active input window is gone, fall back to the
    // current active window
    if (!window)
        window = active_window();
    // If the current active window is also gone, pick some other window
    if (!window && pick_new_active_window(nullptr))
        return;

    if (window && !window->is_minimized() && window->is_visible())
        set_active_input_window(window);
    else
        set_active_input_window(nullptr);
}

Window* WindowManager::set_active_input_window(Window* window)
{
    if (window == m_active_input_window)
        return window;

    Window* previous_input_window = m_active_input_window;
    if (previous_input_window)
        Core::EventLoop::current().post_event(*previous_input_window, make<Event>(Event::WindowInputLeft));

    if (window) {
        m_active_input_window = *window;
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowInputEntered));
    } else {
        m_active_input_window = nullptr;
    }

    return previous_input_window;
}

void WindowManager::set_active_window(Window* new_active_window, bool make_input)
{
    if (new_active_window) {
        if (auto* modal_window = new_active_window->blocking_modal_window()) {
            VERIFY(modal_window->is_modal());
            VERIFY(modal_window != new_active_window);
            new_active_window = modal_window;
            make_input = true;
        }

        if (!window_type_can_become_active(new_active_window->type()))
            return;
    }

    auto* new_active_input_window = new_active_window;
    if (new_active_window && new_active_window->is_accessory()) {
        // The parent of an accessory window is always the active
        // window, but input is routed to the accessory window
        new_active_window = new_active_window->parent_window();
    }

    if (make_input)
        set_active_input_window(new_active_input_window);

    if (new_active_window == m_window_stack.active_window())
        return;

    if (auto* previously_active_window = m_window_stack.active_window()) {
        for (auto& child_window : previously_active_window->child_windows()) {
            if (child_window && child_window->type() == WindowType::Tooltip)
                child_window->request_close();
        }
        Core::EventLoop::current().post_event(*previously_active_window, make<Event>(Event::WindowDeactivated));
        previously_active_window->invalidate(true, true);
        m_window_stack.set_active_window(nullptr);
        m_active_input_tracking_window = nullptr;
        tell_wms_window_state_changed(*previously_active_window);
    }

    if (new_active_window) {
        m_window_stack.set_active_window(new_active_window);
        Core::EventLoop::current().post_event(*new_active_window, make<Event>(Event::WindowActivated));
        new_active_window->invalidate(true, true);
        tell_wms_window_state_changed(*new_active_window);
    }

    // Window shapes may have changed (e.g. shadows for inactive/active windows)
    Compositor::the().invalidate_occlusions();
}

bool WindowManager::set_hovered_window(Window* window)
{
    if (m_hovered_window == window)
        return false;

    if (m_hovered_window)
        Core::EventLoop::current().post_event(*m_hovered_window, make<Event>(Event::WindowLeft));

    m_hovered_window = window;

    if (m_hovered_window)
        Core::EventLoop::current().post_event(*m_hovered_window, make<Event>(Event::WindowEntered));
    return true;
}

ClientConnection const* WindowManager::active_client() const
{
    if (auto* window = m_window_stack.active_window())
        return window->client();
    return nullptr;
}

Cursor const& WindowManager::active_cursor() const
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

    if (m_hovered_window) {
        if (auto* modal_window = const_cast<Window&>(*m_hovered_window).blocking_modal_window()) {
            if (modal_window->cursor())
                return *modal_window->cursor();
        } else if (m_hovered_window->cursor()) {
            return *m_hovered_window->cursor();
        }
    }

    return *m_arrow_cursor;
}

void WindowManager::set_hovered_button(Button* button)
{
    m_hovered_button = button;
}

void WindowManager::set_resize_candidate(Window& window, ResizeDirection direction)
{
    m_resize_candidate = window;
    m_resize_direction = direction;
}

ResizeDirection WindowManager::resize_direction_of_window(Window const& window)
{
    if (&window != m_resize_window)
        return ResizeDirection::None;
    return m_resize_direction;
}

Gfx::IntRect WindowManager::maximized_window_rect(Window const& window, bool relative_to_window_screen) const
{
    auto& screen = Screen::closest_to_rect(window.frame().rect());
    Gfx::IntRect rect = screen.rect();

    // Subtract window title bar (leaving the border)
    rect.set_y(rect.y() + window.frame().titlebar_rect().height() + window.frame().menubar_rect().height());
    rect.set_height(rect.height() - window.frame().titlebar_rect().height() - window.frame().menubar_rect().height());

    if (screen.is_main_screen()) {
        // Subtract taskbar window height if present
        const_cast<WindowManager*>(this)->m_window_stack.for_each_visible_window_of_type_from_back_to_front(WindowType::Taskbar, [&rect](Window& taskbar_window) {
            rect.set_height(rect.height() - taskbar_window.height());
            return IterationDecision::Break;
        });
    }

    constexpr int tasteful_space_above_maximized_window = 1;
    rect.set_y(rect.y() + tasteful_space_above_maximized_window);
    rect.set_height(rect.height() - tasteful_space_above_maximized_window);

    if (relative_to_window_screen)
        rect.translate_by(-screen.rect().location());
    return rect;
}

void WindowManager::start_dnd_drag(ClientConnection& client, String const& text, Gfx::Bitmap const* bitmap, Core::MimeData const& mime_data)
{
    VERIFY(!m_dnd_client);
    m_dnd_client = client;
    m_dnd_text = text;
    m_dnd_bitmap = bitmap;
    m_dnd_mime_data = mime_data;
    Compositor::the().invalidate_cursor();
    m_active_input_tracking_window = nullptr;
}

void WindowManager::end_dnd_drag()
{
    VERIFY(m_dnd_client);
    Compositor::the().invalidate_cursor();
    m_dnd_client = nullptr;
    m_dnd_text = {};
    m_dnd_bitmap = nullptr;
}

Gfx::IntRect WindowManager::dnd_rect() const
{
    int bitmap_width = m_dnd_bitmap ? m_dnd_bitmap->width() : 0;
    int bitmap_height = m_dnd_bitmap ? m_dnd_bitmap->height() : 0;
    int width = font().width(m_dnd_text) + bitmap_width;
    int height = max((int)font().glyph_height(), bitmap_height);
    auto location = Compositor::the().current_cursor_rect().center().translated(8, 8);
    return Gfx::IntRect(location, { width, height }).inflated(16, 8);
}

void WindowManager::invalidate_after_theme_or_font_change()
{
    Compositor::the().set_background_color(m_config->read_entry("Background", "Color", palette().desktop_background().to_string()));
    WindowFrame::reload_config();
    m_window_stack.for_each_window([&](Window& window) {
        window.frame().theme_changed();
        return IterationDecision::Continue;
    });
    ClientConnection::for_each_client([&](ClientConnection& client) {
        client.async_update_system_theme(Gfx::current_system_theme_buffer());
    });
    MenuManager::the().did_change_theme();
    AppletManager::the().did_change_theme();
    Compositor::the().invalidate_occlusions();
    Compositor::the().invalidate_screen();
}

bool WindowManager::update_theme(String theme_path, String theme_name)
{
    auto new_theme = Gfx::load_system_theme(theme_path);
    if (!new_theme.is_valid())
        return false;
    Gfx::set_system_theme(new_theme);
    m_palette = Gfx::PaletteImpl::create_with_anonymous_buffer(new_theme);
    auto wm_config = Core::ConfigFile::open("/etc/WindowServer.ini");
    wm_config->write_entry("Theme", "Name", theme_name);
    wm_config->remove_entry("Background", "Color");
    wm_config->sync();
    invalidate_after_theme_or_font_change();
    return true;
}

void WindowManager::did_popup_a_menu(Badge<Menu>)
{
    // Clear any ongoing input gesture
    if (!m_active_input_tracking_window)
        return;
    m_active_input_tracking_window->set_automatic_cursor_tracking_enabled(false);
    m_active_input_tracking_window = nullptr;
}

void WindowManager::minimize_windows(Window& window, bool minimized)
{
    for_each_window_in_modal_stack(window, [&](auto& w, bool) {
        w.set_minimized(minimized);
        return IterationDecision::Continue;
    });
}

void WindowManager::maximize_windows(Window& window, bool maximized)
{
    for_each_window_in_modal_stack(window, [&](auto& w, bool stack_top) {
        if (stack_top)
            w.set_maximized(maximized);
        if (w.is_minimized())
            w.set_minimized(false);
        return IterationDecision::Continue;
    });
}

Gfx::IntPoint WindowManager::get_recommended_window_position(Gfx::IntPoint const& desired)
{
    // FIXME: Find a  better source for the width and height to shift by.
    Gfx::IntPoint shift(8, Gfx::WindowTheme::current().titlebar_height(Gfx::WindowTheme::WindowType::Normal, palette()) + 10);

    // FIXME: Find a better source for this.
    int taskbar_height = 28;

    Window const* overlap_window = nullptr;
    m_window_stack.for_each_visible_window_of_type_from_front_to_back(WindowType::Normal, [&](Window& window) {
        if (window.default_positioned() && (!overlap_window || overlap_window->window_id() < window.window_id())) {
            overlap_window = &window;
        }
        return IterationDecision::Continue;
    });

    Gfx::IntPoint point;
    if (overlap_window) {
        auto& screen = Screen::closest_to_location(desired);
        point = overlap_window->position() + shift;
        point = { point.x() % screen.width(),
            (point.y() >= (screen.height() - (screen.is_main_screen() ? taskbar_height : 0)))
                ? Gfx::WindowTheme::current().titlebar_height(Gfx::WindowTheme::WindowType::Normal, palette())
                : point.y() };
    } else {
        point = desired;
    }

    return point;
}

void WindowManager::reload_icon_bitmaps_after_scale_change()
{
    reload_config();
    m_window_stack.for_each_window([&](Window& window) {
        auto& window_frame = window.frame();
        window_frame.theme_changed();
        return IterationDecision::Continue;
    });
}

void WindowManager::set_window_with_active_menu(Window* window)
{
    if (m_window_with_active_menu == window)
        return;
    if (window)
        m_window_with_active_menu = window->make_weak_ptr<Window>();
    else
        m_window_with_active_menu = nullptr;
}
}
