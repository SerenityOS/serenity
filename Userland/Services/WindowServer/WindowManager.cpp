/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
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
#include <LibGfx/Font/Font.h>
#include <LibGfx/StylePainter.h>
#include <LibGfx/SystemTheme.h>
#include <Services/Taskbar/TaskbarWindow.h>
#include <WindowServer/Animation.h>
#include <WindowServer/AppletManager.h>
#include <WindowServer/Button.h>
#include <WindowServer/ConnectionFromClient.h>
#include <WindowServer/Cursor.h>
#include <WindowServer/WindowClientEndpoint.h>

namespace WindowServer {

static WindowManager* s_the;

WindowManager& WindowManager::the()
{
    VERIFY(s_the);
    return *s_the;
}

WindowManager::WindowManager(Gfx::PaletteImpl& palette)
    : m_switcher(WindowSwitcher::construct())
    , m_keymap_switcher(KeymapSwitcher::construct())
    , m_palette(palette)
{
    s_the = this;

    {
        // If we haven't created any window stacks, at least create the stationary/main window stack
        auto row = adopt_own(*new RemoveReference<decltype(*m_window_stacks[0])>());
        auto main_window_stack = adopt_own(*new WindowStack(0, 0));
        main_window_stack->set_stationary_window_stack(*main_window_stack);
        m_current_window_stack = main_window_stack.ptr();
        row->append(move(main_window_stack));
        m_window_stacks.append(move(row));
    }

    reload_config();

    m_keymap_switcher->on_keymap_change = [&](ByteString const& keymap) {
        for_each_window_manager([&keymap](WMConnectionFromClient& conn) {
            if (!(conn.event_mask() & WMEventMask::KeymapChanged))
                return IterationDecision::Continue;
            if (conn.window_id() < 0)
                return IterationDecision::Continue;

            conn.async_keymap_changed(conn.window_id(), keymap);
            return IterationDecision::Continue;
        });
    };

    Compositor::the().did_construct_window_manager({});
}

void WindowManager::reload_config()
{
    unsigned workspace_rows = (unsigned)g_config->read_num_entry("Workspaces", "Rows", default_window_stack_rows);
    unsigned workspace_columns = (unsigned)g_config->read_num_entry("Workspaces", "Columns", default_window_stack_columns);
    if (workspace_rows == 0 || workspace_columns == 0 || workspace_rows > max_window_stack_rows || workspace_columns > max_window_stack_columns) {
        workspace_rows = default_window_stack_rows;
        workspace_columns = default_window_stack_columns;
    }
    apply_workspace_settings(workspace_rows, workspace_columns, false);

    m_double_click_speed = g_config->read_num_entry("Input", "DoubleClickSpeed", 250);
    m_mouse_buttons_switched = g_config->read_bool_entry("Mouse", "ButtonsSwitched", false);
    m_natural_scroll = g_config->read_bool_entry("Mouse", "NaturalScroll", false);
    m_cursor_highlight_radius = g_config->read_num_entry("Mouse", "CursorHighlightRadius", 25);
    Color default_highlight_color = Color::NamedColor::Red;
    default_highlight_color.set_alpha(110);
    m_cursor_highlight_color = Color::from_string(g_config->read_entry("Mouse", "CursorHighlightColor")).value_or(default_highlight_color);
    apply_cursor_theme(g_config->read_entry("Mouse", "CursorTheme", "Default"));

    auto reload_graphic = [&](RefPtr<MultiScaleBitmaps>& bitmap, ByteString const& name) {
        if (bitmap) {
            if (!bitmap->load(name))
                bitmap = nullptr;
        } else {
            bitmap = MultiScaleBitmaps::create(name);
        }
    };

    reload_graphic(m_overlay_rect_shadow, g_config->read_entry("Graphics", "OverlayRectShadow"));
    Compositor::the().invalidate_after_theme_or_font_change();

    WindowFrame::reload_config();

    load_system_effects();
}

Gfx::Font const& WindowManager::font() const
{
    return Gfx::FontDatabase::default_font();
}

Gfx::Font const& WindowManager::window_title_font() const
{
    return Gfx::FontDatabase::window_title_font();
}

bool WindowManager::set_screen_layout(ScreenLayout&& screen_layout, bool save, ByteString& error_msg)
{
    if (!Screen::apply_layout(move(screen_layout), error_msg))
        return false;

    reload_icon_bitmaps_after_scale_change();

    tell_wms_screen_rects_changed();

    for_each_window_stack([&](auto& window_stack) {
        window_stack.for_each_window([](Window& window) {
            window.screens().clear_with_capacity();
            window.recalculate_rect();
            return IterationDecision::Continue;
        });
        return IterationDecision::Continue;
    });

    Compositor::the().screen_resolution_changed();

    if (save)
        Screen::layout().save_config(*g_config);
    return true;
}

ScreenLayout WindowManager::get_screen_layout() const
{
    return Screen::layout();
}

bool WindowManager::save_screen_layout(ByteString& error_msg)
{
    if (!Screen::layout().save_config(*g_config)) {
        error_msg = "Could not save";
        return false;
    }
    return true;
}

bool WindowManager::apply_workspace_settings(unsigned rows, unsigned columns, bool save)
{
    VERIFY(rows != 0);
    VERIFY(rows <= max_window_stack_rows);
    VERIFY(columns != 0);
    VERIFY(columns <= max_window_stack_columns);

    auto current_rows = window_stack_rows();
    auto current_columns = window_stack_columns();
    if (rows != current_rows || columns != current_columns) {
        auto& current_window_stack = this->current_window_stack();
        auto current_stack_row = current_window_stack.row();
        auto current_stack_column = current_window_stack.column();
        bool need_rerender = false;
        bool removing_current_stack = current_stack_row > rows - 1 || current_stack_column > columns - 1;
        auto new_current_row = min(current_stack_row, rows - 1);
        auto new_current_column = min(current_stack_column, columns - 1);

        // Collect all windows that were moved. We can't tell the wms at this point because
        // the current window stack may not be valid anymore, until after the move is complete
        Vector<Window*, 32> windows_moved;

        auto merge_window_stack = [&](WindowStack& from_stack, WindowStack& into_stack) {
            auto move_to = WindowStack::MoveAllWindowsTo::Back;

            // TODO: Figure out a better algorithm. We basically always layer it on top, unless
            // it's either being moved to window stack we're viewing or that we will be viewing.
            // In that case we would want to make sure the window stack ends up on top (with no
            // change to the active window)
            bool moving_to_new_current_stack = into_stack.row() == new_current_row && into_stack.column() == new_current_column;
            if (moving_to_new_current_stack)
                move_to = WindowStack::MoveAllWindowsTo::Front;
            from_stack.move_all_windows(into_stack, windows_moved, move_to);
        };

        // While we have too many rows, merge each row too many into the new bottom row
        while (current_rows > rows) {
            auto& row = *m_window_stacks[rows];
            for (size_t column_index = 0; column_index < row.size(); column_index++) {
                merge_window_stack(*row[column_index], *(*m_window_stacks[rows - 1])[column_index]);
                if (rows - 1 == current_stack_row && column_index == current_stack_column)
                    need_rerender = true;
            }
            m_window_stacks.remove(rows);
            current_rows--;
        }
        // While we have too many columns, merge each column too many into the new right most column
        while (current_columns > columns) {
            for (size_t row_index = 0; row_index < current_rows; row_index++) {
                auto& row = *m_window_stacks[row_index];
                merge_window_stack(*row[columns], *row[columns - 1]);
                if (row_index == current_stack_row && columns - 1 == current_stack_column)
                    need_rerender = true;
                row.remove(columns);
            }
            current_columns--;
        }
        // Add more rows if necessary
        while (rows > current_rows) {
            auto row = adopt_own(*new RemoveReference<decltype(*m_window_stacks[0])>());
            for (size_t column_index = 0; column_index < columns; column_index++) {
                auto window_stack = adopt_own(*new WindowStack(current_rows, column_index));
                window_stack->set_stationary_window_stack(*(*m_window_stacks[0])[0]);
                row->append(move(window_stack));
            }
            m_window_stacks.append(move(row));
            current_rows++;
        }
        // Add more columns if necessary
        while (columns > current_columns) {
            for (size_t row_index = 0; row_index < current_rows; row_index++) {
                auto& row = m_window_stacks[row_index];
                while (row->size() < columns) {
                    auto window_stack = adopt_own(*new WindowStack(row_index, row->size()));
                    window_stack->set_stationary_window_stack(*(*m_window_stacks[0])[0]);
                    row->append(move(window_stack));
                }
            }
            current_columns++;
        }

        if (removing_current_stack) {
            // If we're on a window stack that was removed, we need to move...
            m_current_window_stack = (*m_window_stacks[new_current_row])[new_current_column];
            Compositor::the().set_current_window_stack_no_transition(*m_current_window_stack);
            need_rerender = false; // The compositor already called invalidate_for_window_stack_merge_or_change for us
        }

        for (auto* window_moved : windows_moved)
            WindowManager::the().tell_wms_window_state_changed(*window_moved);

        tell_wms_screen_rects_changed(); // updates the available workspaces
        if (current_stack_row != new_current_row || current_stack_column != new_current_column)
            tell_wms_current_window_stack_changed();

        if (need_rerender)
            Compositor::the().invalidate_for_window_stack_merge_or_change();
    }

    if (save) {
        g_config->write_num_entry("Workspaces", "Rows", window_stack_rows());
        g_config->write_num_entry("Workspaces", "Columns", window_stack_columns());
        return !g_config->sync().is_error();
    }
    return true;
}

void WindowManager::set_acceleration_factor(double factor)
{
    ScreenInput::the().set_acceleration_factor(factor);
    dbgln("Saving acceleration factor {} to config file at {}", factor, g_config->filename());
    g_config->write_entry("Mouse", "AccelerationFactor", ByteString::formatted("{}", factor));
    sync_config_to_disk();
}

void WindowManager::set_scroll_step_size(unsigned step_size)
{
    ScreenInput::the().set_scroll_step_size(step_size);
    dbgln("Saving scroll step size {} to config file at {}", step_size, g_config->filename());
    g_config->write_entry("Mouse", "ScrollStepSize", ByteString::number(step_size));
    sync_config_to_disk();
}

void WindowManager::set_double_click_speed(int speed)
{
    VERIFY(speed >= double_click_speed_min && speed <= double_click_speed_max);
    m_double_click_speed = speed;
    dbgln("Saving double-click speed {} to config file at {}", speed, g_config->filename());
    g_config->write_entry("Input", "DoubleClickSpeed", ByteString::number(speed));
    sync_config_to_disk();
}

int WindowManager::double_click_speed() const
{
    return m_double_click_speed;
}

void WindowManager::set_mouse_buttons_switched(bool switched)
{
    m_mouse_buttons_switched = switched;
    dbgln("Saving mouse buttons switched state {} to config file at {}", switched, g_config->filename());
    g_config->write_bool_entry("Mouse", "ButtonsSwitched", switched);
    sync_config_to_disk();
}

bool WindowManager::are_mouse_buttons_switched() const
{
    return m_mouse_buttons_switched;
}

void WindowManager::set_natural_scroll(bool inverted)
{
    m_natural_scroll = inverted;
    dbgln("Saving scroll inverted state {} to config file at {}", inverted, g_config->filename());
    g_config->write_bool_entry("Mouse", "NaturalScroll", inverted);
    sync_config_to_disk();
}

bool WindowManager::is_natural_scroll() const
{
    return m_natural_scroll;
}

WindowStack& WindowManager::window_stack_for_window(Window& window)
{
    if (is_stationary_window_type(window.type()))
        return *(*m_window_stacks[0])[0];
    if (auto* parent = window.parent_window(); parent && !is_stationary_window_type(parent->type()))
        return parent->window_stack();
    return current_window_stack();
}

void WindowManager::add_window(Window& window)
{
    auto& window_stack = window_stack_for_window(window);
    bool is_first_window = window_stack.is_empty();

    window_stack.add(window);

    if (window.is_fullscreen()) {
        auto& screen = Screen::main(); // TODO: support fullscreen windows on other screens!
        window.set_rect(screen.rect());
        window.send_resize_event_to_client();
    }

    if (window.type() != WindowType::Desktop || is_first_window)
        set_active_window(&window);

    if (window.type() == WindowType::Popup)
        notify_active_window_input_preempted();

    if (m_switcher->is_visible() && window.type() != WindowType::WindowSwitcher)
        m_switcher->refresh();

    Compositor::the().invalidate_occlusions();

    window.invalidate(true, true);

    tell_wms_window_state_changed(window);
}

void WindowManager::move_to_front_and_make_active(Window& window)
{
    for_each_window_in_modal_chain(window, [&](auto& w) {
        w.set_minimized(false);
        w.window_stack().move_to_front(w);
        return IterationDecision::Continue;
    });

    if (auto* blocker = window.blocking_modal_window()) {
        blocker->window_stack().move_to_front(*blocker);
        set_active_window(blocker);
    } else {
        window.window_stack().move_to_front(window);
        set_active_window(&window);

        for (auto& child : window.child_windows()) {
            if (!child || !child->is_modal())
                continue;
            if (child->is_rendering_above())
                child->window_stack().move_to_front(*child);
        }
    }

    if (m_switcher->is_visible()) {
        m_switcher->refresh();
        if (!window.is_modal()) {
            m_switcher->select_window(window);
            set_highlight_window(&window);
        }
    }

    Compositor::the().invalidate_occlusions();
}

void WindowManager::remove_window(Window& window)
{
    check_hide_geometry_overlay(window);
    auto* active = active_window();
    bool was_modal = window.is_modal(); // This requires the window to be on a window stack still!
    window.window_stack().remove(window);
    if (active == &window)
        pick_new_active_window(&window);

    if (window.type() == WindowType::Popup)
        notify_active_window_input_restored();

    window.invalidate_last_rendered_screen_rects_now();

    if (m_switcher->is_visible() && window.type() != WindowType::WindowSwitcher)
        m_switcher->refresh();

    Compositor::the().invalidate_occlusions();

    for_each_window_manager([&](WMConnectionFromClient& conn) {
        if (conn.window_id() < 0 || !(conn.event_mask() & WMEventMask::WindowRemovals))
            return IterationDecision::Continue;
        if (!window.is_internal() && !was_modal)
            conn.async_window_removed(conn.window_id(), window.client_id(), window.window_id());
        return IterationDecision::Continue;
    });

    if (m_tile_window_overlay && m_tile_window_overlay->is_window(window))
        stop_tile_window_animation();
}

void WindowManager::greet_window_manager(WMConnectionFromClient& conn)
{
    if (conn.window_id() < 0)
        return;

    tell_wm_about_current_window_stack(conn);

    for_each_window_stack([&](auto& window_stack) {
        window_stack.for_each_window([&](Window& other_window) {
            tell_wm_about_window(conn, other_window);
            tell_wm_about_window_icon(conn, other_window);
            return IterationDecision::Continue;
        });
        return IterationDecision::Continue;
    });
    if (auto* applet_area_window = AppletManager::the().window())
        tell_wms_applet_area_size_changed(applet_area_window->size());
}

void WindowManager::tell_wm_about_window(WMConnectionFromClient& conn, Window& window)
{
    if (conn.window_id() < 0)
        return;
    if (!(conn.event_mask() & WMEventMask::WindowStateChanges))
        return;
    if (window.is_internal())
        return;
    if (window.blocking_modal_window())
        return;

    Window* modeless = window.modeless_ancestor();
    if (!modeless)
        return;
    bool is_blocked = modeless->blocking_modal_window();
    auto is_active = for_each_window_in_modal_chain(*modeless, [&](auto& w) {
        if (w.is_active())
            return IterationDecision::Break;
        return IterationDecision::Continue;
    });

    auto& window_stack = is_stationary_window_type(modeless->type()) ? current_window_stack() : modeless->window_stack();
    conn.async_window_state_changed(conn.window_id(), modeless->client_id(), modeless->window_id(), window_stack.row(), window_stack.column(), is_active, is_blocked, modeless->is_minimized(), modeless->is_frameless(), (i32)modeless->type(), modeless->computed_title(), modeless->rect(), modeless->progress());
}

void WindowManager::tell_wm_about_window_rect(WMConnectionFromClient& conn, Window& window)
{
    if (conn.window_id() < 0)
        return;
    if (!(conn.event_mask() & WMEventMask::WindowRectChanges))
        return;
    if (window.is_internal())
        return;
    conn.async_window_rect_changed(conn.window_id(), window.client_id(), window.window_id(), window.rect());
}

void WindowManager::tell_wm_about_window_icon(WMConnectionFromClient& conn, Window& window)
{
    if (conn.window_id() < 0)
        return;
    if (!(conn.event_mask() & WMEventMask::WindowIconChanges))
        return;
    if (window.is_internal())
        return;
    conn.async_window_icon_bitmap_changed(conn.window_id(), window.client_id(), window.window_id(), window.icon().to_shareable_bitmap());
}

void WindowManager::tell_wm_about_current_window_stack(WMConnectionFromClient& conn)
{
    if (conn.window_id() < 0)
        return;
    if (!(conn.event_mask() & WMEventMask::WorkspaceChanges))
        return;
    auto& window_stack = current_window_stack();
    conn.async_workspace_changed(conn.window_id(), window_stack.row(), window_stack.column());
}

void WindowManager::tell_wms_window_state_changed(Window& window)
{
    for_each_window_manager([&](WMConnectionFromClient& conn) {
        tell_wm_about_window(conn, window);
        return IterationDecision::Continue;
    });
}

void WindowManager::tell_wms_window_icon_changed(Window& window)
{
    for_each_window_manager([&](WMConnectionFromClient& conn) {
        tell_wm_about_window_icon(conn, window);
        return IterationDecision::Continue;
    });
}

void WindowManager::tell_wms_window_rect_changed(Window& window)
{
    for_each_window_manager([&](WMConnectionFromClient& conn) {
        tell_wm_about_window_rect(conn, window);
        return IterationDecision::Continue;
    });
}

void WindowManager::tell_wms_screen_rects_changed()
{
    ConnectionFromClient::for_each_client([&](ConnectionFromClient& client) {
        client.notify_about_new_screen_rects();
    });
}

void WindowManager::tell_wms_applet_area_size_changed(Gfx::IntSize size)
{
    for_each_window_manager([&](WMConnectionFromClient& conn) {
        if (conn.window_id() < 0)
            return IterationDecision::Continue;

        conn.async_applet_area_size_changed(conn.window_id(), size);
        return IterationDecision::Continue;
    });
}

void WindowManager::tell_wms_super_key_pressed()
{
    for_each_window_manager([](WMConnectionFromClient& conn) {
        if (conn.window_id() < 0)
            return IterationDecision::Continue;

        conn.async_super_key_pressed(conn.window_id());
        return IterationDecision::Continue;
    });
}

void WindowManager::tell_wms_super_space_key_pressed()
{
    for_each_window_manager([](WMConnectionFromClient& conn) {
        if (conn.window_id() < 0)
            return IterationDecision::Continue;

        conn.async_super_space_key_pressed(conn.window_id());
        return IterationDecision::Continue;
    });
}

void WindowManager::tell_wms_super_d_key_pressed()
{
    for_each_window_manager([](WMConnectionFromClient& conn) {
        if (conn.window_id() < 0)
            return IterationDecision::Continue;

        conn.async_super_d_key_pressed(conn.window_id());
        return IterationDecision::Continue;
    });
}

void WindowManager::tell_wms_super_digit_key_pressed(u8 digit)
{
    for_each_window_manager([digit](WMConnectionFromClient& conn) {
        if (conn.window_id() < 0)
            return IterationDecision::Continue;

        conn.async_super_digit_key_pressed(conn.window_id(), digit);
        return IterationDecision::Continue;
    });
}

void WindowManager::tell_wms_current_window_stack_changed()
{
    for_each_window_manager([&](WMConnectionFromClient& conn) {
        tell_wm_about_current_window_stack(conn);
        return IterationDecision::Continue;
    });
}

static bool window_type_has_title(WindowType type)
{
    return type == WindowType::Normal;
}

void WindowManager::notify_modified_changed(Window& window)
{
    if (m_switcher->is_visible())
        m_switcher->refresh();

    tell_wms_window_state_changed(window);
}

void WindowManager::notify_title_changed(Window& window)
{
    if (!window_type_has_title(window.type()))
        return;

    dbgln_if(WINDOWMANAGER_DEBUG, "[WM] Window({}) title set to '{}'", &window, window.title());

    if (m_switcher->is_visible())
        m_switcher->refresh();

    tell_wms_window_state_changed(window);
}

void WindowManager::notify_rect_changed(Window& window, Gfx::IntRect const& old_rect, Gfx::IntRect const& new_rect)
{
    dbgln_if(RESIZE_DEBUG, "[WM] Window({}) rect changed {} -> {}", &window, old_rect, new_rect);

    if (m_switcher->is_visible() && window.type() != WindowType::WindowSwitcher)
        m_switcher->refresh();

    tell_wms_window_rect_changed(window);

    if (window.type() == WindowType::Applet)
        AppletManager::the().relayout();

    reevaluate_hover_state_for_window(&window);
}

void WindowManager::notify_opacity_changed(Window&)
{
    Compositor::the().invalidate_occlusions();
}

void WindowManager::notify_minimization_state_changed(Window& window)
{
    tell_wms_window_state_changed(window);

    if (window.client())
        window.client()->async_window_state_changed(window.window_id(), window.is_minimized(), window.is_maximized(), window.is_occluded());

    if (window.is_active() && window.is_minimized())
        pick_new_active_window(&window);
}

void WindowManager::notify_occlusion_state_changed(Window& window)
{
    if (window.client())
        window.client()->async_window_state_changed(window.window_id(), window.is_minimized(), window.is_maximized(), window.is_occluded());
}

void WindowManager::notify_progress_changed(Window& window)
{
    tell_wms_window_state_changed(window);
}

void WindowManager::pick_new_active_window(Window* previous_active)
{
    Window* desktop = nullptr;
    auto result = for_each_visible_window_from_front_to_back([&](Window& candidate) {
        if (candidate.type() == WindowType::Desktop)
            desktop = &candidate;
        if (candidate.type() != WindowType::Normal)
            return IterationDecision::Continue;
        if (candidate.is_destroyed())
            return IterationDecision::Continue;
        if (!previous_active || previous_active != &candidate) {
            set_active_window(&candidate);
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    if (result != IterationDecision::Break)
        set_active_window(desktop);
}

void WindowManager::check_hide_geometry_overlay(Window& window)
{
    if (&window == m_move_window.ptr() || &window == m_resize_window.ptr())
        m_geometry_overlay = nullptr;
}

void WindowManager::start_window_move(Window& window, Gfx::IntPoint origin)
{
    MenuManager::the().close_everyone();

    dbgln_if(MOVE_DEBUG, "[WM] Begin moving Window({})", &window);

    move_to_front_and_make_active(window);
    m_move_window = window;
    m_move_window->set_default_positioned(false);
    m_move_origin = origin;
    m_move_window_origin = window.position();
    m_move_window_cursor_position = window.is_tiled() ? to_floating_cursor_position(m_mouse_down_origin) : m_mouse_down_origin;
    if (system_effects().geometry() == ShowGeometry::OnMoveAndResize || system_effects().geometry() == ShowGeometry::OnMoveOnly) {
        m_geometry_overlay = Compositor::the().create_overlay<WindowGeometryOverlay>(window);
        m_geometry_overlay->set_enabled(true);
    }
    window.invalidate(true, true);
}

void WindowManager::start_window_move(Window& window, MouseEvent const& event)
{
    start_window_move(window, event.position());
}

void WindowManager::start_window_resize(Window& window, Gfx::IntPoint position, MouseButton button, ResizeDirection resize_direction)
{
    MenuManager::the().close_everyone();

    move_to_front_and_make_active(window);
    m_resize_direction = resize_direction;
    if (m_resize_direction == ResizeDirection::None) {
        VERIFY(!m_resize_window);
        return;
    }

    dbgln_if(RESIZE_DEBUG, "[WM] Begin resizing Window({})", &window);

    m_resizing_mouse_button = button;
    m_resize_window = window;
    m_resize_origin = position;
    m_resize_window_original_rect = window.rect();
    if (system_effects().geometry() == ShowGeometry::OnMoveAndResize || system_effects().geometry() == ShowGeometry::OnResizeOnly) {
        m_geometry_overlay = Compositor::the().create_overlay<WindowGeometryOverlay>(window);
        m_geometry_overlay->set_enabled(true);
    }

    set_automatic_cursor_tracking_window(nullptr);

    window.invalidate(true, true);
}

void WindowManager::start_window_resize(Window& window, MouseEvent const& event, ResizeDirection resize_direction)
{
    start_window_resize(window, event.position(), event.button(), resize_direction);
}

void WindowManager::start_tile_window_animation(Gfx::IntRect const& starting_rect)
{
    m_tile_window_overlay_animation = Animation::create();
    m_tile_window_overlay_animation->set_duration(150);
    m_tile_window_overlay_animation->on_update = [this, starting_rect](float progress, Gfx::Painter&, Screen&, Gfx::DisjointIntRectSet&) {
        if (m_tile_window_overlay) {
            auto target_rect = starting_rect.interpolated_to(m_tile_window_overlay->tiled_frame_rect(), progress);
            m_tile_window_overlay->set_overlay_rect(target_rect);
        }
    };
    m_tile_window_overlay_animation->start();
}

void WindowManager::stop_tile_window_animation()
{
    if (m_tile_window_overlay) {
        if (m_geometry_overlay)
            m_geometry_overlay->start_or_stop_move_to_tile_overlay_animation(nullptr);
        m_tile_window_overlay = nullptr;
    }
    m_tile_window_overlay_animation = nullptr;
}

void WindowManager::on_add_to_quick_launch(pid_t pid)
{
    for_each_window_manager([&](WMConnectionFromClient& conn) {
        conn.async_add_to_quick_launch(conn.window_id(), pid);
        return IterationDecision::Continue;
    });
}

void WindowManager::show_tile_window_overlay(Window& window, Screen const& cursor_screen, WindowTileType tile_type)
{
    m_move_window_suggested_tile = tile_type;
    if (tile_type != WindowTileType::None && (!m_tile_window_overlay || !m_tile_window_overlay->is_window(window))) {
        auto tiled_frame_rect = WindowFrame::frame_rect_for_window(window, tiled_window_rect(window, cursor_screen, tile_type));
        m_tile_window_overlay = Compositor::the().create_overlay<TileWindowOverlay>(window, tiled_frame_rect, palette());
        m_tile_window_overlay->set_enabled(true);
        start_tile_window_animation(window.frame().rect());
    } else if (tile_type == WindowTileType::None) {
        stop_tile_window_animation();
    } else {
        auto tiled_frame_rect = WindowFrame::frame_rect_for_window(window, tiled_window_rect(window, cursor_screen, tile_type));
        if (m_tile_window_overlay->tiled_frame_rect() != tiled_frame_rect) {
            m_tile_window_overlay->set_tiled_frame_rect(tiled_frame_rect);
            start_tile_window_animation(m_tile_window_overlay->rect());
            if (m_geometry_overlay)
                m_geometry_overlay->window_rect_changed();
        }
    }
}
TileWindowOverlay* WindowManager::get_tile_window_overlay(Window& window) const
{
    if (m_tile_window_overlay && m_tile_window_overlay->is_window(window))
        return m_tile_window_overlay.ptr();
    return nullptr;
}

bool WindowManager::process_ongoing_window_move(MouseEvent& event)
{
    if (!m_move_window)
        return false;
    if (event.type() == Event::MouseUp && event.button() == MouseButton::Primary) {

        dbgln_if(MOVE_DEBUG, "[WM] Finish moving Window({})", m_move_window);

        bool did_tile = false;
        if (m_move_window_suggested_tile != WindowTileType::None && m_system_effects.tile_window() == TileWindow::ShowTileOverlay) {
            auto& cursor_screen = Screen::closest_to_location(event.position());
            m_move_window->set_tiled(m_move_window_suggested_tile, cursor_screen);
            m_move_window_suggested_tile = WindowTileType::None;
            stop_tile_window_animation();
            did_tile = true;
        } else {
            did_tile = m_move_window->is_tiled();
        }

        if (!did_tile)
            m_move_window->set_floating_rect(m_move_window->rect());

        m_move_window->send_move_event_to_client();
        m_move_window->invalidate(true, true);
        if (m_move_window->is_resizable()) {
            process_event_for_doubleclick(*m_move_window, event);
            if (event.type() == Event::MouseDoubleClick) {
                dbgln_if(DOUBLECLICK_DEBUG, "[WM] Click up became doubleclick!");
                m_move_window->set_maximized(!m_move_window->is_maximized());
            }
        }
        m_move_window = nullptr;
        m_geometry_overlay = nullptr;
        return true;
    }
    if (event.type() == Event::MouseMove) {
        if constexpr (MOVE_DEBUG) {
            dbgln("[WM] Moving, origin: {}, now: {}", m_move_origin, event.position());
            if (m_move_window->is_maximized())
                dbgln("  [!] The window is still maximized. Not moving yet.");
        }

        int const tiling_deadzone = 10;
        int const secondary_deadzone = 2;
        auto& cursor_screen = Screen::closest_to_location(event.position());
        auto desktop = desktop_rect(cursor_screen);
        auto desktop_relative_to_screen = desktop.translated(-cursor_screen.rect().location());
        if (m_move_window->is_maximized()) {
            auto pixels_moved_from_start = event.position().pixels_moved(m_move_origin);
            if (pixels_moved_from_start > 5) {
                dbgln_if(MOVE_DEBUG, "[WM] de-maximizing window");
                m_move_origin = event.position();
                if (m_move_origin.y() <= secondary_deadzone + desktop.top())
                    return true;
                Gfx::IntPoint adjusted_position = event.position().translated(-m_move_window_cursor_position);
                m_move_window->set_maximized(false);
                m_move_window->move_to(adjusted_position);
                m_move_window_origin = m_move_window->position();
            }
        } else {
            bool has_fixed_aspect_ratio = m_move_window->resize_aspect_ratio().has_value();
            bool is_resizable = m_move_window->is_resizable();
            auto tile_window = m_system_effects.tile_window();
            bool allow_tile = !has_fixed_aspect_ratio && is_resizable && tile_window != TileWindow::Never;
            auto pixels_moved_from_start = event.position().pixels_moved(m_move_origin);

            auto apply_window_tile = [&](WindowTileType tile_type) {
                if (tile_window == TileWindow::ShowTileOverlay) {
                    show_tile_window_overlay(*m_move_window, cursor_screen, tile_type);
                } else if (tile_window == TileWindow::TileImmediately) {
                    if (tile_type != WindowTileType::None) {
                        m_move_window->set_tiled(tile_type, cursor_screen);
                        return;
                    }
                }
                auto pos = m_move_window_origin.translated(event.position() - m_move_origin);
                m_move_window->set_position_without_repaint(pos);
            };

            auto event_location_relative_to_screen = event.position().translated(-cursor_screen.rect().location());
            if (allow_tile && event_location_relative_to_screen.x() <= tiling_deadzone) {
                if (event_location_relative_to_screen.y() <= tiling_deadzone + desktop_relative_to_screen.top())
                    apply_window_tile(WindowTileType::TopLeft);
                else if (event_location_relative_to_screen.y() >= desktop_relative_to_screen.height() - tiling_deadzone)
                    apply_window_tile(WindowTileType::BottomLeft);
                else
                    apply_window_tile(WindowTileType::Left);
            } else if (allow_tile && event_location_relative_to_screen.x() >= cursor_screen.width() - tiling_deadzone) {
                if (event_location_relative_to_screen.y() <= tiling_deadzone + desktop.top())
                    apply_window_tile(WindowTileType::TopRight);
                else if (event_location_relative_to_screen.y() >= desktop_relative_to_screen.height() - tiling_deadzone)
                    apply_window_tile(WindowTileType::BottomRight);
                else
                    apply_window_tile(WindowTileType::Right);
            } else if (allow_tile && event_location_relative_to_screen.y() <= secondary_deadzone + desktop_relative_to_screen.top()) {
                apply_window_tile(WindowTileType::Top);
            } else if (allow_tile && event_location_relative_to_screen.y() >= desktop_relative_to_screen.bottom() - 1 - secondary_deadzone) {
                apply_window_tile(WindowTileType::Bottom);
            } else if (!m_move_window->is_tiled()) {
                apply_window_tile(WindowTileType::None);
            } else if (pixels_moved_from_start > 5) {
                Gfx::IntPoint adjusted_position = event.position().translated(-m_move_window_cursor_position);
                m_move_window->set_untiled();
                m_move_window->move_to(adjusted_position);
                m_move_origin = event.position();
                m_move_window_origin = m_move_window->position();
            }
        }
        if (system_effects().geometry() == ShowGeometry::OnMoveAndResize || system_effects().geometry() == ShowGeometry::OnMoveOnly) {
            m_geometry_overlay->window_rect_changed();
        }
    }
    m_move_window->send_move_event_to_client();
    return true;
}

Gfx::IntPoint WindowManager::to_floating_cursor_position(Gfx::IntPoint origin) const
{
    VERIFY(m_move_window);

    Gfx::IntPoint new_position;
    auto dist_from_right = m_move_window->rect().width() - origin.x();
    auto dist_from_bottom = m_move_window->rect().height() - origin.y();
    auto floating_width = m_move_window->floating_rect().width();
    auto floating_height = m_move_window->floating_rect().height();

    if (origin.x() < dist_from_right && origin.x() < floating_width / 2)
        new_position.set_x(origin.x());
    else if (dist_from_right < origin.x() && dist_from_right < floating_width / 2)
        new_position.set_x(floating_width - dist_from_right);
    else
        new_position.set_x(floating_width / 2);

    if (origin.y() < dist_from_bottom && origin.y() < floating_height / 2)
        new_position.set_y(origin.y());
    else if (dist_from_bottom < origin.y() && dist_from_bottom < floating_height / 2)
        new_position.set_y(floating_height - dist_from_bottom);
    else
        new_position.set_y(floating_height / 2);

    return new_position;
}

bool WindowManager::process_ongoing_window_resize(MouseEvent const& event)
{
    if (!m_resize_window)
        return false;

    // Deliver MouseDoubleClick events to the frame
    if (event.type() == Event::MouseUp) {
        auto& frame = m_resize_window->frame();
        auto frame_event = event.translated(-frame.rect().location());
        process_event_for_doubleclick(*m_resize_window, frame_event);
        if (frame_event.type() == Event::MouseDoubleClick)
            frame.handle_mouse_event(frame_event);
    }

    if (event.type() == Event::MouseUp && event.button() == m_resizing_mouse_button) {
        dbgln_if(RESIZE_DEBUG, "[WM] Finish resizing Window({})", m_resize_window);

        if (!m_resize_window->is_tiled())
            m_resize_window->set_floating_rect(m_resize_window->rect());

        m_resize_window->send_resize_event_to_client();
        m_resize_window->invalidate(true, true);
        m_resize_window = nullptr;
        m_geometry_overlay = nullptr;
        m_resizing_mouse_button = MouseButton::None;
        return true;
    }

    if (event.type() != Event::MouseMove)
        return true;

    auto& cursor_screen = ScreenInput::the().cursor_location_screen();
    auto& closest_screen = Screen::closest_to_rect(m_resize_window->rect());
    if (&cursor_screen == &closest_screen) {
        constexpr auto hot_zone = 10;
        Gfx::IntRect tiling_rect = desktop_rect(cursor_screen).shrunken({ hot_zone, hot_zone });
        if ((m_resize_direction == ResizeDirection::Up || m_resize_direction == ResizeDirection::Down)
            && (event.y() >= tiling_rect.bottom() - 1 || event.y() <= tiling_rect.top())) {
            m_resize_window->set_tiled(WindowTileType::VerticallyMaximized);
            return true;
        }
        if ((m_resize_direction == ResizeDirection::Left || m_resize_direction == ResizeDirection::Right)
            && (event.x() > tiling_rect.right() || event.x() <= tiling_rect.left())) {
            m_resize_window->set_tiled(WindowTileType::HorizontallyMaximized);
            return true;
        }
    }

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

    if (!m_resize_window->size_increment().is_empty()) {
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

    if (m_resize_window->is_tiled()) {
        // Check if we should be un-tiling the window. This should happen when one side touching
        // the screen border changes. We need to un-tile because while it is tiled, rendering is
        // constrained to the screen where it's tiled on, and if one of these sides move we should
        // no longer constrain rendering to that screen. Changing the sides not touching a screen
        // border however is fine as long as the screen contains the entire window.
        m_resize_window->check_untile_due_to_resize(new_rect);
    }
    dbgln_if(RESIZE_DEBUG, "[WM] Resizing, original: {}, now: {}", m_resize_window_original_rect, new_rect);

    if (m_resize_window->rect().location() != m_resize_window_original_rect.location()) {
        m_resize_window->set_default_positioned(false);
    }

    m_resize_window->set_rect(new_rect);
    if (system_effects().geometry() == ShowGeometry::OnMoveAndResize || system_effects().geometry() == ShowGeometry::OnResizeOnly) {
        m_geometry_overlay->window_rect_changed();
    }
    m_resize_window->send_resize_event_to_client();
    return true;
}

bool WindowManager::process_ongoing_drag(MouseEvent& event)
{
    if (!m_dnd_client)
        return false;

    auto send_dnd_event = [&](auto callback) {
        auto* window = hovered_window();
        if (!window || !window->client())
            return false;

        auto translated_event = event.translated(-window->position());
        auto mime_data = m_dnd_mime_data->all_data().clone();

        // If the mime data is so large that it causes memory troubles, we should silently drop the drag'n'drop request entirely.
        if (mime_data.is_error()) {
            dbgln("Drag and drop mimetype data nearly caused OOM and was dropped: {}", mime_data.release_error());
            return false;
        }

        callback(*window, translated_event.position(), mime_data.release_value());
        return true;
    };

    if (event.type() == Event::MouseMove) {
        m_dnd_overlay->cursor_moved();

        // We didn't let go of the drag yet, see if we should send some drag move events..
        auto event_sent = send_dnd_event([&](auto& window, auto event_position, auto mime_data) {
            window.client()->async_drag_moved(window.window_id(), event_position, event.button(), event.buttons(), event.modifiers(), m_dnd_text, move(mime_data));
        });

        if (!event_sent)
            set_accepts_drag(false);
    }

    if (event.type() == Event::MouseUp && event.button() == MouseButton::Primary) {
        auto event_sent = send_dnd_event([&](auto& window, auto event_position, auto mime_data) {
            m_dnd_client->async_drag_accepted();
            window.client()->async_drag_dropped(window.window_id(), event_position, event.button(), event.buttons(), event.modifiers(), m_dnd_text, move(mime_data));
        });

        if (!event_sent)
            m_dnd_client->async_drag_cancelled();

        end_dnd_drag();
    }

    return true;
}

void WindowManager::set_cursor_tracking_button(Button* button)
{
    m_cursor_tracking_button = button;
}

auto WindowManager::DoubleClickInfo::metadata_for_button(MouseButton button) const -> ClickMetadata const&
{
    switch (button) {
    case MouseButton::Primary:
        return m_primary;
    case MouseButton::Secondary:
        return m_secondary;
    case MouseButton::Middle:
        return m_middle;
    case MouseButton::Backward:
        return m_backward;
    case MouseButton::Forward:
        return m_forward;
    default:
        VERIFY_NOT_REACHED();
    }
}

auto WindowManager::DoubleClickInfo::metadata_for_button(MouseButton button) -> ClickMetadata&
{
    switch (button) {
    case MouseButton::Primary:
        return m_primary;
    case MouseButton::Secondary:
        return m_secondary;
    case MouseButton::Middle:
        return m_middle;
    case MouseButton::Backward:
        return m_backward;
    case MouseButton::Forward:
        return m_forward;
    default:
        VERIFY_NOT_REACHED();
    }
}

bool WindowManager::is_considered_doubleclick(MouseEvent const& event, DoubleClickInfo::ClickMetadata const& metadata) const
{
    i64 elapsed_ms_since_last_click = metadata.clock.elapsed();
    if (elapsed_ms_since_last_click < m_double_click_speed) {
        auto diff = event.position() - metadata.last_position;
        auto distance_travelled_squared = diff.x() * diff.x() + diff.y() * diff.y();
        if (distance_travelled_squared <= (m_max_distance_for_double_click * m_max_distance_for_double_click))
            return true;
    }
    return false;
}

void WindowManager::system_menu_doubleclick(Window& window, MouseEvent const& event)
{
    // This is a special case. Basically, we're trying to determine whether
    // double clicking on the window menu icon happened. In this case, the
    // WindowFrame only receives a MouseDown event, and since the window
    // menu pops up, it does not see the MouseUp event. But, if they subsequently
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

        event = MouseEvent(Event::MouseDoubleClick, event.position(), event.buttons(), event.button(), event.modifiers(), event.wheel_delta_x(), event.wheel_delta_y());
        // invalidate this now we've delivered a doubleclick, otherwise
        // tripleclick will deliver two doubleclick events (incorrectly).
        metadata.clock = {};
    }

    metadata.last_position = event.position();
}

void WindowManager::deliver_mouse_event(Window& window, MouseEvent const& event)
{
    auto translated_event = event.translated(-window.position());
    window.dispatch_event(translated_event);
    if (translated_event.type() == Event::MouseUp) {
        process_event_for_doubleclick(window, translated_event);
        if (translated_event.type() == Event::MouseDoubleClick)
            window.dispatch_event(translated_event);
    }
}

bool WindowManager::process_ongoing_active_input_mouse_event(MouseEvent const& event)
{
    auto* input_tracking_window = automatic_cursor_tracking_window();
    if (!input_tracking_window)
        return false;

    // At this point, we have delivered the start of an input sequence to a
    // client application. We must keep delivering to that client
    // application until the input sequence is done.
    //
    // This prevents e.g. moving on one window out of the bounds starting
    // a move in that other unrelated window, and other silly shenanigans.
    deliver_mouse_event(*input_tracking_window, event);

    if (event.type() == Event::MouseUp && event.buttons() == 0)
        set_automatic_cursor_tracking_window(nullptr);

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
    auto* blocking_modal_window = window.blocking_modal_window();

    if (event.type() == Event::MouseDown) {
        m_mouse_down_origin = result.is_frame_hit
            ? event.position().translated(-window.position())
            : result.window_relative_position;
    }

    // First check if we should initiate a move or resize (Super+LMB or Super+RMB).
    // In those cases, the event is swallowed by the window manager.
    if (!blocking_modal_window && window.is_movable()) {
        if (!window.is_fullscreen() && m_keyboard_modifiers == Mod_Super && event.type() == Event::MouseDown && event.button() == MouseButton::Primary) {
            start_window_move(window, event);
            return;
        }
        if (window.is_resizable() && m_keyboard_modifiers == Mod_Super && event.type() == Event::MouseDown && event.button() == MouseButton::Secondary && !window.blocking_modal_window()) {
            constexpr ResizeDirection direction_for_hot_area[3][3] = {
                { ResizeDirection::UpLeft, ResizeDirection::Up, ResizeDirection::UpRight },
                { ResizeDirection::Left, ResizeDirection::None, ResizeDirection::Right },
                { ResizeDirection::DownLeft, ResizeDirection::Down, ResizeDirection::DownRight },
            };
            Gfx::IntRect outer_rect = window.frame().rect();
            if (!outer_rect.contains(event.position())) {
                // FIXME: This used to be an VERIFY but crashing WindowServer over this seems silly.
                dbgln("FIXME: !outer_rect.contains(position): outer_rect={}, position={}", outer_rect, event.position());
            }
            int window_relative_x = event.position().x() - outer_rect.x();
            int window_relative_y = event.position().y() - outer_rect.y();
            int hot_area_row = min(2, window_relative_y / (outer_rect.height() / 3));
            int hot_area_column = min(2, window_relative_x / (outer_rect.width() / 3));
            ResizeDirection resize_direction = direction_for_hot_area[hot_area_row][hot_area_column];
            start_window_resize(window, event, resize_direction);
            return;
        }
    }

    if (event.type() == Event::MouseDown) {
        if (window.type() == WindowType::Normal)
            move_to_front_and_make_active(window);
        else if (window.type() == WindowType::Desktop)
            set_active_window(&window);
    }

    if (blocking_modal_window && window.type() != WindowType::Popup) {
        if (event.type() == Event::Type::MouseDown) {
            // We're clicking on something that's blocked by a modal window.
            // Flash the modal window to let the user know about it.
            blocking_modal_window->frame().start_flash_animation();
        }
        // Don't send mouse events to windows blocked by a modal child.
        return;
    }

    if (result.is_frame_hit) {
        // We are hitting the frame, pass the event along to WindowFrame.
        window.frame().handle_mouse_event(event.translated(-window.frame().rect().location()));
        return;
    }

    if (!window.is_automatic_cursor_tracking())
        deliver_mouse_event(window, event);

    if (event.type() == Event::MouseDown)
        set_automatic_cursor_tracking_window(&window);
}

void WindowManager::process_mouse_event(MouseEvent& event)
{
    // 0. Forget the resize candidate (window that we could initiate a resize of from the current cursor position.)
    //    A new resize candidate may be determined if we hit an appropriate part of a window.
    clear_resize_candidate();

    // 1. Process ongoing drag events. This is done first to avoid clashing with global cursor tracking.
    if (process_ongoing_drag(event))
        return;

    // 2. Send the mouse event to all clients with global cursor tracking enabled.
    ConnectionFromClient::for_each_client([&](ConnectionFromClient& conn) {
        if (conn.does_global_mouse_tracking()) {
            conn.async_track_mouse_move(event.position());
        }
    });
    // The automatic cursor tracking window is excluded here because we're sending the event to it
    // in the next step.
    for_each_visible_window_from_front_to_back([&](Window& window) {
        if (window.is_automatic_cursor_tracking() && &window != automatic_cursor_tracking_window())
            deliver_mouse_event(window, event);
        return IterationDecision::Continue;
    });

    // 3. If there's an automatic cursor tracking window, all mouse events go there.
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
    auto result = current_window_stack().hit_test(event.position());

    if (!result.has_value())
        return;

    auto* event_window = result.value().window.ptr();
    if (auto* popup_window = foremost_popup_window()) {
        if (event_window == popup_window)
            process_mouse_event_for_window(result.value(), event);
        else if (event.buttons())
            popup_window->request_close();
        return;
    }

    process_mouse_event_for_window(result.value(), event);
}

void WindowManager::reevaluate_hover_state_for_window(Window* updated_window)
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
        hovered_window = current_window_stack().window_at(cursor_location);
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

Gfx::IntRect WindowManager::desktop_rect(Screen const& screen) const
{
    if (active_fullscreen_window())
        return Screen::main().rect(); // TODO: we should support fullscreen windows on any screen
    auto screen_rect = screen.rect();
    if (screen.is_main_screen())
        screen_rect.set_height(screen.height() - TaskbarWindow::taskbar_height());
    return screen_rect;
}

Gfx::IntRect WindowManager::arena_rect_for_type(Screen& screen, WindowType type) const
{
    switch (type) {
    case WindowType::Desktop:
        return Screen::bounding_rect();
    case WindowType::Normal:
        return desktop_rect(screen);
    case WindowType::Menu:
    case WindowType::WindowSwitcher:
    case WindowType::Taskbar:
    case WindowType::Tooltip:
    case WindowType::Applet:
    case WindowType::Notification:
    case WindowType::Popup:
    case WindowType::Autocomplete:
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
        m_last_processed_buttons = mouse_event.buttons();
        auto include_window_frame = m_dnd_client ? WindowStack::IncludeWindowFrame::Yes : WindowStack::IncludeWindowFrame::No;
        // TODO: handle transitioning between two stacks
        set_hovered_window(current_window_stack().window_at(mouse_event.position(), include_window_frame));
        return;
    }

    if (static_cast<Event&>(event).is_key_event()) {
        process_key_event(static_cast<KeyEvent&>(event));
        return;
    }

    Core::EventReceiver::event(event);
}

bool WindowManager::is_window_in_modal_chain(Window& chain_window, Window& other_window)
{
    auto result = for_each_window_in_modal_chain(chain_window, [&](auto& window) {
        if (&other_window == &window)
            return IterationDecision::Break;
        return IterationDecision::Continue;
    });
    return result;
}

void WindowManager::switch_to_window_stack(WindowStack& window_stack, Window* carry_window, bool show_overlay)
{
    m_carry_window_to_new_stack.clear();
    m_switching_to_window_stack = &window_stack;
    if (carry_window && !is_stationary_window_type(carry_window->type()) && &carry_window->window_stack() != &window_stack) {
        auto& from_stack = carry_window->window_stack();

        for_each_visible_window_from_back_to_front([&](Window& window) {
            if (is_stationary_window_type(window.type()))
                return IterationDecision::Continue;
            if (&window.window_stack() != &carry_window->window_stack())
                return IterationDecision::Continue;
            if (&window == carry_window || is_window_in_modal_chain(*carry_window, window))
                m_carry_window_to_new_stack.append(window);
            return IterationDecision::Continue;
        },
            &from_stack);

        auto* from_active_window = from_stack.active_window();
        bool did_carry_active_window = false;
        for (auto& window : m_carry_window_to_new_stack) {
            if (window == from_active_window)
                did_carry_active_window = true;
            window->set_moving_to_another_stack(true);
            VERIFY(&window->window_stack() == &from_stack);
            from_stack.remove(*window);
            window_stack.add(*window);
        }
        // Before we change to the new stack, find a new active window on the stack we're switching from
        if (did_carry_active_window)
            pick_new_active_window(from_active_window);

        // Now switch to the new stack
        m_current_window_stack = &window_stack;
        if (did_carry_active_window && from_active_window)
            set_active_window(from_active_window);

        // Because we moved windows between stacks we need to invalidate occlusions
        Compositor::the().invalidate_occlusions();
    } else {
        m_current_window_stack = &window_stack;
    }

    Compositor::the().switch_to_window_stack(window_stack, show_overlay);
}

void WindowManager::did_switch_window_stack(Badge<Compositor>, WindowStack& previous_stack, WindowStack& new_stack)
{
    VERIFY(&previous_stack != &new_stack);

    // We are being notified by the compositor, it should not be switching right now!
    VERIFY(!Compositor::the().is_switching_window_stacks());

    if (m_switching_to_window_stack == &new_stack) {
        m_switching_to_window_stack = nullptr;
        if (!m_carry_window_to_new_stack.is_empty()) {
            // switched_to_stack may be different from the stack where the windows were
            // carried to when the user rapidly tries to switch stacks, so make sure to
            // only reset the moving flag if we arrived at our final destination
            for (auto& window_ref : m_carry_window_to_new_stack) {
                if (auto* window = window_ref.ptr()) {
                    window->set_moving_to_another_stack(false);
                    tell_wms_window_state_changed(*window);
                }
            }
            m_carry_window_to_new_stack.clear();
        }
    }

    auto* previous_stack_active_window = previous_stack.active_window();
    auto* new_stack_active_window = new_stack.active_window();
    if (previous_stack_active_window != new_stack_active_window) {
        if (previous_stack_active_window && is_stationary_window_type(previous_stack_active_window->type()))
            notify_previous_active_window(*previous_stack_active_window);
        if (new_stack_active_window && is_stationary_window_type(new_stack_active_window->type()))
            notify_new_active_window(*new_stack_active_window);
    }

    if (!new_stack_active_window)
        pick_new_active_window(nullptr);

    reevaluate_hover_state_for_window();

    tell_wms_current_window_stack_changed();
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

    if (event.type() == Event::KeyDown && event.key() == Key_LeftSuper) {
        m_previous_event_was_super_keydown = true;
    } else if (m_previous_event_was_super_keydown) {
        m_previous_event_was_super_keydown = false;
        if (!m_dnd_client && !automatic_cursor_tracking_window() && event.type() == Event::KeyUp && event.key() == Key_LeftSuper) {
            tell_wms_super_key_pressed();
            return;
        }

        if (event.type() == Event::KeyDown && event.key() == Key_Space) {
            tell_wms_super_space_key_pressed();
            return;
        }

        if (event.type() == Event::KeyDown && event.key() == Key_D) {
            tell_wms_super_d_key_pressed();
            return;
        }

        if (event.type() == Event::KeyDown && event.key() >= Key_0 && event.key() <= Key_9) {
            auto digit = event.key() - Key_0;
            tell_wms_super_digit_key_pressed(digit);
            return;
        }
    }

    if (MenuManager::the().current_menu() && event.key() != Key_LeftSuper) {
        MenuManager::the().dispatch_event(event);
        return;
    }

    if (event.type() == Event::KeyDown) {
        if ((event.modifiers() == Mod_Super && event.key() == Key_Tab) || (event.modifiers() == (Mod_Super | Mod_Shift) && event.key() == Key_Tab))
            m_switcher->show(WindowSwitcher::Mode::ShowAllWindows);
        else if ((event.modifiers() == Mod_Alt && event.key() == Key_Tab) || (event.modifiers() == (Mod_Alt | Mod_Shift) && event.key() == Key_Tab))
            m_switcher->show(WindowSwitcher::Mode::ShowCurrentDesktop);
    }
    if (m_switcher->is_visible()) {
        request_close_fragile_windows();
        m_switcher->on_key_event(event);
        return;
    }

    if (event.type() == Event::KeyDown && (event.modifiers() == (Mod_Alt | Mod_Shift) && (event.key() == Key_LeftShift || event.key() == Key_LeftAlt))) {
        m_keymap_switcher->next_keymap();
        return;
    }

    if (event.type() == Event::KeyDown && (event.modifiers() == (Mod_Ctrl | Mod_Alt) || event.modifiers() == (Mod_Ctrl | Mod_Shift | Mod_Alt)) && (window_stack_columns() > 1 || window_stack_rows() > 1)) {
        auto& current_stack = current_window_stack();
        auto row = current_stack.row();
        auto column = current_stack.column();
        auto handle_window_stack_switch_key = [&]() {
            switch (event.key()) {
            case Key_Left:
                if (column == 0)
                    return true;
                column--;
                return true;
            case Key_Right:
                if (column + 1 >= m_window_stacks[0]->size())
                    return true;
                column++;
                return true;
            case Key_Up:
                if (row == 0)
                    return true;
                row--;
                return true;
            case Key_Down:
                if (row + 1 >= m_window_stacks.size())
                    return true;
                row++;
                return true;
            default:
                return false;
            }
        };
        if (handle_window_stack_switch_key()) {
            request_close_fragile_windows();
            Window* carry_window = nullptr;
            auto& new_window_stack = *(*m_window_stacks[row])[column];
            if (&new_window_stack != &current_stack) {
                if (event.modifiers() == (Mod_Ctrl | Mod_Shift | Mod_Alt))
                    carry_window = this->active_window();
            }
            // Call switch_to_window_stack even if we're not going to switch to another stack.
            // We'll show the window stack switch overlay briefly!
            switch_to_window_stack(new_window_stack, carry_window);
            return;
        }
    }

    auto* event_window = current_window_stack().active_window();
    if (auto* popup_window = foremost_popup_window())
        event_window = popup_window;
    if (!event_window)
        return;

    if (event.type() == Event::KeyDown && event.modifiers() == Mod_Super) {
        if (event.key() == Key_H) {
            m_cursor_highlight_enabled = !m_cursor_highlight_enabled;
            Compositor::the().invalidate_cursor();
            return;
        }
        if (event_window->type() != WindowType::Desktop) {
            if (event.key() == Key_Down) {
                if (event_window->is_resizable() && event_window->is_maximized()) {
                    maximize_windows(*event_window, false);
                    return;
                }
                if (event_window->is_minimizable() && !event_window->is_modal())
                    minimize_windows(*event_window, true);
                return;
            }
            if (event_window->is_resizable()) {
                if (event.key() == Key_Up) {
                    maximize_windows(*event_window, !event_window->is_maximized());
                    return;
                }
                if (event.key() == Key_Left) {
                    if (event_window->tile_type() == WindowTileType::Left) {
                        event_window->set_untiled();
                        return;
                    }
                    if (event_window->is_maximized())
                        maximize_windows(*event_window, false);
                    event_window->set_tiled(WindowTileType::Left);
                    return;
                }
                if (event.key() == Key_Right) {
                    if (event_window->tile_type() == WindowTileType::Right) {
                        event_window->set_untiled();
                        return;
                    }
                    if (event_window->is_maximized())
                        maximize_windows(*event_window, false);
                    event_window->set_tiled(WindowTileType::Right);
                    return;
                }
            }
        }
    }

    if (event.type() == Event::KeyDown && event.modifiers() == (Mod_Super | Mod_Alt) && event_window->type() != WindowType::Desktop) {
        if (event_window->is_resizable()) {
            if (event.key() == Key_Right || event.key() == Key_Left) {
                if (event_window->tile_type() == WindowTileType::HorizontallyMaximized) {
                    event_window->set_untiled();
                    return;
                }
                if (event_window->is_maximized())
                    maximize_windows(*event_window, false);
                event_window->set_tiled(WindowTileType::HorizontallyMaximized);
                return;
            }
            if (event.key() == Key_Up || event.key() == Key_Down) {
                if (event_window->tile_type() == WindowTileType::VerticallyMaximized) {
                    event_window->set_untiled();
                    return;
                }
                if (event_window->is_maximized())
                    maximize_windows(*event_window, false);
                event_window->set_tiled(WindowTileType::VerticallyMaximized);
                return;
            }
        }
    }

    event_window->dispatch_event(event);
}

void WindowManager::set_highlight_window(Window* new_highlight_window)
{
    // NOTE: The highlight window is global across all stacks. That's because we
    // can only have one and we want to be able to highlight it during transitions
    auto* previous_highlight_window = highlight_window();
    if (new_highlight_window == previous_highlight_window)
        return;
    if (!new_highlight_window)
        m_highlight_window = nullptr;
    else
        m_highlight_window = new_highlight_window->make_weak_ptr<Window>();

    if (previous_highlight_window) {
        reevaluate_hover_state_for_window(previous_highlight_window);
        previous_highlight_window->invalidate(true, true);
        Compositor::the().invalidate_screen(previous_highlight_window->frame().render_rect());
    }
    if (new_highlight_window) {
        reevaluate_hover_state_for_window(new_highlight_window);
        new_highlight_window->invalidate(true, true);
        Compositor::the().invalidate_screen(new_highlight_window->frame().render_rect());
    }

    if (active_fullscreen_window()) {
        // Find the Taskbar window and invalidate it so it draws correctly
        for_each_visible_window_from_back_to_front([](Window& window) {
            if (window.type() == WindowType::Taskbar) {
                window.invalidate();
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
    }

    // Invalidate occlusions in case the state change changes geometry
    Compositor::the().invalidate_occlusions();
}

static bool window_type_can_become_active(WindowType type)
{
    return type == WindowType::Normal || type == WindowType::Desktop;
}

void WindowManager::set_active_window(Window* new_active_window)
{
    if (new_active_window) {
        if (auto* blocker = new_active_window->blocking_modal_window()) {
            VERIFY(blocker->is_modal());
            VERIFY(blocker != new_active_window);
            new_active_window = blocker;
        }

        if (!window_type_can_become_active(new_active_window->type()))
            return;
    }

    auto& window_stack = current_window_stack();
    if (new_active_window == window_stack.active_window())
        return;

    if (auto* previously_active_window = window_stack.active_window()) {
        window_stack.set_active_window(nullptr);
        set_automatic_cursor_tracking_window(nullptr);
        notify_previous_active_window(*previously_active_window);
    }

    if (new_active_window) {
        request_close_fragile_windows();
        window_stack.set_active_window(new_active_window);
        notify_new_active_window(*new_active_window);
        reevaluate_hover_state_for_window(new_active_window);
    }

    // Window shapes may have changed (e.g. shadows for inactive/active windows)
    Compositor::the().invalidate_occlusions();
}

void WindowManager::notify_new_active_window(Window& new_active_window)
{
    Core::EventLoop::current().post_event(new_active_window, make<Event>(Event::WindowActivated));
    new_active_window.invalidate(true, true);
    tell_wms_window_state_changed(new_active_window);
}

void WindowManager::notify_previous_active_window(Window& previously_active_window)
{
    for (auto& child_window : previously_active_window.child_windows()) {
        if (child_window && child_window->type() == WindowType::Tooltip)
            child_window->request_close();
    }
    Core::EventLoop::current().post_event(previously_active_window, make<Event>(Event::WindowDeactivated));
    previously_active_window.invalidate(true, true);

    tell_wms_window_state_changed(previously_active_window);
}

void WindowManager::notify_active_window_input_preempted()
{
    if (active_window())
        Core::EventLoop::current().post_event(*active_window(), make<Event>(Event::WindowInputPreempted));
}

void WindowManager::notify_active_window_input_restored()
{
    if (active_window())
        Core::EventLoop::current().post_event(*active_window(), make<Event>(Event::WindowInputRestored));
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

ConnectionFromClient const* WindowManager::active_client() const
{
    if (auto* window = const_cast<WindowManager*>(this)->current_window_stack().active_window())
        return window->client();
    return nullptr;
}

Cursor const& WindowManager::active_cursor() const
{
    if (m_dnd_client) {
        if (m_dnd_accepts_drag)
            return *m_drag_copy_cursor;
        return *m_drag_cursor;
    }

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
        default:
            VERIFY_NOT_REACHED();
            break;
        }
    }

    if (m_automatic_cursor_tracking_window) {
        if (m_automatic_cursor_tracking_window->cursor())
            return *m_automatic_cursor_tracking_window->cursor();
    } else if (m_hovered_window) {
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

Gfx::IntRect WindowManager::tiled_window_rect(Window const& window, Optional<Screen const&> cursor_screen, WindowTileType tile_type) const
{
    VERIFY(tile_type != WindowTileType::None);

    auto const& screen = cursor_screen.has_value() ? cursor_screen.value() : Screen::closest_to_rect(window.frame().rect());
    auto rect = desktop_rect(screen);

    if (tile_type == WindowTileType::Maximized) {
        auto border_thickness = palette().window_border_thickness();
        rect.inflate(border_thickness * 2, border_thickness * 2);
    }

    if (tile_type == WindowTileType::Left
        || tile_type == WindowTileType::TopLeft
        || tile_type == WindowTileType::BottomLeft) {
        rect.set_width(rect.width() / 2);
    }

    if (tile_type == WindowTileType::Right
        || tile_type == WindowTileType::TopRight
        || tile_type == WindowTileType::BottomRight) {
        rect.set_width(rect.width() / 2);
        rect.set_x(rect.x() + rect.width());
    }

    if (tile_type == WindowTileType::Top
        || tile_type == WindowTileType::TopLeft
        || tile_type == WindowTileType::TopRight) {
        rect.set_height(rect.height() / 2);
    }

    if (tile_type == WindowTileType::Bottom
        || tile_type == WindowTileType::BottomLeft
        || tile_type == WindowTileType::BottomRight) {
        auto half_screen_remainder = rect.height() % 2;
        rect.set_height(rect.height() / 2 + half_screen_remainder);
        rect.set_y(rect.y() + rect.height() - half_screen_remainder);
    }

    Gfx::IntRect window_rect = window.rect();
    Gfx::IntRect window_frame_rect = window.frame().rect();

    if (tile_type == WindowTileType::VerticallyMaximized) {
        rect.set_x(window_rect.x());
        rect.set_width(window_rect.width());
    } else {
        rect.set_x(rect.x() + window_rect.x() - window_frame_rect.x());
        rect.set_width(rect.width() - window_frame_rect.width() + window_rect.width());
    }

    if (tile_type == WindowTileType::HorizontallyMaximized) {
        rect.set_y(window_rect.y());
        rect.set_height(window_rect.height());
    } else {
        rect.set_y(rect.y() + window_rect.y() - window_frame_rect.y());
        rect.set_height(rect.height() - window_frame_rect.height() + window_rect.height());
    }
    return rect;
}

void WindowManager::start_dnd_drag(ConnectionFromClient& client, ByteString const& text, Gfx::Bitmap const* bitmap, Core::MimeData const& mime_data)
{
    VERIFY(!m_dnd_client);
    m_dnd_client = client;
    m_dnd_text = text;
    Compositor::the().invalidate_cursor(true);
    m_dnd_overlay = Compositor::the().create_overlay<DndOverlay>(text, bitmap);
    m_dnd_overlay->set_enabled(true);
    m_dnd_mime_data = mime_data;
    set_automatic_cursor_tracking_window(nullptr);
}

void WindowManager::end_dnd_drag()
{
    VERIFY(m_dnd_client);
    Compositor::the().invalidate_cursor();
    m_dnd_client = nullptr;
    m_dnd_text = {};
    m_dnd_overlay = nullptr;
    m_dnd_accepts_drag = false;
}

void WindowManager::set_accepts_drag(bool accepts)
{
    VERIFY(m_dnd_client);
    m_dnd_accepts_drag = accepts;
    Compositor::the().invalidate_cursor();
}

void WindowManager::invalidate_after_theme_or_font_change()
{
    Compositor::the().set_background_color(g_config->read_entry("Background", "Color", palette().desktop_background().to_byte_string()));
    WindowFrame::reload_config();
    for_each_window_stack([&](auto& window_stack) {
        window_stack.for_each_window([&](Window& window) {
            window.frame().theme_changed();
            window.menubar().font_changed(window.rect());
            return IterationDecision::Continue;
        });
        return IterationDecision::Continue;
    });
    ConnectionFromClient::for_each_client([&](ConnectionFromClient& client) {
        client.notify_about_theme_change();
    });
    MenuManager::the().did_change_theme();
    AppletManager::the().did_change_theme();
    Compositor::the().invalidate_after_theme_or_font_change();
}

bool WindowManager::update_theme(ByteString theme_path, ByteString theme_name, bool keep_desktop_background, Optional<ByteString> const& color_scheme_path)
{
    auto error_or_new_theme = Gfx::load_system_theme(theme_path, color_scheme_path);
    if (error_or_new_theme.is_error()) {
        dbgln("WindowManager: Updating theme failed, error {}", error_or_new_theme.error());
        return false;
    }
    auto new_theme = error_or_new_theme.release_value();
    m_theme_overridden = false;
    Gfx::set_system_theme(new_theme);
    m_palette = Gfx::PaletteImpl::create_with_anonymous_buffer(new_theme);
    g_config->write_entry("Theme", "Name", theme_name);
    if (color_scheme_path.has_value() && color_scheme_path.value() != "Custom"sv) {
        g_config->write_bool_entry("Theme", "LoadCustomColorScheme", true);
        g_config->write_entry("Theme", "CustomColorSchemePath", color_scheme_path.value());
        m_preferred_color_scheme = color_scheme_path.value();
    } else if (!color_scheme_path.has_value()) {
        g_config->write_bool_entry("Theme", "LoadCustomColorScheme", false);
        g_config->remove_entry("Theme", "CustomColorSchemePath");
        m_preferred_color_scheme = OptionalNone();
    }
    if (!keep_desktop_background)
        g_config->remove_entry("Background", "Color");
    if (!sync_config_to_disk())
        return false;
    invalidate_after_theme_or_font_change();
    return true;
}

bool WindowManager::set_theme_override(Core::AnonymousBuffer const& theme_override)
{
    if (!theme_override.is_valid())
        return false;
    m_theme_overridden = true;
    Gfx::set_system_theme(theme_override);
    m_palette = Gfx::PaletteImpl::create_with_anonymous_buffer(theme_override);
    invalidate_after_theme_or_font_change();
    return true;
}

Optional<Core::AnonymousBuffer> WindowManager::get_theme_override() const
{
    if (!m_theme_overridden)
        return {};
    return Gfx::current_system_theme_buffer();
}

void WindowManager::clear_theme_override()
{
    m_theme_overridden = false;
    auto previous_theme_name = g_config->read_entry("Theme", "Name");
    auto previous_theme = MUST(Gfx::load_system_theme(ByteString::formatted("/res/themes/{}.ini", previous_theme_name), m_preferred_color_scheme));
    Gfx::set_system_theme(previous_theme);
    m_palette = Gfx::PaletteImpl::create_with_anonymous_buffer(previous_theme);
    invalidate_after_theme_or_font_change();
}

void WindowManager::did_popup_a_menu(Badge<Menu>)
{
    // Clear any ongoing input gesture
    auto* window = automatic_cursor_tracking_window();
    if (!window)
        return;
    window->set_automatic_cursor_tracking_enabled(false);
    set_automatic_cursor_tracking_window(nullptr);
}

void WindowManager::minimize_windows(Window& window, bool minimized)
{
    for_each_window_in_modal_chain(window, [&](auto& w) {
        w.set_minimized(minimized);
        return IterationDecision::Continue;
    });
}

void WindowManager::hide_windows(Window& window, bool hidden)
{
    for_each_window_in_modal_chain(window, [&](auto& w) {
        w.set_hidden(hidden);
        if (!hidden)
            pick_new_active_window(&window);
        return IterationDecision::Continue;
    });
}

void WindowManager::maximize_windows(Window& window, bool maximized)
{
    for_each_window_in_modal_chain(window, [&](auto& w) {
        if (&window == &w) {
            window.set_maximized(maximized);
            return IterationDecision::Continue;
        }
        if (w.is_minimized())
            w.set_minimized(false);
        return IterationDecision::Continue;
    });
}

void WindowManager::set_always_on_top(Window& window, bool always_on_top)
{
    for_each_window_in_modal_chain(window, [&](auto& w) {
        w.set_always_on_top(always_on_top);
        return IterationDecision::Continue;
    });
}

Gfx::IntPoint WindowManager::get_recommended_window_position(Gfx::IntPoint desired)
{
    // FIXME: Find a  better source for the width and height to shift by.
    Gfx::IntPoint shift(8, palette().window_theme().titlebar_height(Gfx::WindowTheme::WindowType::Normal, Gfx::WindowTheme::WindowMode::Other, palette()) + 10);

    Window const* overlap_window = nullptr;
    current_window_stack().for_each_visible_window_of_type_from_front_to_back(WindowType::Normal, [&](Window& window) {
        if (window.is_default_positioned() && (!overlap_window || overlap_window->window_id() < window.window_id())) {
            overlap_window = &window;
        }
        return IterationDecision::Continue;
    });

    Gfx::IntPoint point;
    if (overlap_window) {
        auto& screen = Screen::closest_to_location(desired);
        auto available_rect = desktop_rect(screen);
        point = overlap_window->position() + shift;
        point = { point.x() % screen.width(),
            (point.y() >= available_rect.height())
                ? palette().window_theme().titlebar_height(Gfx::WindowTheme::WindowType::Normal, Gfx::WindowTheme::WindowMode::Other, palette())
                : point.y() };
    } else {
        point = desired;
    }

    return point;
}

void WindowManager::reload_icon_bitmaps_after_scale_change()
{
    reload_config();
    for_each_window_stack([&](auto& window_stack) {
        window_stack.for_each_window([&](Window& window) {
            auto& window_frame = window.frame();
            window_frame.theme_changed();
            return IterationDecision::Continue;
        });
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

WindowStack& WindowManager::get_rendering_window_stacks(WindowStack*& transitioning_window_stack)
{
    return Compositor::the().get_rendering_window_stacks(transitioning_window_stack);
}

void WindowManager::apply_cursor_theme(ByteString const& theme_name)
{
    auto theme_path = ByteString::formatted("/res/cursor-themes/{}/{}", theme_name, "Config.ini");
    auto cursor_theme_config_or_error = Core::ConfigFile::open(theme_path);
    if (cursor_theme_config_or_error.is_error()) {
        dbgln("Unable to open cursor theme '{}': {}", theme_path, cursor_theme_config_or_error.error());
        return;
    }
    auto cursor_theme_config = cursor_theme_config_or_error.release_value();

    auto* current_cursor = Compositor::the().current_cursor();
    auto reload_cursor = [&](RefPtr<Cursor const>& cursor, ByteString const& name) {
        bool is_current_cursor = current_cursor && current_cursor == cursor.ptr();

        static auto const s_default_cursor_path = "/res/cursor-themes/Default/arrow.x2y2.png"sv;
        cursor = Cursor::create(ByteString::formatted("/res/cursor-themes/{}/{}", theme_name, cursor_theme_config->read_entry("Cursor", name)), s_default_cursor_path);

        if (is_current_cursor) {
            Compositor::the().current_cursor_was_reloaded(cursor.ptr());

            if (m_hovered_window) {
                if (auto* modal_window = const_cast<Window&>(*m_hovered_window).blocking_modal_window()) {
                    modal_window->set_cursor(cursor);
                } else if (m_hovered_window->cursor()) {
                    m_hovered_window->set_cursor(cursor);
                }
            }
        }
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
    reload_cursor(m_drag_copy_cursor, "DragCopy");
    reload_cursor(m_wait_cursor, "Wait");
    reload_cursor(m_crosshair_cursor, "Crosshair");
    reload_cursor(m_eyedropper_cursor, "Eyedropper");
    reload_cursor(m_zoom_cursor, "Zoom");

    Compositor::the().invalidate_cursor();
    g_config->write_entry("Mouse", "CursorTheme", theme_name);
    sync_config_to_disk();
}

void WindowManager::set_cursor_highlight_radius(int radius)
{
    // TODO: Validate radius
    m_cursor_highlight_radius = radius;
    Compositor::the().invalidate_cursor();
    g_config->write_num_entry("Mouse", "CursorHighlightRadius", radius);
    sync_config_to_disk();
}

void WindowManager::set_cursor_highlight_color(Gfx::Color color)
{
    m_cursor_highlight_color = color;
    Compositor::the().invalidate_cursor();
    g_config->write_entry("Mouse", "CursorHighlightColor", color.to_byte_string());
    sync_config_to_disk();
}

void WindowManager::apply_system_effects(Vector<bool> effects, ShowGeometry geometry, TileWindow tile_window)
{
    if (m_system_effects == SystemEffects { effects, geometry, tile_window })
        return;

    m_system_effects = { effects, geometry, tile_window };
    g_config->write_bool_entry("Effects", "AnimateMenus", m_system_effects.animate_menus());
    g_config->write_bool_entry("Effects", "FlashMenus", m_system_effects.flash_menus());
    g_config->write_bool_entry("Effects", "AnimateWindows", m_system_effects.animate_windows());
    g_config->write_bool_entry("Effects", "SmoothScrolling", m_system_effects.smooth_scrolling());
    g_config->write_bool_entry("Effects", "TabAccents", m_system_effects.tab_accents());
    g_config->write_bool_entry("Effects", "SplitterKnurls", m_system_effects.splitter_knurls());
    g_config->write_bool_entry("Effects", "Tooltips", m_system_effects.tooltips());
    g_config->write_bool_entry("Effects", "MenuShadow", m_system_effects.menu_shadow());
    g_config->write_bool_entry("Effects", "WindowShadow", m_system_effects.window_shadow());
    g_config->write_bool_entry("Effects", "TooltipShadow", m_system_effects.tooltip_shadow());
    g_config->write_entry("Effects", "ShowGeometry", ShowGeometryTools::enum_to_string(geometry));
    g_config->write_entry("Effects", "TileWindow", TileWindowTools::enum_to_string(tile_window));
    sync_config_to_disk();
}

void WindowManager::load_system_effects()
{
    Vector<bool> effects = {
        g_config->read_bool_entry("Effects", "AnimateMenus", true),
        g_config->read_bool_entry("Effects", "FlashMenus", true),
        g_config->read_bool_entry("Effects", "AnimateWindows", true),
        g_config->read_bool_entry("Effects", "SmoothScrolling", true),
        g_config->read_bool_entry("Effects", "TabAccents", true),
        g_config->read_bool_entry("Effects", "SplitterKnurls", true),
        g_config->read_bool_entry("Effects", "Tooltips", true),
        g_config->read_bool_entry("Effects", "MenuShadow", true),
        g_config->read_bool_entry("Effects", "WindowShadow", true),
        g_config->read_bool_entry("Effects", "TooltipShadow", true)
    };
    ShowGeometry geometry = ShowGeometryTools::string_to_enum(g_config->read_entry("Effects", "ShowGeometry", "OnMoveAndResize"));
    TileWindow tile_window = TileWindowTools::string_to_enum(g_config->read_entry("Effects", "TileWindow", "ShowTileOverlay"));
    m_system_effects = { effects, geometry, tile_window };

    ConnectionFromClient::for_each_client([&](auto& client) {
        client.async_update_system_effects(effects);
    });
}

bool WindowManager::sync_config_to_disk()
{
    if (auto result = g_config->sync(); result.is_error()) {
        dbgln("Failed to save config file: {}", result.error());
        return false;
    }
    return true;
}

Window* WindowManager::foremost_popup_window(WindowStack& stack)
{
    Window* popup_window = nullptr;
    for_each_visible_window_from_front_to_back([&](Window& window) {
        if (window.type() == WindowType::Popup) {
            popup_window = &window;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    },
        &stack);
    return popup_window;
}

void WindowManager::request_close_fragile_windows(WindowStack& stack)
{
    for_each_visible_window_from_back_to_front([&](Window& window) {
        if (is_fragile_window_type(window.type()))
            window.request_close();
        return IterationDecision::Continue;
    },
        &stack);
}

}
