/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HighlightPreviewWidget.h"
#include <AK/String.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/Painter.h>
#include <LibGfx/AntiAliasingPainter.h>

namespace MouseSettings {

HighlightPreviewWidget::HighlightPreviewWidget(Gfx::Palette const& palette)
    : GUI::AbstractThemePreview(palette)
{
    auto cursor_theme = GUI::ConnectionToWindowServer::the().get_cursor_theme();
    m_cursor_bitmap = Gfx::Bitmap::try_load_from_file(String::formatted("/res/cursor-themes/{}/arrow.x2y2.png", cursor_theme)).release_value_but_fixme_should_propagate_errors();
}

void HighlightPreviewWidget::paint_preview(GUI::PaintEvent&)
{
    GUI::Painter painter(*this);
    if (m_radius > 0 && m_color.alpha() > 0) {
        Gfx::AntiAliasingPainter aa_painter { painter };
        Gfx::IntRect highlight_rect { 0, 0, m_radius * 2, m_radius * 2 };
        highlight_rect.center_within(frame_inner_rect());
        aa_painter.fill_ellipse(highlight_rect, m_color);
    }
    painter.blit(m_cursor_bitmap->rect().centered_within(frame_inner_rect()).location(), *m_cursor_bitmap, m_cursor_bitmap->rect());
}

}
