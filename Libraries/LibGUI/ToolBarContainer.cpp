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

namespace GUI {

void ToolBarContainer::child_event(Core::ChildEvent& event)
{
    Frame::child_event(event);

    if (event.type() == Core::Event::ChildAdded) {
        if (event.child() && event.child()->is_widget())
            did_add_toolbar((Widget&)*event.child());
    } else if (event.type() == Core::Event::ChildRemoved) {
        if (event.child() && event.child()->is_widget()) {
            did_remove_toolbar((Widget&)*event.child());
        }
    }
}

void ToolBarContainer::did_remove_toolbar(Widget& toolbar)
{
    m_toolbars.remove_first_matching([&](auto& entry) { return entry.ptr() == &toolbar; });
    recompute_preferred_size();
}

void ToolBarContainer::did_add_toolbar(Widget& toolbar)
{
    m_toolbars.append(toolbar);
    recompute_preferred_size();
}

void ToolBarContainer::custom_layout()
{
    recompute_preferred_size();
}

void ToolBarContainer::recompute_preferred_size()
{
    int visible_toolbar_count = 0;
    int preferred_size = 4;

    for (auto& toolbar : m_toolbars) {
        if (!toolbar.is_visible())
            continue;
        ++visible_toolbar_count;
        if (m_orientation == Gfx::Orientation::Horizontal)
            preferred_size += toolbar.preferred_size().height();
        else
            preferred_size += toolbar.preferred_size().width();
    }

    preferred_size += (visible_toolbar_count - 1) * 2;

    if (m_orientation == Gfx::Orientation::Horizontal)
        set_preferred_size(0, preferred_size);
    else
        set_preferred_size(preferred_size, 0);
}

ToolBarContainer::ToolBarContainer(Gfx::Orientation orientation)
    : m_orientation(orientation)
{
    set_fill_with_background_color(true);

    set_frame_thickness(2);
    set_frame_shape(Gfx::FrameShape::Box);
    set_frame_shadow(Gfx::FrameShadow::Sunken);

    if (m_orientation == Gfx::Orientation::Horizontal)
        set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    else
        set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);

    auto& layout = set_layout<VerticalBoxLayout>();
    layout.set_spacing(2);
    layout.set_margins({ 2, 2, 2, 2 });
}

void ToolBarContainer::paint_event(GUI::PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    for (auto& toolbar : m_toolbars) {
        if (!toolbar.is_visible())
            continue;
        auto rect = toolbar.relative_rect();
        painter.draw_line(rect.top_left().translated(0, -1), rect.top_right().translated(0, -1), palette().threed_highlight());
        painter.draw_line(rect.bottom_left().translated(0, 1), rect.bottom_right().translated(0, 1), palette().threed_shadow1());
    }

    Frame::paint_event(event);
}

}
