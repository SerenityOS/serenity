/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Window.h"
#include "Animation.h"
#include "AppletManager.h"
#include "Compositor.h"
#include "ConnectionFromClient.h"
#include "Event.h"
#include "EventLoop.h"
#include "Screen.h"
#include "WindowManager.h"
#include <AK/Badge.h>
#include <AK/CharacterTypes.h>
#include <AK/Debug.h>
#include <LibCore/Account.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibCore/SessionManagement.h>
#include <LibKeyboard/CharacterMap.h>

namespace WindowServer {

static ByteString default_window_icon_path()
{
    return "/res/icons/16x16/window.png";
}

static Gfx::Bitmap& default_window_icon()
{
    static RefPtr<Gfx::Bitmap> s_icon;
    if (!s_icon)
        s_icon = Gfx::Bitmap::load_from_file(default_window_icon_path()).release_value_but_fixme_should_propagate_errors();
    return *s_icon;
}

static Gfx::Bitmap& minimize_icon()
{
    static RefPtr<Gfx::Bitmap> s_icon;
    if (!s_icon)
        s_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/downward-triangle.png"sv).release_value_but_fixme_should_propagate_errors();
    return *s_icon;
}

static Gfx::Bitmap& maximize_icon()
{
    static RefPtr<Gfx::Bitmap> s_icon;
    if (!s_icon)
        s_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/upward-triangle.png"sv).release_value_but_fixme_should_propagate_errors();
    return *s_icon;
}

static Gfx::Bitmap& restore_icon()
{
    static RefPtr<Gfx::Bitmap> s_icon;
    if (!s_icon)
        s_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/window-restore.png"sv).release_value_but_fixme_should_propagate_errors();
    return *s_icon;
}

static Gfx::Bitmap& close_icon()
{
    static RefPtr<Gfx::Bitmap> s_icon;
    if (!s_icon)
        s_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/window-close.png"sv).release_value_but_fixme_should_propagate_errors();
    return *s_icon;
}

static Gfx::Bitmap& pin_icon()
{
    static RefPtr<Gfx::Bitmap> s_icon;
    if (!s_icon)
        s_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/window-pin.png"sv).release_value_but_fixme_should_propagate_errors();
    return *s_icon;
}

static Gfx::Bitmap& move_icon()
{
    static RefPtr<Gfx::Bitmap> s_icon;
    if (!s_icon)
        s_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/move.png"sv).release_value_but_fixme_should_propagate_errors();
    return *s_icon;
}

Window::Window(Core::EventReceiver& parent, WindowType type)
    : Core::EventReceiver(&parent)
    , m_type(type)
    , m_icon(default_window_icon())
    , m_frame(*this)
{
    WindowManager::the().add_window(*this);
    frame().window_was_constructed({});
}

Window::Window(ConnectionFromClient& client, WindowType window_type, WindowMode window_mode, int window_id, int process_id, bool minimizable, bool closeable, bool frameless, bool resizable, bool fullscreen, Window* parent_window)
    : Core::EventReceiver(&client)
    , m_client(&client)
    , m_type(window_type)
    , m_mode(window_mode)
    , m_minimizable(minimizable)
    , m_closeable(closeable)
    , m_frameless(frameless)
    , m_resizable(resizable)
    , m_fullscreen(fullscreen)
    , m_window_id(window_id)
    , m_client_id(client.client_id())
    , m_icon(default_window_icon())
    , m_frame(*this)
    , m_process_id(process_id)
{
    if (parent_window)
        set_parent_window(*parent_window);
    if (!is_frameless() && type() == WindowType::Normal) {
        if (auto title_username_maybe = compute_title_username(&client); !title_username_maybe.is_error())
            m_title_username = title_username_maybe.release_value();
    }
    WindowManager::the().add_window(*this);
    frame().window_was_constructed({});
}

Window::~Window()
{
    // Detach from client at the start of teardown since we don't want
    // to confuse things by trying to send messages to it.
    m_client = nullptr;

    WindowManager::the().remove_window(*this);
}

void Window::destroy()
{
    m_destroyed = true;
    set_visible(false);
}

void Window::set_title(ByteString const& title)
{
    if (m_title == title)
        return;
    m_title = title;
    frame().invalidate_titlebar();
    WindowManager::the().notify_title_changed(*this);
}

void Window::set_rect(Gfx::IntRect const& rect)
{
    if (m_rect == rect)
        return;
    auto old_rect = m_rect;
    m_rect = rect;
    if (!m_should_show_window_content) {
        m_rect.set_height(0);
    }
    if (rect.is_empty()) {
        m_backing_store = nullptr;
    } else if (is_internal() && (!m_backing_store || old_rect.size() != rect.size())) {
        auto format = has_alpha_channel() ? Gfx::BitmapFormat::BGRA8888 : Gfx::BitmapFormat::BGRx8888;
        m_backing_store = Gfx::Bitmap::create(format, m_rect.size()).release_value_but_fixme_should_propagate_errors();
        m_backing_store_visible_size = m_rect.size();
    }

    if (m_floating_rect.is_empty())
        m_floating_rect = rect;

    invalidate(true, old_rect.size() != rect.size());
    m_frame.window_rect_changed(old_rect, rect);
    invalidate_last_rendered_screen_rects();
}

void Window::set_rect_without_repaint(Gfx::IntRect const& rect)
{
    VERIFY(rect.width() >= 0 && rect.height() >= 0);
    if (m_rect == rect)
        return;
    auto old_rect = m_rect;
    m_rect = rect;

    invalidate(true, old_rect.size() != rect.size());
    m_frame.window_rect_changed(old_rect, rect);
    invalidate_last_rendered_screen_rects();
}

bool Window::apply_minimum_size(Gfx::IntRect& rect)
{
    int new_width = max(m_minimum_size.width(), rect.width());
    int new_height = max(m_minimum_size.height(), rect.height());
    bool did_size_clamp = new_width != rect.width() || new_height != rect.height();

    rect.set_width(new_width);
    rect.set_height(new_height);

    return did_size_clamp;
}

void Window::set_minimum_size(Gfx::IntSize size)
{
    VERIFY(size.width() >= 0 && size.height() >= 0);
    if (m_minimum_size == size)
        return;
    m_minimum_size = size;
}

void Window::handle_mouse_event(MouseEvent const& event)
{
    set_automatic_cursor_tracking_enabled(event.buttons() != 0);

    switch (event.type()) {
    case Event::MouseMove:
        m_client->async_mouse_move(m_window_id, event.position(), (u32)event.button(), event.buttons(), event.modifiers(), event.wheel_delta_x(), event.wheel_delta_y(), event.wheel_raw_delta_x(), event.wheel_raw_delta_y());
        break;
    case Event::MouseDown:
        m_client->async_mouse_down(m_window_id, event.position(), (u32)event.button(), event.buttons(), event.modifiers(), event.wheel_delta_x(), event.wheel_delta_y(), event.wheel_raw_delta_x(), event.wheel_raw_delta_y());
        break;
    case Event::MouseDoubleClick:
        m_client->async_mouse_double_click(m_window_id, event.position(), (u32)event.button(), event.buttons(), event.modifiers(), event.wheel_delta_x(), event.wheel_delta_y(), event.wheel_raw_delta_x(), event.wheel_raw_delta_y());
        break;
    case Event::MouseUp:
        m_client->async_mouse_up(m_window_id, event.position(), (u32)event.button(), event.buttons(), event.modifiers(), event.wheel_delta_x(), event.wheel_delta_y(), event.wheel_raw_delta_x(), event.wheel_raw_delta_y());
        break;
    case Event::MouseWheel:
        m_client->async_mouse_wheel(m_window_id, event.position(), (u32)event.button(), event.buttons(), event.modifiers(), event.wheel_delta_x(), event.wheel_delta_y(), event.wheel_raw_delta_x(), event.wheel_raw_delta_y());
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

void Window::update_window_menu_items()
{
    if (!m_window_menu)
        return;

    m_window_menu_minimize_item->set_text(m_minimized_state != WindowMinimizedState::None ? "&Unminimize" : "Mi&nimize");
    m_window_menu_minimize_item->set_enabled(m_minimizable && !is_modal());

    m_window_menu_maximize_item->set_text(is_maximized() ? "&Restore" : "Ma&ximize");
    m_window_menu_maximize_item->set_enabled(m_resizable);

    m_window_menu_close_item->set_enabled(m_closeable);

    m_window_menu_move_item->set_enabled(m_minimized_state == WindowMinimizedState::None && !is_maximized() && !m_fullscreen);

    if (m_window_menu_always_on_top_item)
        m_window_menu_always_on_top_item->set_checked(m_always_on_top);

    m_window_menu->update_alt_shortcuts_for_items();
}

void Window::set_minimized(bool minimized)
{
    if ((m_minimized_state != WindowMinimizedState::None) == minimized)
        return;
    if (minimized && !m_minimizable)
        return;
    m_minimized_state = minimized ? WindowMinimizedState::Minimized : WindowMinimizedState::None;
    update_window_menu_items();

    if (!blocking_modal_window())
        start_minimize_animation();
    if (!minimized)
        request_update({ {}, size() });

    // Since a minimized window won't be visible we need to invalidate the last rendered
    // rectangles before the next occlusion calculation
    invalidate_last_rendered_screen_rects_now();

    WindowManager::the().notify_minimization_state_changed(*this);
}

void Window::set_hidden(bool hidden)
{
    if ((m_minimized_state != WindowMinimizedState::None) == hidden)
        return;
    if (hidden && !m_minimizable)
        return;
    m_minimized_state = hidden ? WindowMinimizedState::Hidden : WindowMinimizedState::None;
    update_window_menu_items();
    Compositor::the().invalidate_occlusions();
    Compositor::the().invalidate_screen(frame().render_rect());
    if (!blocking_modal_window())
        start_minimize_animation();
    if (!hidden)
        request_update({ {}, size() });
    WindowManager::the().notify_minimization_state_changed(*this);
}

void Window::set_minimizable(bool minimizable)
{
    if (m_minimizable == minimizable)
        return;
    m_minimizable = minimizable;
    update_window_menu_items();
}

void Window::set_closeable(bool closeable)
{
    if (m_closeable == closeable)
        return;
    m_closeable = closeable;
    update_window_menu_items();
}

void Window::set_taskbar_rect(Gfx::IntRect const& rect)
{
    if (m_taskbar_rect == rect)
        return;
    m_taskbar_rect = rect;
}

static Gfx::IntRect interpolate_rect(Gfx::IntRect const& from_rect, Gfx::IntRect const& to_rect, float progress)
{
    auto dx = to_rect.x() - from_rect.x();
    auto dy = to_rect.y() - from_rect.y();
    auto dw = to_rect.width() - from_rect.width();
    auto dh = to_rect.height() - from_rect.height();

    return Gfx::IntRect {
        from_rect.x() + ((float)dx * progress),
        from_rect.y() + ((float)dy * progress),
        from_rect.width() + ((float)dw * progress),
        from_rect.height() + ((float)dh * progress),
    };
}

void Window::start_minimize_animation()
{
    if (&window_stack() != &WindowManager::the().current_window_stack())
        return;
    if (!WindowManager::the().system_effects().animate_windows())
        return;
    if (is_modal()) {
        if (auto modeless = modeless_ancestor(); modeless) {
            auto rect = modeless->taskbar_rect();
            VERIFY(!rect.is_empty());
            m_taskbar_rect = rect;
        }
    }
    m_animation = Animation::create();
    m_animation->set_duration(150);
    m_animation->on_update = [this](float progress, Gfx::Painter& painter, Screen& screen, Gfx::DisjointIntRectSet& flush_rects) {
        Gfx::PainterStateSaver saver(painter);
        painter.set_draw_op(Gfx::Painter::DrawOp::Invert);

        auto from_rect = is_minimized() ? frame().rect() : taskbar_rect();
        auto to_rect = is_minimized() ? taskbar_rect() : frame().rect();

        auto rect = interpolate_rect(from_rect, to_rect, progress);

        painter.draw_rect(rect, Color::Transparent); // Color doesn't matter, we draw inverted
        flush_rects.add(rect.intersected(screen.rect()));
        Compositor::the().invalidate_screen(rect);
    };
    m_animation->on_stop = [this] {
        m_animation = nullptr;
    };
    m_animation->start();
}

void Window::start_launch_animation(Gfx::IntRect const& launch_origin_rect)
{
    if (&window_stack() != &WindowManager::the().current_window_stack())
        return;
    if (!WindowManager::the().system_effects().animate_windows())
        return;

    m_animation = Animation::create();
    m_animation->set_duration(150);
    m_animation->on_update = [this, launch_origin_rect](float progress, Gfx::Painter& painter, Screen& screen, Gfx::DisjointIntRectSet& flush_rects) {
        Gfx::PainterStateSaver saver(painter);
        painter.set_draw_op(Gfx::Painter::DrawOp::Invert);

        auto rect = interpolate_rect(launch_origin_rect, frame().rect(), progress);

        painter.draw_rect(rect, Color::Transparent); // Color doesn't matter, we draw inverted
        flush_rects.add(rect.intersected(screen.rect()));
        Compositor::the().invalidate_screen(rect);
    };
    m_animation->on_stop = [this] {
        m_animation = nullptr;
    };
    m_animation->start();
}

void Window::set_has_alpha_channel(bool value)
{
    if (m_has_alpha_channel == value)
        return;
    m_has_alpha_channel = value;
    Compositor::the().invalidate_occlusions();
}

void Window::set_occluded(bool occluded)
{
    if (m_occluded == occluded)
        return;
    m_occluded = occluded;
    WindowManager::the().notify_occlusion_state_changed(*this);
}

void Window::set_maximized(bool maximized)
{
    if (is_maximized() == maximized)
        return;
    if (maximized && (!is_resizable() || resize_aspect_ratio().has_value()))
        return;
    m_tile_type = maximized ? WindowTileType::Maximized : WindowTileType::None;
    update_window_menu_items();
    if (maximized) {
        exit_roll_up_mode();
        set_rect(WindowManager::the().tiled_window_rect(*this));
    } else {
        exit_roll_up_mode();
        set_rect(m_floating_rect);
    }

    m_frame.did_set_maximized({}, maximized);
    send_resize_event_to_client();
    send_move_event_to_client();
    set_default_positioned(false);

    WindowManager::the().notify_minimization_state_changed(*this);
}

void Window::set_always_on_top(bool always_on_top)
{
    if (m_always_on_top == always_on_top)
        return;

    m_always_on_top = always_on_top;
    update_window_menu_items();

    window_stack().move_always_on_top_windows_to_front();
    Compositor::the().invalidate_occlusions();
}

void Window::set_resizable(bool resizable)
{
    if (m_resizable == resizable)
        return;
    m_resizable = resizable;
    update_window_menu_items();
}

void Window::event(Core::Event& event)
{
    if (is_internal()) {
        VERIFY(parent());
        event.ignore();
        return;
    }

    if (blocking_modal_window() && type() != WindowType::Popup) {
        // Allow windows to process their inactivity after being blocked
        if (event.type() != Event::WindowDeactivated && event.type() != Event::WindowInputPreempted)
            return;
    }

    if (static_cast<Event&>(event).is_mouse_event())
        return handle_mouse_event(static_cast<MouseEvent const&>(event));

    switch (event.type()) {
    case Event::WindowEntered:
        m_client->async_window_entered(m_window_id);
        break;
    case Event::WindowLeft:
        m_client->async_window_left(m_window_id);
        break;
    case Event::KeyDown:
        handle_keydown_event(static_cast<KeyEvent const&>(event));
        break;
    case Event::KeyUp:
        m_client->async_key_up(m_window_id,
            (u32) static_cast<KeyEvent const&>(event).code_point(),
            (u32) static_cast<KeyEvent const&>(event).key(),
            (u8) static_cast<KeyEvent const&>(event).map_entry_index(),
            static_cast<KeyEvent const&>(event).modifiers(),
            (u32) static_cast<KeyEvent const&>(event).scancode());
        break;
    case Event::WindowActivated:
        m_client->async_window_activated(m_window_id);
        break;
    case Event::WindowDeactivated:
        m_client->async_window_deactivated(m_window_id);
        break;
    case Event::WindowInputPreempted:
        m_client->async_window_input_preempted(m_window_id);
        break;
    case Event::WindowInputRestored:
        m_client->async_window_input_restored(m_window_id);
        break;
    case Event::WindowCloseRequest:
        m_client->async_window_close_request(m_window_id);
        break;
    case Event::WindowResized:
        m_client->async_window_resized(m_window_id, static_cast<ResizeEvent const&>(event).rect());
        break;
    case Event::WindowMoved:
        m_client->async_window_moved(m_window_id, static_cast<MoveEvent const&>(event).rect());
        break;
    default:
        break;
    }
}

void Window::handle_keydown_event(KeyEvent const& event)
{
    if (event.modifiers() == Mod_Alt && event.key() == Key_Space && type() == WindowType::Normal && !is_frameless()) {
        auto position = frame().titlebar_rect().bottom_left().moved_up(1).translated(frame().rect().location());
        popup_window_menu(position, WindowMenuDefaultAction::Close);
        return;
    }
    if (event.modifiers() == Mod_Alt && event.code_point() && m_menubar.has_menus()) {
        // When handling alt shortcuts, we only care about the key that has been pressed in addition
        // to alt, not the code point that has been mapped to alt+[key], so we have to look up the
        // kernel map_entry_index value directly from the "unmodified" character map.
        auto character_map_or_error = Keyboard::CharacterMap::fetch_system_map();
        if (!character_map_or_error.is_error()) {
            auto& character_map = character_map_or_error.value();

            auto index = event.map_entry_index();
            if (index != 0xff) {
                u32 character = to_ascii_lowercase(character_map.character_map_data().map[index]);

                Menu* menu_to_open = nullptr;
                m_menubar.for_each_menu([&](Menu& menu) {
                    if (to_ascii_lowercase(menu.alt_shortcut_character()) == character) {
                        menu_to_open = &menu;
                        return IterationDecision::Break;
                    }
                    return IterationDecision::Continue;
                });

                if (menu_to_open) {
                    frame().open_menubar_menu(*menu_to_open);
                    if (!menu_to_open->is_empty())
                        menu_to_open->set_hovered_index(0);
                    return;
                }
            }
        }
    }
    m_client->async_key_down(m_window_id, (u32)event.code_point(), (u32)event.key(), (u8)event.map_entry_index(), event.modifiers(), (u32)event.scancode());
}

void Window::set_visible(bool b)
{
    if (m_visible == b)
        return;
    m_visible = b;

    WindowManager::the().reevaluate_hover_state_for_window(this);

    if (!m_visible)
        WindowManager::the().check_hide_geometry_overlay(*this);
    Compositor::the().invalidate_occlusions();
    if (m_visible) {
        invalidate(true);
    } else {
        // Since the window won't be visible we need to invalidate the last rendered
        // rectangles before the next occlusion calculation
        invalidate_last_rendered_screen_rects_now();
    }
}

void Window::set_frameless(bool frameless)
{
    if (m_frameless == frameless)
        return;
    m_frameless = frameless;
    if (m_visible) {
        Compositor::the().invalidate_occlusions();
        invalidate(true, true);
        invalidate_last_rendered_screen_rects();
    }
}

void Window::invalidate(bool invalidate_frame, bool re_render_frame)
{
    m_invalidated = true;
    m_invalidated_all = true;
    if (invalidate_frame && !m_invalidated_frame) {
        m_invalidated_frame = true;
    }
    if (re_render_frame)
        frame().set_dirty(true);
    m_dirty_rects.clear();
    Compositor::the().invalidate_window();
}

void Window::invalidate(Gfx::IntRect const& rect, bool invalidate_frame)
{
    if (type() == WindowType::Applet) {
        AppletManager::the().invalidate_applet(*this, rect);
        return;
    }

    if (invalidate_no_notify(rect, invalidate_frame))
        Compositor::the().invalidate_window();
}

bool Window::invalidate_no_notify(Gfx::IntRect const& rect, bool invalidate_frame)
{
    if (rect.is_empty())
        return false;
    if (m_invalidated_all) {
        if (invalidate_frame)
            m_invalidated_frame |= true;
        return false;
    }

    auto outer_rect = frame().render_rect();
    auto inner_rect = rect;
    inner_rect.translate_by(position());
    // FIXME: This seems slightly wrong; the inner rect shouldn't intersect the border part of the outer rect.
    inner_rect.intersect(outer_rect);
    if (inner_rect.is_empty())
        return false;

    m_invalidated = true;
    if (invalidate_frame)
        m_invalidated_frame |= true;
    m_dirty_rects.add(inner_rect.translated(-outer_rect.location()));
    return true;
}

void Window::invalidate_last_rendered_screen_rects()
{
    m_invalidate_last_render_rects = true;
    Compositor::the().invalidate_occlusions();
}

void Window::invalidate_last_rendered_screen_rects_now()
{
    // We can't wait for the next occlusion computation because the window will either no longer
    // be around or won't be visible anymore. So we need to invalidate the last rendered rects now.
    if (!m_opaque_rects.is_empty())
        Compositor::the().invalidate_screen(m_opaque_rects);
    if (!m_transparency_rects.is_empty())
        Compositor::the().invalidate_screen(m_transparency_rects);
    m_invalidate_last_render_rects = false;
    Compositor::the().invalidate_occlusions();
}

void Window::refresh_client_size()
{
    client()->async_window_resized(m_window_id, m_rect);
}

void Window::prepare_dirty_rects()
{
    if (m_invalidated_all) {
        if (m_invalidated_frame)
            m_dirty_rects = frame().render_rect();
        else
            m_dirty_rects = rect();
    } else {
        m_dirty_rects.move_by(frame().render_rect().location());
        if (m_invalidated_frame) {
            if (m_invalidated) {
                m_dirty_rects.add(frame().render_rect());
            } else {
                for (auto& rects : frame().render_rect().shatter(rect()))
                    m_dirty_rects.add(rects);
            }
        }
    }
}

void Window::clear_dirty_rects()
{
    m_invalidated_all = false;
    m_invalidated_frame = false;
    m_invalidated = false;
    m_dirty_rects.clear_with_capacity();
}

bool Window::is_active() const
{
    VERIFY(m_window_stack);
    return m_window_stack->active_window() == this;
}

Window* Window::blocking_modal_window()
{
    auto maybe_blocker = WindowManager::the().for_each_window_in_modal_chain(*this, [&](auto& window) {
        if (parent_window() == window.parent_window() && is_blocking())
            return IterationDecision::Continue;
        if (is_descendant_of(window))
            return IterationDecision::Continue;
        if (window.is_blocking() && this != &window)
            return IterationDecision::Break;
        return IterationDecision::Continue;
    });
    return maybe_blocker;
}

void Window::set_default_icon()
{
    m_icon = default_window_icon();
}

void Window::request_update(Gfx::IntRect const& rect, bool ignore_occlusion)
{
    if (rect.is_empty())
        return;
    if (m_pending_paint_rects.is_empty()) {
        deferred_invoke([this, ignore_occlusion] {
            client()->post_paint_message(*this, ignore_occlusion);
        });
    }
    m_pending_paint_rects.add(rect);
}

void Window::ensure_window_menu()
{
    if (!m_window_menu) {
        m_window_menu = Menu::construct(nullptr, -1, "(Window Menu)"_string);
        m_window_menu->set_window_menu_of(*this);

        auto minimize_item = make<MenuItem>(*m_window_menu, (unsigned)WindowMenuAction::MinimizeOrUnminimize, "");
        m_window_menu_minimize_item = minimize_item.ptr();
        m_window_menu->add_item(move(minimize_item));

        auto maximize_item = make<MenuItem>(*m_window_menu, (unsigned)WindowMenuAction::MaximizeOrRestore, "");
        m_window_menu_maximize_item = maximize_item.ptr();
        m_window_menu->add_item(move(maximize_item));

        auto move_item = make<MenuItem>(*m_window_menu, (unsigned)WindowMenuAction::Move, "&Move");
        m_window_menu_move_item = move_item.ptr();
        m_window_menu_move_item->set_icon(&move_icon());
        m_window_menu->add_item(move(move_item));

        m_window_menu->add_item(make<MenuItem>(*m_window_menu, MenuItem::Type::Separator));

        auto roll_up_item = make<MenuItem>(*m_window_menu, (unsigned)WindowMenuAction::ToggleWindowRollUp, "&Roll Up");
        m_window_menu_roll_up_item = roll_up_item.ptr();
        m_window_menu_roll_up_item->set_checked(!m_should_show_window_content);
        roll_up_item->set_checkable(true);
        m_window_menu->add_item(move(roll_up_item));

        auto menubar_visibility_item = make<MenuItem>(*m_window_menu, (unsigned)WindowMenuAction::ToggleMenubarVisibility, "Menu &Bar");
        m_window_menu_menubar_visibility_item = menubar_visibility_item.ptr();
        menubar_visibility_item->set_checkable(true);
        m_window_menu->add_item(move(menubar_visibility_item));

        m_window_menu->add_item(make<MenuItem>(*m_window_menu, MenuItem::Type::Separator));

        if (!is_modal()) {
            auto pin_item = make<MenuItem>(*m_window_menu, (unsigned)WindowMenuAction::ToggleAlwaysOnTop, "Always on &Top");
            m_window_menu_always_on_top_item = pin_item.ptr();
            m_window_menu_always_on_top_item->set_icon(&pin_icon());
            m_window_menu_always_on_top_item->set_checkable(true);
            m_window_menu->add_item(move(pin_item));
            m_window_menu->add_item(make<MenuItem>(*m_window_menu, MenuItem::Type::Separator));
        }

        auto add_to_quick_launch_item = make<MenuItem>(*m_window_menu, (unsigned)WindowMenuAction::AddToQuickLaunch, "&Add to Quick Launch");
        m_window_menu_add_to_quick_launch_item = add_to_quick_launch_item.ptr();
        m_window_menu_add_to_quick_launch_item->set_icon(&pin_icon());
        m_window_menu->add_item(move(add_to_quick_launch_item));

        m_window_menu->add_item(make<MenuItem>(*m_window_menu, MenuItem::Type::Separator));

        auto close_item = make<MenuItem>(*m_window_menu, (unsigned)WindowMenuAction::Close, "&Close");
        m_window_menu_close_item = close_item.ptr();
        m_window_menu_close_item->set_icon(&close_icon());
        m_window_menu_close_item->set_default(true);
        m_window_menu->add_item(move(close_item));

        m_window_menu->on_item_activation = [&](auto& item) {
            handle_window_menu_action(static_cast<WindowMenuAction>(item.identifier()));
        };

        update_window_menu_items();
    }
}

void Window::handle_window_menu_action(WindowMenuAction action)
{
    switch (action) {
    case WindowMenuAction::MinimizeOrUnminimize:
        WindowManager::the().minimize_windows(*this, m_minimized_state == WindowMinimizedState::None);
        if (m_minimized_state == WindowMinimizedState::None)
            WindowManager::the().move_to_front_and_make_active(*this);
        break;
    case WindowMenuAction::MaximizeOrRestore:
        WindowManager::the().maximize_windows(*this, !is_maximized());
        WindowManager::the().move_to_front_and_make_active(*this);
        break;
    case WindowMenuAction::Move:
        WindowManager::the().start_window_move(*this, ScreenInput::the().cursor_location());
        break;
    case WindowMenuAction::Close:
        request_close();
        break;
    case WindowMenuAction::ToggleMenubarVisibility: {
        auto& item = *m_window_menu->item_by_identifier((unsigned)action);
        frame().invalidate();
        item.set_checked(!item.is_checked());
        m_should_show_menubar = item.is_checked();
        frame().invalidate();
        recalculate_rect();
        invalidate_last_rendered_screen_rects();
        break;
    }
    case WindowMenuAction::ToggleWindowRollUp: {
        auto& item = *m_window_menu->item_by_identifier((unsigned)action);
        frame().invalidate();
        item.set_checked(!item.is_checked());
        m_should_show_window_content = !item.is_checked();
        if (!m_should_show_window_content) {
            m_saved_before_roll_up_rect = m_rect;
            m_rect.set_height(0);
        } else {
            m_rect.set_height(m_saved_before_roll_up_rect.height());
        }

        frame().invalidate();
        recalculate_rect();
        invalidate_last_rendered_screen_rects();
        break;
    }
    case WindowMenuAction::ToggleAlwaysOnTop: {
        auto& item = *m_window_menu->item_by_identifier((unsigned)action);
        auto new_is_checked = !item.is_checked();
        item.set_checked(new_is_checked);
        WindowManager::the().set_always_on_top(*this, new_is_checked);
        break;
    }
    case WindowMenuAction::AddToQuickLaunch: {
        if (m_process_id.has_value())
            WindowManager::the().on_add_to_quick_launch(m_process_id.value());
        break;
    }
    }
}

void Window::popup_window_menu(Gfx::IntPoint position, WindowMenuDefaultAction default_action)
{
    ensure_window_menu();
    if (default_action == WindowMenuDefaultAction::BasedOnWindowState) {
        // When clicked on the task bar, determine the default action
        if (!is_active() && !is_minimized())
            default_action = WindowMenuDefaultAction::None;
        else if (is_minimized())
            default_action = WindowMenuDefaultAction::Unminimize;
        else
            default_action = WindowMenuDefaultAction::Minimize;
    }
    m_window_menu_minimize_item->set_default(default_action == WindowMenuDefaultAction::Minimize || default_action == WindowMenuDefaultAction::Unminimize);
    m_window_menu_minimize_item->set_icon(m_minimized_state != WindowMinimizedState::None ? nullptr : &minimize_icon());
    m_window_menu_maximize_item->set_default(default_action == WindowMenuDefaultAction::Maximize || default_action == WindowMenuDefaultAction::Restore);
    m_window_menu_maximize_item->set_icon(is_maximized() ? &restore_icon() : &maximize_icon());
    m_window_menu_close_item->set_default(default_action == WindowMenuDefaultAction::Close);
    m_window_menu_menubar_visibility_item->set_enabled(m_menubar.has_menus());
    m_window_menu_menubar_visibility_item->set_checked(m_menubar.has_menus() && m_should_show_menubar);

    m_window_menu->popup(position);
}

void Window::window_menu_activate_default()
{
    ensure_window_menu();
    m_window_menu->activate_default();
}

void Window::request_close()
{
    if (is_destroyed())
        return;

    Event close_request(Event::WindowCloseRequest);
    event(close_request);
}

void Window::set_fullscreen(bool fullscreen)
{
    if (m_fullscreen == fullscreen)
        return;
    m_fullscreen = fullscreen;
    Gfx::IntRect new_window_rect = m_rect;
    if (m_fullscreen) {
        m_saved_nonfullscreen_rect = m_rect;
        new_window_rect = Screen::main().rect(); // TODO: We should support fullscreen on any screen
    } else if (!m_saved_nonfullscreen_rect.is_empty()) {
        new_window_rect = m_saved_nonfullscreen_rect;
    }

    set_rect(new_window_rect);
    send_resize_event_to_client();
    send_move_event_to_client();
}

WindowTileType Window::tile_type_based_on_rect(Gfx::IntRect const& rect) const
{
    auto& window_screen = Screen::closest_to_rect(this->rect()); // based on currently used rect
    auto tile_type = WindowTileType::None;
    if (window_screen.rect().contains(rect)) {
        auto current_tile_type = this->tile_type();
        bool tiling_to_top = current_tile_type == WindowTileType::Top || current_tile_type == WindowTileType::TopLeft || current_tile_type == WindowTileType::TopRight;
        bool tiling_to_bottom = current_tile_type == WindowTileType::Bottom || current_tile_type == WindowTileType::BottomLeft || current_tile_type == WindowTileType::BottomRight;
        bool tiling_to_left = current_tile_type == WindowTileType::Left || current_tile_type == WindowTileType::TopLeft || current_tile_type == WindowTileType::BottomLeft;
        bool tiling_to_right = current_tile_type == WindowTileType::Right || current_tile_type == WindowTileType::TopRight || current_tile_type == WindowTileType::BottomRight;

        auto ideal_tiled_rect = WindowManager::the().tiled_window_rect(*this, window_screen, current_tile_type);
        bool same_top = ideal_tiled_rect.top() == rect.top();
        bool same_left = ideal_tiled_rect.left() == rect.left();
        bool same_right = ideal_tiled_rect.right() == rect.right();
        bool same_bottom = ideal_tiled_rect.bottom() == rect.bottom();

        // Try to find the most suitable tile type. For example, if a window is currently tiled to the BottomRight and
        // the window is resized upwards as to where it perfectly touches the screen's top border, then the more suitable
        // tile type would be Right, as three sides are lined up perfectly.
        if (tiling_to_top && same_top && same_left && same_right)
            return WindowTileType::Top;
        else if ((tiling_to_top || tiling_to_left) && same_top && same_left)
            return rect.bottom() == WindowManager::the().tiled_window_rect(*this, window_screen, WindowTileType::Bottom).bottom() ? WindowTileType::Left : WindowTileType::TopLeft;
        else if ((tiling_to_top || tiling_to_right) && same_top && same_right)
            return rect.bottom() == WindowManager::the().tiled_window_rect(*this, window_screen, WindowTileType::Bottom).bottom() ? WindowTileType::Right : WindowTileType::TopRight;
        else if (tiling_to_left && same_left && same_top && same_bottom)
            return WindowTileType::Left;
        else if (tiling_to_right && same_right && same_top && same_bottom)
            return WindowTileType::Right;
        else if (tiling_to_bottom && same_bottom && same_left && same_right)
            return WindowTileType::Bottom;
        else if ((tiling_to_bottom || tiling_to_left) && same_bottom && same_left)
            return rect.top() == WindowManager::the().tiled_window_rect(*this, window_screen, WindowTileType::Left).top() ? WindowTileType::Left : WindowTileType::BottomLeft;
        else if ((tiling_to_bottom || tiling_to_right) && same_bottom && same_right)
            return rect.top() == WindowManager::the().tiled_window_rect(*this, window_screen, WindowTileType::Right).top() ? WindowTileType::Right : WindowTileType::BottomRight;
    }
    return tile_type;
}

void Window::check_untile_due_to_resize(Gfx::IntRect const& new_rect)
{
    auto new_tile_type = tile_type_based_on_rect(new_rect);
    if constexpr (RESIZE_DEBUG) {
        if (new_tile_type == WindowTileType::None) {
            auto current_rect = rect();
            auto& window_screen = Screen::closest_to_rect(current_rect);
            if (!(window_screen.rect().contains(new_rect)))
                dbgln("Untiling because new rect {} does not fit into screen #{} rect {}", new_rect, window_screen.index(), window_screen.rect());
            else
                dbgln("Untiling because new rect {} does not touch screen #{} rect {}", new_rect, window_screen.index(), window_screen.rect());
        } else if (new_tile_type != m_tile_type)
            dbgln("Changing tile type from {} to {}", (int)m_tile_type, (int)new_tile_type);
    }
    m_tile_type = new_tile_type;
}

bool Window::set_untiled()
{
    if (m_tile_type == WindowTileType::None)
        return false;
    VERIFY(!resize_aspect_ratio().has_value());

    m_tile_type = WindowTileType::None;
    tile_type_changed();
    return true;
}

void Window::set_tiled(WindowTileType tile_type, Optional<Screen const&> tile_on_screen)
{
    VERIFY(tile_type != WindowTileType::None);

    if (m_tile_type == tile_type)
        return;

    if (resize_aspect_ratio().has_value())
        return;

    if (is_maximized())
        set_maximized(false);

    m_tile_type = tile_type;
    tile_type_changed(tile_on_screen);
}

void Window::exit_roll_up_mode()
{
    if (m_should_show_window_content)
        return;
    m_rect.set_height(m_saved_before_roll_up_rect.height());
    m_should_show_window_content = true;
    if (m_window_menu_roll_up_item)
        m_window_menu_roll_up_item->set_checked(false);
}

void Window::tile_type_changed(Optional<Screen const&> tile_on_screen)
{
    if (m_tile_type != WindowTileType::None) {
        exit_roll_up_mode();
        set_rect(WindowManager::the().tiled_window_rect(*this, tile_on_screen, m_tile_type));
    } else {
        set_rect(m_floating_rect);
    }

    send_resize_event_to_client();
    send_move_event_to_client();
}

void Window::send_resize_event_to_client()
{
    Core::EventLoop::current().post_event(*this, make<ResizeEvent>(m_rect));
}

void Window::send_move_event_to_client()
{
    Core::EventLoop::current().post_event(*this, make<MoveEvent>(m_rect));
}

void Window::detach_client(Badge<ConnectionFromClient>)
{
    m_client = nullptr;
}

void Window::recalculate_rect()
{
    if (!is_resizable())
        return;

    bool send_event = true;
    if (is_tiled()) {
        set_rect(WindowManager::the().tiled_window_rect(*this, {}, m_tile_type));
    } else if (type() == WindowType::Desktop) {
        set_rect(WindowManager::the().arena_rect_for_type(Screen::main(), WindowType::Desktop));
    } else {
        send_event = false;
    }

    if (send_event) {
        send_resize_event_to_client();
    }
}

void Window::add_child_window(Window& child_window)
{
    m_child_windows.append(child_window);
}

void Window::set_parent_window(Window& parent_window)
{
    VERIFY(!m_parent_window);
    m_parent_window = parent_window;
    parent_window.add_child_window(*this);
}

Window* Window::modeless_ancestor()
{
    if (!is_modal())
        return this;
    for (auto parent = m_parent_window; parent; parent = parent->parent_window()) {
        if (!parent->is_modal())
            return parent;
    }
    return nullptr;
}

void Window::set_progress(Optional<int> progress)
{
    if (m_progress == progress)
        return;

    m_progress = progress;
    WindowManager::the().notify_progress_changed(*this);
}

bool Window::is_descendant_of(Window& window) const
{
    for (auto* parent = parent_window(); parent; parent = parent->parent_window())
        if (parent == &window)
            return true;
    return false;
}

Optional<HitTestResult> Window::hit_test(Gfx::IntPoint position, bool include_frame)
{
    if (!m_hit_testing_enabled)
        return {};
    // We need to check the (possibly constrained) render rect to make sure
    // we don't hit-test on a window that is constrained to a screen, but somehow
    // (partially) moved into another screen where it's not rendered
    if (!frame().rect().intersected(frame().render_rect()).contains(position))
        return {};
    if (!rect().contains(position)) {
        if (include_frame)
            return frame().hit_test(position);
        return {};
    }
    bool hit = false;
    u8 threshold = alpha_hit_threshold() * 255;
    if (threshold == 0 || !m_backing_store || !m_backing_store->has_alpha_channel()) {
        hit = true;
    } else {
        auto relative_point = position.translated(-rect().location()) * m_backing_store->scale();
        u8 alpha = 0xff;
        if (m_backing_store->rect().contains(relative_point))
            alpha = m_backing_store->get_pixel(relative_point).alpha();
        hit = alpha >= threshold;
    }
    if (!hit)
        return {};
    return HitTestResult {
        .window = *this,
        .screen_position = position,
        .window_relative_position = position.translated(-rect().location()),
        .is_frame_hit = false,
    };
}

void Window::add_menu(Menu& menu)
{
    m_menubar.add_menu(menu, rect());
    Compositor::the().invalidate_occlusions();
    frame().invalidate();
}

void Window::invalidate_menubar()
{
    if (!m_should_show_menubar || !m_menubar.has_menus())
        return;
    frame().invalidate_menubar();
}

void Window::set_modified(bool modified)
{
    if (m_modified == modified)
        return;

    m_modified = modified;
    WindowManager::the().notify_modified_changed(*this);
    frame().set_button_icons();
    frame().invalidate_titlebar();
}

ByteString Window::computed_title() const
{
    ByteString title = m_title.replace("[*]"sv, is_modified() ? " (*)"sv : ""sv, ReplaceMode::FirstOnly);
    if (m_title_username.has_value())
        title = ByteString::formatted("{} [{}]", title, m_title_username.value());
    if (client() && client()->is_unresponsive())
        return ByteString::formatted("{} (Not responding)", title);
    return title;
}

ErrorOr<Optional<ByteString>> Window::compute_title_username(ConnectionFromClient* client)
{
    if (!client)
        return Error::from_string_literal("Tried to compute title username without a client");
    auto stats = TRY(Core::ProcessStatisticsReader::get_all(true));
    pid_t client_pid = TRY(client->socket().peer_pid());
    auto client_stat = stats.processes.first_matching([&](auto& stat) { return stat.pid == client_pid; });
    if (!client_stat.has_value())
        return Error::from_string_literal("Failed to find client process stat");
    pid_t login_session_pid = TRY(Core::SessionManagement::root_session_id(client_pid));
    auto login_session_stat = stats.processes.first_matching([&](auto& stat) { return stat.pid == login_session_pid; });
    if (!login_session_stat.has_value())
        return Error::from_string_literal("Failed to find login process stat");
    if (login_session_stat.value().uid == client_stat.value().uid)
        return Optional<ByteString> {};
    return client_stat.value().username;
}

}
