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

#include "ClientConnection.h"
#include <AK/Badge.h>
#include <LibGfx/Font.h>
#include <LibGfx/Painter.h>
#include <LibGfx/StylePainter.h>
#include <LibGfx/WindowTheme.h>
#include <WindowServer/Button.h>
#include <WindowServer/Compositor.h>
#include <WindowServer/Event.h>
#include <WindowServer/Screen.h>
#include <WindowServer/Window.h>
#include <WindowServer/WindowFrame.h>
#include <WindowServer/WindowManager.h>

namespace WindowServer {

static Gfx::WindowTheme::WindowType to_theme_window_type(WindowType type)
{
    switch (type) {
    case WindowType::Normal:
        return Gfx::WindowTheme::WindowType::Normal;
    case WindowType::ToolWindow:
        return Gfx::WindowTheme::WindowType::ToolWindow;
    case WindowType::Notification:
        return Gfx::WindowTheme::WindowType::Notification;
    default:
        return Gfx::WindowTheme::WindowType::Other;
    }
}

static Gfx::Bitmap* s_minimize_icon;
static Gfx::Bitmap* s_maximize_icon;
static Gfx::Bitmap* s_restore_icon;
static Gfx::Bitmap* s_close_icon;

static String s_last_title_button_icons_path;
static int s_last_title_button_icons_scale;

static Gfx::Bitmap* s_active_window_shadow;
static Gfx::Bitmap* s_inactive_window_shadow;
static Gfx::Bitmap* s_menu_bar_shadow;
static Gfx::Bitmap* s_menu_shadow;
static Gfx::Bitmap* s_task_bar_shadow;
static Gfx::Bitmap* s_tooltip_shadow;
static String s_last_active_window_shadow_path;
static String s_last_inactive_window_shadow_path;
static String s_last_menu_bar_shadow_path;
static String s_last_menu_shadow_path;
static String s_last_task_bar_shadow_path;
static String s_last_tooltip_shadow_path;

static Gfx::IntRect frame_rect_for_window(Window& window, const Gfx::IntRect& rect)
{
    if (window.is_frameless())
        return rect;
    return Gfx::WindowTheme::current().frame_rect_for_window(to_theme_window_type(window.type()), rect, WindowManager::the().palette());
}

WindowFrame::WindowFrame(Window& window)
    : m_window(window)
{
    auto button = make<Button>(*this, [this](auto&) {
        m_window.request_close();
    });
    m_close_button = button.ptr();
    m_buttons.append(move(button));

    if (window.is_resizable()) {
        auto button = make<Button>(*this, [this](auto&) {
            WindowManager::the().maximize_windows(m_window, !m_window.is_maximized());
        });
        button->on_middle_click = [&](auto&) {
            m_window.set_vertically_maximized();
        };
        m_maximize_button = button.ptr();
        m_buttons.append(move(button));
    }

    if (window.is_minimizable()) {
        auto button = make<Button>(*this, [this](auto&) {
            WindowManager::the().minimize_windows(m_window, true);
        });
        m_minimize_button = button.ptr();
        m_buttons.append(move(button));
    }

    set_button_icons();
}

WindowFrame::~WindowFrame()
{
}

void WindowFrame::set_button_icons()
{
    m_dirty = true;
    if (m_window.is_frameless())
        return;

    m_close_button->set_icon(*s_close_icon);
    if (m_window.is_minimizable())
        m_minimize_button->set_icon(*s_minimize_icon);
    if (m_window.is_resizable())
        m_maximize_button->set_icon(m_window.is_maximized() ? *s_restore_icon : *s_maximize_icon);
}

void WindowFrame::reload_config()
{
    String icons_path = WindowManager::the().palette().title_button_icons_path();
    int icons_scale = WindowManager::the().compositor_icon_scale();

    StringBuilder full_path;
    if (!s_minimize_icon || s_last_title_button_icons_path != icons_path || s_last_title_button_icons_scale != icons_scale) {
        full_path.append(icons_path);
        full_path.append("window-minimize.png");
        if (s_minimize_icon)
            s_minimize_icon->unref();
        if (!(s_minimize_icon = Gfx::Bitmap::load_from_file(full_path.to_string(), icons_scale).leak_ref()))
            s_minimize_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/downward-triangle.png", icons_scale).leak_ref();
        full_path.clear();
    }
    if (!s_maximize_icon || s_last_title_button_icons_path != icons_path || s_last_title_button_icons_scale != icons_scale) {
        full_path.append(icons_path);
        full_path.append("window-maximize.png");
        if (s_maximize_icon)
            s_maximize_icon->unref();
        if (!(s_maximize_icon = Gfx::Bitmap::load_from_file(full_path.to_string(), icons_scale).leak_ref()))
            s_maximize_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/upward-triangle.png", icons_scale).leak_ref();
        full_path.clear();
    }
    if (!s_restore_icon || s_last_title_button_icons_path != icons_path || s_last_title_button_icons_scale != icons_scale) {
        full_path.append(icons_path);
        full_path.append("window-restore.png");
        if (s_restore_icon)
            s_restore_icon->unref();
        if (!(s_restore_icon = Gfx::Bitmap::load_from_file(full_path.to_string(), icons_scale).leak_ref()))
            s_restore_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/window-restore.png", icons_scale).leak_ref();
        full_path.clear();
    }
    if (!s_close_icon || s_last_title_button_icons_path != icons_path || s_last_title_button_icons_scale != icons_scale) {
        full_path.append(icons_path);
        full_path.append("window-close.png");
        if (s_close_icon)
            s_close_icon->unref();
        if (!(s_close_icon = Gfx::Bitmap::load_from_file(full_path.to_string(), icons_scale).leak_ref()))
            s_close_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/window-close.png", icons_scale).leak_ref();
        full_path.clear();
    }

    s_last_title_button_icons_path = icons_path;
    s_last_title_button_icons_scale = icons_scale;

    auto load_shadow = [](const String& path, String& last_path, Gfx::Bitmap*& shadow_bitmap) {
        if (path.is_empty()) {
            last_path = String::empty();
            if (shadow_bitmap) {
                shadow_bitmap->unref();
                shadow_bitmap = nullptr;
            }
        } else if (!shadow_bitmap || shadow_bitmap->scale() != s_last_title_button_icons_scale || last_path != path) {
            if (shadow_bitmap)
                shadow_bitmap->unref();
            shadow_bitmap = Gfx::Bitmap::load_from_file(path, s_last_title_button_icons_scale).leak_ref();
            if (shadow_bitmap)
                last_path = path;
            else
                last_path = String::empty();
        }
    };
    load_shadow(WindowManager::the().palette().active_window_shadow_path(), s_last_active_window_shadow_path, s_active_window_shadow);
    load_shadow(WindowManager::the().palette().inactive_window_shadow_path(), s_last_inactive_window_shadow_path, s_inactive_window_shadow);
    load_shadow(WindowManager::the().palette().menu_bar_shadow_path(), s_last_menu_bar_shadow_path, s_menu_bar_shadow);
    load_shadow(WindowManager::the().palette().menu_shadow_path(), s_last_menu_shadow_path, s_menu_shadow);
    load_shadow(WindowManager::the().palette().task_bar_shadow_path(), s_last_task_bar_shadow_path, s_task_bar_shadow);
    load_shadow(WindowManager::the().palette().tooltip_shadow_path(), s_last_tooltip_shadow_path, s_tooltip_shadow);
}

Gfx::Bitmap* WindowFrame::window_shadow() const
{
    if (m_window.is_frameless())
        return nullptr;
    switch (m_window.type()) {
    case WindowType::Desktop:
        return nullptr;
    case WindowType::Menu:
        return s_menu_shadow;
    case WindowType::Tooltip:
        return s_tooltip_shadow;
    case WindowType::Menubar:
        return s_menu_bar_shadow;
    case WindowType::Taskbar:
        return s_task_bar_shadow;
    default:
        return m_window.is_active() ? s_active_window_shadow : s_inactive_window_shadow;
    }
}

bool WindowFrame::frame_has_alpha() const
{
    if (m_has_alpha_channel)
        return true;
    if (auto* shadow_bitmap = window_shadow(); shadow_bitmap && shadow_bitmap->format() == Gfx::BitmapFormat::RGBA32)
        return true;
    return false;
}

void WindowFrame::did_set_maximized(Badge<Window>, bool maximized)
{
    VERIFY(m_maximize_button);
    m_maximize_button->set_icon(maximized ? *s_restore_icon : *s_maximize_icon);
}

Gfx::IntRect WindowFrame::title_bar_rect() const
{
    return Gfx::WindowTheme::current().title_bar_rect(to_theme_window_type(m_window.type()), m_window.rect(), WindowManager::the().palette());
}

Gfx::IntRect WindowFrame::title_bar_icon_rect() const
{
    return Gfx::WindowTheme::current().title_bar_icon_rect(to_theme_window_type(m_window.type()), m_window.rect(), WindowManager::the().palette());
}

Gfx::IntRect WindowFrame::title_bar_text_rect() const
{
    return Gfx::WindowTheme::current().title_bar_text_rect(to_theme_window_type(m_window.type()), m_window.rect(), WindowManager::the().palette());
}

Gfx::WindowTheme::WindowState WindowFrame::window_state_for_theme() const
{
    auto& wm = WindowManager::the();

    if (m_flash_timer && m_flash_timer->is_active())
        return m_flash_counter & 1 ? Gfx::WindowTheme::WindowState::Active : Gfx::WindowTheme::WindowState::Inactive;

    if (&m_window == wm.m_highlight_window)
        return Gfx::WindowTheme::WindowState::Highlighted;
    if (&m_window == wm.m_move_window)
        return Gfx::WindowTheme::WindowState::Moving;
    if (wm.is_active_window_or_accessory(m_window))
        return Gfx::WindowTheme::WindowState::Active;
    return Gfx::WindowTheme::WindowState::Inactive;
}

void WindowFrame::paint_notification_frame(Gfx::Painter& painter)
{
    auto palette = WindowManager::the().palette();
    Gfx::WindowTheme::current().paint_notification_frame(painter, m_window.rect(), palette, m_buttons.last().relative_rect());
}

String WindowFrame::compute_title_text() const
{
    if (m_window.client() && m_window.client()->is_unresponsive())
        return String::formatted("{} (Not responding)", m_window.title());
    return m_window.title();
}

void WindowFrame::paint_tool_window_frame(Gfx::Painter& painter)
{
    auto palette = WindowManager::the().palette();
    auto leftmost_button_rect = m_buttons.is_empty() ? Gfx::IntRect() : m_buttons.last().relative_rect();
    Gfx::WindowTheme::current().paint_tool_window_frame(painter, window_state_for_theme(), m_window.rect(), compute_title_text(), palette, leftmost_button_rect);
}

void WindowFrame::paint_normal_frame(Gfx::Painter& painter)
{
    auto palette = WindowManager::the().palette();
    auto leftmost_button_rect = m_buttons.is_empty() ? Gfx::IntRect() : m_buttons.last().relative_rect();
    Gfx::WindowTheme::current().paint_normal_frame(painter, window_state_for_theme(), m_window.rect(), compute_title_text(), m_window.icon(), palette, leftmost_button_rect);
}

void WindowFrame::paint(Gfx::Painter& painter, const Gfx::IntRect& rect)
{
    render_to_cache();

    auto frame_rect = render_rect();
    auto window_rect = m_window.rect();

    if (m_top_bottom) {
        auto top_bottom_height = frame_rect.height() - window_rect.height();
        if (m_bottom_y > 0) {
            // We have a top piece
            auto src_rect = rect.intersected({ frame_rect.location(), { frame_rect.width(), m_bottom_y } });
            if (!src_rect.is_empty())
                painter.blit(src_rect.location(), *m_top_bottom, src_rect.translated(-frame_rect.location()), m_opacity);
        }
        if (m_bottom_y < top_bottom_height) {
            // We have a bottom piece
            Gfx::IntRect rect_in_frame { frame_rect.x(), window_rect.bottom() + 1, frame_rect.width(), top_bottom_height - m_bottom_y };
            auto src_rect = rect.intersected(rect_in_frame);
            if (!src_rect.is_empty())
                painter.blit(src_rect.location(), *m_top_bottom, src_rect.translated(-rect_in_frame.x(), -rect_in_frame.y() + m_bottom_y), m_opacity);
        }
    }

    if (m_left_right) {
        auto left_right_width = frame_rect.width() - window_rect.width();
        if (m_right_x > 0) {
            // We have a left piece
            Gfx::IntRect rect_in_frame { frame_rect.x(), window_rect.y(), m_right_x, window_rect.height() };
            auto src_rect = rect.intersected(rect_in_frame);
            if (!src_rect.is_empty())
                painter.blit(src_rect.location(), *m_left_right, src_rect.translated(-rect_in_frame.location()), m_opacity);
        }
        if (m_right_x < left_right_width) {
            // We have a right piece
            Gfx::IntRect rect_in_frame { window_rect.right() + 1, window_rect.y(), left_right_width - m_right_x, window_rect.height() };
            auto src_rect = rect.intersected(rect_in_frame);
            if (!src_rect.is_empty())
                painter.blit(src_rect.location(), *m_left_right, src_rect.translated(-rect_in_frame.x() + m_right_x, -rect_in_frame.y()), m_opacity);
        }
    }
}

void WindowFrame::render(Gfx::Painter& painter)
{
    if (m_window.is_frameless())
        return;

    if (m_window.type() == WindowType::Notification)
        paint_notification_frame(painter);
    else if (m_window.type() == WindowType::Normal)
        paint_normal_frame(painter);
    else if (m_window.type() == WindowType::ToolWindow)
        paint_tool_window_frame(painter);
    else
        return;

    for (auto& button : m_buttons) {
        button.paint(painter);
    }
}

void WindowFrame::theme_changed()
{
    m_dirty = m_shadow_dirty = true;
    m_top_bottom = nullptr;
    m_left_right = nullptr;
    m_bottom_y = m_right_x = 0;

    layout_buttons();
    set_button_icons();

    m_has_alpha_channel = Gfx::WindowTheme::current().frame_uses_alpha(window_state_for_theme(), WindowManager::the().palette());
}

void WindowFrame::render_to_cache()
{
    if (!m_dirty)
        return;
    m_dirty = false;

    m_has_alpha_channel = Gfx::WindowTheme::current().frame_uses_alpha(window_state_for_theme(), WindowManager::the().palette());

    static Gfx::Bitmap* s_tmp_bitmap;
    auto frame_rect = rect();
    auto total_frame_rect = frame_rect;
    Gfx::Bitmap* shadow_bitmap = inflate_for_shadow(total_frame_rect, m_shadow_offset);
    auto window_rect = m_window.rect();
    auto scale = Screen::the().scale_factor();
    if (!s_tmp_bitmap || !s_tmp_bitmap->size().contains(total_frame_rect.size()) || s_tmp_bitmap->scale() != scale) {
        if (s_tmp_bitmap)
            s_tmp_bitmap->unref();
        s_tmp_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::RGBA32, total_frame_rect.size(), scale).leak_ref();
    }

    auto top_bottom_height = total_frame_rect.height() - window_rect.height();
    auto left_right_width = total_frame_rect.width() - window_rect.width();

    if (!m_top_bottom || m_top_bottom->width() != total_frame_rect.width() || m_top_bottom->height() != top_bottom_height || m_top_bottom->scale() != scale) {
        if (top_bottom_height > 0)
            m_top_bottom = Gfx::Bitmap::create(Gfx::BitmapFormat::RGBA32, { total_frame_rect.width(), top_bottom_height }, scale);
        else
            m_top_bottom = nullptr;
        m_shadow_dirty = true;
    }
    if (!m_left_right || m_left_right->height() != total_frame_rect.height() || m_left_right->width() != left_right_width || m_left_right->scale() != scale) {
        if (left_right_width > 0)
            m_left_right = Gfx::Bitmap::create(Gfx::BitmapFormat::RGBA32, { left_right_width, total_frame_rect.height() }, scale);
        else
            m_left_right = nullptr;
        m_shadow_dirty = true;
    }

    auto& frame_rect_to_update = m_shadow_dirty ? total_frame_rect : frame_rect;
    Gfx::IntPoint update_location(m_shadow_dirty ? Gfx::IntPoint { 0, 0 } : m_shadow_offset);

    Gfx::Painter painter(*s_tmp_bitmap);

    // Clear the frame area, not including the window content area, which we don't care about
    for (auto& rect : frame_rect_to_update.shatter(window_rect))
        painter.clear_rect({ rect.location() - frame_rect_to_update.location(), rect.size() }, { 255, 255, 255, 0 });

    if (m_shadow_dirty && shadow_bitmap)
        paint_simple_rect_shadow(painter, { { 0, 0 }, total_frame_rect.size() }, *shadow_bitmap);

    {
        Gfx::PainterStateSaver save(painter);
        painter.translate(m_shadow_offset);
        render(painter);
    }

    if (m_top_bottom && top_bottom_height > 0) {
        m_bottom_y = window_rect.y() - total_frame_rect.y();
        VERIFY(m_bottom_y >= 0);

        Gfx::Painter top_bottom_painter(*m_top_bottom);
        top_bottom_painter.add_clip_rect({ update_location, { frame_rect_to_update.width(), top_bottom_height - update_location.y() - (total_frame_rect.bottom() - frame_rect_to_update.bottom()) } });
        if (m_bottom_y > 0)
            top_bottom_painter.blit({ 0, 0 }, *s_tmp_bitmap, { 0, 0, total_frame_rect.width(), m_bottom_y }, 1.0, false);
        if (m_bottom_y < top_bottom_height)
            top_bottom_painter.blit({ 0, m_bottom_y }, *s_tmp_bitmap, { 0, total_frame_rect.height() - (total_frame_rect.bottom() - window_rect.bottom()), total_frame_rect.width(), top_bottom_height - m_bottom_y }, 1.0, false);
    } else {
        m_bottom_y = 0;
    }

    if (left_right_width > 0) {
        m_right_x = window_rect.x() - total_frame_rect.x();
        VERIFY(m_right_x >= 0);

        Gfx::Painter left_right_painter(*m_left_right);
        left_right_painter.add_clip_rect({ update_location, { left_right_width - update_location.x() - (total_frame_rect.right() - frame_rect_to_update.right()), window_rect.height() } });
        if (m_right_x > 0)
            left_right_painter.blit({ 0, 0 }, *s_tmp_bitmap, { 0, m_bottom_y, m_right_x, window_rect.height() }, 1.0, false);
        if (m_right_x < left_right_width)
            left_right_painter.blit({ m_right_x, 0 }, *s_tmp_bitmap, { (window_rect.right() - total_frame_rect.x()) + 1, m_bottom_y, total_frame_rect.width() - (total_frame_rect.right() - window_rect.right()), window_rect.height() }, 1.0, false);
    } else {
        m_right_x = 0;
    }

    m_shadow_dirty = false;
}

void WindowFrame::set_opacity(float opacity)
{
    if (m_opacity == opacity)
        return;
    bool was_opaque = is_opaque();
    m_opacity = opacity;
    if (was_opaque != is_opaque())
        Compositor::the().invalidate_occlusions();
    Compositor::the().invalidate_screen(render_rect());
    WindowManager::the().notify_opacity_changed(m_window);
}

Gfx::IntRect WindowFrame::inflated_for_shadow(const Gfx::IntRect& frame_rect) const
{
    if (auto* shadow = window_shadow()) {
        auto total_shadow_size = shadow->height();
        return frame_rect.inflated(total_shadow_size, total_shadow_size);
    }
    return frame_rect;
}

Gfx::Bitmap* WindowFrame::inflate_for_shadow(Gfx::IntRect& frame_rect, Gfx::IntPoint& shadow_offset) const
{
    auto* shadow = window_shadow();
    if (shadow) {
        auto total_shadow_size = shadow->height();
        frame_rect.inflate(total_shadow_size, total_shadow_size);
        auto offset = total_shadow_size / 2;
        shadow_offset = { offset, offset };
    } else {
        shadow_offset = {};
    }
    return shadow;
}

Gfx::IntRect WindowFrame::rect() const
{
    return frame_rect_for_window(m_window, m_window.rect());
}

Gfx::IntRect WindowFrame::render_rect() const
{
    return inflated_for_shadow(rect());
}

void WindowFrame::invalidate_title_bar()
{
    m_dirty = true;
    invalidate(title_bar_rect());
}

void WindowFrame::invalidate(Gfx::IntRect relative_rect)
{
    auto frame_rect = rect();
    auto window_rect = m_window.rect();
    relative_rect.move_by(frame_rect.x() - window_rect.x(), frame_rect.y() - window_rect.y());
    m_dirty = true;
    m_window.invalidate(relative_rect, true);
}

void WindowFrame::notify_window_rect_changed(const Gfx::IntRect& old_rect, const Gfx::IntRect& new_rect)
{
    layout_buttons();

    auto old_frame_rect = inflated_for_shadow(frame_rect_for_window(m_window, old_rect));
    auto new_frame_rect = inflated_for_shadow(frame_rect_for_window(m_window, new_rect));
    if (old_frame_rect.size() != new_frame_rect.size())
        m_dirty = m_shadow_dirty = true;
    auto& compositor = Compositor::the();
    for (auto& dirty : old_frame_rect.shatter(new_frame_rect))
        compositor.invalidate_screen(dirty);
    if (!m_window.is_opaque())
        compositor.invalidate_screen(new_frame_rect);

    compositor.invalidate_occlusions();

    WindowManager::the().notify_rect_changed(m_window, old_rect, new_rect);
}

void WindowFrame::layout_buttons()
{
    auto button_rects = Gfx::WindowTheme::current().layout_buttons(to_theme_window_type(m_window.type()), m_window.rect(), WindowManager::the().palette(), m_buttons.size());
    for (size_t i = 0; i < m_buttons.size(); i++)
        m_buttons[i].set_relative_rect(button_rects[i]);
}

bool WindowFrame::hit_test(const Gfx::IntPoint& point) const
{
    if (m_window.is_frameless())
        return false;
    auto frame_rect = rect();
    if (!frame_rect.contains(point))
        return false;
    auto window_rect = m_window.rect();
    if (window_rect.contains(point))
        return false;

    u8 alpha_threshold = Gfx::WindowTheme::current().frame_alpha_hit_threshold(window_state_for_theme()) * 255;
    if (alpha_threshold == 0)
        return true;
    u8 alpha = 0xff;
    auto relative_point = point.translated(-render_rect().location());
    if (point.y() < window_rect.y()) {
        if (m_top_bottom)
            alpha = m_top_bottom->get_pixel(relative_point * m_top_bottom->scale()).alpha();
    } else if (point.y() > window_rect.bottom()) {
        if (m_top_bottom)
            alpha = m_top_bottom->get_pixel(relative_point.x() * m_top_bottom->scale(), m_bottom_y * m_top_bottom->scale() + point.y() - window_rect.bottom() - 1).alpha();
    } else if (point.x() < window_rect.x()) {
        if (m_left_right)
            alpha = m_left_right->get_pixel(relative_point.x() * m_left_right->scale(), (relative_point.y() - m_bottom_y) * m_left_right->scale()).alpha();
    } else if (point.x() > window_rect.right()) {
        if (m_left_right)
            alpha = m_left_right->get_pixel(m_right_x * m_left_right->scale() + point.x() - window_rect.right() - 1, (relative_point.y() - m_bottom_y) * m_left_right->scale()).alpha();
    } else {
        return false;
    }
    return alpha >= alpha_threshold;
}

void WindowFrame::on_mouse_event(const MouseEvent& event)
{
    VERIFY(!m_window.is_fullscreen());

    auto& wm = WindowManager::the();
    if (m_window.type() != WindowType::Normal && m_window.type() != WindowType::ToolWindow && m_window.type() != WindowType::Notification)
        return;

    if (m_window.type() == WindowType::Normal || m_window.type() == WindowType::ToolWindow) {
        if (event.type() == Event::MouseDown)
            wm.move_to_front_and_make_active(m_window);

        if (m_window.blocking_modal_window())
            return;

        if (title_bar_icon_rect().contains(event.position())) {
            if (event.type() == Event::MouseDown && (event.button() == MouseButton::Left || event.button() == MouseButton::Right)) {
                // Manually start a potential double click. Since we're opening
                // a menu, we will only receive the MouseDown event, so we
                // need to record that fact. If the user subsequently clicks
                // on the same area, the menu will get closed, and we will
                // receive a MouseUp event, but because windows have changed
                // we don't get a MouseDoubleClick event. We can however record
                // this click, and when we receive the MouseUp event check if
                // it would have been considered a double click, if it weren't
                // for the fact that we opened and closed a window in the meanwhile
                auto& wm = WindowManager::the();
                wm.start_menu_doubleclick(m_window, event);

                m_window.popup_window_menu(title_bar_rect().bottom_left().translated(rect().location()), WindowMenuDefaultAction::Close);
                return;
            } else if (event.type() == Event::MouseUp && event.button() == MouseButton::Left) {
                // Since the MouseDown event opened a menu, another MouseUp
                // from the second click outside the menu wouldn't be considered
                // a double click, so let's manually check if it would otherwise
                // have been be considered to be one
                auto& wm = WindowManager::the();
                if (wm.is_menu_doubleclick(m_window, event)) {
                    // It is a double click, so perform activate the default item
                    m_window.window_menu_activate_default();
                }
                return;
            }
        }
    }

    // This is slightly hackish, but expand the title bar rect by two pixels downwards,
    // so that mouse events between the title bar and window contents don't act like
    // mouse events on the border.
    auto adjusted_title_bar_rect = title_bar_rect();
    adjusted_title_bar_rect.set_height(adjusted_title_bar_rect.height() + 2);

    if (adjusted_title_bar_rect.contains(event.position())) {
        wm.clear_resize_candidate();

        if (event.type() == Event::MouseDown)
            wm.move_to_front_and_make_active(m_window);

        for (auto& button : m_buttons) {
            if (button.relative_rect().contains(event.position()))
                return button.on_mouse_event(event.translated(-button.relative_rect().location()));
        }
        if (event.type() == Event::MouseDown) {
            if ((m_window.type() == WindowType::Normal || m_window.type() == WindowType::ToolWindow) && event.button() == MouseButton::Right) {
                auto default_action = m_window.is_maximized() ? WindowMenuDefaultAction::Restore : WindowMenuDefaultAction::Maximize;
                m_window.popup_window_menu(event.position().translated(rect().location()), default_action);
                return;
            }
            if (m_window.is_movable() && event.button() == MouseButton::Left)
                wm.start_window_move(m_window, event.translated(rect().location()));
        }
        return;
    }

    if (m_window.is_resizable() && event.type() == Event::MouseMove && event.buttons() == 0) {
        constexpr ResizeDirection direction_for_hot_area[3][3] = {
            { ResizeDirection::UpLeft, ResizeDirection::Up, ResizeDirection::UpRight },
            { ResizeDirection::Left, ResizeDirection::None, ResizeDirection::Right },
            { ResizeDirection::DownLeft, ResizeDirection::Down, ResizeDirection::DownRight },
        };
        Gfx::IntRect outer_rect = { {}, rect().size() };
        VERIFY(outer_rect.contains(event.position()));
        int window_relative_x = event.x() - outer_rect.x();
        int window_relative_y = event.y() - outer_rect.y();
        int hot_area_row = min(2, window_relative_y / (outer_rect.height() / 3));
        int hot_area_column = min(2, window_relative_x / (outer_rect.width() / 3));
        wm.set_resize_candidate(m_window, direction_for_hot_area[hot_area_row][hot_area_column]);
        Compositor::the().invalidate_cursor();
        return;
    }

    if (m_window.is_resizable() && event.type() == Event::MouseDown && event.button() == MouseButton::Left)
        wm.start_window_resize(m_window, event.translated(rect().location()));
}

void WindowFrame::start_flash_animation()
{
    if (!m_flash_timer) {
        m_flash_timer = Core::Timer::construct(100, [this] {
            VERIFY(m_flash_counter);
            invalidate_title_bar();
            if (!--m_flash_counter)
                m_flash_timer->stop();
        });
    }
    m_flash_counter = 8;
    m_flash_timer->start();
}

void WindowFrame::paint_simple_rect_shadow(Gfx::Painter& painter, const Gfx::IntRect& containing_rect, const Gfx::Bitmap& shadow_bitmap) const
{
    // The layout of the shadow_bitmap is defined like this:
    // +---------+----+---------+----+----+----+
    // |   TL    | T  |   TR    | LT | L  | LB |
    // +---------+----+---------+----+----+----+
    // |   BL    | B  |   BR    | RT | R  | RB |
    // +---------+----+---------+----+----+----+
    // Located strictly on the top or bottom of the rectangle, above or below of the content:
    //   TL = top-left     T = top     TR = top-right
    //   BL = bottom-left  B = bottom  BR = bottom-right
    // Located on the left or right of the rectangle, but not above or below of the content:
    //   LT = left-top     L = left    LB = left-bottom
    //   RT = right-top    R = right   RB = right-bottom
    // So, the bitmap has two rows and 6 column, two of which are twice as wide.
    // The height divided by two defines a cell size, and width of each
    // column must be the same as the height of the cell, except for the
    // first an third column, which are twice as wide.
    if (shadow_bitmap.height() % 2 != 0) {
        dbgln("Can't paint simple rect shadow, shadow bitmap height {} is not even", shadow_bitmap.height());
        return;
    }
    auto base_size = shadow_bitmap.height() / 2;
    if (shadow_bitmap.width() != base_size * (6 + 2)) {
        if (shadow_bitmap.width() % base_size != 0)
            dbgln("Can't paint simple rect shadow, shadow bitmap width {} is not a multiple of {}", shadow_bitmap.width(), base_size);
        else
            dbgln("Can't paint simple rect shadow, shadow bitmap width {} but expected {}", shadow_bitmap.width(), base_size * (6 + 2));
        return;
    }

    // The containing_rect should have been inflated appropriately
    VERIFY(containing_rect.size().contains(Gfx::IntSize { base_size, base_size }));

    auto sides_height = containing_rect.height() - 2 * base_size;
    auto half_height = sides_height / 2;
    auto containing_horizontal_rect = containing_rect;

    int horizontal_shift = 0;
    if (half_height < base_size) {
        // If the height is too small we need to shift the left/right accordingly
        horizontal_shift = base_size - half_height;
        containing_horizontal_rect.set_left(containing_horizontal_rect.left() + horizontal_shift);
        containing_horizontal_rect.set_right(containing_horizontal_rect.right() - 2 * horizontal_shift);
    }
    auto half_width = containing_horizontal_rect.width() / 2;
    auto paint_horizontal = [&](int y, int src_row) {
        if (half_width <= 0)
            return;
        Gfx::PainterStateSaver save(painter);
        painter.add_clip_rect({ containing_horizontal_rect.left(), y, containing_horizontal_rect.width(), base_size });
        int corner_piece_width = min(containing_horizontal_rect.width() / 2, base_size * 2);
        int left_corners_right = containing_horizontal_rect.left() + corner_piece_width;
        int right_corners_left = max(containing_horizontal_rect.right() - corner_piece_width + 1, left_corners_right + 1);
        painter.blit({ containing_horizontal_rect.left(), y }, shadow_bitmap, { 0, src_row * base_size, corner_piece_width, base_size });
        painter.blit({ right_corners_left, y }, shadow_bitmap, { 5 * base_size - corner_piece_width, src_row * base_size, corner_piece_width, base_size });
        if (containing_horizontal_rect.width() > 2 * corner_piece_width) {
            for (int x = left_corners_right; x < right_corners_left; x += base_size) {
                auto width = min(right_corners_left - x, base_size);
                painter.blit({ x, y }, shadow_bitmap, { corner_piece_width, src_row * base_size, width, base_size });
            }
        }
    };

    paint_horizontal(containing_rect.top(), 0);
    paint_horizontal(containing_rect.bottom() - base_size + 1, 1);

    auto paint_vertical = [&](int x, int src_row, int hshift, int hsrcshift) {
        Gfx::PainterStateSaver save(painter);
        painter.add_clip_rect({ x, containing_rect.y() + base_size, base_size, containing_rect.height() - 2 * base_size });
        int corner_piece_height = min(half_height, base_size);
        int top_corners_bottom = base_size + corner_piece_height;
        int bottom_corners_top = base_size + max(half_height, sides_height - corner_piece_height);
        painter.blit({ x + hshift, containing_rect.top() + top_corners_bottom - corner_piece_height }, shadow_bitmap, { base_size * 5 + hsrcshift, src_row * base_size, base_size - hsrcshift, corner_piece_height });
        painter.blit({ x + hshift, containing_rect.top() + bottom_corners_top }, shadow_bitmap, { base_size * 7 + hsrcshift, src_row * base_size + base_size - corner_piece_height, base_size - hsrcshift, corner_piece_height });
        if (sides_height > 2 * base_size) {
            for (int y = top_corners_bottom; y < bottom_corners_top; y += base_size) {
                auto height = min(bottom_corners_top - y, base_size);
                painter.blit({ x, containing_rect.top() + y }, shadow_bitmap, { base_size * 6, src_row * base_size, base_size, height });
            }
        }
    };

    paint_vertical(containing_rect.left(), 0, horizontal_shift, 0);
    paint_vertical(containing_rect.right() - base_size + 1, 1, 0, horizontal_shift);
}

}
