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

void FilterPreviewWidget::set_layer(RefPtr<PixelPaint::Layer> layer)
{
    m_layer = layer;
}

void FilterPreviewWidget::set_bitmap(RefPtr<Gfx::Bitmap> const& bitmap)
{
    m_bitmap = bitmap;
    clear_filter();
}

void FilterPreviewWidget::set_filter(Filter* filter)
{
    if (filter) {
        filter->apply(*m_filtered_bitmap, *m_bitmap);

        // If we have a layer set and the image has an active selection we only want the filter to apply to the
        // selected region. This will walk the image and for every pixel that is outside the selection, restore it
        // from the original bitmap.
        if (m_layer && !m_layer->image().selection().is_empty()) {
            for (int y = 0; y < m_filtered_bitmap->height(); ++y) {
                for (int x = 0; x < m_filtered_bitmap->width(); ++x) {
                    if (!m_layer->image().selection().is_selected(m_layer->location().translated(x, y))) {
                        m_filtered_bitmap->set_pixel(x, y, m_bitmap->get_pixel(x, y));
                    }
                }
            }
        }
    } else {
        m_filtered_bitmap = m_bitmap->clone().release_value();
    }
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
