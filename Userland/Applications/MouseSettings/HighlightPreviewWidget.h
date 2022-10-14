/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Timer.h>
#include <LibGUI/AbstractThemePreview.h>
#include <LibGfx/Color.h>
#include <LibGfx/CursorParams.h>

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

    void set_color(Gfx::Color color)
    {
        m_color = color;
        update();
    }

private:
    explicit HighlightPreviewWidget(Gfx::Palette const& palette);

    ErrorOr<void> reload_cursor();

    RefPtr<Gfx::Bitmap> m_cursor_bitmap;
    Gfx::CursorParams m_cursor_params;
    RefPtr<Core::Timer> m_frame_timer;

    int m_cursor_frame { 0 };
    int m_radius { 0 };
    Gfx::Color m_color;
};

}
