/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>

namespace Profiler {

class Process;
class Profile;
class TimelineView;

class TimelineTrack final : public GUI::Frame {
    C_OBJECT(TimelineTrack);

public:
    virtual ~TimelineTrack() override;

private:
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;

    explicit TimelineTrack(TimelineView&, Profile&, Process const&);

    u64 timestamp_at_x(int x) const;

    TimelineView& m_view;
    Profile& m_profile;
    Process const& m_process;
};

}
