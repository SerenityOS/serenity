/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TimelineHeader.h"
#include "Process.h"
#include <AK/LexicalPath.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Painter.h>

namespace Profiler {

TimelineHeader::TimelineHeader(Process const& process)
    : m_process(process)
{
    set_frame_shape(Gfx::FrameShape::Panel);
    set_frame_shadow(Gfx::FrameShadow::Raised);
    set_fixed_size(200, 40);

    m_icon = GUI::FileIconProvider::icon_for_executable(m_process.executable).bitmap_for_size(32);
    m_text = String::formatted("{} ({})", LexicalPath(m_process.executable).basename(), m_process.pid);
}

TimelineHeader::~TimelineHeader()
{
}

void TimelineHeader::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    Gfx::IntRect icon_rect { frame_thickness() + 2, 0, 32, 32 };
    icon_rect.center_vertically_within(frame_inner_rect());

    if (m_icon)
        painter.blit(icon_rect.location(), *m_icon, m_icon->rect());

    Gfx::IntRect text_rect {
        icon_rect.right() + 6,
        icon_rect.y(),
        width() - 32,
        32
    };
    text_rect.center_vertically_within(frame_inner_rect());

    painter.draw_text(text_rect, m_text, Gfx::TextAlignment::CenterLeft);
}

}
