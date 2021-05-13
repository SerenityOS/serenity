/*
 * Copyright (c) 2021 Tobias Christiansen <tobi@tobyase.de>
 * 
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLLegendElement.h>
#include <LibWeb/Layout/FieldSet.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Painting/BorderPainting.h>

namespace Web::Layout {

FieldSet::FieldSet(DOM::Document& document, HTML::HTMLFieldSetElement* element, NonnullRefPtr<CSS::StyleProperties> style)
    : BlockBox(document, element, move(style))
{
}

FieldSet::~FieldSet()
{
}

void FieldSet::layout_legend()
{
    auto legend = first_child_of_type_including_subtree<Legend>();

    if (!legend)
        return;

    Gfx::FloatPoint fieldset_position = { bordered_rect().x(), bordered_rect().y() };

    Gfx::FloatPoint legend_offset_from_fieldset = { border_length_left_of_legend(), -(line_height() / 2) };
    legend->set_offset(fieldset_position + legend_offset_from_fieldset);

    m_legend = legend;
}

void FieldSet::paint_border(PaintContext& context)
{
    auto bordered_rect = this->bordered_rect();
    Painting::paint_border(context, Painting::BorderEdge::Left, bordered_rect, computed_values());
    Painting::paint_border(context, Painting::BorderEdge::Right, bordered_rect, computed_values());
    Painting::paint_border(context, Painting::BorderEdge::Bottom, bordered_rect, computed_values());

    if (!m_legend) {
        Painting::paint_border(context, Painting::BorderEdge::Top, bordered_rect, computed_values());
        return;
    }

    auto legend_width = m_legend->width();
    auto padding_and_border_width_overhead_left = box_model().border.left + box_model().padding.left;
    auto padding_and_border_width_overhead_right = box_model().border.right + box_model().padding.right;

    // Painting the top border:
    // ---------------- Legend            ---------------
    //  segment_before  space for legend   segment_after

    auto segment_before_rect = bordered_rect;
    segment_before_rect.set_width(max(border_length_left_of_legend() - padding_and_border_width_overhead_left - padding_and_border_width_overhead_right, 0.0f));

    auto segment_after_rect = bordered_rect;
    segment_after_rect.set_width(width() - legend_width - border_length_left_of_legend() - padding_and_border_width_overhead_left - padding_and_border_width_overhead_right);
    segment_after_rect.set_x(absolute_rect().x() + border_length_left_of_legend() + padding_and_border_width_overhead_right + 2 * padding_and_border_width_overhead_left + legend_width);

    Painting::paint_border(context, Painting::BorderEdge::Top, segment_before_rect, computed_values());
    Painting::paint_border(context, Painting::BorderEdge::Top, segment_after_rect, computed_values());
}

}
