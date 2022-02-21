/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibWeb/Layout/SVGFormattingContext.h>
#include <LibWeb/Layout/SVGGeometryBox.h>

namespace Web::Layout {

SVGFormattingContext::SVGFormattingContext(FormattingState& state, Box const& box, FormattingContext* parent)
    : FormattingContext(Type::SVG, state, box, parent)
{
}

SVGFormattingContext::~SVGFormattingContext()
{
}

void SVGFormattingContext::run(Box const& box, LayoutMode)
{
    box.for_each_in_subtree_of_type<SVGBox>([&](auto const& descendant) {
        if (is<SVGGeometryBox>(descendant)) {
            auto const& geometry_box = static_cast<SVGGeometryBox const&>(descendant);
            auto& path = const_cast<SVGGeometryBox&>(geometry_box).dom_node().get_path();
            auto bounding_box = path.bounding_box();

            // Stroke increases the path's size by stroke_width/2 per side.
            auto stroke_width = geometry_box.dom_node().stroke_width().value_or(0);
            bounding_box.inflate(stroke_width, stroke_width);

            auto& geometry_box_state = m_state.get_mutable(geometry_box);
            geometry_box_state.offset = bounding_box.top_left();
            geometry_box_state.content_width = bounding_box.width();
            geometry_box_state.content_height = bounding_box.height();
        }

        return IterationDecision::Continue;
    });
}

}
