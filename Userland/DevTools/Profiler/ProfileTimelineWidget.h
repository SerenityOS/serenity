/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>

namespace Profiler {

class Profile;

class ProfileTimelineWidget final : public GUI::Frame {
    C_OBJECT(ProfileTimelineWidget)
public:
    virtual ~ProfileTimelineWidget() override;

private:
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;

    explicit ProfileTimelineWidget(Profile&);

    u64 timestamp_at_x(int x) const;

    Profile& m_profile;

    bool m_selecting { false };
    u64 m_select_start_time { 0 };
    u64 m_select_end_time { 0 };
    u64 m_hover_time { 0 };
};

}
