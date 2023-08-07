/*
 * Copyright (c) 2021-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibWeb/Layout/BlockFormattingContext.h>
#include <LibWeb/Layout/SVGFormattingContext.h>
#include <LibWeb/Layout/SVGGeometryBox.h>
#include <LibWeb/Layout/SVGSVGBox.h>
#include <LibWeb/Layout/SVGTextBox.h>
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

struct ViewBoxTransform {
    CSSPixelPoint offset;
    double scale_factor;
};

// https://svgwg.org/svg2-draft/coords.html#PreserveAspectRatioAttribute
static ViewBoxTransform scale_and_align_viewbox_content(SVG::PreserveAspectRatio const& preserve_aspect_ratio,
    SVG::ViewBox const& view_box, Gfx::FloatSize viewbox_scale, auto const& svg_box_state)
{
    ViewBoxTransform viewbox_transform {};

    switch (preserve_aspect_ratio.meet_or_slice) {
    case SVG::PreserveAspectRatio::MeetOrSlice::Meet:
        // meet (the default) - Scale the graphic such that:
        // - aspect ratio is preserved
        // - the entire ‘viewBox’ is visible within the SVG viewport
        // - the ‘viewBox’ is scaled up as much as possible, while still meeting the other criteria
        viewbox_transform.scale_factor = min(viewbox_scale.width(), viewbox_scale.height());
        break;
    case SVG::PreserveAspectRatio::MeetOrSlice::Slice:
        // slice - Scale the graphic such that:
        // aspect ratio is preserved
        // the entire SVG viewport is covered by the ‘viewBox’
        // the ‘viewBox’ is scaled down as much as possible, while still meeting the other criteria
        viewbox_transform.scale_factor = max(viewbox_scale.width(), viewbox_scale.height());
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    // Handle X alignment:
    if (svg_box_state.has_definite_width()) {
        switch (preserve_aspect_ratio.align) {
        case SVG::PreserveAspectRatio::Align::xMinYMin:
        case SVG::PreserveAspectRatio::Align::xMinYMid:
        case SVG::PreserveAspectRatio::Align::xMinYMax:
            // Align the <min-x> of the element's ‘viewBox’ with the smallest X value of the SVG viewport.
            viewbox_transform.offset.translate_by(0, 0);
            break;
        case SVG::PreserveAspectRatio::Align::None: {
            // Do not force uniform scaling. Scale the graphic content of the given element non-uniformly
            // if necessary such that the element's bounding box exactly matches the SVG viewport rectangle.
            // FIXME: None is unimplemented (treat as xMidYMid)
            [[fallthrough]];
        }
        case SVG::PreserveAspectRatio::Align::xMidYMin:
        case SVG::PreserveAspectRatio::Align::xMidYMid:
        case SVG::PreserveAspectRatio::Align::xMidYMax:
            // Align the midpoint X value of the element's ‘viewBox’ with the midpoint X value of the SVG viewport.
            viewbox_transform.offset.translate_by((svg_box_state.content_width() - (view_box.width * viewbox_transform.scale_factor)) / 2, 0);
            break;
        case SVG::PreserveAspectRatio::Align::xMaxYMin:
        case SVG::PreserveAspectRatio::Align::xMaxYMid:
        case SVG::PreserveAspectRatio::Align::xMaxYMax:
            // Align the <min-x>+<width> of the element's ‘viewBox’ with the maximum X value of the SVG viewport.
            viewbox_transform.offset.translate_by((svg_box_state.content_width() - (view_box.width * viewbox_transform.scale_factor)), 0);
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    if (svg_box_state.has_definite_width()) {
        switch (preserve_aspect_ratio.align) {
        case SVG::PreserveAspectRatio::Align::xMinYMin:
        case SVG::PreserveAspectRatio::Align::xMidYMin:
        case SVG::PreserveAspectRatio::Align::xMaxYMin:
            // Align the <min-y> of the element's ‘viewBox’ with the smallest Y value of the SVG viewport.
            viewbox_transform.offset.translate_by(0, 0);
            break;
        case SVG::PreserveAspectRatio::Align::None: {
            // Do not force uniform scaling. Scale the graphic content of the given element non-uniformly
            // if necessary such that the element's bounding box exactly matches the SVG viewport rectangle.
            // FIXME: None is unimplemented (treat as xMidYMid)
            [[fallthrough]];
        }
        case SVG::PreserveAspectRatio::Align::xMinYMid:
        case SVG::PreserveAspectRatio::Align::xMidYMid:
        case SVG::PreserveAspectRatio::Align::xMaxYMid:
            // Align the midpoint Y value of the element's ‘viewBox’ with the midpoint Y value of the SVG viewport.
            viewbox_transform.offset.translate_by(0, (svg_box_state.content_height() - (view_box.height * viewbox_transform.scale_factor)) / 2);
            break;
        case SVG::PreserveAspectRatio::Align::xMinYMax:
        case SVG::PreserveAspectRatio::Align::xMidYMax:
        case SVG::PreserveAspectRatio::Align::xMaxYMax:
            // Align the <min-y>+<height> of the element's ‘viewBox’ with the maximum Y value of the SVG viewport.
            viewbox_transform.offset.translate_by(0, (svg_box_state.content_height() - (view_box.height * viewbox_transform.scale_factor)));
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    return viewbox_transform;
}

void SVGFormattingContext::run(Box const& box, LayoutMode layout_mode, AvailableSpace const& available_space)
{
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

    box.for_each_in_subtree_of_type<Box>([&](Box const& descendant) {
        if (is<SVGGeometryBox>(descendant)) {
            auto const& geometry_box = static_cast<SVGGeometryBox const&>(descendant);
            auto& geometry_box_state = m_state.get_mutable(geometry_box);
            auto& dom_node = const_cast<SVGGeometryBox&>(geometry_box).dom_node();

            auto& path = dom_node.get_path();
            auto path_transform = dom_node.get_transform();

            double viewbox_scale = 1;
            auto maybe_view_box = dom_node.view_box();
            if (maybe_view_box.has_value()) {
                // FIXME: This should allow just one of width or height to be specified.
                // E.g. We should be able to layout <svg width="100%"> where height is unspecified/auto.
                if (!svg_box_state.has_definite_width() || !svg_box_state.has_definite_height()) {
                    dbgln_if(LIBWEB_CSS_DEBUG, "FIXME: Attempting to layout indefinitely sized SVG with a viewbox -- this likely won't work!");
                }

                auto view_box = maybe_view_box.value();
                auto scale_width = svg_box_state.has_definite_width() ? svg_box_state.content_width() / view_box.width : 1;
                auto scale_height = svg_box_state.has_definite_height() ? svg_box_state.content_height() / view_box.height : 1;

                // The initial value for preserveAspectRatio is xMidYMid meet.
                auto preserve_aspect_ratio = svg_svg_element.preserve_aspect_ratio().value_or(SVG::PreserveAspectRatio {});
                auto viewbox_transform = scale_and_align_viewbox_content(preserve_aspect_ratio, view_box, { scale_width, scale_height }, svg_box_state);
                path_transform = Gfx::AffineTransform {}.translate(viewbox_transform.offset.to_type<float>()).scale(viewbox_transform.scale_factor, viewbox_transform.scale_factor).translate({ -view_box.min_x, -view_box.min_y }).multiply(path_transform);
                viewbox_scale = viewbox_transform.scale_factor;
            }

            // Stroke increases the path's size by stroke_width/2 per side.
            auto path_bounding_box = path_transform.map(path.bounding_box()).to_type<CSSPixels>();
            CSSPixels stroke_width = static_cast<double>(geometry_box.dom_node().visible_stroke_width()) * viewbox_scale;
            path_bounding_box.inflate(stroke_width, stroke_width);
            geometry_box_state.set_content_offset(path_bounding_box.top_left());
            geometry_box_state.set_content_width(path_bounding_box.width());
            geometry_box_state.set_content_height(path_bounding_box.height());
        } else if (is<SVGSVGBox>(descendant)) {
            SVGFormattingContext nested_context(m_state, descendant, this);
            nested_context.run(descendant, layout_mode, available_space);
        } else if (is<SVGTextBox>(descendant)) {
            auto const& svg_text_box = static_cast<SVGTextBox const&>(descendant);
            // NOTE: This hack creates a layout state to ensure the existence of a paintable box node in LayoutState::commit(), even when none of the values from UsedValues impact the SVG text.
            m_state.get_mutable(svg_text_box);
        } else if (is<SVGGraphicsBox>(descendant)) {
            // Same hack as above.
            // FIXME: This should be sized based on its children.
            auto const& svg_graphics_box = static_cast<SVGGraphicsBox const&>(descendant);
            m_state.get_mutable(svg_graphics_box);
        }

        return IterationDecision::Continue;
    });
}

}
