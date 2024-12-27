/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Antonio Di Stefano <tonio9681@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/LexicalPath.h>
#include <AK/StringView.h>
#include <LibCore/ConfigFile.h>
#include <LibGUI/AbstractThemePreview.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>

namespace GUI {

AbstractThemePreview::AbstractThemePreview(Gfx::Palette const& preview_palette)
    : m_preview_palette(preview_palette)
{
    m_active_window_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/window.png"sv).release_value_but_fixme_should_propagate_errors();
    m_inactive_window_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/window.png"sv).release_value_but_fixme_should_propagate_errors();

    m_default_close_bitmap = Gfx::Bitmap::load_from_file("/res/icons/16x16/window-close.png"sv).release_value_but_fixme_should_propagate_errors();
    m_default_maximize_bitmap = Gfx::Bitmap::load_from_file("/res/icons/16x16/upward-triangle.png"sv).release_value_but_fixme_should_propagate_errors();
    m_default_minimize_bitmap = Gfx::Bitmap::load_from_file("/res/icons/16x16/downward-triangle.png"sv).release_value_but_fixme_should_propagate_errors();

    VERIFY(m_active_window_icon);
    VERIFY(m_inactive_window_icon);
    VERIFY(m_default_close_bitmap);
    VERIFY(m_default_maximize_bitmap);
    VERIFY(m_default_minimize_bitmap);

    load_theme_bitmaps();
}

void AbstractThemePreview::load_theme_bitmaps()
{
    auto load_bitmap = [](ByteString const& path, ByteString& last_path, RefPtr<Gfx::Bitmap>& bitmap) {
        if (path.is_empty()) {
            last_path = ByteString::empty();
            bitmap = nullptr;
        } else if (last_path != path) {
            auto bitmap_or_error = Gfx::Bitmap::load_from_file(path);
            if (bitmap_or_error.is_error()) {
                last_path = ByteString::empty();
                bitmap = nullptr;
            } else {
                last_path = path;
                bitmap = bitmap_or_error.release_value();
            }
        }
    };

    auto buttons_path = m_preview_palette.title_button_icons_path();

    load_bitmap(LexicalPath::absolute_path(buttons_path, "window-close.png"), m_last_close_path, m_close_bitmap);
    load_bitmap(LexicalPath::absolute_path(buttons_path, "window-maximize.png"), m_last_maximize_path, m_maximize_bitmap);
    load_bitmap(LexicalPath::absolute_path(buttons_path, "window-minimize.png"), m_last_minimize_path, m_minimize_bitmap);

    load_bitmap(m_preview_palette.active_window_shadow_path(), m_last_active_window_shadow_path, m_active_window_shadow);
    load_bitmap(m_preview_palette.inactive_window_shadow_path(), m_last_inactive_window_shadow_path, m_inactive_window_shadow);
    load_bitmap(m_preview_palette.menu_shadow_path(), m_last_menu_shadow_path, m_menu_shadow);
    load_bitmap(m_preview_palette.taskbar_shadow_path(), m_last_taskbar_shadow_path, m_taskbar_shadow);
    load_bitmap(m_preview_palette.tooltip_shadow_path(), m_last_tooltip_shadow_path, m_tooltip_shadow);
}

void AbstractThemePreview::set_preview_palette(Gfx::Palette& palette)
{
    m_preview_palette = palette;
    palette_changed();
    if (on_palette_change)
        on_palette_change();
    load_theme_bitmaps();
    update();
}

void AbstractThemePreview::set_theme(Core::AnonymousBuffer const& theme_buffer)
{
    VERIFY(theme_buffer.is_valid());
    m_preview_palette = Gfx::Palette(Gfx::PaletteImpl::create_with_anonymous_buffer(theme_buffer));
    set_preview_palette(m_preview_palette);
}

ErrorOr<void> AbstractThemePreview::set_theme_from_file(StringView path, NonnullOwnPtr<Core::File> file)
{
    auto config_file = TRY(Core::ConfigFile::open(path, move(file)));
    auto theme = TRY(Gfx::load_system_theme(config_file));

    m_preview_palette = Gfx::Palette(Gfx::PaletteImpl::create_with_anonymous_buffer(theme));
    set_preview_palette(m_preview_palette);
    return {};
}

void AbstractThemePreview::paint_window(StringView title, Gfx::IntRect const& rect, Gfx::WindowTheme::WindowState state, Gfx::Bitmap const& icon, int button_count)
{
    GUI::Painter gui_painter(*this);

    auto window_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, this->rect().size()).release_value();
    GUI::Painter painter(window_bitmap);
    painter.translate(-this->rect().location());

    struct Button {
        Gfx::IntRect rect;
        RefPtr<Gfx::Bitmap> bitmap;
    };

    Array possible_buttons {
        Button { {}, m_close_bitmap.is_null() ? m_default_close_bitmap : m_close_bitmap },
        Button { {}, m_maximize_bitmap.is_null() ? m_default_maximize_bitmap : m_maximize_bitmap },
        Button { {}, m_minimize_bitmap.is_null() ? m_default_minimize_bitmap : m_minimize_bitmap }
    };

    auto buttons = possible_buttons.span().trim(button_count);

    int button_idx = 0;
    for (auto rect : m_preview_palette.window_theme().layout_buttons(Gfx::WindowTheme::WindowType::Normal, Gfx::WindowTheme::WindowMode::Other, rect, m_preview_palette, buttons.size(), false))
        buttons[button_idx++].rect = rect;

    auto frame_rect = m_preview_palette.window_theme().frame_rect_for_window(Gfx::WindowTheme::WindowType::Normal, Gfx::WindowTheme::WindowMode::Other, rect, m_preview_palette, 0);

    auto paint_shadow = [](Gfx::Painter& painter, Gfx::IntRect& frame_rect, Gfx::Bitmap const& shadow_bitmap) {
        auto total_shadow_size = shadow_bitmap.height();
        auto shadow_rect = frame_rect.inflated(total_shadow_size, total_shadow_size);
        Gfx::StylePainter::paint_simple_rect_shadow(painter, shadow_rect, shadow_bitmap);
    };

    if ((state == Gfx::WindowTheme::WindowState::Active || state == Gfx::WindowTheme::WindowState::Highlighted) && m_active_window_shadow) {
        paint_shadow(painter, frame_rect, *m_active_window_shadow);
    } else if (state == Gfx::WindowTheme::WindowState::Inactive && m_inactive_window_shadow) {
        paint_shadow(painter, frame_rect, *m_inactive_window_shadow);
    }

    Gfx::PainterStateSaver saver(painter);
    painter.translate(frame_rect.location());
    m_preview_palette.window_theme().paint_normal_frame(painter, state, Gfx::WindowTheme::WindowMode::Other, rect, title, icon, m_preview_palette, buttons.last().rect, 0, false);
    painter.fill_rect(rect.translated(-frame_rect.location()), m_preview_palette.color(Gfx::ColorRole::Background));

    auto inactive_button_opacity = m_preview_palette.window_title_button_inactive_alpha() / 255.0f;
    for (auto& button : buttons) {
        if (!m_preview_palette.title_buttons_icon_only())
            Gfx::StylePainter::paint_button(painter, button.rect, m_preview_palette, Gfx::ButtonStyle::Normal, false);
        auto bitmap_rect = button.bitmap->rect().centered_within(button.rect);
        painter.blit(bitmap_rect.location(), *button.bitmap, button.bitmap->rect(), state == Gfx::WindowTheme::WindowState::Inactive ? inactive_button_opacity : 1.0f);
    }

    gui_painter.blit(this->rect().location(), *window_bitmap, window_bitmap->rect());
}

void AbstractThemePreview::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);
    GUI::Painter painter(*this);

    painter.add_clip_rect(event.rect());
    painter.add_clip_rect(frame_inner_rect());

    painter.fill_rect(frame_inner_rect(), m_preview_palette.desktop_background());
    paint_preview(event);
}

void AbstractThemePreview::center_window_group_within(Span<Window> windows, Gfx::IntRect const& bounds)
{
    VERIFY(windows.size() >= 1);

    auto to_frame_rect = [&](Gfx::IntRect const& rect) {
        return m_preview_palette.window_theme().frame_rect_for_window(
            Gfx::WindowTheme::WindowType::Normal, Gfx::WindowTheme::WindowMode::Other, rect, m_preview_palette, 0);
    };

    auto leftmost_x_value = windows[0].rect.x();
    auto topmost_y_value = windows[0].rect.y();
    auto combind_frame_rect = to_frame_rect(windows[0].rect);
    for (auto& window : windows.slice(1)) {
        if (window.rect.x() < leftmost_x_value)
            leftmost_x_value = window.rect.x();
        if (window.rect.y() < topmost_y_value)
            topmost_y_value = window.rect.y();
        combind_frame_rect = combind_frame_rect.united(to_frame_rect(window.rect));
    }

    combind_frame_rect.center_within(bounds);
    auto frame_offset = to_frame_rect({}).location();
    for (auto& window : windows) {
        window.rect.set_left(combind_frame_rect.left() + (window.rect.x() - leftmost_x_value) - frame_offset.x());
        window.rect.set_top(combind_frame_rect.top() + (window.rect.y() - topmost_y_value) - frame_offset.y());
    }
}
}
