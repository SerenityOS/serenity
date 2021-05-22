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

    void set_scale(float);

private:
    virtual void event(Core::Event&) override;
    virtual void paint_event(GUI::PaintEvent&) override;

    explicit TimelineTrack(TimelineView const&, Profile const&, Process const&);

    TimelineView const& m_view;
    Profile const& m_profile;
    Process const& m_process;
};

}
