/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
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

CSSPixels SVGFormattingContext::automatic_content_height() const
{
    return 0;
}

void SVGFormattingContext::run(Box const& box, LayoutMode, [[maybe_unused]] AvailableSpace const& available_space)
{
    // FIXME: This entire thing is an ad-hoc hack.

    auto& svg_svg_element = verify_cast<SVG::SVGSVGElement>(*box.dom_node());

    auto root_offset = m_state.get(box).offset;

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

            auto& svg_svg_state = m_state.get(static_cast<Box const&>(*svg_svg_element.layout_node()));

            if (svg_svg_state.has_definite_width() && svg_svg_state.has_definite_height()) {
                geometry_box_state.set_content_offset({ 0, 0 });
                geometry_box_state.set_content_width(svg_svg_state.content_width());
                geometry_box_state.set_content_height(svg_svg_state.content_height());
                return IterationDecision::Continue;
            }

            // FIXME: Allow for one of {width, height} to not be specified}
            if (svg_svg_element.has_attribute(HTML::AttributeNames::width)) {
            }

            if (svg_svg_element.has_attribute(HTML::AttributeNames::height)) {
            }

            auto& path = dom_node.get_path();
            auto path_bounding_box = path.bounding_box().to_type<CSSPixels>();

            // Stroke increases the path's size by stroke_width/2 per side.
            CSSPixels stroke_width = geometry_box.dom_node().stroke_width().value_or(0);
            path_bounding_box.inflate(stroke_width, stroke_width);

            auto& maybe_view_box = svg_svg_element.view_box();

            if (maybe_view_box.has_value()) {
                auto view_box = maybe_view_box.value();
                CSSPixelPoint viewbox_offset = { view_box.min_x, view_box.min_y };
                geometry_box_state.set_content_offset(path_bounding_box.top_left() + viewbox_offset);

                geometry_box_state.set_content_width(view_box.width);
                geometry_box_state.set_content_height(view_box.height);

                return IterationDecision::Continue;
            }

            geometry_box_state.set_content_offset(path_bounding_box.top_left());
            geometry_box_state.set_content_width(path_bounding_box.width());
            geometry_box_state.set_content_height(path_bounding_box.height());
        }

        return IterationDecision::Continue;
    });
}

}
