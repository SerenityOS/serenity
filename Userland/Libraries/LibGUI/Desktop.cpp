/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <AK/TemporaryChange.h>
#include <LibConfig/Client.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/Desktop.h>
#include <string.h>

namespace GUI {

Desktop& Desktop::the()
{
    static Desktop s_the;
    return s_the;
}

void Desktop::did_receive_screen_rects(Badge<ConnectionToWindowServer>, Vector<Gfx::IntRect, 4> const& rects, size_t main_screen_index, unsigned workspace_rows, unsigned workspace_columns)
{
    m_main_screen_index = main_screen_index;
    m_rects = rects;
    if (!m_rects.is_empty()) {
        m_bounding_rect = m_rects[0];
        for (size_t i = 1; i < m_rects.size(); i++)
            m_bounding_rect = m_bounding_rect.united(m_rects[i]);
    } else {
        m_bounding_rect = {};
    }

    m_workspace_rows = workspace_rows;
    m_workspace_columns = workspace_columns;

    for (auto& callback : m_receive_rects_callbacks)
        callback(*this);
}

void Desktop::set_background_color(StringView background_color)
{
    ConnectionToWindowServer::the().async_set_background_color(background_color);
}

void Desktop::set_wallpaper_mode(StringView mode)
{
    ConnectionToWindowServer::the().async_set_wallpaper_mode(mode);
}

ByteString Desktop::wallpaper_path() const
{
    return Config::read_string("WindowManager"sv, "Background"sv, "Wallpaper"sv);
}

RefPtr<Gfx::Bitmap> Desktop::wallpaper_bitmap() const
{
    return ConnectionToWindowServer::the().get_wallpaper().bitmap();
}

bool Desktop::apply_wallpaper(RefPtr<Gfx::Bitmap const> wallpaper_bitmap, Optional<StringView> path)
{
    if (m_is_setting_desktop_wallpaper)
        return false;

    if (!wallpaper_bitmap && path.has_value() && !path->is_empty()) {
        constexpr auto scale_factor = 1;
        auto maybe_wallpaper_bitmap = Gfx::Bitmap::load_from_file(*path, scale_factor, rect().size());
        if (maybe_wallpaper_bitmap.is_error()) {
            dbgln("Failed to load wallpaper bitmap from path: {}", maybe_wallpaper_bitmap.error());
            return false;
        }
        wallpaper_bitmap = maybe_wallpaper_bitmap.release_value();
    }

    TemporaryChange is_setting_desktop_wallpaper_change(m_is_setting_desktop_wallpaper, true);
    return ConnectionToWindowServer::the().set_wallpaper(wallpaper_bitmap ? wallpaper_bitmap->to_shareable_bitmap() : Gfx::ShareableBitmap {});
}

bool Desktop::set_wallpaper(RefPtr<Gfx::Bitmap const> wallpaper_bitmap, Optional<StringView> path)
{
    bool result = apply_wallpaper(wallpaper_bitmap, path);

    if (result && path.has_value()) {
        dbgln("Saving wallpaper path '{}' to ConfigServer", *path);
        Config::write_string("WindowManager"sv, "Background"sv, "Wallpaper"sv, *path);
    }

    return result;
}

bool Desktop::load_current_wallpaper()
{
    auto current_wallpaper = Config::read_string("WindowManager"sv, "Background"sv, "Wallpaper"sv, ""sv);
    return apply_wallpaper({}, current_wallpaper);
}

}
