/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Frame.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

REGISTER_WIDGET(GUI, Frame)

namespace GUI {

Frame::Frame()
{
    set_frame_thickness(2);
    set_frame_shape(Gfx::FrameShape::Container);
    set_frame_shadow(Gfx::FrameShadow::Sunken);

    REGISTER_INT_PROPERTY("thickness", frame_thickness, set_frame_thickness);
    REGISTER_ENUM_PROPERTY("shadow", frame_shadow, set_frame_shadow, Gfx::FrameShadow,
        { Gfx::FrameShadow::Plain, "Plain" },
        { Gfx::FrameShadow::Raised, "Raised" },
        { Gfx::FrameShadow::Sunken, "Sunken" });
    REGISTER_ENUM_PROPERTY("shape", frame_shape, set_frame_shape, Gfx::FrameShape,
        { Gfx::FrameShape::NoFrame, "NoFrame" },
        { Gfx::FrameShape::Box, "Box" },
        { Gfx::FrameShape::Container, "Container" },
        { Gfx::FrameShape::Panel, "Panel" },
        { Gfx::FrameShape::Window, "Window" });
}

Frame::~Frame()
{
}

void Frame::set_frame_thickness(int thickness)
{
    if (m_thickness == thickness)
        return;
    m_thickness = thickness;
    set_grabbable_margins(thickness);
}

void Frame::paint_event(PaintEvent& event)
{
    if (m_shape == Gfx::FrameShape::NoFrame)
        return;

    Painter painter(*this);
    painter.add_clip_rect(event.rect());
    Gfx::StylePainter::paint_frame(painter, rect(), palette(), m_shape, m_shadow, m_thickness, spans_entire_window_horizontally());
}

Gfx::IntRect Frame::children_clip_rect() const
{
    return frame_inner_rect();
}

}
