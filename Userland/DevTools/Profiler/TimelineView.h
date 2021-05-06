/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace Profiler {

class TimelineTrack;

class TimelineView final : public GUI::Widget {
    C_OBJECT(TimelineView);

public:
    virtual ~TimelineView() override;

    Function<void()> on_selection_change;

    bool is_selecting() const { return m_selecting; }
    u64 select_start_time() const { return m_select_start_time; }
    u64 select_end_time() const { return m_select_end_time; }
    u64 hover_time() const { return m_hover_time; }

    void set_selecting(Badge<TimelineTrack>, bool value)
    {
        if (m_selecting == value)
            return;
        m_selecting = value;
    }

    void set_select_start_time(Badge<TimelineTrack>, u64 value)
    {
        if (m_select_start_time == value)
            return;
        m_select_start_time = value;
        update();
        if (on_selection_change)
            on_selection_change();
    }
    void set_select_end_time(Badge<TimelineTrack>, u64 value)
    {
        if (m_select_end_time == value)
            return;
        m_select_end_time = value;
        update();
        if (on_selection_change)
            on_selection_change();
    }
    void set_hover_time(Badge<TimelineTrack>, u64 value)
    {
        if (m_hover_time == value)
            return;
        m_hover_time = value;
        update();
        if (on_selection_change)
            on_selection_change();
    }

private:
    TimelineView();

    bool m_selecting { false };
    u64 m_select_start_time { 0 };
    u64 m_select_end_time { 0 };
    u64 m_hover_time { 0 };
};

}
