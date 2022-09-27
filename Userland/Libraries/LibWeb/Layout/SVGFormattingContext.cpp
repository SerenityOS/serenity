/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibWeb/Layout/SVGFormattingContext.h>
#include <LibWeb/Layout/SVGGeometryBox.h>
#include <LibWeb/SVG/SVGSVGElement.h>

namespace Web::Layout {

SVGFormattingContext::SVGFormattingContext(LayoutState& state, Box const& box, FormattingContext* parent)
    : FormattingContext(Type::SVG, state, box, parent)
{
}

SVGFormattingContext::~SVGFormattingContext() = default;

float SVGFormattingContext::automatic_content_height() const
{
    return 0;
}

void SVGFormattingContext::run(Box const& box, LayoutMode, [[maybe_unused]] AvailableSpace const& available_space)
{
    auto& svg_svg_element = verify_cast<SVG::SVGSVGElement>(*box.dom_node());

    box.for_each_in_subtree_of_type<SVGBox>([&](SVGBox const& descendant) {
        if (is<SVGGeometryBox>(descendant)) {
            auto const& geometry_box = static_cast<SVGGeometryBox const&>(descendant);

            auto& geometry_box_state = m_state.get_mutable(geometry_box);

            auto& dom_node = const_cast<SVGGeometryBox&>(geometry_box).dom_node();

            if (svg_svg_element.has_attribute(HTML::AttributeNames::width) && svg_svg_element.has_attribute(HTML::AttributeNames::height)) {
                geometry_box_state.set_content_offset({ 0, 0 });
                auto& layout_node = *svg_svg_element.layout_node();

                // FIXME: Allow for relative lengths here
                geometry_box_state.set_content_width(layout_node.computed_values().width().resolved(layout_node, { 0, CSS::Length::Type::Px }).to_px(layout_node));
                geometry_box_state.set_content_height(layout_node.computed_values().height().resolved(layout_node, { 0, CSS::Length::Type::Px }).to_px(layout_node));

                return IterationDecision::Continue;
            }

            // FIXME: Allow for one of {width, height} to not be specified}
            if (svg_svg_element.has_attribute(HTML::AttributeNames::width)) {
            }

            if (svg_svg_element.has_attribute(HTML::AttributeNames::height)) {
            }

            auto& path = dom_node.get_path();
            auto path_bounding_box = path.bounding_box();

            // Stroke increases the path's size by stroke_width/2 per side.
            auto stroke_width = geometry_box.dom_node().stroke_width().value_or(0);
            path_bounding_box.inflate(stroke_width, stroke_width);

            auto& maybe_view_box = svg_svg_element.view_box();

            if (maybe_view_box.has_value()) {
                auto view_box = maybe_view_box.value();
                Gfx::FloatPoint viewbox_offset = { view_box.min_x, view_box.min_y };
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
