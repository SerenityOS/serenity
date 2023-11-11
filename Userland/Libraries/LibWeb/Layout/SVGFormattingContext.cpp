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
#include <LibWeb/SVG/SVGSymbolElement.h>
#include <LibWeb/SVG/SVGUseElement.h>

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
            viewbox_transform.offset.translate_by((svg_box_state.content_width() - CSSPixels::nearest_value_for(view_box.width * viewbox_transform.scale_factor)) / 2, 0);
            break;
        case SVG::PreserveAspectRatio::Align::xMaxYMin:
        case SVG::PreserveAspectRatio::Align::xMaxYMid:
        case SVG::PreserveAspectRatio::Align::xMaxYMax:
            // Align the <min-x>+<width> of the element's ‘viewBox’ with the maximum X value of the SVG viewport.
            viewbox_transform.offset.translate_by((svg_box_state.content_width() - CSSPixels::nearest_value_for(view_box.width * viewbox_transform.scale_factor)), 0);
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
            viewbox_transform.offset.translate_by(0, (svg_box_state.content_height() - CSSPixels::nearest_value_for(view_box.height * viewbox_transform.scale_factor)) / 2);
            break;
        case SVG::PreserveAspectRatio::Align::xMinYMax:
        case SVG::PreserveAspectRatio::Align::xMidYMax:
        case SVG::PreserveAspectRatio::Align::xMaxYMax:
            // Align the <min-y>+<height> of the element's ‘viewBox’ with the maximum Y value of the SVG viewport.
            viewbox_transform.offset.translate_by(0, (svg_box_state.content_height() - CSSPixels::nearest_value_for(view_box.height * viewbox_transform.scale_factor)));
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    return viewbox_transform;
}

static bool should_ensure_creation_of_paintable(Node const& node)
{
    if (is<SVGGraphicsBox>(node))
        return true;
    if (node.dom_node()) {
        if (is<SVG::SVGUseElement>(*node.dom_node()))
            return true;
        if (is<SVG::SVGSymbolElement>(*node.dom_node()))
            return true;
    }
    return false;
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

    auto compute_viewbox_transform = [&](auto const& viewbox) -> Gfx::AffineTransform {
        if (!viewbox.has_value())
            return {};

        // FIXME: This should allow just one of width or height to be specified.
        // E.g. We should be able to layout <svg width="100%"> where height is unspecified/auto.
        if (!svg_box_state.has_definite_width() || !svg_box_state.has_definite_height()) {
            dbgln_if(LIBWEB_CSS_DEBUG, "FIXME: Attempting to layout indefinitely sized SVG with a viewbox -- this likely won't work!");
        }

        auto scale_width = svg_box_state.has_definite_width() ? svg_box_state.content_width() / viewbox->width : 1;
        auto scale_height = svg_box_state.has_definite_height() ? svg_box_state.content_height() / viewbox->height : 1;

        // The initial value for preserveAspectRatio is xMidYMid meet.
        auto preserve_aspect_ratio = svg_svg_element.preserve_aspect_ratio().value_or(SVG::PreserveAspectRatio {});
        auto viewbox_offset_and_scale = scale_and_align_viewbox_content(preserve_aspect_ratio, *viewbox, { scale_width, scale_height }, svg_box_state);

        CSSPixelPoint offset = viewbox_offset_and_scale.offset;
        return Gfx::AffineTransform {}.translate(offset.to_type<float>()).scale(viewbox_offset_and_scale.scale_factor, viewbox_offset_and_scale.scale_factor).translate({ -viewbox->min_x, -viewbox->min_y });
    };

    box.for_each_in_subtree([&](Node const& descendant) {
        if (is<SVGGraphicsBox>(descendant)) {
            auto const& graphics_box = static_cast<SVGGraphicsBox const&>(descendant);
            auto& dom_node = const_cast<SVGGraphicsBox&>(graphics_box).dom_node();
            auto viewbox = dom_node.view_box();

            // https://svgwg.org/svg2-draft/coords.html#ViewBoxAttribute
            if (viewbox.has_value()) {
                if (viewbox->width < 0 || viewbox->height < 0) {
                    // A negative value for <width> or <height> is an error and invalidates the ‘viewBox’ attribute.
                    viewbox = {};
                } else if (viewbox->width == 0 || viewbox->height == 0) {
                    // A value of zero disables rendering of the element.
                    return IterationDecision::Continue;
                }
            }

            auto& graphics_box_state = m_state.get_mutable(graphics_box);
            auto svg_transform = dom_node.get_transform();

            Gfx::AffineTransform viewbox_transform = compute_viewbox_transform(viewbox);
            graphics_box_state.set_computed_svg_transforms(Painting::SVGGraphicsPaintable::ComputedTransforms(viewbox_transform, svg_transform));
            auto to_css_pixels_transform = Gfx::AffineTransform {}.multiply(viewbox_transform).multiply(svg_transform);

            Gfx::Path path;
            if (is<SVGGeometryBox>(descendant)) {
                path = static_cast<SVG::SVGGeometryElement&>(dom_node).get_path();
            } else if (is<SVGTextBox>(descendant)) {
                auto& text_element = static_cast<SVG::SVGTextPositioningElement&>(dom_node);

                auto& font = graphics_box.font();
                auto text_contents = text_element.text_contents();
                Utf8View text_utf8 { text_contents };
                auto text_width = font.width(text_utf8);

                auto text_offset = text_element.get_offset();
                // https://svgwg.org/svg2-draft/text.html#TextAnchoringProperties
                switch (text_element.text_anchor().value_or(SVG::TextAnchor::Start)) {
                case SVG::TextAnchor::Start:
                    // The rendered characters are aligned such that the start of the resulting rendered text is at the initial
                    // current text position.
                    break;
                case SVG::TextAnchor::Middle: {
                    // The rendered characters are shifted such that the geometric middle of the resulting rendered text
                    // (determined from the initial and final current text position before applying the text-anchor property)
                    // is at the initial current text position.
                    text_offset.translate_by(-text_width / 2, 0);
                    break;
                }
                case SVG::TextAnchor::End: {
                    // The rendered characters are shifted such that the end of the resulting rendered text (final current text
                    // position before applying the text-anchor property) is at the initial current text position.
                    text_offset.translate_by(-text_width, 0);
                    break;
                }
                default:
                    VERIFY_NOT_REACHED();
                }

                path.move_to(text_offset);
                path.text(text_utf8, font);
            }

            auto path_bounding_box = to_css_pixels_transform.map(path.bounding_box()).to_type<CSSPixels>();
            // Stroke increases the path's size by stroke_width/2 per side.
            CSSPixels stroke_width = CSSPixels::nearest_value_for(graphics_box.dom_node().visible_stroke_width() * viewbox_transform.x_scale());
            path_bounding_box.inflate(stroke_width, stroke_width);
            graphics_box_state.set_content_offset(path_bounding_box.top_left());
            graphics_box_state.set_content_width(path_bounding_box.width());
            graphics_box_state.set_content_height(path_bounding_box.height());
            graphics_box_state.set_computed_svg_path(move(path));
        } else if (is<SVGSVGBox>(descendant)) {
            SVGFormattingContext nested_context(m_state, static_cast<SVGSVGBox const&>(descendant), this);
            nested_context.run(static_cast<SVGSVGBox const&>(descendant), layout_mode, available_space);
        } else if (should_ensure_creation_of_paintable(descendant)) {
            // NOTE: This hack creates a layout state to ensure the existence of
            //       a paintable in LayoutState::commit().
            m_state.get_mutable(static_cast<NodeWithStyleAndBoxModelMetrics const&>(descendant));
        }

        return IterationDecision::Continue;
    });

    // https://svgwg.org/svg2-draft/struct.html#Groups
    // 5.2. Grouping: the ‘g’ element
    // The ‘g’ element is a container element for grouping together related graphics elements.
    box.for_each_in_subtree_of_type<Box>([&](Box const& descendant) {
        if (is<SVGGraphicsBox>(descendant) && !is<SVGGeometryBox>(descendant) && !is<SVGTextBox>(descendant)) {
            auto const& svg_graphics_box = static_cast<SVGGraphicsBox const&>(descendant);
            auto& graphics_box_state = m_state.get_mutable(svg_graphics_box);
            auto smallest_x_position = CSSPixels(0);
            auto smallest_y_position = CSSPixels(0);
            auto greatest_x_position = CSSPixels(0);
            auto greatest_y_position = CSSPixels(0);

            descendant.for_each_in_subtree_of_type<Box>([&](Box const& child_of_svg_container) {
                auto& box_state = m_state.get_mutable(child_of_svg_container);
                smallest_x_position = box_state.offset.x();
                smallest_y_position = box_state.offset.y();
                return IterationDecision::Break;
            });

            descendant.for_each_in_subtree_of_type<Box>([&](Box const& child_of_svg_container) {
                auto& box_state = m_state.get_mutable(child_of_svg_container);
                smallest_x_position = min(smallest_x_position, box_state.offset.x());
                smallest_y_position = min(smallest_y_position, box_state.offset.y());
                greatest_x_position = max(greatest_x_position, box_state.offset.x() + box_state.content_width());
                greatest_y_position = max(greatest_y_position, box_state.offset.y() + box_state.content_height());
                return IterationDecision::Continue;
            });

            graphics_box_state.set_content_x(smallest_x_position);
            graphics_box_state.set_content_y(smallest_y_position);
            graphics_box_state.set_content_width(greatest_x_position - smallest_x_position);
            graphics_box_state.set_content_height(greatest_y_position - smallest_y_position);
        }
        return IterationDecision::Continue;
    });
}

}
