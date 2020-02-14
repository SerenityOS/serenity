/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Assertions.h>
#include <AK/StringBuilder.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ProgressBar.h>
#include <LibGfx/Palette.h>

namespace GUI {

ProgressBar::ProgressBar(Widget* parent)
    : Frame(parent)
{
    set_frame_shape(Gfx::FrameShape::Container);
    set_frame_shadow(Gfx::FrameShadow::Sunken);
    set_frame_thickness(2);
}

ProgressBar::~ProgressBar()
{
}

void ProgressBar::set_value(int value)
{
    if (m_value == value)
        return;
    m_value = value;
    update();
}

void ProgressBar::set_range(int min, int max)
{
    ASSERT(min < max);
    m_min = min;
    m_max = max;
    m_value = clamp(m_value, m_min, m_max);
}

void ProgressBar::paint_event(PaintEvent& event)
{
    Frame::paint_event(event);

    Painter painter(*this);
    auto rect = frame_inner_rect();
    painter.add_clip_rect(rect);
    painter.add_clip_rect(event.rect());

    String progress_text;
    if (m_format != Format::NoText) {
        // Then we draw the progress text over the gradient.
        // We draw it twice, once offset (1, 1) for a drop shadow look.
        StringBuilder builder;
        builder.append(m_caption);
        if (m_format == Format::Percentage) {
            float range_size = m_max - m_min;
            float progress = (m_value - m_min) / range_size;
            builder.appendf("%d%%", (int)(progress * 100));
        } else if (m_format == Format::ValueSlashMax) {
            builder.appendf("%d/%d", m_value, m_max);
        }
        progress_text = builder.to_string();
    }

    Gfx::StylePainter::paint_progress_bar(painter, rect, palette(), m_min, m_max, m_value, progress_text);
}

}
