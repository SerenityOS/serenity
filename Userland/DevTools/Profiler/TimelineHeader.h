/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>

namespace Profiler {

class Process;

class TimelineHeader final : public GUI::Frame {
    C_OBJECT(TimelineHeader);

public:
    virtual ~TimelineHeader();

private:
    TimelineHeader(Process const&);

    virtual void paint_event(GUI::PaintEvent&) override;

    Process const& m_process;
    RefPtr<Gfx::Bitmap> m_icon;
    String m_text;
};

}
