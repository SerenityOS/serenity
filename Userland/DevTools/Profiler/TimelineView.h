/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace Profiler {

class Profile;
class TimelineTrack;

class TimelineView final : public GUI::Widget {
    C_OBJECT(TimelineView);

public:
    virtual ~TimelineView() override = default;

    Function<void()> on_selection_change;
    Function<void()> on_scale_change;

    float scale() const { return m_scale; }

    bool is_selecting() const { return m_selecting; }
    u64 select_start_time() const { return m_select_start_time; }
    u64 select_end_time() const { return m_select_end_time; }
    u64 hover_time() const { return m_hover_time; }

    void set_selecting(bool value)
    {
        if (m_selecting == value)
            return;
        m_selecting = value;
    }

    void set_select_start_time(u64 value)
    {
        if (m_select_start_time == value)
            return;
        m_select_start_time = value;
        update();
        if (on_selection_change)
            on_selection_change();
    }
    void set_select_end_time(u64 value)
    {
        if (m_select_end_time == value)
            return;
        m_select_end_time = value;
        update();
        if (on_selection_change)
            on_selection_change();
    }
    void set_hover_time(u64 value)
    {
        if (m_hover_time == value)
            return;
        m_hover_time = value;
        update();
        if (on_selection_change)
            on_selection_change();
    }

private:
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousewheel_event(GUI::MouseEvent&) override;

    explicit TimelineView(Profile&);

    u64 timestamp_at_x(int x) const;

    Profile& m_profile;
    bool m_selecting { false };
    u64 m_select_start_time { 0 };
    u64 m_select_end_time { 0 };
    u64 m_hover_time { 0 };
    float m_scale { 10 };
};

}
