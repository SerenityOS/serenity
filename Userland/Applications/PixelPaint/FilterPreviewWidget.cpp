/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FilterPreviewWidget.h"
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Rect.h>

REGISTER_WIDGET(PixelPaint, FilterPreviewWidget);

namespace PixelPaint {

FilterPreviewWidget::FilterPreviewWidget()
{
}

FilterPreviewWidget::~FilterPreviewWidget()
{
}

void FilterPreviewWidget::set_bitmap(const RefPtr<Gfx::Bitmap>& bitmap)
{
    m_bitmap = bitmap;
    clear_filter();
}

void FilterPreviewWidget::set_filter(Filter* filter)
{
    if (filter)
        filter->apply(*m_filtered_bitmap, *m_bitmap);
    else
        m_filtered_bitmap = m_bitmap->clone().release_value();
    repaint();
}

void FilterPreviewWidget::clear_filter()
{
    set_filter(nullptr);
}

void FilterPreviewWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    auto preview_rect = event.rect();
    auto bitmap_rect = m_filtered_bitmap->rect();

    int scaled_width, scaled_height, dx = 0, dy = 0;
    if (preview_rect.height() > preview_rect.width()) {
        scaled_width = preview_rect.width();
        scaled_height = ((float)bitmap_rect.height() / bitmap_rect.width()) * scaled_width;
        dy = (preview_rect.height() - scaled_height) / 2;
    } else {
        scaled_height = preview_rect.height();
        scaled_width = ((float)bitmap_rect.width() / bitmap_rect.height()) * scaled_height;
        dx = (preview_rect.width() - scaled_width) / 2;
    }

    Gfx::IntRect scaled_rect(preview_rect.x() + dx, preview_rect.y() + dy, scaled_width, scaled_height);

    painter.draw_scaled_bitmap(scaled_rect, *m_filtered_bitmap, m_filtered_bitmap->rect());
}

}
