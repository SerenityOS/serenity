/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/AbstractThemePreview.h>
#include <LibGfx/Color.h>

namespace MouseSettings {

class HighlightPreviewWidget final : public GUI::AbstractThemePreview {
    C_OBJECT(HighlightPreviewWidget)
public:
    virtual ~HighlightPreviewWidget() override = default;

    virtual void paint_preview(GUI::PaintEvent&) override;

    void set_radius(int radius)
    {
        m_radius = radius;
        update();
    }

    void set_color(Gfx::Color const& color)
    {
        m_color = color;
        update();
    }

private:
    explicit HighlightPreviewWidget(Gfx::Palette const& palette);

    RefPtr<Gfx::Bitmap> m_cursor_bitmap;

    int m_radius { 0 };
    Gfx::Color m_color;
};

}
