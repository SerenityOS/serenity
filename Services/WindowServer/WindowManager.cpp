/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "WindowManager.h"
#include "Compositor.h"
#include "EventLoop.h"
#include "Menu.h"
#include "MenuBar.h"
#include "MenuItem.h"
#include "Screen.h"
#include "Window.h"
#include <AK/LogStream.h>
#include <AK/SharedBuffer.h>
#include <AK/StdLibExtras.h>
#include <AK/Vector.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/Painter.h>
#include <LibGfx/StylePainter.h>
#include <LibGfx/SystemTheme.h>
#include <WindowServer/AppletManager.h>
#include <WindowServer/Button.h>
#include <WindowServer/ClientConnection.h>
#include <WindowServer/Cursor.h>
#include <WindowServer/WindowClientEndpoint.h>
#include <errno.h>
#include <serenity.h>
#include <stdio.h>
#include <time.h>
#include <unistd.h>

//#define WINDOWMANAGER_DEBUG
//#define RESIZE_DEBUG
//#define MOVE_DEBUG
//#define DOUBLECLICK_DEBUG

namespace WindowServer {

static WindowManager* s_the;

WindowManager& WindowManager::the()
{
    ASSERT(s_the);
    return *s_the;
}

WindowManager::WindowManager(const Gfx::PaletteImpl& palette)
    : m_palette(palette)
{
    s_the = this;

    reload_config(false);

    Compositor::the().invalidate_screen();
    Compositor::the().invalidate_occlusions();
    Compositor::the().compose();
}

WindowManager::~WindowManager()
{
}

NonnullRefPtr<Cursor> WindowManager::get_cursor(const String& name, const Gfx::IntPoint& hotspot)
{
    auto path = m_config->read_entry("Cursor", name, "/res/cursors/arrow.png");
    auto gb = Gfx::Bitmap::load_from_file(path);
    if (gb)
        return Cursor::create(*gb, hotspot);
    return Cursor::create(*Gfx::Bitmap::load_from_file("/res/cursors/arrow.png"));
}

NonnullRefPtr<Cursor> WindowManager::get_cursor(const String& name)
{
    auto path = m_config->read_entry("Cursor", name, "/res/cursors/arrow.png");
    auto gb = Gfx::Bitmap::load_from_file(path);

    if (gb)
        return Cursor::create(*gb);
    return Cursor::create(*Gfx::Bitmap::load_from_file("/res/cursors/arrow.png"));
}

void WindowManager::reload_config(bool set_screen)
{
    m_config = Core::ConfigFile::open("/etc/WindowServer/WindowServer.ini");

    m_double_click_speed = m_config->read_num_entry("Input", "DoubleClickSpeed", 250);

    if (set_screen) {
        set_resolution(m_config->read_num_entry("Screen", "Width", 1920), m_config->read_num_entry("Screen", "Height", 1080));
    }

    m_arrow_cursor = get_cursor("Arrow", { 2, 2 });
    m_hand_cursor = get_cursor("Hand", { 8, 4 });
    m_help_cursor = get_cursor("Help", { 1, 1 });
    m_resize_horizontally_cursor = get_cursor("ResizeH");
    m_resize_vertically_cursor = get_cursor("ResizeV");
    m_resize_diagonally_tlbr_cursor = get_cursor("ResizeDTLBR");
    m_resize_diagonally_bltr_cursor = get_cursor("ResizeDBLTR");
    m_resize_column_cursor = get_cursor("ResizeColumn");
    m_resize_row_cursor = get_cursor("ResizeRow");
    m_i_beam_cursor = get_cursor("IBeam");
    m_disallowed_cursor = get_cursor("Disallowed");
    m_move_cursor = get_cursor("Move");
    m_drag_cursor = get_cursor("Drag");
    m_wait_cursor = get_cursor("Wait");
}

const Gfx::Font& WindowManager::font() const
{
    return Gfx::Font::default_font();
}

const Gfx::Font& WindowManager::window_title_font() const
{
    return Gfx::Font::default_bold_font();
}

bool WindowManager::set_resolution(int width, int height)
{
    bool success = Compositor::the().set_resolution(width, height);
    MenuManager::the().set_needs_window_resize();
    ClientConnection::for_each_client([&](ClientConnection& client) {
        client.notify_about_new_screen_rect(Screen::the().rect());
    });
    if (success) {
        for_each_window([](Window& window) {
            window.recalculate_rect();
            return IterationDecision::Continue;
        });
    }
    if (m_config) {
        if (success) {
            dbg() << "Saving resolution: " << Gfx::IntSize(width, height) << " to config file at " << m_config->file_name();
            m_config->write_num_entry("Screen", "Width", width);
            m_config->write_num_entry("Screen", "Height", height);
            m_config->sync();
        } else {
            dbg() << "Saving fallback resolution: " << resolution() << " to config file at " << m_config->file_name();
            m_config->write_num_entry("Screen", "Width", resolution().width());
            m_config->write_num_entry("Screen", "Height", resolution().height());
            m_config->sync();
        }
    }
    return success;
}

Gfx::IntSize WindowManager::resolution() const
{
    return Screen::the().size();
}

void WindowManager::add_window(Window& window)
{
    bool is_first_window = m_windows_in_order.is_empty();

    m_windows_in_order.append(&window);

    if (window.is_fullscreen()) {
        Core::EventLoop::current().post_event(window, make<ResizeEvent>(Screen::the().rect()));
        window.set_rect(Screen::the().rect());
    }

    if (window.type() != WindowType::Desktop || is_first_window)
        set_active_window(&window);

    if (m_switcher.is_visible() && window.type() != WindowType::WindowSwitcher)
        m_switcher.refresh();

    Compositor::the().invalidate_occlusions();

    if (window.listens_to_wm_events()) {
        for_each_window([&](Window& other_window) {
            if (&window != &other_window) {
                tell_wm_listener_about_window(window, other_window);
                tell_wm_listener_about_window_icon(window, other_window);
            }
            return IterationDecision::Continue;
        });
    }

    tell_wm_listeners_window_state_changed(window);
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
    if (m_windows_in_order.tail() != &window)
        window.invalidate();
    m_windows_in_order.remove(&window);
    m_windows_in_order.append(&window);

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
    m_windows_in_order.remove(&window);
    auto* active = active_window();
    auto* active_input = active_input_window();
    if (active == &window || active_input == &window || (active && window.is_descendant_of(*active)) || (active_input && active_input != active && window.is_descendant_of(*active_input)))
        pick_new_active_window(&window);

    Compositor::the().invalidate_screen(window.frame().rect());

    if (m_switcher.is_visible() && window.type() != WindowType::WindowSwitcher)
        m_switcher.refresh();

    Compositor::the().invalidate_occlusions();

    for_each_window_listening_to_wm_events([&window](Window& listener) {
        if (!(listener.wm_event_mask() & WMEventMask::WindowRemovals))
            return IterationDecision::Continue;
        if (!window.is_internal() && !window.is_modal())
            listener.client()->post_message(Messages::WindowClient::WM_WindowRemoved(listener.window_id(), window.client_id(), window.window_id()));
        return IterationDecision::Continue;
    });
}

void WindowManager::tell_wm_listener_about_window(Window& listener, Window& window)
{
    if (!(listener.wm_event_mask() & WMEventMask::WindowStateChanges))
        return;
    if (window.is_internal())
        return;
    auto* parent = window.parent_window();
    listener.client()->post_message(Messages::WindowClient::WM_WindowStateChanged(listener.window_id(), window.client_id(), window.window_id(), parent ? parent->client_id() : -1, parent ? parent->window_id() : -1, window.is_active(), window.is_minimized(), window.is_modal_dont_unparent(), window.is_frameless(), (i32)window.type(), window.title(), window.rect(), window.progress()));
}

void WindowManager::tell_wm_listener_about_window_rect(Window& listener, Window& window)
{
    if (!(listener.wm_event_mask() & WMEventMask::WindowRectChanges))
        return;
    if (window.is_internal())
        return;
    listener.client()->post_message(Messages::WindowClient::WM_WindowRectChanged(listener.window_id(), window.client_id(), window.window_id(), window.rect()));
}

void WindowManager::tell_wm_listener_about_window_icon(Window& listener, Window& window)
{
    if (!(listener.wm_event_mask() & WMEventMask::WindowIconChanges))
        return;
    if (window.is_internal())
        return;
    if (window.icon().shbuf_id() == -1)
        return;
#ifdef WINDOWMANAGER_DEBUG
    dbg() << "WindowServer: Sharing icon buffer " << window.icon().shbuf_id() << " with PID " << listener.client()->client_pid();
#endif
    if (shbuf_allow_pid(window.icon().shbuf_id(), listener.client()->client_pid()) < 0) {
        ASSERT_NOT_REACHED();
    }
    listener.client()->post_message(Messages::WindowClient::WM_WindowIconBitmapChanged(listener.window_id(), window.client_id(), window.window_id(), window.icon().shbuf_id(), window.icon().size()));
}

void WindowManager::tell_wm_listeners_window_state_changed(Window& window)
{
    for_each_window_listening_to_wm_events([&](Window& listener) {
        tell_wm_listener_about_window(listener, window);
        return IterationDecision::Continue;
    });
}

void WindowManager::tell_wm_listeners_window_icon_changed(Window& window)
{
    for_each_window_listening_to_wm_events([&](Window& listener) {
        tell_wm_listener_about_window_icon(listener, window);
        return IterationDecision::Continue;
    });
}

void WindowManager::tell_wm_listeners_window_rect_changed(Window& window)
{
    for_each_window_listening_to_wm_events([&](Window& listener) {
        tell_wm_listener_about_window_rect(listener, window);
        return IterationDecision::Continue;
    });
}

void WindowManager::notify_title_changed(Window& window)
{
    if (window.type() != WindowType::Normal)
        return;
#ifdef WINDOWMANAGER_DEBUG
    dbg() << "[WM] Window{" << &window << "} title set to \"" << window.title() << '"';
#endif
    if (m_switcher.is_visible())
        m_switcher.refresh();

    tell_wm_listeners_window_state_changed(window);
}

void WindowManager::notify_modal_unparented(Window& window)
{
    if (window.type() != WindowType::Normal)
        return;
#ifdef WINDOWMANAGER_DEBUG
    dbg() << "[WM] Modal Window{" << &window << "} was unparented";
#endif
    if (m_switcher.is_visible())
        m_switcher.refresh();

    tell_wm_listeners_window_state_changed(window);
}

void WindowManager::notify_rect_changed(Window& window, const Gfx::IntRect& old_rect, const Gfx::IntRect& new_rect)
{
    UNUSED_PARAM(old_rect);
    UNUSED_PARAM(new_rect);
#ifdef RESIZE_DEBUG
    dbg() << "[WM] Window " << &window << " rect changed " << old_rect << " -> " << new_rect;
#endif
    if (m_switcher.is_visible() && window.type() != WindowType::WindowSwitcher)
        m_switcher.refresh();

    tell_wm_listeners_window_rect_changed(window);

    if (window.type() == WindowType::MenuApplet)
        AppletManager::the().calculate_applet_rects(MenuManager::the().window());

    MenuManager::the().refresh();
}

void WindowManager::notify_opacity_changed(Window&)
{
    Compositor::the().invalidate_occlusions();
}

void WindowManager::notify_minimization_state_changed(Window& window)
{
    tell_wm_listeners_window_state_changed(window);

    if (window.client())
        window.client()->post_message(Messages::WindowClient::WindowStateChanged(window.window_id(), window.is_minimized(), window.is_occluded()));

    if (window.is_active() && window.is_minimized())
        pick_new_active_window(&window);
}

void WindowManager::notify_occlusion_state_changed(Window& window)
{
    if (window.client())
        window.client()->post_message(Messages::WindowClient::WindowStateChanged(window.window_id(), window.is_minimized(), window.is_occluded()));
}

void WindowManager::notify_progress_changed(Window& window)
{
    tell_wm_listeners_window_state_changed(window);
}

bool WindowManager::pick_new_active_window(Window* previous_active)
{
    bool new_window_picked = false;
    Window* first_candidate = nullptr;
    for_each_visible_window_of_type_from_front_to_back(WindowType::Normal, [&](Window& candidate) {
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

void WindowManager::start_window_move(Window& window, const MouseEvent& event)
{
#ifdef MOVE_DEBUG
    dbg() << "[WM] Begin moving Window{" << &window << "}";
#endif
    move_to_front_and_make_active(window);
    m_move_window = window.make_weak_ptr();
    m_move_window->set_default_positioned(false);
    m_move_origin = event.position();
    m_move_window_origin = window.position();
    window.invalidate();
}

void WindowManager::start_window_resize(Window& window, const Gfx::IntPoint& position, MouseButton button)
{
    move_to_front_and_make_active(window);
    constexpr ResizeDirection direction_for_hot_area[3][3] = {
        { ResizeDirection::UpLeft, ResizeDirection::Up, ResizeDirection::UpRight },
        { ResizeDirection::Left, ResizeDirection::None, ResizeDirection::Right },
        { ResizeDirection::DownLeft, ResizeDirection::Down, ResizeDirection::DownRight },
    };
    Gfx::IntRect outer_rect = window.frame().rect();
    if (!outer_rect.contains(position)) {
        // FIXME: This used to be an ASSERT but crashing WindowServer over this seems silly.
        dbg() << "FIXME: !outer_rect.contains(position): outer_rect=" << outer_rect << ", position=" << position;
    }
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
    dbg() << "[WM] Begin resizing Window{" << &window << "}";
#endif
    m_resizing_mouse_button = button;
    m_resize_window = window.make_weak_ptr();
    m_resize_origin = position;
    m_resize_window_original_rect = window.rect();

    window.invalidate();

    if (hot_area_row == 0 || hot_area_column == 0) {
        m_resize_window->set_default_positioned(false);
    }
}

void WindowManager::start_window_resize(Window& window, const MouseEvent& event)
{
    start_window_resize(window, event.position(), event.button());
}

bool WindowManager::process_ongoing_window_move(MouseEvent& event, Window*& hovered_window)
{
    if (!m_move_window)
        return false;
    if (event.type() == Event::MouseUp && event.button() == MouseButton::Left) {
#ifdef MOVE_DEBUG
        dbg() << "[WM] Finish moving Window{" << m_move_window << "}";
#endif

        m_move_window->invalidate();
        if (m_move_window->rect().contains(event.position()))
            hovered_window = m_move_window;
        if (m_move_window->is_resizable()) {
            process_event_for_doubleclick(*m_move_window, event);
            if (event.type() == Event::MouseDoubleClick) {
#if defined(DOUBLECLICK_DEBUG)
                dbg() << "[WM] Click up became doubleclick!";
#endif
                m_move_window->set_maximized(!m_move_window->is_maximized());
            }
        }
        m_move_window = nullptr;
        return true;
    }
    if (event.type() == Event::MouseMove) {

#ifdef MOVE_DEBUG
        dbg() << "[WM] Moving, origin: " << m_move_origin << ", now: " << event.position();
        if (m_move_window->is_maximized()) {
            dbg() << "  [!] The window is still maximized. Not moving yet.";
        }

#endif

        const int maximization_deadzone = 2;

        if (m_move_window->is_maximized()) {
            auto pixels_moved_from_start = event.position().pixels_moved(m_move_origin);
            // dbg() << "[WM] " << pixels_moved_from_start << " moved since start of window move";
            if (pixels_moved_from_start > 5) {
                // dbg() << "[WM] de-maximizing window";
                m_move_origin = event.position();
                if (m_move_origin.y() <= maximization_deadzone)
                    return true;
                auto width_before_resize = m_move_window->width();
                m_move_window->set_maximized(false);
                m_move_window->move_to(m_move_origin.x() - (m_move_window->width() * ((float)m_move_origin.x() / width_before_resize)), m_move_origin.y());
                m_move_window_origin = m_move_window->position();
            }
        } else {
            bool is_resizable = m_move_window->is_resizable();
            auto pixels_moved_from_start = event.position().pixels_moved(m_move_origin);
            const int tiling_deadzone = 5;

            if (is_resizable && event.y() <= maximization_deadzone) {
                m_move_window->set_tiled(WindowTileType::None);
                m_move_window->set_maximized(true);
                return true;
            }
            if (is_resizable && event.x() <= tiling_deadzone) {
                m_move_window->set_tiled(WindowTileType::Left);
            } else if (is_resizable && event.x() >= Screen::the().width() - tiling_deadzone) {
                m_move_window->set_tiled(WindowTileType::Right);
            } else if (pixels_moved_from_start > 5 || m_move_window->tiled() == WindowTileType::None) {
                m_move_window->set_tiled(WindowTileType::None);
                Gfx::IntPoint pos = m_move_window_origin.translated(event.position() - m_move_origin);
                m_move_window->set_position_without_repaint(pos);
                if (m_move_window->rect().contains(event.position()))
                    hovered_window = m_move_window;
            }
            return true;
        }
    }
    return false;
}

bool WindowManager::process_ongoing_window_resize(const MouseEvent& event, Window*& hovered_window)
{
    if (!m_resize_window)
        return false;

    if (event.type() == Event::MouseUp && event.button() == m_resizing_mouse_button) {
#ifdef RESIZE_DEBUG
        dbg() << "[WM] Finish resizing Window{" << m_resize_window << "}";
#endif
        Core::EventLoop::current().post_event(*m_resize_window, make<ResizeEvent>(m_resize_window->rect()));
        m_resize_window->invalidate();
        if (m_resize_window->rect().contains(event.position()))
            hovered_window = m_resize_window;
        m_resize_window = nullptr;
        m_resizing_mouse_button = MouseButton::None;
        return true;
    }

    if (event.type() != Event::MouseMove)
        return false;

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
    Gfx::IntSize minimum_size { 50, 50 };

    new_rect.set_width(max(minimum_size.width(), new_rect.width() + change_w));
    new_rect.set_height(max(minimum_size.height(), new_rect.height() + change_h));

    if (!m_resize_window->size_increment().is_null()) {
        int horizontal_incs = (new_rect.width() - m_resize_window->base_size().width()) / m_resize_window->size_increment().width();
        new_rect.set_width(m_resize_window->base_size().width() + horizontal_incs * m_resize_window->size_increment().width());
        int vertical_incs = (new_rect.height() - m_resize_window->base_size().height()) / m_resize_window->size_increment().height();
        new_rect.set_height(m_resize_window->base_size().height() + vertical_incs * m_resize_window->size_increment().height());
    }

    if (m_resize_window->resize_aspect_ratio().has_value()) {
        auto& ratio = m_resize_window->resize_aspect_ratio().value();
        if (abs(change_w) > abs(change_h)) {
            new_rect.set_height(new_rect.width() * ratio.height() / ratio.width());
        } else {
            new_rect.set_width(new_rect.height() * ratio.width() / ratio.height());
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
    Core::EventLoop::current().post_event(*m_resize_window, make<ResizeEvent>(new_rect));
    return true;
}

bool WindowManager::process_ongoing_drag(MouseEvent& event, Window*& hovered_window)
{
    if (!m_dnd_client)
        return false;

    if (event.type() == Event::MouseMove) {
        // We didn't let go of the drag yet, see if we should send some drag move events..
        for_each_visible_window_from_front_to_back([&](Window& window) {
            if (!window.rect().contains(event.position()))
                return IterationDecision::Continue;
            hovered_window = &window;
            auto translated_event = event.translated(-window.position());
            translated_event.set_drag(true);
            translated_event.set_drag_data_type(m_dnd_data_type);
            deliver_mouse_event(window, translated_event);
            return IterationDecision::Break;
        });
    }

    if (!(event.type() == Event::MouseUp && event.button() == MouseButton::Left))
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
        m_dnd_client->post_message(Messages::WindowClient::DragAccepted());
        if (hovered_window->client()) {
            auto translated_event = event.translated(-hovered_window->position());
            hovered_window->client()->post_message(Messages::WindowClient::DragDropped(hovered_window->window_id(), translated_event.position(), m_dnd_text, m_dnd_data_type, m_dnd_data));
        }
    } else {
        m_dnd_client->post_message(Messages::WindowClient::DragCancelled());
    }

    end_dnd_drag();
    return true;
}

void WindowManager::set_cursor_tracking_button(Button* button)
{
    m_cursor_tracking_button = button ? button->make_weak_ptr() : nullptr;
}

auto WindowManager::DoubleClickInfo::metadata_for_button(MouseButton button) const -> const ClickMetadata&
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
        ASSERT_NOT_REACHED();
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
        ASSERT_NOT_REACHED();
    }
}

// #define DOUBLECLICK_DEBUG

bool WindowManager::is_considered_doubleclick(const MouseEvent& event, const DoubleClickInfo::ClickMetadata& metadata) const
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

void WindowManager::start_menu_doubleclick(Window& window, const MouseEvent& event)
{
    // This is a special case. Basically, we're trying to determine whether
    // double clicking on the window menu icon happened. In this case, the
    // WindowFrame only receives a MouseDown event, and since the window
    // menu popus up, it does not see the MouseUp event. But, if they subsequently
    // click there again, the menu is closed and we receive a MouseUp event.
    // So, in order to be able to detect a double click when a menu is being
    // opened by the MouseDown event, we need to consider the MouseDown event
    // as a potential double-click trigger
    ASSERT(event.type() == Event::MouseDown);

    auto& metadata = m_double_click_info.metadata_for_button(event.button());
    if (&window != m_double_click_info.m_clicked_window) {
        // we either haven't clicked anywhere, or we haven't clicked on this
        // window. set the current click window, and reset the timers.
#if defined(DOUBLECLICK_DEBUG)
        dbg() << "Initial mousedown on window " << &window << " for menu (previous was " << m_double_click_info.m_clicked_window << ')';
#endif
        m_double_click_info.m_clicked_window = window.make_weak_ptr();
        m_double_click_info.reset();
    }

    metadata.last_position = event.position();
    metadata.clock.start();
}

bool WindowManager::is_menu_doubleclick(Window& window, const MouseEvent& event) const
{
    ASSERT(event.type() == Event::MouseUp);

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
    ASSERT(event.type() == Event::MouseUp);

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

    if (!metadata.clock.is_valid() || !is_considered_doubleclick(event, metadata)) {
        // either the clock is invalid because we haven't clicked on this
        // button on this window yet, so there's nothing to do, or this
        // isn't considered to be a double click. either way, restart the
        // clock
        metadata.clock.start();
    } else {
#if defined(DOUBLECLICK_DEBUG)
        dbg() << "Transforming MouseUp to MouseDoubleClick (" << metadata.clock.elapsed() << " < " << m_double_click_speed << ")!";
#endif
        event = MouseEvent(Event::MouseDoubleClick, event.position(), event.buttons(), event.button(), event.modifiers(), event.wheel_delta());
        // invalidate this now we've delivered a doubleclick, otherwise
        // tripleclick will deliver two doubleclick events (incorrectly).
        metadata.clock = {};
    }

    metadata.last_position = event.position();
}

void WindowManager::deliver_mouse_event(Window& window, MouseEvent& event)
{
    window.dispatch_event(event);
    if (event.type() == Event::MouseUp) {
        process_event_for_doubleclick(window, event);
        if (event.type() == Event::MouseDoubleClick)
            window.dispatch_event(event);
    }
}

void WindowManager::process_mouse_event(MouseEvent& event, Window*& hovered_window)
{
    HashTable<Window*> windows_who_received_mouse_event_due_to_cursor_tracking;

    // We need to process ongoing drag events first. Otherwise, global tracking
    // and dnd collides, leading to duplicate GUI::DragOperation instances
    if (process_ongoing_drag(event, hovered_window))
        return;

    for (auto* window = m_windows_in_order.tail(); window; window = window->prev()) {
        if (!window->global_cursor_tracking() || !window->is_visible() || window->is_minimized() || window->is_blocked_by_modal_window())
            continue;
        windows_who_received_mouse_event_due_to_cursor_tracking.set(window);
        auto translated_event = event.translated(-window->position());
        deliver_mouse_event(*window, translated_event);
    }

    hovered_window = nullptr;

    if (process_ongoing_window_move(event, hovered_window))
        return;

    if (process_ongoing_window_resize(event, hovered_window))
        return;

    if (m_cursor_tracking_button)
        return m_cursor_tracking_button->on_mouse_event(event.translated(-m_cursor_tracking_button->screen_rect().location()));

    // This is quite hackish, but it's how the Button hover effect is implemented.
    if (m_hovered_button && event.type() == Event::MouseMove)
        m_hovered_button->on_mouse_event(event.translated(-m_hovered_button->screen_rect().location()));

    // FIXME: Now that the menubar has a dedicated window, is this special-casing really necessary?
    if (MenuManager::the().has_open_menu() || menubar_rect().contains(event.position())) {
        for_each_visible_window_of_type_from_front_to_back(WindowType::MenuApplet, [&](auto& window) {
            if (!window.rect_in_menubar().contains(event.position()))
                return IterationDecision::Continue;
            hovered_window = &window;
            return IterationDecision::Break;
        });
        clear_resize_candidate();
        MenuManager::the().dispatch_event(event);
        return;
    }

    Window* event_window_with_frame = nullptr;

    if (m_active_input_tracking_window) {
        // At this point, we have delivered the start of an input sequence to a
        // client application. We must keep delivering to that client
        // application until the input sequence is done.
        //
        // This prevents e.g. moving on one window out of the bounds starting
        // a move in that other unrelated window, and other silly shenanigans.
        if (!windows_who_received_mouse_event_due_to_cursor_tracking.contains(m_active_input_tracking_window)) {
            auto translated_event = event.translated(-m_active_input_tracking_window->position());
            deliver_mouse_event(*m_active_input_tracking_window, translated_event);
            windows_who_received_mouse_event_due_to_cursor_tracking.set(m_active_input_tracking_window.ptr());
        }
        if (event.type() == Event::MouseUp && event.buttons() == 0) {
            m_active_input_tracking_window = nullptr;
        }

        for_each_visible_window_from_front_to_back([&](auto& window) {
            if (window.frame().rect().contains(event.position())) {
                hovered_window = &window;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
    } else {
        auto process_mouse_event_for_window = [&](Window& window) {
            if (&window != m_resize_candidate.ptr())
                clear_resize_candidate();

            // First check if we should initiate a move or resize (Logo+LMB or Logo+RMB).
            // In those cases, the event is swallowed by the window manager.
            if (window.is_movable()) {
                if (!window.is_fullscreen() && m_keyboard_modifiers == Mod_Logo && event.type() == Event::MouseDown && event.button() == MouseButton::Left) {
                    hovered_window = &window;
                    start_window_move(window, event);
                    return;
                }
                if (window.is_resizable() && m_keyboard_modifiers == Mod_Logo && event.type() == Event::MouseDown && event.button() == MouseButton::Right && !window.is_blocked_by_modal_window()) {
                    hovered_window = &window;
                    start_window_resize(window, event);
                    return;
                }
            }

            if (m_keyboard_modifiers == Mod_Logo && event.type() == Event::MouseWheel) {
                float opacity_change = -event.wheel_delta() * 0.05f;
                float new_opacity = window.opacity() + opacity_change;
                if (new_opacity < 0.05f)
                    new_opacity = 0.05f;
                if (new_opacity > 1.0f)
                    new_opacity = 1.0f;
                window.set_opacity(new_opacity);
                return;
            }

            // Well okay, let's see if we're hitting the frame or the window inside the frame.
            if (window.rect().contains(event.position())) {
                if (event.type() == Event::MouseDown) {
                    if (window.type() == WindowType::Normal)
                        move_to_front_and_make_active(window);
                    else if (window.type() == WindowType::Desktop)
                        set_active_window(&window);
                }

                hovered_window = &window;
                if (!window.global_cursor_tracking() && !windows_who_received_mouse_event_due_to_cursor_tracking.contains(&window) && !window.is_blocked_by_modal_window()) {
                    auto translated_event = event.translated(-window.position());
                    deliver_mouse_event(window, translated_event);
                    if (event.type() == Event::MouseDown) {
                        m_active_input_tracking_window = window.make_weak_ptr();
                    }
                }
                return;
            }

            // We are hitting the frame, pass the event along to WindowFrame.
            window.frame().on_mouse_event(event.translated(-window.frame().rect().location()));
            event_window_with_frame = &window;
        };

        if (auto* fullscreen_window = active_fullscreen_window()) {
            process_mouse_event_for_window(*fullscreen_window);
        } else {
            for_each_visible_window_from_front_to_back([&](Window& window) {
                auto window_frame_rect = window.frame().rect();
                if (!window_frame_rect.contains(event.position()))
                    return IterationDecision::Continue;
                process_mouse_event_for_window(window);
                return IterationDecision::Break;
            });
        }

        // Clicked outside of any window
        if (!hovered_window && !event_window_with_frame && event.type() == Event::MouseDown)
            set_active_window(nullptr);
    }

    if (event_window_with_frame != m_resize_candidate.ptr())
        clear_resize_candidate();
}

void WindowManager::clear_resize_candidate()
{
    if (m_resize_candidate)
        Compositor::the().invalidate_cursor();
    m_resize_candidate = nullptr;
}

Gfx::IntRect WindowManager::menubar_rect() const
{
    if (active_fullscreen_window())
        return {};
    return MenuManager::the().menubar_rect();
}

Gfx::IntRect WindowManager::desktop_rect() const
{
    if (active_fullscreen_window())
        return {};
    return {
        0,
        menubar_rect().bottom() + 1,
        Screen::the().width(),
        Screen::the().height() - menubar_rect().height() - 28
    };
}

void WindowManager::event(Core::Event& event)
{
    if (static_cast<Event&>(event).is_mouse_event()) {
        Window* hovered_window = nullptr;
        process_mouse_event(static_cast<MouseEvent&>(event), hovered_window);
        set_hovered_window(hovered_window);
        return;
    }

    if (static_cast<Event&>(event).is_key_event()) {
        auto& key_event = static_cast<const KeyEvent&>(event);
        m_keyboard_modifiers = key_event.modifiers();

        if (key_event.type() == Event::KeyDown && key_event.key() == Key_Escape && m_dnd_client) {
            m_dnd_client->post_message(Messages::WindowClient::DragCancelled());
            end_dnd_drag();
            return;
        }

        if (MenuManager::the().current_menu()) {
            MenuManager::the().dispatch_event(event);
            return;
        }

        if (key_event.type() == Event::KeyDown && ((key_event.modifiers() == Mod_Logo && key_event.key() == Key_Tab) || (key_event.modifiers() == (Mod_Logo | Mod_Shift) && key_event.key() == Key_Tab)))
            m_switcher.show();
        if (m_switcher.is_visible()) {
            m_switcher.on_key_event(key_event);
            return;
        }

        if (m_active_input_window) {
            if (key_event.type() == Event::KeyDown && key_event.modifiers() == Mod_Logo) {
                if (key_event.key() == Key_Down) {
                    if (m_active_input_window->is_resizable() && m_active_input_window->is_maximized()) {
                        maximize_windows(*m_active_input_window, false);
                        return;
                    }
                    if (m_active_input_window->is_minimizable())
                        minimize_windows(*m_active_input_window, true);
                    return;
                }
                if (m_active_input_window->is_resizable()) {
                    if (key_event.key() == Key_Up) {
                        maximize_windows(*m_active_input_window, !m_active_input_window->is_maximized());
                        return;
                    }
                    if (key_event.key() == Key_Left) {
                        if (m_active_input_window->tiled() != WindowTileType::None) {
                            m_active_input_window->set_tiled(WindowTileType::None);
                            return;
                        }
                        if (m_active_input_window->is_maximized())
                            maximize_windows(*m_active_input_window, false);
                        m_active_input_window->set_tiled(WindowTileType::Left);
                        return;
                    }
                    if (key_event.key() == Key_Right) {
                        if (m_active_input_window->tiled() != WindowTileType::None) {
                            m_active_input_window->set_tiled(WindowTileType::None);
                            return;
                        }
                        if (m_active_input_window->is_maximized())
                            maximize_windows(*m_active_input_window, false);
                        m_active_input_window->set_tiled(WindowTileType::Right);
                        return;
                    }
                }
            }
            m_active_input_window->dispatch_event(event);
            return;
        }
    }

    Core::Object::event(event);
}

void WindowManager::set_highlight_window(Window* window)
{
    if (window == m_highlight_window)
        return;
    if (auto* previous_highlight_window = m_highlight_window.ptr())
        previous_highlight_window->invalidate();
    m_highlight_window = window ? window->make_weak_ptr() : nullptr;
    if (m_highlight_window)
        m_highlight_window->invalidate();
}

bool WindowManager::is_active_window_or_accessory(Window& window) const
{
    if (m_active_window == &window)
        return true;

    if (!window.is_accessory())
        return false;

    return m_active_window == window.parent_window();
}

static bool window_type_can_become_active(WindowType type)
{
    return type == WindowType::Normal || type == WindowType::Desktop;
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

    set_active_input_window(window);
}

Window* WindowManager::set_active_input_window(Window* window)
{
    if (window == m_active_input_window)
        return window;

    Window* previous_input_window = m_active_input_window;
    if (previous_input_window)
        Core::EventLoop::current().post_event(*previous_input_window, make<Event>(Event::WindowInputLeft));

    if (window) {
        m_active_input_window = window->make_weak_ptr();
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowInputEntered));
    } else {
        m_active_input_window = nullptr;
    }

    return previous_input_window;
}

void WindowManager::set_active_window(Window* window, bool make_input)
{
    if (window) {
        if (auto* modal_window = window->is_blocked_by_modal_window()) {
            ASSERT(modal_window->is_modal());
            ASSERT(modal_window != window);
            window = modal_window;
            make_input = true;
        }

        if (!window_type_can_become_active(window->type()))
            return;
    }

    auto* new_active_input_window = window;
    if (window && window->is_accessory()) {
        // The parent of an accessory window is always the active
        // window, but input is routed to the accessory window
        window = window->parent_window();
    }

    if (make_input)
        set_active_input_window(new_active_input_window);

    if (window == m_active_window)
        return;

    auto* previously_active_window = m_active_window.ptr();

    ClientConnection* previously_active_client = nullptr;
    ClientConnection* active_client = nullptr;

    if (previously_active_window) {
        previously_active_client = previously_active_window->client();
        Core::EventLoop::current().post_event(*previously_active_window, make<Event>(Event::WindowDeactivated));
        previously_active_window->invalidate();
        m_active_window = nullptr;
        m_active_input_tracking_window = nullptr;
        tell_wm_listeners_window_state_changed(*previously_active_window);
    }

    if (window) {
        m_active_window = window->make_weak_ptr();
        active_client = m_active_window->client();
        Core::EventLoop::current().post_event(*m_active_window, make<Event>(Event::WindowActivated));
        m_active_window->invalidate();
        if (auto* client = window->client()) {
            MenuManager::the().set_current_menubar(client->app_menubar());
        } else {
            MenuManager::the().set_current_menubar(nullptr);
        }
        tell_wm_listeners_window_state_changed(*m_active_window);
    } else {
        MenuManager::the().set_current_menubar(nullptr);
    }

    if (active_client != previously_active_client) {
        if (previously_active_client)
            previously_active_client->deboost();
        if (active_client)
            active_client->boost();
    }
}

void WindowManager::set_hovered_window(Window* window)
{
    if (m_hovered_window == window)
        return;

    if (m_hovered_window)
        Core::EventLoop::current().post_event(*m_hovered_window, make<Event>(Event::WindowLeft));

    m_hovered_window = window ? window->make_weak_ptr() : nullptr;

    if (m_hovered_window)
        Core::EventLoop::current().post_event(*m_hovered_window, make<Event>(Event::WindowEntered));
}

const ClientConnection* WindowManager::active_client() const
{
    if (m_active_window)
        return m_active_window->client();
    return nullptr;
}

void WindowManager::notify_client_changed_app_menubar(ClientConnection& client)
{
    if (active_client() == &client)
        MenuManager::the().set_current_menubar(client.app_menubar());
}

const Cursor& WindowManager::active_cursor() const
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

    if (m_hovered_window && m_hovered_window->cursor())
        return *m_hovered_window->cursor();

    return *m_arrow_cursor;
}

void WindowManager::set_hovered_button(Button* button)
{
    m_hovered_button = button ? button->make_weak_ptr() : nullptr;
}

void WindowManager::set_resize_candidate(Window& window, ResizeDirection direction)
{
    m_resize_candidate = window.make_weak_ptr();
    m_resize_direction = direction;
}

ResizeDirection WindowManager::resize_direction_of_window(const Window& window)
{
    if (&window != m_resize_window)
        return ResizeDirection::None;
    return m_resize_direction;
}

Gfx::IntRect WindowManager::maximized_window_rect(const Window& window) const
{
    Gfx::IntRect rect = Screen::the().rect();

    // Subtract window title bar (leaving the border)
    rect.set_y(rect.y() + window.frame().title_bar_rect().height());
    rect.set_height(rect.height() - window.frame().title_bar_rect().height());

    // Subtract menu bar
    rect.set_y(rect.y() + menubar_rect().height());
    rect.set_height(rect.height() - menubar_rect().height());

    // Subtract taskbar window height if present
    const_cast<WindowManager*>(this)->for_each_visible_window_of_type_from_back_to_front(WindowType::Taskbar, [&rect](Window& taskbar_window) {
        rect.set_height(rect.height() - taskbar_window.height());
        return IterationDecision::Break;
    });

    constexpr int tasteful_space_above_maximized_window = 2;
    rect.set_y(rect.y() + tasteful_space_above_maximized_window);
    rect.set_height(rect.height() - tasteful_space_above_maximized_window);

    return rect;
}

void WindowManager::start_dnd_drag(ClientConnection& client, const String& text, Gfx::Bitmap* bitmap, const String& data_type, const String& data)
{
    ASSERT(!m_dnd_client);
    m_dnd_client = client.make_weak_ptr();
    m_dnd_text = text;
    m_dnd_bitmap = bitmap;
    m_dnd_data_type = data_type;
    m_dnd_data = data;
    Compositor::the().invalidate_cursor();
    m_active_input_tracking_window = nullptr;
}

void WindowManager::end_dnd_drag()
{
    ASSERT(m_dnd_client);
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
    return Gfx::IntRect(location, { width, height }).inflated(4, 4);
}

bool WindowManager::update_theme(String theme_path, String theme_name)
{
    auto new_theme = Gfx::load_system_theme(theme_path);
    if (!new_theme)
        return false;
    ASSERT(new_theme);
    Gfx::set_system_theme(*new_theme);
    m_palette = Gfx::PaletteImpl::create_with_shared_buffer(*new_theme);
    Compositor::the().set_background_color(palette().desktop_background().to_string());
    HashTable<ClientConnection*> notified_clients;
    for_each_window([&](Window& window) {
        if (window.client()) {
            if (!notified_clients.contains(window.client())) {
                window.client()->post_message(Messages::WindowClient::UpdateSystemTheme(Gfx::current_system_theme_buffer_id()));
                notified_clients.set(window.client());
            }
        }
        window.frame().layout_buttons();
        window.frame().set_button_icons();
        return IterationDecision::Continue;
    });
    MenuManager::the().did_change_theme();
    auto wm_config = Core::ConfigFile::open("/etc/WindowServer/WindowServer.ini");
    wm_config->write_entry("Theme", "Name", theme_name);
    wm_config->sync();
    Compositor::the().invalidate_screen();
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

Gfx::IntPoint WindowManager::get_recommended_window_position(const Gfx::IntPoint& desired)
{
    // FIXME: Find a  better source for the width and height to shift by.
    Gfx::IntPoint shift(8, palette().window_title_height() + 10);

    // FIXME: Find a better source for this.
    int taskbar_height = 28;
    int menubar_height = MenuManager::the().menubar_rect().height();

    const Window* overlap_window = nullptr;
    for_each_visible_window_of_type_from_front_to_back(WindowType::Normal, [&](Window& window) {
        if (window.default_positioned() && (!overlap_window || overlap_window->window_id() < window.window_id())) {
            overlap_window = &window;
        }
        return IterationDecision::Continue;
    });

    Gfx::IntPoint point;
    if (overlap_window) {
        point = overlap_window->position() + shift;
        point = { point.x() % Screen::the().width(),
            (point.y() >= (Screen::the().height() - taskbar_height))
                ? menubar_height + palette().window_title_height()
                : point.y() };
    } else {
        point = desired;
    }

    return point;
}
}
