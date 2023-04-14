/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/BlockFormattingContext.h>
#include <LibWeb/Layout/SVGFormattingContext.h>
#include <LibWeb/Layout/SVGGeometryBox.h>
#include <LibWeb/SVG/SVGForeignObjectElement.h>
#include <LibWeb/SVG/SVGSVGElement.h>

namespace Web::Layout {

SVGFormattingContext::SVGFormattingContext(LayoutState& state, Box const& box, FormattingContext* parent)
    : FormattingContext(Type::SVG, state, box, parent)
{
}

SVGFormattingContext::~SVGFormattingContext() = default;

CSSPixels SVGFormattingContext::automatic_content_width() const
{
    return 0;
}

CSSPixels SVGFormattingContext::automatic_content_height() const
{
    return 0;
}

void SVGFormattingContext::run(Box const& box, LayoutMode, [[maybe_unused]] AvailableSpace const& available_space)
{
    // FIXME: This entire thing is an ad-hoc hack.

    auto& svg_svg_element = verify_cast<SVG::SVGSVGElement>(*box.dom_node());

    auto svg_box_state = m_state.get(box);
    auto root_offset = svg_box_state.offset;

    box.for_each_child_of_type<BlockContainer>([&](BlockContainer const& child_box) {
        if (is<SVG::SVGForeignObjectElement>(child_box.dom_node())) {
            Layout::BlockFormattingContext bfc(m_state, child_box, this);
            bfc.run(child_box, LayoutMode::Normal, available_space);

            auto& child_state = m_state.get_mutable(child_box);
            child_state.set_content_offset(child_state.offset.translated(root_offset));
        }
        return IterationDecision::Continue;
    });

    box.for_each_in_subtree_of_type<SVGBox>([&](SVGBox const& descendant) {
        if (is<SVGGeometryBox>(descendant)) {
            auto const& geometry_box = static_cast<SVGGeometryBox const&>(descendant);
            auto& geometry_box_state = m_state.get_mutable(geometry_box);
            auto& dom_node = const_cast<SVGGeometryBox&>(geometry_box).dom_node();

            auto& path = dom_node.get_path();
            auto transform = dom_node.get_transform();

            auto& maybe_view_box = svg_svg_element.view_box();
            float viewbox_scale = 1.0f;

            CSSPixelPoint offset {};
            if (maybe_view_box.has_value()) {
                auto view_box = maybe_view_box.value();
                // FIXME: This should allow just one of width or height to be specified.
                // E.g. We should be able to layout <svg width="100%"> where height is unspecified/auto.
                if (!svg_box_state.has_definite_width() || !svg_box_state.has_definite_height()) {
                    dbgln("FIXME: Attempting to layout indefinitely sized SVG with a viewbox -- this likely won't work!");
                }
                auto scale_width = svg_box_state.has_definite_width() ? svg_box_state.content_width().value() / view_box.width : 1;
                auto scale_height = svg_box_state.has_definite_height() ? svg_box_state.content_height().value() / view_box.height : 1;
                viewbox_scale = min(scale_width, scale_height);

                // Center the viewbox within the SVG element:
                if (svg_box_state.has_definite_width())
                    offset.translate_by((svg_box_state.content_width() - (view_box.width * viewbox_scale)) / 2, 0);
                if (svg_box_state.has_definite_height())
                    offset.translate_by(0, (svg_box_state.content_height() - (view_box.height * viewbox_scale)) / 2);

                transform = Gfx::AffineTransform {}.scale(viewbox_scale, viewbox_scale).translate({ -view_box.min_x, -view_box.min_y }).multiply(transform);
            }

            // Stroke increases the path's size by stroke_width/2 per side.
            auto path_bounding_box = transform.map(path.bounding_box()).to_type<CSSPixels>();
            CSSPixels stroke_width = geometry_box.dom_node().stroke_width().value_or(0);
            path_bounding_box.inflate(stroke_width, stroke_width);
            geometry_box_state.set_content_offset(path_bounding_box.top_left() + offset);
            geometry_box_state.set_content_width(path_bounding_box.width());
            geometry_box_state.set_content_height(path_bounding_box.height());
        }

        return IterationDecision::Continue;
    });
}

}
