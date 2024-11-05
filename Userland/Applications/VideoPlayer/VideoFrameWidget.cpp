/*
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>

#include "VideoFrameWidget.h"

namespace VideoPlayer {

VideoFrameWidget::VideoFrameWidget()
{
    REGISTER_BOOL_PROPERTY("auto_resize", auto_resize, set_auto_resize);
    set_auto_resize(true);
}

void VideoFrameWidget::set_bitmap(Gfx::Bitmap const* bitmap)
{
    if (m_bitmap == bitmap)
        return;

    m_bitmap = bitmap;
    if (m_bitmap && m_auto_resize)
        set_fixed_size(m_bitmap->size());

    update();
}

void VideoFrameWidget::set_sizing_mode(VideoSizingMode value)
{
    if (value == m_sizing_mode)
        return;
    m_sizing_mode = value;

    update();
}

void VideoFrameWidget::set_auto_resize(bool value)
{
    m_auto_resize = value;

    if (m_bitmap)
        set_fixed_size(m_bitmap->size());
}

void VideoFrameWidget::mousedown_event(GUI::MouseEvent&)
{
    if (on_click)
        on_click();
}

void VideoFrameWidget::doubleclick_event(GUI::MouseEvent&)
{
    if (on_doubleclick)
        on_doubleclick();
}

void VideoFrameWidget::paint_event(GUI::PaintEvent& event)
{
    Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.fill_rect(frame_inner_rect(), Gfx::Color::Black);

    if (!m_bitmap)
        return;

    if (m_sizing_mode == VideoSizingMode::Stretch) {
        painter.draw_scaled_bitmap(frame_inner_rect(), *m_bitmap, m_bitmap->rect(), 1.0f, Gfx::ScalingMode::BilinearBlend);
        return;
    }

    auto center = frame_inner_rect().center();

    if (m_sizing_mode == VideoSizingMode::FullSize) {
        painter.blit(center.translated(-m_bitmap->width() / 2, -m_bitmap->height() / 2), *m_bitmap, m_bitmap->rect());
        return;
    }

    VERIFY(m_sizing_mode < VideoSizingMode::Sentinel);

    auto aspect_ratio = m_bitmap->width() / static_cast<float>(m_bitmap->height());
    auto display_aspect_ratio = frame_inner_rect().width() / static_cast<float>(frame_inner_rect().height());

    Gfx::IntSize display_size;
    if ((display_aspect_ratio > aspect_ratio) == (m_sizing_mode == VideoSizingMode::Fit)) {
        display_size = {
            (frame_inner_rect().height() * m_bitmap->width()) / m_bitmap->height(),
            frame_inner_rect().height(),
        };
    } else {
        display_size = {
            frame_inner_rect().width(),
            (frame_inner_rect().width() * m_bitmap->height()) / m_bitmap->width(),
        };
    }

    auto display_rect = Gfx::IntRect(center.translated(-display_size.width() / 2, -display_size.height() / 2), display_size);
    painter.draw_scaled_bitmap(display_rect, *m_bitmap, m_bitmap->rect(), 1.0f, Gfx::ScalingMode::BilinearBlend);
}

}
