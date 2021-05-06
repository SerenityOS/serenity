/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace Profiler {

class ProfileTimelineWidget;

class TimelineView final : public GUI::Widget {
    C_OBJECT(TimelineView);

public:
    virtual ~TimelineView() override;

    bool is_selecting() const { return m_selecting; }
    u64 select_start_time() const { return m_select_start_time; }
    u64 select_end_time() const { return m_select_end_time; }
    u64 hover_time() const { return m_hover_time; }

    void set_selecting(Badge<ProfileTimelineWidget>, bool value)
    {
        m_selecting = value;
        update();
    }
    void set_select_start_time(Badge<ProfileTimelineWidget>, u64 value)
    {
        m_select_start_time = value;
        update();
    }
    void set_select_end_time(Badge<ProfileTimelineWidget>, u64 value)
    {
        m_select_end_time = value;
        update();
    }
    void set_hover_time(Badge<ProfileTimelineWidget>, u64 value)
    {
        m_hover_time = value;
        update();
    }

private:
    TimelineView();

    bool m_selecting { false };
    u64 m_select_start_time { 0 };
    u64 m_select_end_time { 0 };
    u64 m_hover_time { 0 };
};

}
