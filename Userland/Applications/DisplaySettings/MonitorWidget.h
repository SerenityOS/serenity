/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <LibGUI/Widget.h>

namespace DisplaySettings {

class MonitorWidget final : public GUI::Widget {
    C_OBJECT_ABSTRACT(MonitorWidget);

public:
    static ErrorOr<NonnullRefPtr<MonitorWidget>> try_create();
    bool set_wallpaper(String path);
    Optional<StringView> wallpaper() const;

    void set_wallpaper_mode(String mode);
    StringView wallpaper_mode() const;

    RefPtr<Gfx::Bitmap> wallpaper_bitmap() const { return m_wallpaper_bitmap; }

    void set_desktop_resolution(Gfx::IntSize resolution);
    Gfx::IntSize desktop_resolution();

    void set_desktop_scale_factor(int scale_factor) { m_desktop_scale_factor = scale_factor; }
    int desktop_scale_factor() const { return m_desktop_scale_factor; }

    void set_background_color(Gfx::Color background_color);
    Gfx::Color background_color();

private:
    MonitorWidget() = default;

    void redraw_desktop_if_needed();

    virtual void paint_event(GUI::PaintEvent& event) override;

    Gfx::IntRect m_monitor_rect;
    RefPtr<Gfx::Bitmap> m_monitor_bitmap;
    RefPtr<Gfx::Bitmap> m_desktop_bitmap;
    bool m_desktop_dirty { true };

    Optional<String> m_desktop_wallpaper_path;
    RefPtr<Gfx::Bitmap> m_wallpaper_bitmap;
    String m_desktop_wallpaper_mode;
    Gfx::IntSize m_desktop_resolution;
    int m_desktop_scale_factor { 1 };
    Gfx::Color m_desktop_color;

    bool is_different_to_current_wallpaper_path(String const& path)
    {
        if (!m_desktop_wallpaper_path.has_value()) {
            if (path.is_empty())
                return false;
            return true;
        }
        if (m_desktop_wallpaper_path.value() == path)
            return false;
        return true;
    }
};

}
