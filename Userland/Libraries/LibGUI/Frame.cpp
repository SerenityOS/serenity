/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
        { Gfx::FrameShape::VerticalLine, "VerticalLine" },
        { Gfx::FrameShape::HorizontalLine, "HorizontalLine" });
}

Frame::~Frame()
{
}

void Frame::set_frame_thickness(int thickness)
{
    if (m_thickness == thickness)
        return;
    m_thickness = thickness;
    set_content_margins({ thickness, thickness, thickness, thickness });
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
