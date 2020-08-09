/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/MappedFile.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageDecoder.h>

namespace GUI {

ImageWidget::ImageWidget(const StringView&)
    : m_timer(Core::Timer::construct())

{
    set_frame_thickness(0);
    set_frame_shadow(Gfx::FrameShadow::Plain);
    set_frame_shape(Gfx::FrameShape::NoFrame);
    set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    set_auto_resize(true);
}

ImageWidget::~ImageWidget()
{
}

void ImageWidget::set_bitmap(const Gfx::Bitmap* bitmap)
{
    if (m_bitmap == bitmap)
        return;

    m_bitmap = bitmap;
    if (m_bitmap && m_auto_resize)
        set_preferred_size(m_bitmap->width(), m_bitmap->height());

    update();
}

void ImageWidget::set_auto_resize(bool value)
{
    m_auto_resize = value;

    if (m_bitmap)
        set_preferred_size(m_bitmap->width(), m_bitmap->height());
}

void ImageWidget::animate()
{
    m_current_frame_index = (m_current_frame_index + 1) % m_image_decoder->frame_count();

    const auto& current_frame = m_image_decoder->frame(m_current_frame_index);
    set_bitmap(current_frame.image);

    if (current_frame.duration != m_timer->interval()) {
        m_timer->restart(current_frame.duration);
    }

    if (m_current_frame_index == m_image_decoder->frame_count() - 1) {
        ++m_loops_completed;
        if (m_loops_completed > 0 && m_loops_completed == m_image_decoder->loop_count()) {
            m_timer->stop();
        }
    }
}

void ImageWidget::load_from_file(const StringView& path)
{
    MappedFile mapped_file(path);
    if (!mapped_file.is_valid())
        return;

    m_image_decoder = Gfx::ImageDecoder::create((const u8*)mapped_file.data(), mapped_file.size());
    auto bitmap = m_image_decoder->bitmap();
    ASSERT(bitmap);

    set_bitmap(bitmap);

    if (path.ends_with(".gif")) {
        if (m_image_decoder->is_animated() && m_image_decoder->frame_count() > 1) {
            const auto& first_frame = m_image_decoder->frame(0);
            m_timer->set_interval(first_frame.duration);
            m_timer->on_timeout = [this] { animate(); };
            m_timer->start();
        }
    }
}

void ImageWidget::mousedown_event(GUI::MouseEvent&)
{
    if (on_click)
        on_click();
}

void ImageWidget::paint_event(PaintEvent& event)
{
    Frame::paint_event(event);

    if (!m_bitmap) {
        return;
    }

    Painter painter(*this);

    if (m_should_stretch) {
        painter.draw_scaled_bitmap(frame_inner_rect(), *m_bitmap, m_bitmap->rect());
    } else {
        auto location = frame_inner_rect().center().translated(-(m_bitmap->width() / 2), -(m_bitmap->height() / 2));
        painter.blit(location, *m_bitmap, m_bitmap->rect());
    }
}
}
