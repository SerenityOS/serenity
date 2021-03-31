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

#include "Window.h"
#include "AppletManager.h"
#include "ClientConnection.h"
#include "Compositor.h"
#include "Event.h"
#include "EventLoop.h"
#include "Screen.h"
#include "WindowManager.h"
#include <AK/Badge.h>
#include <WindowServer/WindowClientEndpoint.h>

namespace WindowServer {

const static Gfx::IntSize s_default_normal_minimum_size = { 50, 50 };

static String default_window_icon_path()
{
    return "/res/icons/16x16/window.png";
}

static Gfx::Bitmap& default_window_icon()
{
    static Gfx::Bitmap* s_icon;
    if (!s_icon)
        s_icon = Gfx::Bitmap::load_from_file(default_window_icon_path()).leak_ref();
    return *s_icon;
}

static Gfx::Bitmap& minimize_icon()
{
    static Gfx::Bitmap* s_icon;
    if (!s_icon)
        s_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/downward-triangle.png").leak_ref();
    return *s_icon;
}

static Gfx::Bitmap& maximize_icon()
{
    static Gfx::Bitmap* s_icon;
    if (!s_icon)
        s_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/upward-triangle.png").leak_ref();
    return *s_icon;
}

static Gfx::Bitmap& restore_icon()
{
    static Gfx::Bitmap* s_icon;
    if (!s_icon)
        s_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/window-restore.png").leak_ref();
    return *s_icon;
}

static Gfx::Bitmap& close_icon()
{
    static Gfx::Bitmap* s_icon;
    if (!s_icon)
        s_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/window-close.png").leak_ref();
    return *s_icon;
}

Window::Window(Core::Object& parent, WindowType type)
    : Core::Object(&parent)
    , m_type(type)
    , m_icon(default_window_icon())
    , m_frame(*this)
{
    // Set default minimum size for Normal windows
    if (m_type == WindowType::Normal)
        m_minimum_size = s_default_normal_minimum_size;

    WindowManager::the().add_window(*this);
}

Window::Window(ClientConnection& client, WindowType window_type, int window_id, bool modal, bool minimizable, bool frameless, bool resizable, bool fullscreen, bool accessory, Window* parent_window)
    : Core::Object(&client)
    , m_client(&client)
    , m_type(window_type)
    , m_modal(modal)
    , m_minimizable(minimizable)
    , m_frameless(frameless)
    , m_resizable(resizable)
    , m_fullscreen(fullscreen)
    , m_accessory(accessory)
    , m_window_id(window_id)
    , m_client_id(client.client_id())
    , m_icon(default_window_icon())
    , m_frame(*this)
{
    // FIXME: This should not be hard-coded here.
    if (m_type == WindowType::Taskbar) {
        m_wm_event_mask = WMEventMask::WindowStateChanges | WMEventMask::WindowRemovals | WMEventMask::WindowIconChanges;
        m_listens_to_wm_events = true;
    }

    // Set default minimum size for Normal windows
    if (m_type == WindowType::Normal)
        m_minimum_size = s_default_normal_minimum_size;

    if (parent_window)
        set_parent_window(*parent_window);
    WindowManager::the().add_window(*this);
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

void Window::set_title(const String& title)
{
    if (m_title == title)
        return;
    m_title = title;
    frame().invalidate_title_bar();
    WindowManager::the().notify_title_changed(*this);
}

void Window::set_rect(const Gfx::IntRect& rect)
{
    if (m_rect == rect)
        return;
    auto old_rect = m_rect;
    m_rect = rect;
    if (rect.is_empty()) {
        m_backing_store = nullptr;
    } else if (!m_client && (!m_backing_store || old_rect.size() != rect.size())) {
        m_backing_store = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, m_rect.size());
    }

    invalidate(true, old_rect.size() != rect.size());
    m_frame.notify_window_rect_changed(old_rect, rect); // recomputes occlusions
}

void Window::set_rect_without_repaint(const Gfx::IntRect& rect)
{
    VERIFY(!rect.is_empty());
    if (m_rect == rect)
        return;
    auto old_rect = m_rect;
    m_rect = rect;

    if (old_rect.size() == m_rect.size()) {
        auto delta = m_rect.location() - old_rect.location();
        for (auto& child_window : m_child_windows) {
            if (child_window)
                child_window->move_by(delta);
        }
    }

    invalidate(true, old_rect.size() != rect.size());
    m_frame.notify_window_rect_changed(old_rect, rect); // recomputes occlusions
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

void Window::nudge_into_desktop(bool force_titlebar_visible)
{
    Gfx::IntRect arena = WindowManager::the().arena_rect_for_type(type());
    auto min_visible = 1;
    if (type() == WindowType::Normal)
        min_visible = 30;

    // Push the frame around such that at least `min_visible` pixels of the *frame* are in the desktop rect.
    auto old_frame_rect = frame().rect();
    Gfx::IntRect new_frame_rect = {
        clamp(old_frame_rect.x(), arena.left() + min_visible - width(), arena.right() - min_visible),
        clamp(old_frame_rect.y(), arena.top() + min_visible - height(), arena.bottom() - min_visible),
        old_frame_rect.width(),
        old_frame_rect.height(),
    };

    // Make sure that at least half of the titlebar is visible.
    auto min_frame_y = arena.top() - (y() - old_frame_rect.y()) / 2;
    if (force_titlebar_visible && new_frame_rect.y() < min_frame_y) {
        new_frame_rect.set_y(min_frame_y);
    }

    // Deduce new window rect:
    Gfx::IntRect new_window_rect = {
        x() + new_frame_rect.x() - old_frame_rect.x(),
        y() + new_frame_rect.y() - old_frame_rect.y(),
        width(),
        height(),
    };
    set_rect(new_window_rect);
}

void Window::set_minimum_size(const Gfx::IntSize& size)
{
    VERIFY(!size.is_empty());

    if (m_minimum_size == size)
        return;

    // Disallow setting minimum zero widths or heights.
    if (size.width() == 0 || size.height() == 0)
        return;

    m_minimum_size = size;
}

void Window::handle_mouse_event(const MouseEvent& event)
{
    set_automatic_cursor_tracking_enabled(event.buttons() != 0);

    switch (event.type()) {
    case Event::MouseMove:
        m_client->post_message(Messages::WindowClient::MouseMove(m_window_id, event.position(), (u32)event.button(), event.buttons(), event.modifiers(), event.wheel_delta(), event.is_drag(), event.mime_types()));
        break;
    case Event::MouseDown:
        m_client->post_message(Messages::WindowClient::MouseDown(m_window_id, event.position(), (u32)event.button(), event.buttons(), event.modifiers(), event.wheel_delta()));
        break;
    case Event::MouseDoubleClick:
        m_client->post_message(Messages::WindowClient::MouseDoubleClick(m_window_id, event.position(), (u32)event.button(), event.buttons(), event.modifiers(), event.wheel_delta()));
        break;
    case Event::MouseUp:
        m_client->post_message(Messages::WindowClient::MouseUp(m_window_id, event.position(), (u32)event.button(), event.buttons(), event.modifiers(), event.wheel_delta()));
        break;
    case Event::MouseWheel:
        m_client->post_message(Messages::WindowClient::MouseWheel(m_window_id, event.position(), (u32)event.button(), event.buttons(), event.modifiers(), event.wheel_delta()));
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

void Window::update_menu_item_text(PopupMenuItem item)
{
    if (m_window_menu) {
        m_window_menu->item((int)item).set_text(item == PopupMenuItem::Minimize ? (m_minimized ? "Unminimize" : "Minimize") : (m_maximized ? "Restore" : "Maximize"));
        m_window_menu->redraw();
    }
}

void Window::update_menu_item_enabled(PopupMenuItem item)
{
    if (m_window_menu) {
        m_window_menu->item((int)item).set_enabled(item == PopupMenuItem::Minimize ? m_minimizable : m_resizable);
        m_window_menu->redraw();
    }
}

void Window::set_minimized(bool minimized)
{
    if (m_minimized == minimized)
        return;
    if (minimized && !m_minimizable)
        return;
    m_minimized = minimized;
    update_menu_item_text(PopupMenuItem::Minimize);
    Compositor::the().invalidate_occlusions();
    Compositor::the().invalidate_screen(frame().render_rect());
    if (!blocking_modal_window())
        start_minimize_animation();
    if (!minimized)
        request_update({ {}, size() });
    WindowManager::the().notify_minimization_state_changed(*this);
}

void Window::set_minimizable(bool minimizable)
{
    if (m_minimizable == minimizable)
        return;
    m_minimizable = minimizable;
    update_menu_item_enabled(PopupMenuItem::Minimize);
    // TODO: Hide/show (or alternatively change enabled state of) window minimize button dynamically depending on value of m_minimizable
}

void Window::set_taskbar_rect(const Gfx::IntRect& rect)
{
    m_taskbar_rect = rect;
    m_have_taskbar_rect = !m_taskbar_rect.is_empty();
}

void Window::start_minimize_animation()
{
    if (!m_have_taskbar_rect) {
        // If this is a modal window, it may not have its own taskbar
        // button, so there is no rectangle. In that case, walk the
        // modal stack until we find a window that may have one
        WindowManager::the().for_each_window_in_modal_stack(*this, [&](Window& w, bool) {
            if (w.has_taskbar_rect()) {
                // We purposely do NOT set m_have_taskbar_rect to true here
                // because we want to only copy the rectangle from the
                // window that has it, but since this window wouldn't receive
                // any updates down the road we want to query it again
                // next time we want to start the animation
                m_taskbar_rect = w.taskbar_rect();

                VERIFY(!m_have_taskbar_rect); // should remain unset!
                return IterationDecision::Break;
            };
            return IterationDecision::Continue;
        });
    }
    m_minimize_animation_step = 0;
}

void Window::set_opacity(float opacity)
{
    if (m_opacity == opacity)
        return;
    bool was_opaque = is_opaque();
    m_opacity = opacity;
    if (was_opaque != is_opaque())
        Compositor::the().invalidate_occlusions();
    invalidate(false);
    WindowManager::the().notify_opacity_changed(*this);
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

void Window::set_maximized(bool maximized, Optional<Gfx::IntPoint> fixed_point)
{
    if (m_maximized == maximized)
        return;
    if (maximized && (!is_resizable() || resize_aspect_ratio().has_value()))
        return;
    m_tiled = WindowTileType::None;
    m_maximized = maximized;
    update_menu_item_text(PopupMenuItem::Maximize);
    if (maximized) {
        m_unmaximized_rect = m_rect;
        set_rect(WindowManager::the().maximized_window_rect(*this));
    } else {
        if (fixed_point.has_value()) {
            auto new_rect = Gfx::IntRect(m_rect);
            new_rect.set_size_around(m_unmaximized_rect.size(), fixed_point.value());
            set_rect(new_rect);
        } else {
            set_rect(m_unmaximized_rect);
        }
    }
    m_frame.did_set_maximized({}, maximized);
    Core::EventLoop::current().post_event(*this, make<ResizeEvent>(m_rect));
    set_default_positioned(false);
}
void Window::set_vertically_maximized()
{
    if (m_maximized)
        return;
    if (!is_resizable() || resize_aspect_ratio().has_value())
        return;

    auto max_rect = WindowManager::the().maximized_window_rect(*this);

    auto new_rect = Gfx::IntRect(
        Gfx::IntPoint(rect().x(), max_rect.y()),
        Gfx::IntSize(rect().width(), max_rect.height()));
    set_rect(new_rect);
    Core::EventLoop::current().post_event(*this, make<ResizeEvent>(new_rect));
}
void Window::set_resizable(bool resizable)
{
    if (m_resizable == resizable)
        return;
    m_resizable = resizable;
    update_menu_item_enabled(PopupMenuItem::Maximize);
    // TODO: Hide/show (or alternatively change enabled state of) window maximize button dynamically depending on value of is_resizable()
}

void Window::event(Core::Event& event)
{
    if (!m_client) {
        VERIFY(parent());
        event.ignore();
        return;
    }

    if (blocking_modal_window()) {
        // We still want to handle the WindowDeactivated event below when a new modal is
        // created to notify its parent window, despite it being "blocked by modal window".
        if (event.type() != Event::WindowDeactivated)
            return;
    }

    if (static_cast<Event&>(event).is_mouse_event())
        return handle_mouse_event(static_cast<const MouseEvent&>(event));

    switch (event.type()) {
    case Event::WindowEntered:
        m_client->post_message(Messages::WindowClient::WindowEntered(m_window_id));
        break;
    case Event::WindowLeft:
        m_client->post_message(Messages::WindowClient::WindowLeft(m_window_id));
        break;
    case Event::KeyDown:
        m_client->post_message(
            Messages::WindowClient::KeyDown(m_window_id,
                (u32) static_cast<const KeyEvent&>(event).code_point(),
                (u32) static_cast<const KeyEvent&>(event).key(),
                static_cast<const KeyEvent&>(event).modifiers(),
                (u32) static_cast<const KeyEvent&>(event).scancode()));
        break;
    case Event::KeyUp:
        m_client->post_message(
            Messages::WindowClient::KeyUp(m_window_id,
                (u32) static_cast<const KeyEvent&>(event).code_point(),
                (u32) static_cast<const KeyEvent&>(event).key(),
                static_cast<const KeyEvent&>(event).modifiers(),
                (u32) static_cast<const KeyEvent&>(event).scancode()));
        break;
    case Event::WindowActivated:
        m_client->post_message(Messages::WindowClient::WindowActivated(m_window_id));
        break;
    case Event::WindowDeactivated:
        m_client->post_message(Messages::WindowClient::WindowDeactivated(m_window_id));
        break;
    case Event::WindowInputEntered:
        m_client->post_message(Messages::WindowClient::WindowInputEntered(m_window_id));
        break;
    case Event::WindowInputLeft:
        m_client->post_message(Messages::WindowClient::WindowInputLeft(m_window_id));
        break;
    case Event::WindowCloseRequest:
        m_client->post_message(Messages::WindowClient::WindowCloseRequest(m_window_id));
        break;
    case Event::WindowResized:
        m_client->post_message(Messages::WindowClient::WindowResized(m_window_id, static_cast<const ResizeEvent&>(event).rect()));
        break;
    default:
        break;
    }
}

void Window::set_global_cursor_tracking_enabled(bool enabled)
{
    m_global_cursor_tracking_enabled = enabled;
}

void Window::set_visible(bool b)
{
    if (m_visible == b)
        return;
    m_visible = b;

    Compositor::the().invalidate_occlusions();
    if (m_visible)
        invalidate(true);
    else
        Compositor::the().invalidate_screen(frame().render_rect());
}

void Window::set_frameless(bool frameless)
{
    if (m_frameless == frameless)
        return;
    auto render_rect_before = frame().render_rect();
    m_frameless = frameless;
    if (m_visible) {
        Compositor::the().invalidate_occlusions();
        invalidate(true, true);
        Compositor::the().invalidate_screen(frameless ? render_rect_before : frame().render_rect());
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

void Window::invalidate(const Gfx::IntRect& rect, bool with_frame)
{
    if (type() == WindowType::MenuApplet) {
        AppletManager::the().invalidate_applet(*this, rect);
        return;
    }

    if (invalidate_no_notify(rect, with_frame))
        Compositor::the().invalidate_window();
}

bool Window::invalidate_no_notify(const Gfx::IntRect& rect, bool with_frame)
{
    if (rect.is_empty())
        return false;
    if (m_invalidated_all) {
        if (with_frame)
            m_invalidated_frame |= true;
        return false;
    }

    auto outer_rect = frame().render_rect();
    auto inner_rect = rect;
    inner_rect.move_by(position());
    // FIXME: This seems slightly wrong; the inner rect shouldn't intersect the border part of the outer rect.
    inner_rect.intersect(outer_rect);
    if (inner_rect.is_empty())
        return false;

    m_invalidated = true;
    if (with_frame)
        m_invalidated_frame |= true;
    m_dirty_rects.add(inner_rect.translated(-outer_rect.location()));
    return true;
}

void Window::refresh_client_size()
{
    client()->post_message(Messages::WindowClient::WindowResized(m_window_id, m_rect));
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
    return WindowManager::the().active_window() == this;
}

Window* Window::blocking_modal_window()
{
    // A window is blocked if any immediate child, or any child further
    // down the chain is modal
    for (auto& window : m_child_windows) {
        if (window && !window->is_destroyed()) {
            if (window->is_modal())
                return window;

            if (auto* blocking_window = window->blocking_modal_window())
                return blocking_window;
        }
    }
    return nullptr;
}

void Window::set_default_icon()
{
    m_icon = default_window_icon();
}

void Window::request_update(const Gfx::IntRect& rect, bool ignore_occlusion)
{
    if (rect.is_empty())
        return;
    if (m_pending_paint_rects.is_empty()) {
        deferred_invoke([this, ignore_occlusion](auto&) {
            client()->post_paint_message(*this, ignore_occlusion);
        });
    }
    m_pending_paint_rects.add(rect);
}

void Window::ensure_window_menu()
{
    if (!m_window_menu) {
        m_window_menu = Menu::construct(nullptr, -1, "(Window Menu)");
        m_window_menu->set_window_menu_of(*this);

        auto minimize_item = make<MenuItem>(*m_window_menu, 1, m_minimized ? "Unminimize" : "Minimize");
        m_window_menu_minimize_item = minimize_item.ptr();
        m_window_menu->add_item(move(minimize_item));

        auto maximize_item = make<MenuItem>(*m_window_menu, 2, m_maximized ? "Restore" : "Maximize");
        m_window_menu_maximize_item = maximize_item.ptr();
        m_window_menu->add_item(move(maximize_item));

        m_window_menu->add_item(make<MenuItem>(*m_window_menu, MenuItem::Type::Separator));

        auto menubar_visibility_item = make<MenuItem>(*m_window_menu, 4, "Menu bar");
        m_window_menu_menubar_visibility_item = menubar_visibility_item.ptr();
        menubar_visibility_item->set_checkable(true);
        m_window_menu->add_item(move(menubar_visibility_item));

        auto close_item = make<MenuItem>(*m_window_menu, 3, "Close");
        m_window_menu_close_item = close_item.ptr();
        m_window_menu_close_item->set_icon(&close_icon());
        m_window_menu_close_item->set_default(true);
        m_window_menu->add_item(move(close_item));

        m_window_menu->item((int)PopupMenuItem::Minimize).set_enabled(m_minimizable);
        m_window_menu->item((int)PopupMenuItem::Maximize).set_enabled(m_resizable);

        m_window_menu->on_item_activation = [&](auto& item) {
            switch (item.identifier()) {
            case 1:
                WindowManager::the().minimize_windows(*this, !m_minimized);
                if (!m_minimized)
                    WindowManager::the().move_to_front_and_make_active(*this);
                break;
            case 2:
                WindowManager::the().maximize_windows(*this, !m_maximized);
                WindowManager::the().move_to_front_and_make_active(*this);
                break;
            case 3:
                request_close();
                break;
            case 4:
                frame().invalidate();
                item.set_checked(!item.is_checked());
                m_should_show_menubar = item.is_checked();
                frame().invalidate();
                Compositor::the().invalidate_occlusions();
                Compositor::the().invalidate_screen();
                break;
            }
        };
    }
}

void Window::popup_window_menu(const Gfx::IntPoint& position, WindowMenuDefaultAction default_action)
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
    m_window_menu_minimize_item->set_icon(m_minimized ? nullptr : &minimize_icon());
    m_window_menu_maximize_item->set_default(default_action == WindowMenuDefaultAction::Maximize || default_action == WindowMenuDefaultAction::Restore);
    m_window_menu_maximize_item->set_icon(m_maximized ? &restore_icon() : &maximize_icon());
    m_window_menu_close_item->set_default(default_action == WindowMenuDefaultAction::Close);
    m_window_menu_menubar_visibility_item->set_enabled(menubar());
    m_window_menu_menubar_visibility_item->set_checked(menubar() && m_should_show_menubar);

    m_window_menu->popup(position);
}

void Window::window_menu_activate_default()
{
    ensure_window_menu();
    m_window_menu->activate_default();
}

void Window::request_close()
{
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
        new_window_rect = Screen::the().rect();
    } else if (!m_saved_nonfullscreen_rect.is_empty()) {
        new_window_rect = m_saved_nonfullscreen_rect;
    }

    Core::EventLoop::current().post_event(*this, make<ResizeEvent>(new_window_rect));
    set_rect(new_window_rect);
}

Gfx::IntRect Window::tiled_rect(WindowTileType tiled) const
{
    VERIFY(tiled != WindowTileType::None);

    int frame_width = (m_frame.rect().width() - m_rect.width()) / 2;
    int title_bar_height = m_frame.title_bar_rect().height();
    int menu_height = WindowManager::the().maximized_window_rect(*this).y();
    int max_height = WindowManager::the().maximized_window_rect(*this).height();

    switch (tiled) {
    case WindowTileType::Left:
        return Gfx::IntRect(0,
            menu_height,
            Screen::the().width() / 2 - frame_width,
            max_height);
    case WindowTileType::Right:
        return Gfx::IntRect(Screen::the().width() / 2 + frame_width,
            menu_height,
            Screen::the().width() / 2 - frame_width,
            max_height);
    case WindowTileType::Top:
        return Gfx::IntRect(0,
            menu_height,
            Screen::the().width() - frame_width,
            (max_height - title_bar_height) / 2 - frame_width);
    case WindowTileType::Bottom:
        return Gfx::IntRect(0,
            menu_height + (title_bar_height + max_height) / 2 + frame_width,
            Screen::the().width() - frame_width,
            (max_height - title_bar_height) / 2 - frame_width);
    case WindowTileType::TopLeft:
        return Gfx::IntRect(0,
            menu_height,
            Screen::the().width() / 2 - frame_width,
            (max_height - title_bar_height) / 2 - frame_width);
    case WindowTileType::TopRight:
        return Gfx::IntRect(Screen::the().width() / 2 + frame_width,
            menu_height,
            Screen::the().width() / 2 - frame_width,
            (max_height - title_bar_height) / 2 - frame_width);
    case WindowTileType::BottomLeft:
        return Gfx::IntRect(0,
            menu_height + (title_bar_height + max_height) / 2 + frame_width,
            Screen::the().width() / 2 - frame_width,
            (max_height - title_bar_height) / 2);
    case WindowTileType::BottomRight:
        return Gfx::IntRect(Screen::the().width() / 2 + frame_width,
            menu_height + (title_bar_height + max_height) / 2 + frame_width,
            Screen::the().width() / 2 - frame_width,
            (max_height - title_bar_height) / 2);
    default:
        VERIFY_NOT_REACHED();
    }
}

bool Window::set_untiled(Optional<Gfx::IntPoint> fixed_point)
{
    if (m_tiled == WindowTileType::None)
        return false;
    VERIFY(!resize_aspect_ratio().has_value());

    m_tiled = WindowTileType::None;

    if (fixed_point.has_value()) {
        auto new_rect = Gfx::IntRect(m_rect);
        new_rect.set_size_around(m_untiled_rect.size(), fixed_point.value());
        set_rect(new_rect);
    } else {
        set_rect(m_untiled_rect);
    }

    Core::EventLoop::current().post_event(*this, make<ResizeEvent>(m_rect));

    return true;
}

void Window::set_tiled(WindowTileType tiled)
{
    VERIFY(tiled != WindowTileType::None);

    if (m_tiled == tiled)
        return;

    if (resize_aspect_ratio().has_value())
        return;

    if (m_tiled == WindowTileType::None)
        m_untiled_rect = m_rect;
    m_tiled = tiled;

    set_rect(tiled_rect(tiled));
    Core::EventLoop::current().post_event(*this, make<ResizeEvent>(m_rect));
}

void Window::detach_client(Badge<ClientConnection>)
{
    m_client = nullptr;
}

void Window::recalculate_rect()
{
    if (!is_resizable())
        return;

    bool send_event = true;
    if (m_tiled != WindowTileType::None) {
        set_rect(tiled_rect(m_tiled));
    } else if (is_maximized()) {
        set_rect(WindowManager::the().maximized_window_rect(*this));
    } else if (type() == WindowType::Desktop) {
        set_rect(WindowManager::the().desktop_rect());
    } else {
        send_event = false;
    }

    if (send_event) {
        Core::EventLoop::current().post_event(*this, make<ResizeEvent>(m_rect));
    }
}

void Window::add_child_window(Window& child_window)
{
    m_child_windows.append(child_window);
}

void Window::add_accessory_window(Window& accessory_window)
{
    m_accessory_windows.append(accessory_window);
}

void Window::set_parent_window(Window& parent_window)
{
    VERIFY(!m_parent_window);
    m_parent_window = parent_window;
    if (m_accessory)
        parent_window.add_accessory_window(*this);
    else
        parent_window.add_child_window(*this);
}

bool Window::is_accessory() const
{
    if (!m_accessory)
        return false;
    if (parent_window() != nullptr)
        return true;

    // If accessory window was unparented, convert to a regular window
    const_cast<Window*>(this)->set_accessory(false);
    return false;
}

bool Window::is_accessory_of(Window& window) const
{
    if (!is_accessory())
        return false;
    return parent_window() == &window;
}

void Window::modal_unparented()
{
    m_modal = false;
    WindowManager::the().notify_modal_unparented(*this);
}

bool Window::is_modal() const
{
    if (!m_modal)
        return false;
    if (!m_parent_window) {
        const_cast<Window*>(this)->modal_unparented();
        return false;
    }
    return true;
}

void Window::set_progress(int progress)
{
    if (m_progress == progress)
        return;

    m_progress = progress;
    WindowManager::the().notify_progress_changed(*this);
}

bool Window::is_descendant_of(Window& window) const
{
    for (auto* parent = parent_window(); parent; parent = parent->parent_window()) {
        if (parent == &window)
            return true;
        for (auto& accessory : parent->accessory_windows()) {
            if (accessory == &window)
                return true;
        }
    }
    return false;
}

bool Window::hit_test(const Gfx::IntPoint& point, bool include_frame) const
{
    if (!frame().rect().contains(point))
        return false;
    if (!rect().contains(point)) {
        if (include_frame)
            return frame().hit_test(point);
        return false;
    }
    if (!m_hit_testing_enabled)
        return false;
    u8 threshold = alpha_hit_threshold() * 255;
    if (threshold == 0 || !m_backing_store || !m_backing_store->has_alpha_channel())
        return true;
    auto relative_point = point.translated(-rect().location()) * m_backing_store->scale();
    auto color = m_backing_store->get_pixel(relative_point);
    return color.alpha() >= threshold;
}

void Window::set_menubar(MenuBar* menubar)
{
    if (m_menubar == menubar)
        return;
    m_menubar = menubar;
    if (m_menubar) {
        // FIXME: Maybe move this to the theming system?
        static constexpr auto menubar_menu_margin = 14;

        auto& wm = WindowManager::the();
        Gfx::IntPoint next_menu_location { 0, 0 };
        auto menubar_rect = Gfx::WindowTheme::current().menubar_rect(Gfx::WindowTheme::WindowType::Normal, rect(), wm.palette(), 1);
        m_menubar->for_each_menu([&](Menu& menu) {
            int text_width = wm.font().width(menu.name());
            menu.set_rect_in_window_menubar({ next_menu_location.x(), 0, text_width + menubar_menu_margin, menubar_rect.height() });
            next_menu_location.move_by(menu.rect_in_window_menubar().width(), 0);
            return IterationDecision::Continue;
        });
    }
    Compositor::the().invalidate_occlusions();
    frame().invalidate();
}

void Window::invalidate_menubar()
{
    if (!m_should_show_menubar || !menubar())
        return;
    // FIXME: This invalidates way more than the menubar!
    frame().invalidate();
}

}
