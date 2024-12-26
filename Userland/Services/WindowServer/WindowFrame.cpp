/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConnectionFromClient.h"
#include <AK/Badge.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Painter.h>
#include <LibGfx/StylePainter.h>
#include <LibGfx/WindowTheme.h>
#include <WindowServer/Button.h>
#include <WindowServer/Compositor.h>
#include <WindowServer/Event.h>
#include <WindowServer/MultiScaleBitmaps.h>
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
    case WindowType::Notification:
        return Gfx::WindowTheme::WindowType::Notification;
    default:
        return Gfx::WindowTheme::WindowType::Other;
    }
}

static Gfx::WindowTheme::WindowMode to_theme_window_mode(WindowMode mode)
{
    switch (mode) {
    case WindowMode::RenderAbove:
        return Gfx::WindowTheme::WindowMode::RenderAbove;
    default:
        return Gfx::WindowTheme::WindowMode::Other;
    }
}

static Button::Icon s_minimize_icon;
static Button::Icon s_maximize_icon;
static Button::Icon s_restore_icon;
static Button::Icon s_close_icon;
static Button::Icon s_close_modified_icon;

static RefPtr<MultiScaleBitmaps> s_active_window_shadow;
static RefPtr<MultiScaleBitmaps> s_inactive_window_shadow;
static RefPtr<MultiScaleBitmaps> s_menu_shadow;
static RefPtr<MultiScaleBitmaps> s_taskbar_shadow;
static RefPtr<MultiScaleBitmaps> s_tooltip_shadow;
static ByteString s_last_active_window_shadow_path;
static ByteString s_last_inactive_window_shadow_path;
static ByteString s_last_menu_shadow_path;
static ByteString s_last_taskbar_shadow_path;
static ByteString s_last_tooltip_shadow_path;

static Gfx::WindowTheme& current_window_theme()
{
    auto& wm = WindowManager::the();
    return wm.palette().window_theme();
}

Gfx::IntRect WindowFrame::frame_rect_for_window(Window& window, Gfx::IntRect const& rect)
{
    if (window.is_frameless())
        return rect;
    int menu_row_count = (window.menubar().has_menus() && window.should_show_menubar()) ? 1 : 0;
    return current_window_theme().frame_rect_for_window(to_theme_window_type(window.type()), to_theme_window_mode(window.mode()), rect, WindowManager::the().palette(), menu_row_count);
}

WindowFrame::WindowFrame(Window& window)
    : m_window(window)
{
    // Because Window constructs a WindowFrame during its construction, we need
    // to be careful and defer doing initialization that assumes a fully
    // constructed Window. It is fully constructed when Window notifies us with
    // a call to WindowFrame::window_was_constructed.
}

void WindowFrame::window_was_constructed(Badge<Window>)
{
    if (m_window.is_closeable()) {
        auto button = make<Button>(*this, [this](auto&) {
            m_window.handle_window_menu_action(WindowMenuAction::Close);
        });
        m_close_button = button.ptr();
        m_buttons.append(move(button));
    }

    if (m_window.is_resizable()) {
        auto button = make<Button>(*this, [this](auto&) {
            m_window.handle_window_menu_action(WindowMenuAction::MaximizeOrRestore);
        });
        button->on_middle_click = [&](auto&) {
            if (m_window.tile_type() == WindowTileType::VerticallyMaximized)
                m_window.set_untiled();
            else
                m_window.set_tiled(WindowTileType::VerticallyMaximized);
        };
        button->on_secondary_click = [&](auto&) {
            if (m_window.tile_type() == WindowTileType::HorizontallyMaximized)
                m_window.set_untiled();
            else
                m_window.set_tiled(WindowTileType::HorizontallyMaximized);
        };
        m_maximize_button = button.ptr();
        m_buttons.append(move(button));
    }

    if (m_window.is_minimizable() && !m_window.is_modal()) {
        auto button = make<Button>(*this, [this](auto&) {
            m_window.handle_window_menu_action(WindowMenuAction::MinimizeOrUnminimize);
        });
        m_minimize_button = button.ptr();
        m_buttons.append(move(button));
    }

    set_button_icons();

    m_has_alpha_channel = current_window_theme().frame_uses_alpha(window_state_for_theme(), WindowManager::the().palette());
}

WindowFrame::~WindowFrame() = default;

void WindowFrame::set_button_icons()
{
    set_dirty();
    if (m_window.is_frameless())
        return;

    auto button_style = WindowManager::the().palette().title_buttons_icon_only()
        ? Button::Style::IconOnly
        : Button::Style::Normal;

    if (m_window.is_closeable()) {
        m_close_button->set_icon(m_window.is_modified() ? s_close_modified_icon : s_close_icon);
        m_close_button->set_style(button_style);
    }
    if (m_window.is_minimizable() && !m_window.is_modal()) {
        m_minimize_button->set_icon(s_minimize_icon);
        m_minimize_button->set_style(button_style);
    }
    if (m_window.is_resizable()) {
        m_maximize_button->set_icon(m_window.is_maximized() ? s_restore_icon : s_maximize_icon);
        m_maximize_button->set_style(button_style);
    }
}

void WindowFrame::reload_config()
{
    ByteString icons_path = WindowManager::the().palette().title_button_icons_path();

    auto reload_bitmap = [&](RefPtr<MultiScaleBitmaps>& multiscale_bitmap, StringView path, StringView default_path = ""sv) {
        StringBuilder full_path;
        full_path.append(icons_path);
        full_path.append(path);
        if (multiscale_bitmap)
            multiscale_bitmap->load(full_path.string_view(), default_path);
        else
            multiscale_bitmap = MultiScaleBitmaps::create(full_path.string_view(), default_path);
    };

    auto reload_icon = [&](Button::Icon& icon, StringView name, StringView default_path) {
        StringBuilder full_name;
        full_name.append(name);
        full_name.append(".png"sv);
        reload_bitmap(icon.bitmap, full_name.string_view(), default_path);
        // Note: No default for hover bitmaps
        full_name.clear();
        full_name.append(name);
        full_name.append("-hover.png"sv);
        reload_bitmap(icon.hover_bitmap, full_name.string_view());
    };
    reload_icon(s_minimize_icon, "window-minimize"sv, "/res/icons/16x16/downward-triangle.png"sv);
    reload_icon(s_maximize_icon, "window-maximize"sv, "/res/icons/16x16/upward-triangle.png"sv);
    reload_icon(s_restore_icon, "window-restore"sv, "/res/icons/16x16/window-restore.png"sv);
    reload_icon(s_close_icon, "window-close"sv, "/res/icons/16x16/window-close.png"sv);
    reload_icon(s_close_modified_icon, "window-close-modified"sv, "/res/icons/16x16/window-close-modified.png"sv);

    auto load_shadow = [](ByteString const& path, ByteString& last_path, RefPtr<MultiScaleBitmaps>& shadow_bitmap) {
        if (path.is_empty()) {
            last_path = ByteString::empty();
            shadow_bitmap = nullptr;
        } else if (!shadow_bitmap || last_path != path) {
            if (shadow_bitmap)
                shadow_bitmap->load(path);
            else
                shadow_bitmap = MultiScaleBitmaps::create(path);
            if (shadow_bitmap)
                last_path = path;
            else
                last_path = ByteString::empty();
        }
    };
    load_shadow(WindowManager::the().palette().active_window_shadow_path(), s_last_active_window_shadow_path, s_active_window_shadow);
    load_shadow(WindowManager::the().palette().inactive_window_shadow_path(), s_last_inactive_window_shadow_path, s_inactive_window_shadow);
    load_shadow(WindowManager::the().palette().menu_shadow_path(), s_last_menu_shadow_path, s_menu_shadow);
    load_shadow(WindowManager::the().palette().taskbar_shadow_path(), s_last_taskbar_shadow_path, s_taskbar_shadow);
    load_shadow(WindowManager::the().palette().tooltip_shadow_path(), s_last_tooltip_shadow_path, s_tooltip_shadow);
}

MultiScaleBitmaps const* WindowFrame::shadow_bitmap() const
{
    if (m_window.is_frameless() && !m_window.has_forced_shadow())
        return nullptr;
    switch (m_window.type()) {
    case WindowType::Desktop:
        return nullptr;
    case WindowType::Menu:
        if (!WindowManager::the().system_effects().menu_shadow())
            return nullptr;
        return s_menu_shadow;
    case WindowType::Autocomplete:
    case WindowType::Tooltip:
        if (!WindowManager::the().system_effects().tooltip_shadow())
            return nullptr;
        return s_tooltip_shadow;
    case WindowType::Taskbar:
        return s_taskbar_shadow;
    case WindowType::AppletArea:
        return nullptr;
    case WindowType::WindowSwitcher:
        return nullptr;
    case WindowType::Popup:
        if (!WindowManager::the().system_effects().window_shadow())
            return nullptr;
        if (!m_window.has_forced_shadow())
            return nullptr;
        return s_active_window_shadow;
    default:
        if (!WindowManager::the().system_effects().window_shadow())
            return nullptr;
        // FIXME: Support shadow for themes with border radius
        if (WindowManager::the().palette().window_border_radius() > 0)
            return nullptr;
        if (auto* highlight_window = WindowManager::the().highlight_window())
            return highlight_window == &m_window ? s_active_window_shadow : s_inactive_window_shadow;
        return m_window.is_active() ? s_active_window_shadow : s_inactive_window_shadow;
    }
}

bool WindowFrame::has_shadow() const
{
    if (auto* shadow_bitmap = this->shadow_bitmap(); shadow_bitmap && shadow_bitmap->format() == Gfx::BitmapFormat::BGRA8888)
        return true;
    return false;
}

void WindowFrame::did_set_maximized(Badge<Window>, bool maximized)
{
    VERIFY(m_maximize_button);
    set_dirty();
    m_maximize_button->set_icon(maximized ? s_restore_icon : s_maximize_icon);
}

Gfx::IntRect WindowFrame::menubar_rect() const
{
    if (!m_window.menubar().has_menus() || !m_window.should_show_menubar())
        return {};
    return current_window_theme().menubar_rect(to_theme_window_type(m_window.type()), to_theme_window_mode(m_window.mode()), m_window.rect(), WindowManager::the().palette(), menu_row_count());
}

Gfx::IntRect WindowFrame::titlebar_rect() const
{
    return current_window_theme().titlebar_rect(to_theme_window_type(m_window.type()), to_theme_window_mode(m_window.mode()), m_window.rect(), WindowManager::the().palette());
}

Gfx::IntRect WindowFrame::titlebar_icon_rect() const
{
    return current_window_theme().titlebar_icon_rect(to_theme_window_type(m_window.type()), to_theme_window_mode(m_window.mode()), m_window.rect(), WindowManager::the().palette());
}

Gfx::IntRect WindowFrame::titlebar_text_rect() const
{
    return current_window_theme().titlebar_text_rect(to_theme_window_type(m_window.type()), to_theme_window_mode(m_window.mode()), m_window.rect(), WindowManager::the().palette());
}

Gfx::WindowTheme::WindowState WindowFrame::window_state_for_theme() const
{
    auto& wm = WindowManager::the();

    if (m_flash_timer && m_flash_timer->is_active())
        return m_flash_counter & 1 ? Gfx::WindowTheme::WindowState::Highlighted : Gfx::WindowTheme::WindowState::Inactive;

    if (&m_window == wm.highlight_window())
        return Gfx::WindowTheme::WindowState::Highlighted;
    if (&m_window == wm.m_move_window)
        return Gfx::WindowTheme::WindowState::Moving;
    if (m_window.is_active())
        return Gfx::WindowTheme::WindowState::Active;
    return Gfx::WindowTheme::WindowState::Inactive;
}

void WindowFrame::paint_notification_frame(Gfx::Painter& painter)
{
    auto palette = WindowManager::the().palette();
    current_window_theme().paint_notification_frame(painter, to_theme_window_mode(m_window.mode()), m_window.rect(), palette, m_buttons.last()->relative_rect());
}

void WindowFrame::paint_menubar(Gfx::Painter& painter)
{
    auto& wm = WindowManager::the();
    auto& font = wm.font();
    auto palette = wm.palette();
    auto menubar_rect = this->menubar_rect();

    painter.fill_rect(menubar_rect, palette.window());

    Gfx::PainterStateSaver saver(painter);
    painter.add_clip_rect(menubar_rect);
    painter.translate(menubar_rect.location());

    m_window.menubar().for_each_menu([&](Menu& menu) {
        bool paint_as_flashed = ((&menu) == m_window.menubar().flashed_menu());
        if (paint_as_flashed) {
            auto flashed_rect = menu.rect_in_window_menubar();
            flashed_rect.shrink(2, 2);
            painter.fill_rect(flashed_rect, palette.selection());
        }

        auto text_rect = menu.rect_in_window_menubar();
        Color text_color = (paint_as_flashed ? palette.selection_text() : palette.window_text());
        auto is_open = menu.is_open();
        if (is_open)
            text_rect.translate_by(1, 1);
        bool paint_as_pressed = is_open;
        bool paint_as_hovered = !paint_as_pressed && &menu == MenuManager::the().hovered_menu();
        if (paint_as_pressed || paint_as_hovered) {
            Gfx::StylePainter::paint_button(painter, menu.rect_in_window_menubar(), palette, Gfx::ButtonStyle::Coolbar, paint_as_pressed, paint_as_hovered);
        }
        painter.draw_ui_text(text_rect, menu.name(), font, Gfx::TextAlignment::Center, text_color);
        return IterationDecision::Continue;
    });
}

void WindowFrame::paint_normal_frame(Gfx::Painter& painter)
{
    auto palette = WindowManager::the().palette();
    current_window_theme().paint_normal_frame(painter, window_state_for_theme(), to_theme_window_mode(m_window.mode()), m_window.rect(), m_window.computed_title(), m_window.icon(), palette, leftmost_titlebar_button_rect(), menu_row_count(), m_window.is_modified());

    if (m_window.menubar().has_menus() && m_window.should_show_menubar())
        paint_menubar(painter);
}

void WindowFrame::paint(Screen& screen, Gfx::Painter& painter, Gfx::IntRect const& rect)
{
    if (auto* cached = render_to_cache(screen))
        cached->paint(*this, painter, rect);
}

void WindowFrame::PerScaleRenderedCache::paint(WindowFrame& frame, Gfx::Painter& painter, Gfx::IntRect const& rect)
{
    auto frame_rect = frame.unconstrained_render_rect();
    auto window_rect = frame.window().rect();
    if (m_top_bottom) {
        auto top_bottom_height = frame_rect.height() - window_rect.height();
        if (m_bottom_y > 0) {
            // We have a top piece
            auto src_rect = rect.intersected(Gfx::Rect { frame_rect.location(), { frame_rect.width(), m_bottom_y } });
            if (!src_rect.is_empty())
                painter.blit(src_rect.location(), *m_top_bottom, src_rect.translated(-frame_rect.location()));
        }
        if (m_bottom_y < top_bottom_height) {
            // We have a bottom piece
            Gfx::IntRect rect_in_frame { frame_rect.x(), window_rect.bottom(), frame_rect.width(), top_bottom_height - m_bottom_y };
            auto src_rect = rect.intersected(rect_in_frame);
            if (!src_rect.is_empty())
                painter.blit(src_rect.location(), *m_top_bottom, src_rect.translated(-rect_in_frame.x(), -rect_in_frame.y() + m_bottom_y));
        }
    }

    if (m_left_right) {
        auto left_right_width = frame_rect.width() - window_rect.width();
        if (m_right_x > 0) {
            // We have a left piece
            Gfx::IntRect rect_in_frame { frame_rect.x(), window_rect.y(), m_right_x, window_rect.height() };
            auto src_rect = rect.intersected(rect_in_frame);
            if (!src_rect.is_empty())
                painter.blit(src_rect.location(), *m_left_right, src_rect.translated(-rect_in_frame.location()));
        }
        if (m_right_x < left_right_width) {
            // We have a right piece
            Gfx::IntRect rect_in_frame { window_rect.right(), window_rect.y(), left_right_width - m_right_x, window_rect.height() };
            auto src_rect = rect.intersected(rect_in_frame);
            if (!src_rect.is_empty())
                painter.blit(src_rect.location(), *m_left_right, src_rect.translated(-rect_in_frame.x() + m_right_x, -rect_in_frame.y()));
        }
    }
}

void WindowFrame::render(Screen& screen, Gfx::Painter& painter)
{
    if (m_window.is_frameless())
        return;

    if (m_window.type() == WindowType::Notification)
        paint_notification_frame(painter);
    else if (m_window.type() == WindowType::Normal)
        paint_normal_frame(painter);
    else
        return;

    for (auto& button : m_buttons)
        button->paint(screen, painter);
}

void WindowFrame::theme_changed()
{
    m_rendered_cache = {};

    layout_buttons();
    set_button_icons();

    m_has_alpha_channel = current_window_theme().frame_uses_alpha(window_state_for_theme(), WindowManager::the().palette());
}

auto WindowFrame::render_to_cache(Screen& screen) -> PerScaleRenderedCache*
{
    auto scale = screen.scale_factor();
    PerScaleRenderedCache* rendered_cache;
    auto cached_it = m_rendered_cache.find(scale);
    if (cached_it == m_rendered_cache.end()) {
        auto new_rendered_cache = make<PerScaleRenderedCache>();
        rendered_cache = new_rendered_cache.ptr();
        m_rendered_cache.set(scale, move(new_rendered_cache));
    } else {
        rendered_cache = cached_it->value.ptr();
    }
    rendered_cache->render(*this, screen);
    return rendered_cache;
}

void WindowFrame::PerScaleRenderedCache::render(WindowFrame& frame, Screen& screen)
{
    if (!m_dirty)
        return;
    m_dirty = false;

    auto scale = screen.scale_factor();

    auto frame_rect = frame.rect();

    auto frame_rect_including_shadow = frame_rect;
    auto* shadow_bitmap = frame.shadow_bitmap();
    Gfx::IntPoint shadow_offset;

    if (shadow_bitmap) {
        auto total_shadow_size = shadow_bitmap->bitmap(screen.scale_factor()).height();
        frame_rect_including_shadow.inflate(total_shadow_size, total_shadow_size);
        auto offset = total_shadow_size / 2;
        shadow_offset = { offset, offset };
    }

    auto window_rect = frame.window().rect();

    // TODO: if we stop using a scaling factor we should clear cached bitmaps from this map
    static HashMap<int, RefPtr<Gfx::Bitmap>> s_tmp_bitmap_cache;
    Gfx::Bitmap* tmp_bitmap;
    {
        auto tmp_it = s_tmp_bitmap_cache.find(scale);
        if (tmp_it == s_tmp_bitmap_cache.end() || !tmp_it->value->size().contains(frame_rect_including_shadow.size())) {
            // Explicitly clear the old bitmap first so this works on machines with very little memory
            if (tmp_it != s_tmp_bitmap_cache.end())
                tmp_it->value = nullptr;

            auto bitmap_or_error = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, frame_rect_including_shadow.size(), scale);
            if (bitmap_or_error.is_error()) {
                s_tmp_bitmap_cache.remove(scale);
                dbgln("Could not create bitmap of size {}: {}", frame_rect_including_shadow.size(), bitmap_or_error.error());
                return;
            }
            auto bitmap = bitmap_or_error.release_value();
            tmp_bitmap = bitmap.ptr();
            if (tmp_it != s_tmp_bitmap_cache.end())
                tmp_it->value = move(bitmap);
            else
                s_tmp_bitmap_cache.set(scale, move(bitmap));
        } else {
            tmp_bitmap = tmp_it->value.ptr();
        }
    }

    VERIFY(tmp_bitmap);

    auto top_bottom_height = frame_rect_including_shadow.height() - window_rect.height();
    auto left_right_width = frame_rect_including_shadow.width() - window_rect.width();

    if (!m_top_bottom || m_top_bottom->width() != frame_rect_including_shadow.width() || m_top_bottom->height() != top_bottom_height || m_top_bottom->scale() != scale) {
        if (top_bottom_height > 0)
            m_top_bottom = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { frame_rect_including_shadow.width(), top_bottom_height }, scale).release_value_but_fixme_should_propagate_errors();
        else
            m_top_bottom = nullptr;
        m_shadow_dirty = true;
    }
    if (!m_left_right || m_left_right->height() != frame_rect_including_shadow.height() || m_left_right->width() != left_right_width || m_left_right->scale() != scale) {
        if (left_right_width > 0)
            m_left_right = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { left_right_width, frame_rect_including_shadow.height() }, scale).release_value_but_fixme_should_propagate_errors();
        else
            m_left_right = nullptr;
        m_shadow_dirty = true;
    }

    auto& frame_rect_to_update = m_shadow_dirty ? frame_rect_including_shadow : frame_rect;
    Gfx::IntPoint update_location(m_shadow_dirty ? Gfx::IntPoint { 0, 0 } : shadow_offset);

    Gfx::Painter painter(*tmp_bitmap);

    // Clear the frame area, not including the window content area, which we don't care about
    for (auto& rect : frame_rect_to_update.shatter(window_rect))
        painter.clear_rect({ rect.location() - frame_rect_to_update.location(), rect.size() }, { 255, 255, 255, 0 });

    if (m_shadow_dirty && shadow_bitmap)
        Gfx::StylePainter::paint_simple_rect_shadow(painter, { { 0, 0 }, frame_rect_including_shadow.size() }, shadow_bitmap->bitmap(screen.scale_factor()));

    {
        Gfx::PainterStateSaver save(painter);
        painter.translate(shadow_offset);
        frame.render(screen, painter);
    }

    if (m_top_bottom && top_bottom_height > 0) {
        m_bottom_y = window_rect.y() - frame_rect_including_shadow.y();
        VERIFY(m_bottom_y >= 0);

        Gfx::Painter top_bottom_painter(*m_top_bottom);
        top_bottom_painter.add_clip_rect({ update_location, { frame_rect_to_update.width(), top_bottom_height - update_location.y() - (frame_rect_including_shadow.bottom() - frame_rect_to_update.bottom()) } });
        if (m_bottom_y > 0)
            top_bottom_painter.blit({ 0, 0 }, *tmp_bitmap, { 0, 0, frame_rect_including_shadow.width(), m_bottom_y }, 1.f, false);
        if (m_bottom_y < top_bottom_height)
            top_bottom_painter.blit({ 0, m_bottom_y }, *tmp_bitmap, { 0, frame_rect_including_shadow.height() - (frame_rect_including_shadow.bottom() - window_rect.bottom()), frame_rect_including_shadow.width(), top_bottom_height - m_bottom_y }, 1.f, false);
    } else {
        m_bottom_y = 0;
    }

    if (left_right_width > 0) {
        m_right_x = window_rect.x() - frame_rect_including_shadow.x();
        VERIFY(m_right_x >= 0);

        Gfx::Painter left_right_painter(*m_left_right);
        left_right_painter.add_clip_rect({ update_location, { left_right_width - update_location.x() - (frame_rect_including_shadow.right() - frame_rect_to_update.right()), window_rect.height() } });
        if (m_right_x > 0)
            left_right_painter.blit({ 0, 0 }, *tmp_bitmap, { 0, m_bottom_y, m_right_x, window_rect.height() }, 1.0, false);
        if (m_right_x < left_right_width)
            left_right_painter.blit({ m_right_x, 0 }, *tmp_bitmap, { window_rect.right() - frame_rect_including_shadow.x(), m_bottom_y, frame_rect_including_shadow.width() - (frame_rect_including_shadow.right() - window_rect.right()), window_rect.height() }, 1.0, false);
    } else {
        m_right_x = 0;
    }

    m_shadow_dirty = false;
}

Gfx::IntRect WindowFrame::inflated_for_shadow(Gfx::IntRect const& frame_rect) const
{
    if (auto* shadow = shadow_bitmap()) {
        auto total_shadow_size = shadow->default_bitmap().height();
        return frame_rect.inflated(total_shadow_size, total_shadow_size);
    }
    return frame_rect;
}

Gfx::IntRect WindowFrame::rect() const
{
    return frame_rect_for_window(m_window, m_window.rect());
}

Gfx::IntRect WindowFrame::constrained_render_rect_to_screen(Gfx::IntRect const& render_rect) const
{
    if (m_window.is_tiled())
        return render_rect.intersected(Screen::closest_to_rect(rect()).rect());
    return render_rect;
}

Gfx::IntRect WindowFrame::leftmost_titlebar_button_rect() const
{
    if (!m_buttons.is_empty())
        return m_buttons.last()->relative_rect();

    auto rect = titlebar_rect();
    rect.translate_by(rect.width(), 0);
    return rect;
}

Gfx::IntRect WindowFrame::render_rect() const
{
    return constrained_render_rect_to_screen(inflated_for_shadow(rect()));
}

Gfx::IntRect WindowFrame::unconstrained_render_rect() const
{
    return inflated_for_shadow(rect());
}

Gfx::DisjointIntRectSet WindowFrame::opaque_render_rects() const
{
    auto border_radius = WindowManager::the().palette().window_border_radius();
    if (has_alpha_channel() || border_radius > 0) {
        if (m_window.is_opaque())
            return constrained_render_rect_to_screen(m_window.rect());
        return {};
    }
    if (m_window.is_opaque())
        return constrained_render_rect_to_screen(rect());
    Gfx::DisjointIntRectSet opaque_rects;
    opaque_rects.add_many(constrained_render_rect_to_screen(rect()).shatter(m_window.rect()));
    return opaque_rects;
}

Gfx::DisjointIntRectSet WindowFrame::transparent_render_rects() const
{
    auto border_radius = WindowManager::the().palette().window_border_radius();
    if (has_alpha_channel() || border_radius > 0) {
        if (m_window.is_opaque()) {
            Gfx::DisjointIntRectSet transparent_rects;
            transparent_rects.add_many(render_rect().shatter(m_window.rect()));
            return transparent_rects;
        }
        return render_rect();
    }

    auto total_render_rect = render_rect();
    Gfx::DisjointIntRectSet transparent_rects;
    if (has_shadow())
        transparent_rects.add_many(total_render_rect.shatter(rect()));
    if (!m_window.is_opaque())
        transparent_rects.add(m_window.rect().intersected(total_render_rect));
    return transparent_rects;
}

void WindowFrame::invalidate_titlebar()
{
    set_dirty();
    invalidate(titlebar_rect());
}

void WindowFrame::invalidate()
{
    auto frame_rect = render_rect();
    invalidate(Gfx::IntRect { frame_rect.location() - m_window.position(), frame_rect.size() });
    m_window.invalidate(true, true);
}

void WindowFrame::invalidate_menubar()
{
    invalidate(menubar_rect());
}

void WindowFrame::invalidate(Gfx::IntRect relative_rect)
{
    auto frame_rect = rect();
    auto window_rect = m_window.rect();
    relative_rect.translate_by(frame_rect.x() - window_rect.x(), frame_rect.y() - window_rect.y());
    set_dirty();
    m_window.invalidate(relative_rect, true);
}

void WindowFrame::window_rect_changed(Gfx::IntRect const& old_rect, Gfx::IntRect const& new_rect)
{
    layout_buttons();

    set_dirty(true);
    WindowManager::the().notify_rect_changed(m_window, old_rect, new_rect);
}

void WindowFrame::layout_buttons()
{
    auto button_rects = current_window_theme().layout_buttons(to_theme_window_type(m_window.type()), to_theme_window_mode(m_window.mode()), m_window.rect(), WindowManager::the().palette(), m_buttons.size(), m_window.is_maximized());
    for (size_t i = 0; i < m_buttons.size(); i++)
        m_buttons[i]->set_relative_rect(button_rects[i]);
}

Optional<HitTestResult> WindowFrame::hit_test(Gfx::IntPoint position)
{
    if (m_window.is_frameless() || m_window.is_fullscreen())
        return {};
    if (!constrained_render_rect_to_screen(rect()).contains(position)) {
        // Checking just frame_rect is not enough. If we constrain rendering
        // a window to one screen (e.g. when it's maximized or tiled) so that
        // the frame doesn't bleed into the adjacent screen(s), then we need
        // to also check that we're within these bounds.
        return {};
    }
    auto window_rect = m_window.rect();
    if (window_rect.contains(position))
        return {};

    auto* screen = Screen::find_by_location(position);
    if (!screen)
        return {};
    auto* cached = render_to_cache(*screen);
    if (!cached)
        return {};

    auto window_relative_position = position.translated(-unconstrained_render_rect().location());
    return cached->hit_test(*this, position, window_relative_position);
}

Optional<HitTestResult> WindowFrame::PerScaleRenderedCache::hit_test(WindowFrame& frame, Gfx::IntPoint position, Gfx::IntPoint window_relative_position)
{
    HitTestResult result {
        .window = frame.window(),
        .screen_position = position,
        .window_relative_position = window_relative_position,
        .is_frame_hit = true,
    };

    u8 alpha_threshold = current_window_theme().frame_alpha_hit_threshold(frame.window_state_for_theme()) * 255;
    if (alpha_threshold == 0)
        return result;
    u8 alpha = 0xff;

    auto window_rect = frame.window().rect();
    if (position.y() < window_rect.y()) {
        if (m_top_bottom) {
            auto scaled_relative_point = window_relative_position * m_top_bottom->scale();
            if (m_top_bottom->rect().contains(scaled_relative_point))
                alpha = m_top_bottom->get_pixel(scaled_relative_point).alpha();
        }
    } else if (position.y() >= window_rect.bottom()) {
        if (m_top_bottom) {
            Gfx::IntPoint scaled_relative_point { window_relative_position.x() * m_top_bottom->scale(), m_bottom_y * m_top_bottom->scale() + position.y() - window_rect.bottom() };
            if (m_top_bottom->rect().contains(scaled_relative_point))
                alpha = m_top_bottom->get_pixel(scaled_relative_point).alpha();
        }
    } else if (position.x() < window_rect.x()) {
        if (m_left_right) {
            Gfx::IntPoint scaled_relative_point { window_relative_position.x() * m_left_right->scale(), (window_relative_position.y() - m_bottom_y) * m_left_right->scale() };
            if (m_left_right->rect().contains(scaled_relative_point))
                alpha = m_left_right->get_pixel(scaled_relative_point).alpha();
        }
    } else if (position.x() >= window_rect.right()) {
        if (m_left_right) {
            Gfx::IntPoint scaled_relative_point { m_right_x * m_left_right->scale() + position.x() - window_rect.right(), (window_relative_position.y() - m_bottom_y) * m_left_right->scale() };
            if (m_left_right->rect().contains(scaled_relative_point))
                alpha = m_left_right->get_pixel(scaled_relative_point).alpha();
        }
    } else {
        return {};
    }
    if (alpha >= alpha_threshold)
        return result;
    return {};
}

bool WindowFrame::handle_titlebar_icon_mouse_event(MouseEvent const& event)
{
    auto& wm = WindowManager::the();

    if (event.type() == Event::MouseDown && (event.button() == MouseButton::Primary || event.button() == MouseButton::Secondary)) {
        // Manually start a potential double click. Since we're opening
        // a menu, we will only receive the MouseDown event, so we
        // need to record that fact. If the user subsequently clicks
        // on the same area, the menu will get closed, and we will
        // receive a MouseUp event, but because windows have changed
        // we don't get a MouseDoubleClick event. We can however record
        // this click, and when we receive the MouseUp event check if
        // it would have been considered a double click, if it weren't
        // for the fact that we opened and closed a window in the meanwhile
        wm.system_menu_doubleclick(m_window, event);

        m_window.popup_window_menu(titlebar_rect().bottom_left().moved_up(1).translated(rect().location()), WindowMenuDefaultAction::Close);
        return true;
    } else if (event.type() == Event::MouseUp && event.button() == MouseButton::Primary) {
        // Since the MouseDown event opened a menu, another MouseUp
        // from the second click outside the menu wouldn't be considered
        // a double click, so let's manually check if it would otherwise
        // have been be considered to be one
        if (wm.is_menu_doubleclick(m_window, event)) {
            // It is a double click, so perform activate the default item
            m_window.window_menu_activate_default();
        }
        return true;
    }
    return false;
}

void WindowFrame::handle_titlebar_mouse_event(MouseEvent const& event)
{
    auto& wm = WindowManager::the();

    if (titlebar_icon_rect().contains(event.position())) {
        if (handle_titlebar_icon_mouse_event(event))
            return;
    }

    for (auto& button : m_buttons) {
        if (button->relative_rect().contains(event.position()))
            return button->on_mouse_event(event.translated(-button->relative_rect().location()));
    }

    if (event.type() == Event::MouseDown) {
        if (m_window.type() == WindowType::Normal && event.button() == MouseButton::Secondary) {
            auto default_action = m_window.is_maximized() ? WindowMenuDefaultAction::Restore : WindowMenuDefaultAction::Maximize;
            m_window.popup_window_menu(event.position().translated(rect().location()), default_action);
            return;
        }
        if (m_window.is_movable() && event.button() == MouseButton::Primary)
            wm.start_window_move(m_window, event.translated(rect().location()));
    }
}

void WindowFrame::handle_mouse_event(MouseEvent const& event)
{
    VERIFY(!m_window.is_fullscreen());

    if (m_window.type() != WindowType::Normal && m_window.type() != WindowType::Notification)
        return;

    auto& wm = WindowManager::the();
    if (m_window.type() == WindowType::Normal) {
        if (event.type() == Event::MouseDown)
            wm.move_to_front_and_make_active(m_window);
    }

    if (m_window.blocking_modal_window())
        return;

    // This is slightly hackish, but expand the title bar rect by two pixels downwards,
    // so that mouse events between the title bar and window contents don't act like
    // mouse events on the border.
    auto adjusted_titlebar_rect = titlebar_rect();
    adjusted_titlebar_rect.set_height(adjusted_titlebar_rect.height() + 2);

    if (adjusted_titlebar_rect.contains(event.position())) {
        handle_titlebar_mouse_event(event);
        return;
    }

    if (menubar_rect().contains(event.position())) {
        handle_menubar_mouse_event(event);
        return;
    }

    handle_border_mouse_event(event);
}

void WindowFrame::handle_border_mouse_event(MouseEvent const& event)
{
    if (!m_window.is_resizable())
        return;

    auto& wm = WindowManager::the();

    constexpr ResizeDirection direction_for_hot_area[3][3] = {
        { ResizeDirection::UpLeft, ResizeDirection::Up, ResizeDirection::UpRight },
        { ResizeDirection::Left, ResizeDirection::None, ResizeDirection::Right },
        { ResizeDirection::DownLeft, ResizeDirection::Down, ResizeDirection::DownRight },
    };
    Gfx::IntRect outer_rect = { {}, rect().size() };
    VERIFY(outer_rect.contains(event.position()));
    int window_relative_x = event.x() - outer_rect.x();
    int window_relative_y = event.y() - outer_rect.y();
    int corner_size = titlebar_rect().height();
    int hot_area_row = (window_relative_y < corner_size) ? 0 : (window_relative_y > outer_rect.height() - corner_size) ? 2
                                                                                                                       : 1;
    int hot_area_column = (window_relative_x < corner_size) ? 0 : (window_relative_x > outer_rect.width() - corner_size) ? 2
                                                                                                                         : 1;
    ResizeDirection resize_direction = direction_for_hot_area[hot_area_row][hot_area_column];

    // Double click latches a window's edge to the screen's edge
    if (event.type() == Event::MouseDoubleClick) {
        latch_window_to_screen_edge(resize_direction);
        return;
    }

    if (event.type() == Event::MouseMove && event.buttons() == 0) {
        wm.set_resize_candidate(m_window, resize_direction);
        Compositor::the().invalidate_cursor();
        return;
    }

    if (event.type() == Event::MouseDown && event.button() == MouseButton::Primary)
        wm.start_window_resize(m_window, event.translated(rect().location()), resize_direction);
}

void WindowFrame::handle_menubar_mouse_event(MouseEvent const& event)
{
    Menu* hovered_menu = nullptr;
    auto menubar_rect = this->menubar_rect();
    auto adjusted_position = event.position().translated(-menubar_rect.location());
    m_window.menubar().for_each_menu([&](Menu& menu) {
        if (menu.rect_in_window_menubar().contains(adjusted_position)) {
            hovered_menu = &menu;
            handle_menu_mouse_event(menu, event);
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    if (!hovered_menu && event.type() == Event::Type::MouseDown)
        MenuManager::the().close_everyone();
    if (hovered_menu != MenuManager::the().hovered_menu()) {
        MenuManager::the().set_hovered_menu(hovered_menu);
        invalidate(menubar_rect);
    }
}

void WindowFrame::open_menubar_menu(Menu& menu)
{
    auto menubar_rect = this->menubar_rect();
    MenuManager::the().close_everyone();
    auto position = menu.rect_in_window_menubar().bottom_left().moved_up(1).translated(rect().location()).translated(menubar_rect.location());
    menu.set_unadjusted_position(position);
    auto& window = menu.ensure_menu_window(position);
    auto window_rect = window.rect();
    auto& screen = Screen::closest_to_rect(window_rect);
    auto window_border_thickness = 1;

    // If the menu is off the right edge of the screen align its right edge with the edge of the screen.
    if (window_rect.right() - 1 > screen.width())
        position = position.translated(((window_rect.right() - 1 - screen.width()) * -1) - window_border_thickness, 0);

    // If the menu is below the bottom of the screen move it to appear above the menubar.
    if (window_rect.bottom() - 1 > screen.height())
        position = position.translated(0, (window_rect.height() * -1) - menubar_rect.height());

    window.set_rect(position.x(), position.y(), window_rect.width(), window_rect.height());

    MenuManager::the().open_menu(menu);
    WindowManager::the().set_window_with_active_menu(&m_window);
    invalidate(menubar_rect);
}

void WindowFrame::handle_menu_mouse_event(Menu& menu, MouseEvent const& event)
{
    auto menubar_rect = this->menubar_rect();
    bool is_hover_with_any_menu_open = event.type() == MouseEvent::MouseMove && &m_window == WindowManager::the().window_with_active_menu();
    bool is_mousedown_with_left_button = event.type() == MouseEvent::MouseDown && event.button() == MouseButton::Primary;
    bool should_open_menu = &menu != MenuManager::the().current_menu() && (is_hover_with_any_menu_open || is_mousedown_with_left_button);
    bool should_close_menu = &menu == MenuManager::the().current_menu() && is_mousedown_with_left_button;

    if (should_open_menu) {
        open_menubar_menu(menu);
        return;
    }

    if (should_close_menu) {
        invalidate(menubar_rect);
        MenuManager::the().close_everyone();
    }
}

void WindowFrame::start_flash_animation()
{
    if (!m_flash_timer) {
        m_flash_timer = Core::Timer::create_repeating(100, [this] {
            VERIFY(m_flash_counter);
            invalidate_titlebar();
            if (!--m_flash_counter)
                m_flash_timer->stop();
        });
    }
    m_flash_counter = 8;
    m_flash_timer->start();
}

int WindowFrame::menu_row_count() const
{
    if (!m_window.should_show_menubar())
        return 0;
    return m_window.menubar().has_menus() ? 1 : 0;
}

void WindowFrame::latch_window_to_screen_edge(ResizeDirection resize_direction)
{
    auto window_rect = m_window.rect();
    auto frame_rect = rect();
    auto& screen = Screen::closest_to_rect(window_rect);
    auto screen_rect = WindowManager::the().desktop_rect(screen);

    if (resize_direction == ResizeDirection::UpLeft
        || resize_direction == ResizeDirection::Up
        || resize_direction == ResizeDirection::UpRight)
        window_rect.inflate(frame_rect.top() - screen_rect.top(), 0, 0, 0);

    if (resize_direction == ResizeDirection::UpRight
        || resize_direction == ResizeDirection::Right
        || resize_direction == ResizeDirection::DownRight)
        window_rect.inflate(0, screen_rect.right() - frame_rect.right(), 0, 0);

    if (resize_direction == ResizeDirection::DownLeft
        || resize_direction == ResizeDirection::Down
        || resize_direction == ResizeDirection::DownRight)
        window_rect.inflate(0, 0, screen_rect.bottom() - frame_rect.bottom(), 0);

    if (resize_direction == ResizeDirection::UpLeft
        || resize_direction == ResizeDirection::Left
        || resize_direction == ResizeDirection::DownLeft)
        window_rect.inflate(0, 0, 0, frame_rect.left() - screen_rect.left());

    // If required, maintain fixed aspect ratio by scaling the other dimension appropriately
    if (m_window.resize_aspect_ratio().has_value()) {
        auto& ratio = m_window.resize_aspect_ratio().value();

        if (window_rect.width() == m_window.rect().width()) {
            // Up or Down
            window_rect.set_width(window_rect.height() * ratio.width() / ratio.height());
        } else {
            // Left, Right, UpLeft, UpRight, DownLeft or DownRight
            window_rect.set_height(window_rect.width() * ratio.height() / ratio.width());

            // Match bottom corner of the frame and the screen
            if (resize_direction == ResizeDirection::DownLeft
                || resize_direction == ResizeDirection::DownRight) {
                auto new_frame_rect = frame_rect_for_window(m_window, window_rect);
                window_rect.translate_by(0, screen_rect.bottom() - new_frame_rect.bottom());
            }
        }
    }

    m_window.set_rect(window_rect);
}

}
