/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TimelineTrack.h"
#include "Histogram.h"
#include "Profile.h"
#include "TimelineView.h"
#include <LibGUI/Painter.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>

namespace Profiler {

TimelineTrack::TimelineTrack(TimelineView const& view, Profile const& profile, Process const& process)
    : m_view(view)
    , m_profile(profile)
    , m_process(process)
{
    set_fill_with_background_color(true);
    set_background_role(Gfx::ColorRole::Base);
    set_fixed_height(40);
    set_scale(view.scale());
    set_frame_thickness(1);
}

TimelineTrack::~TimelineTrack()
{
}

void TimelineTrack::set_scale(float scale)
{
    set_fixed_width(m_profile.length_in_ms() / scale);
}

void TimelineTrack::event(Core::Event& event)
{
    switch (event.type()) {
    case GUI::Event::MouseUp:
    case GUI::Event::MouseDown:
    case GUI::Event::MouseMove:
        event.ignore();
    default:
        break;
    }
    GUI::Frame::event(event);
}

void TimelineTrack::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    const u64 start_of_trace = m_profile.first_timestamp();
    const u64 end_of_trace = start_of_trace + m_profile.length_in_ms();

    const auto clamp_timestamp = [start_of_trace, end_of_trace](u64 timestamp) -> u64 {
        return min(end_of_trace, max(timestamp, start_of_trace));
    };

    float column_width = (float)frame_inner_rect().width() / (float)m_profile.length_in_ms();
    size_t columns = frame_inner_rect().width() / column_width;

    Histogram kernel_histogram { start_of_trace, end_of_trace, columns };
    Histogram usermode_histogram { start_of_trace, end_of_trace, columns };
    auto bucket_count = kernel_histogram.size();

    for (auto& event : m_profile.events()) {
        if (event.pid != m_process.pid)
            continue;

        if (!m_process.valid_at(event.serial))
            continue;

        auto& histogram = event.in_kernel ? kernel_histogram : usermode_histogram;
        histogram.insert(clamp_timestamp(event.timestamp), 1 + event.lost_samples);
    }

    decltype(kernel_histogram.at(0)) max_value = 0;
    for (size_t bucket = 0; bucket < bucket_count; bucket++) {
        auto value = kernel_histogram.at(bucket) + usermode_histogram.at(bucket);
        if (value > max_value)
            max_value = value;
    }

    float frame_height = (float)frame_inner_rect().height() / (float)max_value;

    for (size_t bucket = 0; bucket < bucket_count; bucket++) {
        auto kernel_value = kernel_histogram.at(bucket);
        auto usermode_value = usermode_histogram.at(bucket);
        if (kernel_value + usermode_value == 0)
            continue;

        auto t = bucket;

        int x = (int)((float)t * column_width);
        int cw = max(1, (int)column_width);

        int kernel_column_height = frame_inner_rect().height() - (int)((float)kernel_value * frame_height);
        int usermode_column_height = frame_inner_rect().height() - (int)((float)(kernel_value + usermode_value) * frame_height);

        constexpr auto kernel_color = Color::from_rgb(0xc25e5a);
        constexpr auto usermode_color = Color::from_rgb(0x5a65c2);
        painter.fill_rect({ x, frame_thickness() + usermode_column_height, cw, height() - frame_thickness() * 2 }, usermode_color);
        painter.fill_rect({ x, frame_thickness() + kernel_column_height, cw, height() - frame_thickness() * 2 }, kernel_color);
    }

    u64 normalized_start_time = clamp_timestamp(min(m_view.select_start_time(), m_view.select_end_time()));
    u64 normalized_end_time = clamp_timestamp(max(m_view.select_start_time(), m_view.select_end_time()));
    u64 normalized_hover_time = clamp_timestamp(m_view.hover_time());

    int select_start_x = (int)((float)(normalized_start_time - start_of_trace) * column_width);
    int select_end_x = (int)((float)(normalized_end_time - start_of_trace) * column_width);
    int select_hover_x = (int)((float)(normalized_hover_time - start_of_trace) * column_width);
    painter.fill_rect({ select_start_x, frame_thickness(), select_end_x - select_start_x, height() - frame_thickness() * 2 }, Color(0, 0, 0, 60));
    painter.fill_rect({ select_hover_x, frame_thickness(), 1, height() - frame_thickness() * 2 }, Color::NamedColor::Black);
}

}
