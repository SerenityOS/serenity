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

#include "ProfileTimelineWidget.h"
#include "Profile.h"
#include <LibGUI/GPainter.h>

ProfileTimelineWidget::ProfileTimelineWidget(Profile& profile, GWidget* parent)
    : GFrame(parent)
    , m_profile(profile)
{
    set_frame_thickness(2);
    set_frame_shadow(FrameShadow::Sunken);
    set_frame_shape(FrameShape::Container);
    set_background_color(Color::White);
    set_fill_with_background_color(true);
    set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    set_preferred_size(0, 80);
}

ProfileTimelineWidget::~ProfileTimelineWidget()
{
}

void ProfileTimelineWidget::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);

    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    float column_width = (float)frame_inner_rect().width() / (float)m_profile.length_in_ms();
    float frame_height = (float)frame_inner_rect().height() / (float)m_profile.deepest_stack_depth();

    for (auto& sample : m_profile.samples()) {
        u64 t = sample.timestamp - m_profile.first_timestamp();
        int x = (int)((float)t * column_width);
        int cw = max(1, (int)column_width);

        int column_height = frame_inner_rect().height() - (int)((float)sample.frames.size() * frame_height);

        bool in_kernel = sample.in_kernel;
        Color color = in_kernel ? Color::from_rgb(0xc25e5a) : Color::from_rgb(0x5a65c2);
        for (int i = 0; i < cw; ++i)
            painter.draw_line({ x + i, frame_thickness() + column_height }, { x + i, height() - frame_thickness() * 2 }, color);
    }

    u64 normalized_start_time = min(m_select_start_time, m_select_end_time);
    u64 normalized_end_time = max(m_select_start_time, m_select_end_time);

    int select_start_x = (int)((float)(normalized_start_time - m_profile.first_timestamp()) * column_width);
    int select_end_x = (int)((float)(normalized_end_time - m_profile.first_timestamp()) * column_width);
    painter.fill_rect({ select_start_x, frame_thickness(), select_end_x - select_start_x, height() - frame_thickness() * 2 }, Color(0, 0, 0, 60));
}

u64 ProfileTimelineWidget::timestamp_at_x(int x) const
{
    float column_width = (float)frame_inner_rect().width() / (float)m_profile.length_in_ms();
    float ms_into_profile = (float)x / column_width;
    return m_profile.first_timestamp() + (u64)ms_into_profile;
}

void ProfileTimelineWidget::mousedown_event(GMouseEvent& event)
{
    if (event.button() != GMouseButton::Left)
        return;

    m_selecting = true;
    m_select_start_time = timestamp_at_x(event.x());
    m_select_end_time = m_select_start_time;
    m_profile.set_timestamp_filter_range(m_select_start_time, m_select_end_time);
    update();
}

void ProfileTimelineWidget::mousemove_event(GMouseEvent& event)
{
    if (!m_selecting)
        return;

    m_select_end_time = timestamp_at_x(event.x());
    m_profile.set_timestamp_filter_range(m_select_start_time, m_select_end_time);
    update();
}

void ProfileTimelineWidget::mouseup_event(GMouseEvent& event)
{
    if (event.button() != GMouseButton::Left)
        return;

    m_selecting = false;
    if (m_select_start_time == m_select_end_time)
        m_profile.clear_timestamp_filter_range();
}
