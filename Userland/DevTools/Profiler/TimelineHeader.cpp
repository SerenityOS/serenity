/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TimelineHeader.h"
#include "Process.h"
#include "Profile.h"
#include <AK/LexicalPath.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>

namespace Profiler {

TimelineHeader::TimelineHeader(Profile& profile, Process const& process)
    : m_profile(profile)
    , m_process(process)
{
    set_frame_style(Gfx::FrameStyle::RaisedPanel);
    set_fixed_size(200, 40);
    update_selection();

    m_icon = GUI::FileIconProvider::icon_for_executable(m_process.executable).bitmap_for_size(32);
    m_text = ByteString::formatted("{} ({})", LexicalPath::basename(m_process.executable), m_process.pid);
}

void TimelineHeader::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.fill_rect(frame_inner_rect(), m_selected ? palette().selection() : palette().button());

    Gfx::IntRect icon_rect { frame_thickness() + 2, 0, 32, 32 };
    icon_rect.center_vertically_within(frame_inner_rect());

    if (m_icon)
        painter.blit(icon_rect.location(), *m_icon, m_icon->rect());

    Gfx::IntRect text_rect {
        icon_rect.right() + 5,
        icon_rect.y(),
        width() - 32,
        32
    };
    text_rect.center_vertically_within(frame_inner_rect());

    auto const& font = m_selected ? painter.font().bold_variant() : painter.font();
    auto color = m_selected ? palette().selection_text() : palette().button_text();
    painter.draw_text(text_rect, m_text, font, Gfx::TextAlignment::CenterLeft, color);
}

void TimelineHeader::update_selection()
{
    m_selected = m_profile.has_process_filter() && m_profile.process_filter_contains(m_process.pid, m_process.start_valid);
    update();
}

void TimelineHeader::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Primary)
        return;
    m_selected = !m_selected;
    on_selection_change(m_selected);
}

}
