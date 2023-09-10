/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TimelineTrack.h"
#include "Profile.h"
#include "TimelineView.h"
#include <LibGUI/Application.h>
#include <LibGUI/Painter.h>
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
    set_frame_style(Gfx::FrameStyle::SunkenPanel);
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
        break;
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

    u64 const start_of_trace = m_profile.first_timestamp();
    u64 const end_of_trace = start_of_trace + m_profile.length_in_ms();

    auto const clamp_timestamp = [start_of_trace, end_of_trace](u64 timestamp) -> u64 {
        return min(end_of_trace, max(timestamp, start_of_trace));
    };

    recompute_histograms_if_needed({ start_of_trace, end_of_trace, (size_t)m_profile.length_in_ms() });

    float column_width = this->column_width();
    float frame_height = (float)frame_inner_rect().height() / (float)m_max_value;

    for_each_signpost([&](auto& signpost) {
        int x = (int)((float)(signpost.timestamp - start_of_trace) * column_width);
        int y1 = frame_thickness();
        int y2 = height() - frame_thickness() * 2;

        painter.draw_line({ x, y1 }, { x, y2 }, Color::Magenta);

        return IterationDecision::Continue;
    });

    for (size_t bucket = 0; bucket < m_kernel_histogram->size(); bucket++) {
        auto kernel_value = m_kernel_histogram->at(bucket);
        auto user_value = m_user_histogram->at(bucket);
        if (kernel_value + user_value == 0)
            continue;

        auto t = bucket;

        int x = (int)((float)t * column_width);
        int cw = max(1, (int)column_width);

        int kernel_column_height = frame_inner_rect().height() - (int)((float)kernel_value * frame_height);
        int user_column_height = frame_inner_rect().height() - (int)((float)(kernel_value + user_value) * frame_height);

        constexpr auto kernel_color = Color::from_rgb(0xc25e5a);
        constexpr auto user_color = Color::from_rgb(0x5a65c2);
        painter.fill_rect({ x, frame_thickness() + user_column_height, cw, height() - frame_thickness() * 2 }, user_color);
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

template<typename Callback>
void TimelineTrack::for_each_signpost(Callback callback)
{
    m_profile.for_each_signpost([&](auto& signpost) {
        if (signpost.pid != m_process.pid)
            return IterationDecision::Continue;

        if (!m_process.valid_at(signpost.serial))
            return IterationDecision::Continue;

        return callback(signpost);
    });
}

void TimelineTrack::mousemove_event(GUI::MouseEvent& event)
{
    auto column_width = this->column_width();
    bool hovering_a_signpost = false;

    for_each_signpost([&](auto& signpost) {
        int x = (int)((float)(signpost.timestamp - m_profile.first_timestamp()) * column_width);
        constexpr int hoverable_padding = 2;
        Gfx::IntRect hoverable_rect { x - hoverable_padding, frame_thickness(), hoverable_padding * 2, height() - frame_thickness() * 2 };
        if (hoverable_rect.contains_horizontally(event.x())) {
            auto const& data = signpost.data.template get<Profile::Event::SignpostData>();
            GUI::Application::the()->show_tooltip_immediately(MUST(String::formatted("{}, {}", data.string, data.arg)), this);
            hovering_a_signpost = true;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    if (!hovering_a_signpost)
        GUI::Application::the()->hide_tooltip();
}

void TimelineTrack::recompute_histograms_if_needed(HistogramInputs const& inputs)
{
    if (m_cached_histogram_inputs == inputs && m_kernel_histogram.has_value())
        return;

    auto const clamp_timestamp = [&inputs](u64 timestamp) -> u64 {
        return min(inputs.end, max(timestamp, inputs.start));
    };

    m_kernel_histogram = Histogram { inputs.start, inputs.end, inputs.columns };
    m_user_histogram = Histogram { inputs.start, inputs.end, inputs.columns };

    for (auto const& event : m_profile.events()) {
        if (event.pid != m_process.pid)
            continue;

        if (!m_process.valid_at(event.serial))
            continue;

        auto& histogram = event.in_kernel ? *m_kernel_histogram : *m_user_histogram;
        histogram.insert(clamp_timestamp(event.timestamp), 1 + event.lost_samples);
    }

    auto shorter_histogram_size = min(m_kernel_histogram->size(), m_user_histogram->size());
    for (size_t bucket = 0; bucket < shorter_histogram_size; ++bucket) {
        auto value = m_kernel_histogram->at(bucket) + m_user_histogram->at(bucket);
        if (value > m_max_value)
            m_max_value = value;
    }

    auto& longer_histogram = m_kernel_histogram->size() > m_user_histogram->size() ? *m_kernel_histogram : *m_user_histogram;
    for (size_t bucket = shorter_histogram_size; bucket < longer_histogram.size(); ++bucket) {
        auto value = longer_histogram.at(bucket);
        if (value > m_max_value)
            m_max_value = value;
    }
}

float TimelineTrack::column_width() const
{
    return (float)frame_inner_rect().width() / (float)m_profile.length_in_ms();
}

}
