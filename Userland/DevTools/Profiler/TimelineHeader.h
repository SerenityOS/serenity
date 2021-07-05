/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>

namespace Profiler {

class Profile;
struct Process;

class TimelineHeader final : public GUI::Frame {
    C_OBJECT(TimelineHeader);

public:
    virtual ~TimelineHeader();

    Function<void(bool)> on_selection_change;

    void update_selection();

private:
    TimelineHeader(Profile& profile, Process const&);

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;

    Profile& m_profile;
    Process const& m_process;
    RefPtr<Gfx::Bitmap> m_icon;
    String m_text;
    bool m_selected;
};

}
