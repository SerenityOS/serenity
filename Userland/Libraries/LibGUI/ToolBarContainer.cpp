/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <LibGUI/BoxLayout.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ToolBarContainer.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

REGISTER_WIDGET(GUI, ToolBarContainer)

namespace GUI {

ToolBarContainer::ToolBarContainer(Gfx::Orientation orientation)
    : m_orientation(orientation)
{
    set_fill_with_background_color(true);

    set_frame_thickness(2);
    set_frame_shape(Gfx::FrameShape::Box);
    set_frame_shadow(Gfx::FrameShadow::Sunken);

    auto& layout = set_layout<VerticalBoxLayout>();
    layout.set_spacing(2);
    layout.set_margins({ 2, 2, 2, 2 });

    set_shrink_to_fit(true);
}

void ToolBarContainer::paint_event(GUI::PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    for_each_child_widget([&](auto& widget) {
        if (widget.is_visible()) {
            auto rect = widget.relative_rect();
            painter.draw_line(rect.top_left().translated(0, -1), rect.top_right().translated(0, -1), palette().threed_highlight());
            painter.draw_line(rect.bottom_left().translated(0, 1), rect.bottom_right().translated(0, 1), palette().threed_shadow1());
        }
        return IterationDecision::Continue;
    });

    Frame::paint_event(event);
}

}
