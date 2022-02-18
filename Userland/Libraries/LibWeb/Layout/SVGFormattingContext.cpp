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

SVGFormattingContext::SVGFormattingContext(Box& box, FormattingContext* parent)
    : FormattingContext(Type::SVG, box, parent)
{
}

SVGFormattingContext::~SVGFormattingContext()
{
}

void SVGFormattingContext::run(Box& box, LayoutMode)
{
    box.for_each_in_subtree_of_type<SVGBox>([&](auto& descendant) {
        if (is<SVGGeometryBox>(descendant)) {
            auto& geometry_box = static_cast<SVGGeometryBox&>(descendant);
            auto& path = geometry_box.dom_node().get_path();
            auto bounding_box = path.bounding_box();

            // Stroke increases the path's size by stroke_width/2 per side.
            auto stroke_width = geometry_box.dom_node().stroke_width().value_or(0);
            bounding_box.inflate(stroke_width, stroke_width);

            geometry_box.set_offset(bounding_box.top_left());
            geometry_box.set_content_size(bounding_box.size());
        }

        return IterationDecision::Continue;
    });
}

}
