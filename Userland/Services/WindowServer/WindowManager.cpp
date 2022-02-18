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
    : m_switcher(WindowSwitcher::construct())
    , m_keymap_switcher(KeymapSwitcher::construct())
    , m_palette(palette)
{
    s_the = this;

    {
        // If we haven't created any window stacks, at least create the stationary/main window stack
        auto row = adopt_own(*new RemoveReference<decltype(m_window_stacks[0])>());
        auto main_window_stack = adopt_own(*new WindowStack(0, 0));
        main_window_stack->set_stationary_window_stack(*main_window_stack);
        m_current_window_stack = main_window_stack.ptr();
        row->append(move(main_window_stack));
        m_window_stacks.append(move(row));
    }

    reload_config();

    m_keymap_switcher->on_keymap_change = [&](String const& keymap) {
        for_each_window_manager([&keymap](WMClientConnection& conn) {
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

WindowManager::~WindowManager()
{
}

void WindowManager::reload_config()
{
    m_config = Core::ConfigFile::open("/etc/WindowServer.ini", Core::ConfigFile::AllowWriting::Yes).release_value_but_fixme_should_propagate_errors();

    unsigned workspace_rows = (unsigned)m_config->read_num_entry("Workspace", "Rows", default_window_stack_rows);
    unsigned workspace_columns = (unsigned)m_config->read_num_entry("Workspace", "Columns", default_window_stack_columns);
    if (workspace_rows == 0 || workspace_columns == 0 || workspace_rows > max_window_stack_rows || workspace_columns > max_window_stack_columns) {
        workspace_rows = default_window_stack_rows;
        workspace_columns = default_window_stack_columns;
    }
    apply_workspace_settings(workspace_rows, workspace_columns, false);

    m_double_click_speed = m_config->read_num_entry("Input", "DoubleClickSpeed", 250);
    m_buttons_switched = m_config->read_bool_entry("Mouse", "ButtonsSwitched", false);
    apply_cursor_theme(m_config->read_entry("Mouse", "CursorTheme", "Default"));

    auto reload_graphic = [&](RefPtr<MultiScaleBitmaps>& bitmap, String const& name) {
        if (bitmap) {
            if (!bitmap->load(name))
                bitmap = nullptr;
        } else {
            bitmap = MultiScaleBitmaps::create(name);
        }
    };

    reload_graphic(m_overlay_rect_shadow, m_config->read_entry("Graphics", "OverlayRectShadow"));
    Compositor::the().invalidate_after_theme_or_font_change();

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

    tell_wms_screen_rects_changed();

    for_each_window_stack([&](auto& window_stack) {
        window_stack.for_each_window([](Window& window) {
            window.screens().clear_with_capacity();
            window.recalculate_rect();
            return IterationDecision::Continue;
        });
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
            auto& row = m_window_stacks[rows];
            for (size_t column_index = 0; column_index < row.size(); column_index++) {
                merge_window_stack(row[column_index], m_window_stacks[rows - 1][column_index]);
                if (rows - 1 == current_stack_row && column_index == current_stack_column)
                    need_rerender = true;
            }
            m_window_stacks.remove(rows);
            current_rows--;
        }
        // While we have too many columns, merge each column too many into the new right most column
        while (current_columns > columns) {
            for (size_t row_index = 0; row_index < current_rows; row_index++) {
                auto& row = m_window_stacks[row_index];
                merge_window_stack(row[columns], row[columns - 1]);
                if (row_index == current_stack_row && columns - 1 == current_stack_column)
                    need_rerender = true;
                row.remove(columns);
            }
            current_columns--;
        }
        // Add more rows if necessary
        while (rows > current_rows) {
            auto row = adopt_own(*new RemoveReference<decltype(m_window_stacks[0])>());
            for (size_t column_index = 0; column_index < columns; column_index++) {
                auto window_stack = adopt_own(*new WindowStack(current_rows, column_index));
                window_stack->set_stationary_window_stack(m_window_stacks[0][0]);
                row->append(move(window_stack));
            }
            m_window_stacks.append(move(row));
            current_rows++;
        }
        // Add more columns if necessary
        while (columns > current_columns) {
            for (size_t row_index = 0; row_index < current_rows; row_index++) {
                auto& row = m_window_stacks[row_index];
                while (row.size() < columns) {
                    auto window_stack = adopt_own(*new WindowStack(row_index, row.size()));
                    window_stack->set_stationary_window_stack(m_window_stacks[0][0]);
                    row.append(move(window_stack));
                }
            }
            current_columns++;
        }

        if (removing_current_stack) {
            // If we're on a window stack that was removed, we need to move...
            m_current_window_stack = &m_window_stacks[new_current_row][new_current_column];
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
        m_config->write_num_entry("Workspace", "Rows", window_stack_rows());
        m_config->write_num_entry("Workspace", "Columns", window_stack_columns());
        return !m_config->sync().is_error();
    }
    return true;
}

void WindowManager::set_acceleration_factor(double factor)
{
    ScreenInput::the().set_acceleration_factor(factor);
    dbgln("Saving acceleration factor {} to config file at {}", factor, m_config->filename());
    m_config->write_entry("Mouse", "AccelerationFactor", String::formatted("{}", factor));
    if (auto result = m_config->sync(); result.is_error())
        dbgln("Failed to save config file: {}", result.error());
}

void WindowManager::set_scroll_step_size(unsigned step_size)
{
    ScreenInput::the().set_scroll_step_size(step_size);
    dbgln("Saving scroll step size {} to config file at {}", step_size, m_config->filename());
    m_config->write_entry("Mouse", "ScrollStepSize", String::number(step_size));
    if (auto result = m_config->sync(); result.is_error())
        dbgln("Failed to save config file: {}", result.error());
}

void WindowManager::set_double_click_speed(int speed)
{
    VERIFY(speed >= double_click_speed_min && speed <= double_click_speed_max);
    m_double_click_speed = speed;
    dbgln("Saving double-click speed {} to config file at {}", speed, m_config->filename());
    m_config->write_entry("Input", "DoubleClickSpeed", String::number(speed));
    if (auto result = m_config->sync(); result.is_error())
        dbgln("Failed to save config file: {}", result.error());
}

int WindowManager::double_click_speed() const
{
    return m_double_click_speed;
}

void WindowManager::set_buttons_switched(bool switched)
{
    m_buttons_switched = switched;
    dbgln("Saving mouse buttons switched state {} to config file at {}", switched, m_config->filename());
    m_config->write_bool_entry("Mouse", "ButtonsSwitched", switched);
    if (auto result = m_config->sync(); result.is_error())
        dbgln("Failed to save config file: {}", result.error());
}

bool WindowManager::get_buttons_switched() const
{
    return m_buttons_switched;
}

WindowStack& WindowManager::window_stack_for_window(Window& window)
{
    if (is_stationary_window_type(window.type()))
        return m_window_stacks[0][0];
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
        Core::EventLoop::current().post_event(window, make<ResizeEvent>(screen.rect()));
        window.set_rect(screen.rect());
    }

    if (window.type() != WindowType::Desktop || is_first_window)
        set_active_window(&window);

    if (m_switcher->is_visible() && window.type() != WindowType::WindowSwitcher)
        m_switcher->refresh();

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
    window.window_stack().move_to_front(window);

    if (make_active)
        set_active_window(&window, make_input);

    if (m_switcher->is_visible()) {
        m_switcher->refresh();
        if (!window.is_accessory()) {
            m_switcher->select_window(window);
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
    check_hide_geometry_overlay(window);
    auto* active = active_window();
    auto* active_input = active_input_window();
    bool was_modal = window.is_modal(); // This requires the window to be on a window stack still!
    window.window_stack().remove(window);
    if (active == &window || active_input == &window || (active && window.is_descendant_of(*active)) || (active_input && active_input != active && window.is_descendant_of(*active_input)))
        pick_new_active_window(&window);

    window.invalidate_last_rendered_screen_rects_now();

    if (m_switcher->is_visible() && window.type() != WindowType::WindowSwitcher)
        m_switcher->refresh();

    Compositor::the().invalidate_occlusions();

    for_each_window_manager([&](WMClientConnection& conn) {
        if (conn.window_id() < 0 || !(conn.event_mask() & WMEventMask::WindowRemovals))
            return IterationDecision::Continue;
        if (!window.is_internal() && !was_modal)
            conn.async_window_removed(conn.window_id(), window.client_id(), window.window_id());
        return IterationDecision::Continue;
    });
}

void WindowManager::greet_window_manager(WMClientConnection& conn)
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

void WindowManager::tell_wm_about_window(WMClientConnection& conn, Window& window)
{
    if (conn.window_id() < 0)
        return;
    if (!(conn.event_mask() & WMEventMask::WindowStateChanges))
        return;
    if (window.is_internal())
        return;
    auto* parent = window.parent_window();
    auto& window_stack = is_stationary_window_type(window.type()) ? current_window_stack() : window.window_stack();
    conn.async_window_state_changed(conn.window_id(), window.client_id(), window.window_id(), parent ? parent->client_id() : -1, parent ? parent->window_id() : -1, window_stack.row(), window_stack.column(), window.is_active(), window.is_minimized(), window.is_modal_dont_unparent(), window.is_frameless(), (i32)window.type(), window.computed_title(), window.rect(), window.progress());
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

void WindowManager::tell_wm_about_current_window_stack(WMClientConnection& conn)
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

void WindowManager::tell_wms_screen_rects_changed()
{
    ClientConnection::for_each_client([&](ClientConnection& client) {
        client.notify_about_new_screen_rects();
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

void WindowManager::tell_wms_super_space_key_pressed()
{
    for_each_window_manager([](WMClientConnection& conn) {
        if (conn.window_id() < 0)
            return IterationDecision::Continue;

        conn.async_super_space_key_pressed(conn.window_id());
        return IterationDecision::Continue;
    });
}

void WindowManager::tell_wms_current_window_stack_changed()
{
    for_each_window_manager([&](WMClientConnection& conn) {
        tell_wm_about_current_window_stack(conn);
        return IterationDecision::Continue;
    });
}

static bool window_type_has_title(WindowType type)
{
    return type == WindowType::Normal || type == WindowType::ToolWindow;
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

void WindowManager::notify_modal_unparented(Window& window)
{
    if (window.type() != WindowType::Normal)
        return;

    dbgln_if(WINDOWMANAGER_DEBUG, "[WM] Window({}) was unparented", &window);

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

    for_each_visible_window_from_front_to_back([&](Window& candidate) {
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

void WindowManager::check_hide_geometry_overlay(Window& window)
{
    if (&window == m_move_window.ptr() || &window == m_resize_window.ptr())
        m_geometry_overlay = nullptr;
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
    m_move_window_cursor_position = window.is_tiled() ? to_floating_cursor_position(m_mouse_down_origin) : m_mouse_down_origin;
    m_geometry_overlay = Compositor::the().create_overlay<WindowGeometryOverlay>(window);
    m_geometry_overlay->set_enabled(true);
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
    m_geometry_overlay = Compositor::the().create_overlay<WindowGeometryOverlay>(window);
    m_geometry_overlay->set_enabled(true);

    current_window_stack().set_active_input_tracking_window(nullptr);

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
    if (event.type() == Event::MouseUp && event.button() == MouseButton::Primary) {

        dbgln_if(MOVE_DEBUG, "[WM] Finish moving Window({})", m_move_window);

        if (!m_move_window->is_tiled())
            m_move_window->set_floating_rect(m_move_window->rect());

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

        const int tiling_deadzone = 10;
        const int secondary_deadzone = 2;
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
            bool is_resizable = m_move_window->is_resizable();
            auto pixels_moved_from_start = event.position().pixels_moved(m_move_origin);

            auto event_location_relative_to_screen = event.position().translated(-cursor_screen.rect().location());
            if (is_resizable && event_location_relative_to_screen.x() <= tiling_deadzone) {
                if (event_location_relative_to_screen.y() <= tiling_deadzone + desktop_relative_to_screen.top())
                    m_move_window->set_tiled(WindowTileType::TopLeft);
                else if (event_location_relative_to_screen.y() >= desktop_relative_to_screen.height() - tiling_deadzone)
                    m_move_window->set_tiled(WindowTileType::BottomLeft);
                else
                    m_move_window->set_tiled(WindowTileType::Left);
            } else if (is_resizable && event_location_relative_to_screen.x() >= cursor_screen.width() - tiling_deadzone) {
                if (event_location_relative_to_screen.y() <= tiling_deadzone + desktop.top())
                    m_move_window->set_tiled(WindowTileType::TopRight);
                else if (event_location_relative_to_screen.y() >= desktop_relative_to_screen.height() - tiling_deadzone)
                    m_move_window->set_tiled(WindowTileType::BottomRight);
                else
                    m_move_window->set_tiled(WindowTileType::Right);
            } else if (is_resizable && event_location_relative_to_screen.y() <= secondary_deadzone + desktop_relative_to_screen.top()) {
                m_move_window->set_tiled(WindowTileType::Top);
            } else if (is_resizable && event_location_relative_to_screen.y() >= desktop_relative_to_screen.bottom() - secondary_deadzone) {
                m_move_window->set_tiled(WindowTileType::Bottom);
            } else if (!m_move_window->is_tiled()) {
                Gfx::IntPoint pos = m_move_window_origin.translated(event.position() - m_move_origin);
                m_move_window->set_position_without_repaint(pos);
                // "Bounce back" the window if it would end up too far outside the screen.
                // If the user has let go of Mod_Super, maybe they didn't intentionally press it to begin with.
                // Therefore, refuse to go into a state where knowledge about super-drags is necessary.
                bool force_titlebar_visible = !(m_keyboard_modifiers & Mod_Super);
                m_move_window->nudge_into_desktop(&cursor_screen, force_titlebar_visible);
            } else if (pixels_moved_from_start > 5) {
                Gfx::IntPoint adjusted_position = event.position().translated(-m_move_window_cursor_position);
                m_move_window->set_untiled();
                m_move_window->move_to(adjusted_position);
                m_move_origin = event.position();
                m_move_window_origin = m_move_window->position();
            }
        }

        m_geometry_overlay->window_rect_changed();
    }
    return true;
}

Gfx::IntPoint WindowManager::to_floating_cursor_position(Gfx::IntPoint const& origin) const
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

    if (event.type() == Event::MouseMove) {
        const int vertical_maximize_deadzone = 5;
        auto& cursor_screen = ScreenInput::the().cursor_location_screen();
        if (&cursor_screen == &Screen::closest_to_rect(m_resize_window->rect())) {
            auto desktop_rect = this->desktop_rect(cursor_screen);
            if (event.y() >= desktop_rect.bottom() - vertical_maximize_deadzone + 1 || event.y() <= desktop_rect.top() + vertical_maximize_deadzone - 1) {
                dbgln_if(RESIZE_DEBUG, "Should tile as VerticallyMaximized");
                m_resize_window->set_tiled(WindowTileType::VerticallyMaximized);
                m_resize_window = nullptr;
                m_geometry_overlay = nullptr;
                m_resizing_mouse_button = MouseButton::None;
                return true;
            }
        }
    }

    if (event.type() == Event::MouseUp && event.button() == m_resizing_mouse_button) {
        dbgln_if(RESIZE_DEBUG, "[WM] Finish resizing Window({})", m_resize_window);

        if (!m_resize_window->is_tiled())
            m_resize_window->set_floating_rect(m_resize_window->rect());

        Core::EventLoop::current().post_event(*m_resize_window, make<ResizeEvent>(m_resize_window->rect()));
        m_resize_window->invalidate(true, true);
        m_resize_window = nullptr;
        m_geometry_overlay = nullptr;
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

    if (m_resize_window->is_tiled()) {
        // Check if we should be un-tiling the window. This should happen when one side touching
        // the screen border changes. We need to un-tile because while it is tiled, rendering is
        // constrained to the screen where it's tiled on, and if one of these sides move we should
        // no longer constrain rendering to that screen. Changing the sides not touching a screen
        // border however is fine as long as the screen contains the entire window.
        m_resize_window->check_untile_due_to_resize(new_rect);
    }
    dbgln_if(RESIZE_DEBUG, "[WM] Resizing, original: {}, now: {}", m_resize_window_original_rect, new_rect);

    m_resize_window->set_rect(new_rect);
    m_geometry_overlay->window_rect_changed();
    Core::EventLoop::current().post_event(*m_resize_window, make<ResizeEvent>(new_rect));
    return true;
}

bool WindowManager::process_ongoing_drag(MouseEvent& event)
{
    if (!m_dnd_client)
        return false;

    if (event.type() == Event::MouseMove) {
        m_dnd_overlay->cursor_moved();

        // We didn't let go of the drag yet, see if we should send some drag move events..
        for_each_visible_window_from_front_to_back([&](Window& window) {
            if (!window.rect().contains(event.position()))
                return IterationDecision::Continue;
            event.set_drag(true);
            event.set_mime_data(*m_dnd_mime_data);
            deliver_mouse_event(window, event, false);
            return IterationDecision::Break;
        });
    }

    if (!(event.type() == Event::MouseUp && event.button() == MouseButton::Primary))
        return true;

    if (auto* window = current_window_stack().window_at(event.position())) {
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
    auto& window_stack = current_window_stack();
    auto* input_tracking_window = window_stack.active_input_tracking_window();
    if (!input_tracking_window)
        return false;

    // At this point, we have delivered the start of an input sequence to a
    // client application. We must keep delivering to that client
    // application until the input sequence is done.
    //
    // This prevents e.g. moving on one window out of the bounds starting
    // a move in that other unrelated window, and other silly shenanigans.
    deliver_mouse_event(*input_tracking_window, event, true);

    if (event.type() == Event::MouseUp && event.buttons() == 0)
        window_stack.set_active_input_tracking_window(nullptr);

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

    if (blocking_modal_window) {
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

    if (!window.is_automatic_cursor_tracking()) {
        deliver_mouse_event(window, event, true);
    }

    if (event.type() == Event::MouseDown)
        current_window_stack().set_active_input_tracking_window(&window);
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
    ClientConnection::for_each_client([&](ClientConnection& conn) {
        if (conn.does_global_mouse_tracking()) {
            conn.async_track_mouse_move(event.position());
        }
    });
    // The active input tracking window is excluded here because we're sending the event to it
    // in the next step.
    auto& window_stack = current_window_stack();
    for_each_visible_window_from_front_to_back([&](Window& window) {
        if (window.is_automatic_cursor_tracking() && &window != window_stack.active_input_tracking_window())
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
    auto result = current_window_stack().hit_test(event.position());

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
        // TODO: handle transitioning between two stacks
        set_hovered_window(current_window_stack().window_at(mouse_event.position(), WindowStack::IncludeWindowFrame::No));
        return;
    }

    if (static_cast<Event&>(event).is_key_event()) {
        process_key_event(static_cast<KeyEvent&>(event));
        return;
    }

    Core::Object::event(event);
}

bool WindowManager::is_window_in_modal_stack(Window& window_in_modal_stack, Window& other_window)
{
    auto result = for_each_window_in_modal_stack(window_in_modal_stack, [&](auto& window, auto) {
        if (&other_window == &window)
            return IterationDecision::Break;
        for (auto& accessory : window.accessory_windows()) {
            if (accessory.ptr() == &other_window)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return result == IterationDecision::Break;
}

void WindowManager::switch_to_window_stack(WindowStack& window_stack, Window* carry_window, bool show_overlay)
{
    m_carry_window_to_new_stack.clear();
    m_switching_to_window_stack = &window_stack;
    if (carry_window && !is_stationary_window_type(carry_window->type()) && &carry_window->window_stack() != &window_stack) {
        auto& from_stack = carry_window->window_stack();

        auto* blocking_modal = carry_window->blocking_modal_window();
        for_each_visible_window_from_back_to_front([&](Window& window) {
            if (is_stationary_window_type(window.type()))
                return IterationDecision::Continue;

            if (&window.window_stack() != &carry_window->window_stack())
                return IterationDecision::Continue;
            if (&window == carry_window || ((carry_window->is_modal() || blocking_modal) && is_window_in_modal_stack(*carry_window, window)))
                m_carry_window_to_new_stack.append(window);
            return IterationDecision::Continue;
        },
            &from_stack);

        auto* from_active_window = from_stack.active_window();
        auto* from_active_input_window = from_stack.active_input_window();
        bool did_carry_active_window = false;
        bool did_carry_active_input_window = false;
        for (auto& window : m_carry_window_to_new_stack) {
            if (window == from_active_window)
                did_carry_active_window = true;
            if (window == from_active_input_window)
                did_carry_active_input_window = true;
            window->set_moving_to_another_stack(true);
            VERIFY(&window->window_stack() == &from_stack);
            from_stack.remove(*window);
            window_stack.add(*window);
        }
        // Before we change to the new stack, find a new active window on the stack we're switching from
        if (did_carry_active_window || did_carry_active_input_window)
            pick_new_active_window(from_active_window);

        // Now switch to the new stack
        m_current_window_stack = &window_stack;
        if (did_carry_active_window && from_active_window)
            set_active_window(from_active_window, from_active_input_window == from_active_window);
        if (did_carry_active_input_window && from_active_input_window && from_active_input_window != from_active_window)
            set_active_input_window(from_active_input_window);

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
    auto* previous_stack_active_input_window = previous_stack.active_input_window();
    auto* new_stack_active_window = new_stack.active_window();
    auto* new_stack_active_input_window = new_stack.active_input_window();
    if (previous_stack_active_input_window && previous_stack_active_input_window != new_stack_active_input_window)
        notify_previous_active_input_window(*previous_stack_active_input_window);
    if (new_stack_active_input_window && previous_stack_active_input_window != new_stack_active_input_window)
        notify_new_active_input_window(*new_stack_active_input_window);
    if (previous_stack_active_window != new_stack_active_window) {
        if (previous_stack_active_window && is_stationary_window_type(previous_stack_active_window->type()))
            notify_previous_active_window(*previous_stack_active_window);
        if (new_stack_active_window && is_stationary_window_type(new_stack_active_window->type()))
            notify_new_active_window(*new_stack_active_window);
    }

    if (!new_stack_active_input_window)
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

    // FIXME: This is fragile, the kernel should send a signal when we switch back to the WindowManager's framebuffer
    if (event.type() == Event::KeyDown && (event.modifiers() & Mod_Alt) && (event.key() == Key_ExclamationPoint || event.key() == Key_1)) {
        Compositor::the().invalidate_screen();
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
        if (!m_dnd_client && !current_window_stack().active_input_tracking_window() && event.type() == Event::KeyUp && event.key() == Key_Super) {
            tell_wms_super_key_pressed();
            return;
        }

        if (event.type() == Event::KeyDown && event.key() == Key_Space) {
            tell_wms_super_space_key_pressed();
            return;
        }
    }

    if (MenuManager::the().current_menu() && event.key() != Key_Super) {
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
        m_switcher->on_key_event(event);
        return;
    }

    if (event.type() == Event::KeyDown && (event.modifiers() == (Mod_Alt | Mod_Shift) && (event.key() == Key_Shift || event.key() == Key_Alt))) {
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
                if (column + 1 >= m_window_stacks[0].size())
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
            Window* carry_window = nullptr;
            auto& new_window_stack = m_window_stacks[row][column];
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

    auto* active_input_window = current_window_stack().active_input_window();
    if (!active_input_window)
        return;

    if (event.type() == Event::KeyDown && event.modifiers() == Mod_Super && active_input_window->type() != WindowType::Desktop) {
        if (event.key() == Key_Down) {
            if (active_input_window->is_resizable() && active_input_window->is_maximized()) {
                maximize_windows(*active_input_window, false);
                return;
            }
            if (active_input_window->is_minimizable())
                minimize_windows(*active_input_window, true);
            return;
        }
        if (active_input_window->is_resizable()) {
            if (event.key() == Key_Up) {
                maximize_windows(*active_input_window, !active_input_window->is_maximized());
                return;
            }
            if (event.key() == Key_Left) {
                if (active_input_window->tile_type() == WindowTileType::Left) {
                    active_input_window->set_untiled();
                    return;
                }
                if (active_input_window->is_maximized())
                    maximize_windows(*active_input_window, false);
                active_input_window->set_tiled(WindowTileType::Left);
                return;
            }
            if (event.key() == Key_Right) {
                if (active_input_window->tile_type() == WindowTileType::Right) {
                    active_input_window->set_untiled();
                    return;
                }
                if (active_input_window->is_maximized())
                    maximize_windows(*active_input_window, false);
                active_input_window->set_tiled(WindowTileType::Right);
                return;
            }
        }
    }

    if (event.type() == Event::KeyDown && event.modifiers() == (Mod_Super | Mod_Alt) && active_input_window->type() != WindowType::Desktop) {
        if (active_input_window->is_resizable()) {
            if (event.key() == Key_Right || event.key() == Key_Left) {
                if (active_input_window->tile_type() == WindowTileType::HorizontallyMaximized) {
                    active_input_window->set_untiled();
                    return;
                }
                if (active_input_window->is_maximized())
                    maximize_windows(*active_input_window, false);
                active_input_window->set_tiled(WindowTileType::HorizontallyMaximized);
                return;
            }
            if (event.key() == Key_Up || event.key() == Key_Down) {
                if (active_input_window->tile_type() == WindowTileType::VerticallyMaximized) {
                    active_input_window->set_untiled();
                    return;
                }
                if (active_input_window->is_maximized())
                    maximize_windows(*active_input_window, false);
                active_input_window->set_tiled(WindowTileType::VerticallyMaximized);
                return;
            }
        }
    }

    active_input_window->dispatch_event(event);
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
    auto& window_stack = current_window_stack();
    auto* previous_input_window = window_stack.active_input_window();
    if (window == previous_input_window)
        return window;

    if (previous_input_window)
        notify_previous_active_input_window(*previous_input_window);

    window_stack.set_active_input_window(window);
    if (window)
        notify_new_active_input_window(*window);

    return previous_input_window;
}

void WindowManager::notify_new_active_input_window(Window& new_input_window)
{
    Core::EventLoop::current().post_event(new_input_window, make<Event>(Event::WindowInputEntered));
}

void WindowManager::notify_previous_active_input_window(Window& previous_input_window)
{
    Core::EventLoop::current().post_event(previous_input_window, make<Event>(Event::WindowInputLeft));
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

    auto& window_stack = current_window_stack();
    if (new_active_window == window_stack.active_window())
        return;

    if (auto* previously_active_window = window_stack.active_window()) {
        window_stack.set_active_window(nullptr);
        window_stack.set_active_input_tracking_window(nullptr);
        notify_previous_active_window(*previously_active_window);
    }

    if (new_active_window) {
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
    if (auto* window = const_cast<WindowManager*>(this)->current_window_stack().active_window())
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

Gfx::IntRect WindowManager::tiled_window_rect(Window const& window, WindowTileType tile_type, bool relative_to_window_screen) const
{
    VERIFY(tile_type != WindowTileType::None);

    auto& screen = Screen::closest_to_rect(window.frame().rect());
    Gfx::IntRect rect = screen.rect();

    if (screen.is_main_screen()) {
        // Subtract taskbar window height if present
        const_cast<WindowManager*>(this)->current_window_stack().for_each_visible_window_of_type_from_back_to_front(WindowType::Taskbar, [&rect](Window& taskbar_window) {
            rect.set_height(rect.height() - taskbar_window.height());
            return IterationDecision::Break;
        });
    }

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
        rect.set_x(rect.width());
    }

    if (tile_type == WindowTileType::Top
        || tile_type == WindowTileType::TopLeft
        || tile_type == WindowTileType::TopRight) {
        rect.set_height(rect.height() / 2);
    }

    if (tile_type == WindowTileType::Bottom
        || tile_type == WindowTileType::BottomLeft
        || tile_type == WindowTileType::BottomRight) {
        auto half_screen_reminder = rect.height() % 2;
        rect.set_height(rect.height() / 2 + half_screen_reminder);
        rect.set_y(rect.height() - half_screen_reminder);
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

    if (relative_to_window_screen)
        rect.translate_by(-screen.rect().location());
    return rect;
}

void WindowManager::start_dnd_drag(ClientConnection& client, String const& text, Gfx::Bitmap const* bitmap, Core::MimeData const& mime_data)
{
    VERIFY(!m_dnd_client);
    m_dnd_client = client;
    m_dnd_text = text;
    m_dnd_overlay = Compositor::the().create_overlay<DndOverlay>(text, bitmap);
    m_dnd_overlay->set_enabled(true);
    m_dnd_mime_data = mime_data;
    Compositor::the().invalidate_cursor();
    current_window_stack().set_active_input_tracking_window(nullptr);
}

void WindowManager::end_dnd_drag()
{
    VERIFY(m_dnd_client);
    Compositor::the().invalidate_cursor();
    m_dnd_client = nullptr;
    m_dnd_text = {};
    m_dnd_overlay = nullptr;
}

void WindowManager::invalidate_after_theme_or_font_change()
{
    Compositor::the().set_background_color(m_config->read_entry("Background", "Color", palette().desktop_background().to_string()));
    WindowFrame::reload_config();
    for_each_window_stack([&](auto& window_stack) {
        window_stack.for_each_window([&](Window& window) {
            window.frame().theme_changed();
            return IterationDecision::Continue;
        });
        return IterationDecision::Continue;
    });
    ClientConnection::for_each_client([&](ClientConnection& client) {
        client.async_update_system_theme(Gfx::current_system_theme_buffer());
    });
    MenuManager::the().did_change_theme();
    AppletManager::the().did_change_theme();
    Compositor::the().invalidate_after_theme_or_font_change();
}

bool WindowManager::update_theme(String theme_path, String theme_name)
{
    auto new_theme = Gfx::load_system_theme(theme_path);
    if (!new_theme.is_valid())
        return false;
    Gfx::set_system_theme(new_theme);
    m_palette = Gfx::PaletteImpl::create_with_anonymous_buffer(new_theme);
    m_config->write_entry("Theme", "Name", theme_name);
    m_config->remove_entry("Background", "Color");
    if (auto result = m_config->sync(); result.is_error()) {
        dbgln("Failed to save config file: {}", result.error());
        return false;
    }
    invalidate_after_theme_or_font_change();
    return true;
}

void WindowManager::did_popup_a_menu(Badge<Menu>)
{
    // Clear any ongoing input gesture
    auto* active_input_tracking_window = current_window_stack().active_input_tracking_window();
    if (!active_input_tracking_window)
        return;
    active_input_tracking_window->set_automatic_cursor_tracking_enabled(false);
    current_window_stack().set_active_input_tracking_window(nullptr);
}

void WindowManager::minimize_windows(Window& window, bool minimized)
{
    for_each_window_in_modal_stack(window, [&](auto& w, bool) {
        w.set_minimized(minimized);
        return IterationDecision::Continue;
    });
}

void WindowManager::hide_windows(Window& window, bool hidden)
{
    for_each_window_in_modal_stack(window, [&](auto& w, bool) {
        w.set_hidden(hidden);

        if (!hidden)
            pick_new_active_window(&window);

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

void WindowManager::set_always_on_top(Window& window, bool always_on_top)
{
    for_each_window_in_modal_stack(window, [&](auto& w, bool) {
        w.set_always_on_top(always_on_top);
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
    current_window_stack().for_each_visible_window_of_type_from_front_to_back(WindowType::Normal, [&](Window& window) {
        if (window.is_default_positioned() && (!overlap_window || overlap_window->window_id() < window.window_id())) {
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

void WindowManager::apply_cursor_theme(String const& theme_name)
{
    auto theme_path = String::formatted("/res/cursor-themes/{}/{}", theme_name, "Config.ini");
    auto cursor_theme_config_or_error = Core::ConfigFile::open(theme_path);
    if (cursor_theme_config_or_error.is_error()) {
        dbgln("Unable to open cursor theme '{}': {}", theme_path, cursor_theme_config_or_error.error());
        return;
    }
    auto cursor_theme_config = cursor_theme_config_or_error.release_value();

    auto* current_cursor = Compositor::the().current_cursor();
    auto reload_cursor = [&](RefPtr<Cursor>& cursor, const String& name) {
        bool is_current_cursor = current_cursor && current_cursor == cursor.ptr();

        static auto const s_default_cursor_path = "/res/cursor-themes/Default/arrow.x2y2.png";
        cursor = Cursor::create(String::formatted("/res/cursor-themes/{}/{}", theme_name, cursor_theme_config->read_entry("Cursor", name)), s_default_cursor_path);

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
    reload_cursor(m_wait_cursor, "Wait");
    reload_cursor(m_crosshair_cursor, "Crosshair");
    reload_cursor(m_eyedropper_cursor, "Eyedropper");
    reload_cursor(m_zoom_cursor, "Zoom");

    Compositor::the().invalidate_cursor();
    m_config->write_entry("Mouse", "CursorTheme", theme_name);
}

}
