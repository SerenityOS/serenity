/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Painter.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(GUI, HorizontalSeparator)
REGISTER_WIDGET(GUI, VerticalSeparator)

constexpr int minimum_size = 8;

namespace GUI {

SeparatorWidget::SeparatorWidget(Gfx::Orientation orientation)
    : m_orientation(orientation)
{
    set_preferred_size(SpecialDimension::Fit);
}

void SeparatorWidget::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    if (m_orientation == Gfx::Orientation::Vertical) {
        painter.translate(rect().center().x() - 1, 0);
        painter.draw_line({ 0, 0 }, { 0, rect().bottom() - 1 }, palette().threed_shadow1());
        painter.draw_line({ 1, 0 }, { 1, rect().bottom() - 1 }, palette().threed_highlight());
    } else {
        painter.translate(0, rect().center().y() - 1);
        painter.draw_line({ 0, 0 }, { rect().right() - 1, 0 }, palette().threed_shadow1());
        painter.draw_line({ 0, 1 }, { rect().right() - 1, 1 }, palette().threed_highlight());
    }
}

Optional<UISize> SeparatorWidget::calculated_preferred_size() const
{
    if (m_orientation == Gfx::Orientation::Vertical)
        return UISize { minimum_size, SpecialDimension::OpportunisticGrow };
    return UISize { SpecialDimension::OpportunisticGrow, minimum_size };
}

Optional<UISize> SeparatorWidget::calculated_min_size() const
{
    if (m_orientation == Gfx::Orientation::Vertical)
        return UISize { minimum_size, 0 };
    return UISize { 0, minimum_size };
}

}
