/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Antonio Di Stefano <tonio9681@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Palette.h>
#include <LibGfx/WindowTheme.h>

namespace GUI {

class AbstractThemePreview : public GUI::Frame {
    C_OBJECT_ABSTRACT(AbstractThemePreview);

public:
    virtual ~AbstractThemePreview() override = default;

    Gfx::Palette& preview_palette() { return m_preview_palette; }
    void set_preview_palette(Gfx::Palette&);
    ErrorOr<void> set_theme_from_file(StringView path, NonnullOwnPtr<Core::File>);
    void set_theme(Core::AnonymousBuffer const&);

    void paint_window(StringView title, Gfx::IntRect const& rect, Gfx::WindowTheme::WindowState, Gfx::Bitmap const& icon, int button_count = 3);

    Function<void()> on_palette_change;

    struct Window {
        Gfx::IntRect& rect;
    };
    void center_window_group_within(Span<Window> windows, Gfx::IntRect const& bounds);

protected:
    explicit AbstractThemePreview(Gfx::Palette const&);

    inline Gfx::Bitmap const& active_window_icon() const
    {
        VERIFY(m_active_window_icon);
        return *m_active_window_icon;
    }
    inline Gfx::Bitmap const& inactive_window_icon() const
    {
        VERIFY(m_inactive_window_icon);
        return *m_inactive_window_icon;
    }

    virtual void palette_changed() {};

private:
    virtual void paint_preview(GUI::PaintEvent&) = 0;

    void load_theme_bitmaps();

    virtual void paint_event(GUI::PaintEvent&) override;

    Gfx::Palette m_preview_palette;

    RefPtr<Gfx::Bitmap> m_active_window_icon;
    RefPtr<Gfx::Bitmap> m_inactive_window_icon;

    RefPtr<Gfx::Bitmap> m_default_close_bitmap;
    RefPtr<Gfx::Bitmap> m_default_maximize_bitmap;
    RefPtr<Gfx::Bitmap> m_default_minimize_bitmap;
    RefPtr<Gfx::Bitmap> m_close_bitmap;
    RefPtr<Gfx::Bitmap> m_maximize_bitmap;
    RefPtr<Gfx::Bitmap> m_minimize_bitmap;
    ByteString m_last_close_path;
    ByteString m_last_maximize_path;
    ByteString m_last_minimize_path;

    RefPtr<Gfx::Bitmap> m_active_window_shadow;
    RefPtr<Gfx::Bitmap> m_inactive_window_shadow;
    RefPtr<Gfx::Bitmap> m_menu_shadow;
    RefPtr<Gfx::Bitmap> m_taskbar_shadow;
    RefPtr<Gfx::Bitmap> m_tooltip_shadow;
    ByteString m_last_active_window_shadow_path;
    ByteString m_last_inactive_window_shadow_path;
    ByteString m_last_menu_shadow_path;
    ByteString m_last_taskbar_shadow_path;
    ByteString m_last_tooltip_shadow_path;
};

}
