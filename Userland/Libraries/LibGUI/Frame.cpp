/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Frame.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

REGISTER_WIDGET(GUI, Frame)

namespace GUI {

Frame::Frame()
{
    set_frame_style(Gfx::FrameStyle::SunkenContainer);

    REGISTER_ENUM_PROPERTY("frame_style", frame_style, set_frame_style, Gfx::FrameStyle,
        { Gfx::FrameStyle::NoFrame, "NoFrame" },
        { Gfx::FrameStyle::Window, "Window" },
        { Gfx::FrameStyle::Plain, "Plain" },
        { Gfx::FrameStyle::RaisedBox, "RaisedBox" },
        { Gfx::FrameStyle::SunkenBox, "SunkenBox" },
        { Gfx::FrameStyle::RaisedContainer, "RaisedContainer" },
        { Gfx::FrameStyle::SunkenContainer, "SunkenContainer" },
        { Gfx::FrameStyle::RaisedPanel, "RaisedPanel" },
        { Gfx::FrameStyle::SunkenPanel, "SunkenPanel" });
}

void Frame::set_frame_style(Gfx::FrameStyle style)
{
    if (m_style == style)
        return;
    m_style = style;
    set_grabbable_margins(frame_thickness());
    layout_relevant_change_occurred();
}

int Frame::frame_thickness() const
{
    switch (m_style) {
    case Gfx::FrameStyle::NoFrame:
        return 0;
    case Gfx::FrameStyle::Plain:
    case Gfx::FrameStyle::RaisedPanel:
    case Gfx::FrameStyle::SunkenPanel:
        return 1;
    default:
        return 2;
    }
}

void Frame::paint_event(PaintEvent& event)
{
    if (m_style == Gfx::FrameStyle::NoFrame)
        return;

    Painter painter(*this);
    painter.add_clip_rect(event.rect());
    bool skip_vertical_lines = window()->is_maximized() && spans_entire_window_horizontally();
    Gfx::StylePainter::paint_frame(painter, rect(), palette(), m_style, skip_vertical_lines);
}

Gfx::IntRect Frame::children_clip_rect() const
{
    return frame_inner_rect();
}

}
