/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TimelineView.h"
#include "Profile.h"
#include "TimelineTrack.h"
#include <LibGUI/BoxLayout.h>

namespace Profiler {

TimelineView::TimelineView(Profile& profile)
    : m_profile(profile)
{
    set_layout<GUI::VerticalBoxLayout>();
    set_shrink_to_fit(true);
}

u64 TimelineView::timestamp_at_x(int x) const
{
    float column_width = (float)width() / (float)m_profile.length_in_ms();
    float ms_into_profile = (float)x / column_width;
    return m_profile.first_timestamp() + (u64)ms_into_profile;
}

void TimelineView::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Primary)
        return;

    set_selecting(true);
    set_select_start_time(timestamp_at_x(event.x()));
    set_select_end_time(select_start_time());
    m_profile.set_timestamp_filter_range(select_start_time(), select_end_time());
    update();
}

void TimelineView::mousemove_event(GUI::MouseEvent& event)
{
    set_hover_time(timestamp_at_x(event.x()));

    if (is_selecting()) {
        set_select_end_time(hover_time());
        m_profile.set_timestamp_filter_range(select_start_time(), select_end_time());
    }

    update();
}

void TimelineView::mouseup_event(GUI::MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Primary)
        return;

    set_selecting(false);
    if (select_start_time() == select_end_time())
        m_profile.clear_timestamp_filter_range();
}

void TimelineView::mousewheel_event(GUI::MouseEvent& event)
{
    if (event.modifiers() == Mod_Ctrl) {
        event.accept();
        m_scale += event.wheel_delta_y();
        m_scale = clamp(m_scale, 1.0f, 100.0f);
        for_each_child_of_type<TimelineTrack>([&](auto& track) {
            track.set_scale(m_scale);
            return IterationDecision::Continue;
        });
        on_scale_change();
    }
}

}
