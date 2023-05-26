/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/BoxLayout.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

REGISTER_WIDGET(GUI, ToolbarContainer)

namespace GUI {

ToolbarContainer::ToolbarContainer(Gfx::Orientation orientation)
    : m_orientation(orientation)
{
    set_fill_with_background_color(true);
    set_frame_style(Gfx::FrameStyle::SunkenBox);
    set_layout<VerticalBoxLayout>(GUI::Margins {}, 2);
    set_shrink_to_fit(true);
}

void ToolbarContainer::paint_event(GUI::PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    for_each_child_widget([&](auto& widget) {
        if (widget.is_visible()) {
            auto rect = widget.relative_rect();
            painter.draw_line(rect.top_left().moved_up(1), rect.top_right().translated(-1), palette().threed_highlight());
            painter.draw_line(rect.bottom_left(), rect.bottom_right().moved_left(1), palette().threed_shadow1());
        }
        return IterationDecision::Continue;
    });

    Frame::paint_event(event);
}

}
