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
#include <LibCore/File.h>
#include <LibGUI/AbstractThemePreview.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>

namespace GUI {

AbstractThemePreview::AbstractThemePreview(Gfx::Palette const& preview_palette)
    : m_preview_palette(preview_palette)
{
    m_active_window_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/window.png").release_value_but_fixme_should_propagate_errors();
    m_inactive_window_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/window.png").release_value_but_fixme_should_propagate_errors();

    m_default_close_bitmap = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/window-close.png").release_value_but_fixme_should_propagate_errors();
    m_default_maximize_bitmap = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/upward-triangle.png").release_value_but_fixme_should_propagate_errors();
    m_default_minimize_bitmap = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/downward-triangle.png").release_value_but_fixme_should_propagate_errors();

    VERIFY(m_active_window_icon);
    VERIFY(m_inactive_window_icon);
    VERIFY(m_default_close_bitmap);
    VERIFY(m_default_maximize_bitmap);
    VERIFY(m_default_minimize_bitmap);

    load_theme_bitmaps();
}

void AbstractThemePreview::load_theme_bitmaps()
{
    auto load_bitmap = [](String const& path, String& last_path, RefPtr<Gfx::Bitmap>& bitmap) {
        if (path.is_empty()) {
            last_path = String::empty();
            bitmap = nullptr;
        } else if (last_path != path) {
            auto bitmap_or_error = Gfx::Bitmap::try_load_from_file(path);
            if (bitmap_or_error.is_error()) {
                last_path = String::empty();
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

void AbstractThemePreview::set_preview_palette(Gfx::Palette const& palette)
{
    m_preview_palette = palette;
    palette_changed();
    if (on_palette_change)
        on_palette_change();
    load_theme_bitmaps();
    update();
}

void AbstractThemePreview::set_theme_from_file(Core::File& file)
{
    auto config_file = Core::ConfigFile::open(file.filename(), file.leak_fd()).release_value_but_fixme_should_propagate_errors();
    auto theme = Gfx::load_system_theme(config_file);
    VERIFY(theme.is_valid());

    m_preview_palette = Gfx::Palette(Gfx::PaletteImpl::create_with_anonymous_buffer(theme));
    set_preview_palette(m_preview_palette);
    if (on_theme_load_from_file)
        on_theme_load_from_file(file.filename());
}

void AbstractThemePreview::paint_window(StringView title, Gfx::IntRect const& rect, Gfx::WindowTheme::WindowState state, Gfx::Bitmap const& icon, int button_count)
{
    GUI::Painter painter(*this);

    struct Button {
        Gfx::IntRect rect;
        RefPtr<Gfx::Bitmap> bitmap;
    };

    int window_button_width = m_preview_palette.window_title_button_width();
    int window_button_height = m_preview_palette.window_title_button_height();
    auto titlebar_text_rect = Gfx::WindowTheme::current().titlebar_text_rect(Gfx::WindowTheme::WindowType::Normal, rect, m_preview_palette);
    int pos = titlebar_text_rect.right() + 1;

    Array possible_buttons {
        Button { {}, m_close_bitmap.is_null() ? m_default_close_bitmap : m_close_bitmap },
        Button { {}, m_maximize_bitmap.is_null() ? m_default_maximize_bitmap : m_maximize_bitmap },
        Button { {}, m_minimize_bitmap.is_null() ? m_default_minimize_bitmap : m_minimize_bitmap }
    };

    auto buttons = possible_buttons.span().trim(button_count);

    for (auto& button : buttons) {
        pos -= window_button_width;
        Gfx::IntRect button_rect { pos, 0, window_button_width, window_button_height };
        button_rect.center_vertically_within(titlebar_text_rect);
        button.rect = button_rect;
    }

    auto frame_rect = Gfx::WindowTheme::current().frame_rect_for_window(Gfx::WindowTheme::WindowType::Normal, rect, m_preview_palette, 0);

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
    Gfx::WindowTheme::current().paint_normal_frame(painter, state, rect, title, icon, m_preview_palette, buttons.last().rect, 0, false);
    painter.fill_rect(rect.translated(-frame_rect.location()), m_preview_palette.color(Gfx::ColorRole::Background));

    for (auto& button : buttons) {
        if (!m_preview_palette.title_buttons_icon_only())
            Gfx::StylePainter::paint_button(painter, button.rect, m_preview_palette, Gfx::ButtonStyle::Normal, false);
        auto bitmap_rect = button.bitmap->rect().centered_within(button.rect);
        painter.blit(bitmap_rect.location(), *button.bitmap, button.bitmap->rect());
    }
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
        return Gfx::WindowTheme::current().frame_rect_for_window(
            Gfx::WindowTheme::WindowType::Normal, rect, m_preview_palette, 0);
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
