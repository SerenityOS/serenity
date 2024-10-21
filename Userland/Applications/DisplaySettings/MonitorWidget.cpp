/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MonitorWidget.h"
#include <LibGUI/Desktop.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibThreading/BackgroundAction.h>

REGISTER_WIDGET(DisplaySettings, MonitorWidget)

namespace DisplaySettings {

ErrorOr<NonnullRefPtr<MonitorWidget>> MonitorWidget::try_create()
{
    auto monitor_widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) MonitorWidget()));
    monitor_widget->m_desktop_resolution = GUI::Desktop::the().rect().size();
    monitor_widget->m_monitor_bitmap = TRY(Gfx::Bitmap::load_from_file("/res/graphics/monitor.png"sv));
    monitor_widget->m_desktop_bitmap = TRY(Gfx::Bitmap::create(monitor_widget->m_monitor_bitmap->format(), { 280, 158 }));
    monitor_widget->m_monitor_rect = { { 12, 13 }, monitor_widget->m_desktop_bitmap->size() };
    monitor_widget->set_fixed_size(304, 201);
    return monitor_widget;
}

bool MonitorWidget::set_wallpaper(String path)
{
    if (!is_different_to_current_wallpaper_path(path))
        return false;

    (void)Threading::BackgroundAction<NonnullRefPtr<Gfx::Bitmap>>::construct(
        [path](auto&) -> ErrorOr<NonnullRefPtr<Gfx::Bitmap>> {
            if (path.is_empty())
                return Error::from_errno(ENOENT);
            return Gfx::Bitmap::load_from_file(path);
        },

        [this, path](NonnullRefPtr<Gfx::Bitmap> bitmap) -> ErrorOr<void> {
            // If we've been requested to change while we were loading the bitmap, don't bother spending the cost to
            // move and render the now stale bitmap.
            if (is_different_to_current_wallpaper_path(path))
                return {};
            m_wallpaper_bitmap = move(bitmap);
            m_desktop_dirty = true;
            update();
            return {};
        },
        [this, path](Error) -> void {
            m_wallpaper_bitmap = nullptr;
        });

    if (path.is_empty())
        m_desktop_wallpaper_path = OptionalNone();
    else
        m_desktop_wallpaper_path = path;

    return true;
}

Optional<StringView> MonitorWidget::wallpaper() const
{
    if (m_desktop_wallpaper_path.has_value())
        return m_desktop_wallpaper_path->bytes_as_string_view();
    return OptionalNone();
}

void MonitorWidget::set_wallpaper_mode(String mode)
{
    if (m_desktop_wallpaper_mode == mode)
        return;
    m_desktop_wallpaper_mode = move(mode);
    m_desktop_dirty = true;
    update();
}

StringView MonitorWidget::wallpaper_mode() const
{
    return m_desktop_wallpaper_mode;
}

void MonitorWidget::set_desktop_resolution(Gfx::IntSize resolution)
{
    if (m_desktop_resolution == resolution)
        return;
    m_desktop_resolution = resolution;
    m_desktop_dirty = true;
    update();
}

Gfx::IntSize MonitorWidget::desktop_resolution()
{
    return m_desktop_resolution;
}

void MonitorWidget::set_background_color(Gfx::Color color)
{
    if (m_desktop_color == color)
        return;
    m_desktop_color = color;
    m_desktop_dirty = true;
    update();
}

Gfx::Color MonitorWidget::background_color()
{
    return m_desktop_color;
}

void MonitorWidget::redraw_desktop_if_needed()
{
    if (!m_desktop_dirty)
        return;

    m_desktop_dirty = false;

    GUI::Painter painter(*m_desktop_bitmap);
    painter.fill_rect(m_desktop_bitmap->rect(), m_desktop_color);

    if (!m_wallpaper_bitmap)
        return;

    float sw = (float)m_desktop_bitmap->width() / (float)m_desktop_resolution.width();
    float sh = (float)m_desktop_bitmap->height() / (float)m_desktop_resolution.height();

    auto scaled_size = m_wallpaper_bitmap->size().to_type<float>().scaled(sw, sh).to_type<int>();
    auto scaled_bitmap_or_error = m_wallpaper_bitmap->scaled(sw, sh);
    if (scaled_bitmap_or_error.is_error()) {
        GUI::MessageBox::show_error(window(), "There was an error updating the desktop preview"sv);
        return;
    }
    auto scaled_bitmap = scaled_bitmap_or_error.release_value();

    if (m_desktop_wallpaper_mode == "Center"sv) {
        auto centered_rect = Gfx::IntRect({}, scaled_size).centered_within(m_desktop_bitmap->rect());
        painter.blit(centered_rect.location(), *scaled_bitmap, scaled_bitmap->rect());
    } else if (m_desktop_wallpaper_mode == "Tile"sv) {
        painter.draw_tiled_bitmap(m_desktop_bitmap->rect(), *scaled_bitmap);
    } else if (m_desktop_wallpaper_mode == "Stretch"sv) {
        painter.draw_scaled_bitmap(m_desktop_bitmap->rect(), *m_wallpaper_bitmap, m_wallpaper_bitmap->rect(), 1.f, Gfx::ScalingMode::BilinearBlend);
    } else {
        VERIFY_NOT_REACHED();
    }
}

void MonitorWidget::paint_event(GUI::PaintEvent& event)
{
    redraw_desktop_if_needed();

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.blit({ 0, 0 }, *m_monitor_bitmap, m_monitor_bitmap->rect());
    painter.blit(m_monitor_rect.location(), *m_desktop_bitmap, m_desktop_bitmap->rect());
}

}
