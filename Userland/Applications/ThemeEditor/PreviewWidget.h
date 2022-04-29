/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Antonio Di Stefano <tonio9681@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/AbstractThemePreview.h>
#include <LibGUI/Frame.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Filters/ColorBlindnessFilter.h>
#include <LibGfx/Palette.h>

namespace ThemeEditor {

class MiniWidgetGallery;

class PreviewWidget final : public GUI::AbstractThemePreview {
    C_OBJECT(PreviewWidget);

public:
    virtual ~PreviewWidget() override = default;

    void set_color_filter(OwnPtr<Gfx::ColorBlindnessFilter>);

private:
    explicit PreviewWidget(Gfx::Palette const&);

    virtual void paint_preview(GUI::PaintEvent&) override;
    virtual void second_paint_event(GUI::PaintEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void drop_event(GUI::DropEvent&) override;
    virtual void palette_changed() override;

    void paint_hightlight_window();
    void update_preview_window_locations();

    Gfx::IntRect m_active_window_rect;
    Gfx::IntRect m_inactive_window_rect;
    Gfx::IntRect m_highlight_window_rect;

    OwnPtr<Gfx::ColorBlindnessFilter> m_color_filter = nullptr;
    RefPtr<MiniWidgetGallery> m_gallery;
};

}
