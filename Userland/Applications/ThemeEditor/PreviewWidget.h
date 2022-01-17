/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Antonio Di Stefano <tonio9681@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Filters/ColorBlindnessFilter.h>
#include <LibGfx/Palette.h>

namespace ThemeEditor {

class MiniWidgetGallery;

class PreviewWidget final : public GUI::Frame {
    C_OBJECT(PreviewWidget);

public:
    virtual ~PreviewWidget() override;

    const Gfx::Palette& preview_palette() const { return m_preview_palette; }
    void set_preview_palette(const Gfx::Palette&);
    void set_theme_from_file(Core::File&);

    void set_color_filter(OwnPtr<Gfx::ColorBlindnessFilter>);

    Function<void(String const&)> on_theme_load_from_file;

private:
    explicit PreviewWidget(const Gfx::Palette&);

    void load_theme_bitmaps();

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void second_paint_event(GUI::PaintEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void drop_event(GUI::DropEvent&) override;

    Gfx::Palette m_preview_palette;

    OwnPtr<Gfx::ColorBlindnessFilter> m_color_filter = nullptr;

    RefPtr<Gfx::Bitmap> m_active_window_icon;
    RefPtr<Gfx::Bitmap> m_inactive_window_icon;

    RefPtr<MiniWidgetGallery> m_gallery;

    RefPtr<Gfx::Bitmap> m_default_close_bitmap;
    RefPtr<Gfx::Bitmap> m_default_maximize_bitmap;
    RefPtr<Gfx::Bitmap> m_default_minimize_bitmap;
    RefPtr<Gfx::Bitmap> m_close_bitmap;
    RefPtr<Gfx::Bitmap> m_maximize_bitmap;
    RefPtr<Gfx::Bitmap> m_minimize_bitmap;
    String m_last_close_path;
    String m_last_maximize_path;
    String m_last_minimize_path;

    RefPtr<Gfx::Bitmap> m_active_window_shadow;
    RefPtr<Gfx::Bitmap> m_inactive_window_shadow;
    RefPtr<Gfx::Bitmap> m_menu_shadow;
    RefPtr<Gfx::Bitmap> m_taskbar_shadow;
    RefPtr<Gfx::Bitmap> m_tooltip_shadow;
    String m_last_active_window_shadow_path;
    String m_last_inactive_window_shadow_path;
    String m_last_menu_shadow_path;
    String m_last_taskbar_shadow_path;
    String m_last_tooltip_shadow_path;
};

}
