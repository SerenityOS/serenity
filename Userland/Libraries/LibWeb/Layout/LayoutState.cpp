/*
 * Copyright (c) 2022-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibWeb/Layout/AvailableSpace.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/LayoutState.h>
#include <LibWeb/Layout/TextNode.h>
#include <LibWeb/Layout/Viewport.h>

namespace Web::Layout {

LayoutState::LayoutState(LayoutState const* parent)
    : m_parent(parent)
    , m_root(find_root())
{
}

LayoutState::~LayoutState()
{
}

LayoutState::UsedValues& LayoutState::get_mutable(NodeWithStyleAndBoxModelMetrics const& box)
{
    if (auto* used_values = used_values_per_layout_node.get(&box).value_or(nullptr))
        return *used_values;

    for (auto const* ancestor = m_parent; ancestor; ancestor = ancestor->m_parent) {
        if (auto* ancestor_used_values = ancestor->used_values_per_layout_node.get(&box).value_or(nullptr)) {
            auto cow_used_values = adopt_own(*new UsedValues(*ancestor_used_values));
            auto* cow_used_values_ptr = cow_used_values.ptr();
            used_values_per_layout_node.set(&box, move(cow_used_values));
            return *cow_used_values_ptr;
        }
    }

    auto const* containing_block_used_values = box.is_viewport() ? nullptr : &get(*box.containing_block());

    auto new_used_values = adopt_own(*new UsedValues);
    auto* new_used_values_ptr = new_used_values.ptr();
    new_used_values->set_node(const_cast<NodeWithStyleAndBoxModelMetrics&>(box), containing_block_used_values);
    used_values_per_layout_node.set(&box, move(new_used_values));
    return *new_used_values_ptr;
}

LayoutState::UsedValues const& LayoutState::get(NodeWithStyleAndBoxModelMetrics const& box) const
{
    if (auto const* used_values = used_values_per_layout_node.get(&box).value_or(nullptr))
        return *used_values;

    for (auto const* ancestor = m_parent; ancestor; ancestor = ancestor->m_parent) {
        if (auto const* ancestor_used_values = ancestor->used_values_per_layout_node.get(&box).value_or(nullptr))
            return *ancestor_used_values;
    }

    auto const* containing_block_used_values = box.is_viewport() ? nullptr : &get(*box.containing_block());

    auto new_used_values = adopt_own(*new UsedValues);
    auto* new_used_values_ptr = new_used_values.ptr();
    new_used_values->set_node(const_cast<NodeWithStyleAndBoxModelMetrics&>(box), containing_block_used_values);
    const_cast<LayoutState*>(this)->used_values_per_layout_node.set(&box, move(new_used_values));
    return *new_used_values_ptr;
}

// https://www.w3.org/TR/css-overflow-3/#scrollable-overflow
static CSSPixelRect measure_scrollable_overflow(Box const& box)
{
    if (!box.paintable_box())
        return {};

    auto& paintable_box = const_cast<Painting::PaintableBox&>(*box.paintable_box());

    if (paintable_box.scrollable_overflow_rect().has_value())
        return paintable_box.scrollable_overflow_rect().value();

    // The scrollable overflow area is the union of:

    // - The scroll container’s own padding box.
    auto scrollable_overflow_rect = paintable_box.absolute_padding_box_rect();

    // - All line boxes directly contained by the scroll container.
    if (box.is_block_container() && box.children_are_inline()) {
        auto const& line_boxes = verify_cast<Painting::PaintableWithLines>(*box.paintable_box()).line_boxes();
        for (auto const& line_box : line_boxes) {
            scrollable_overflow_rect = scrollable_overflow_rect.united(line_box.absolute_rect());
        }
    }

    // - The border boxes of all boxes for which it is the containing block
    //   and whose border boxes are positioned not wholly in the negative scrollable overflow region,
    //   FIXME: accounting for transforms by projecting each box onto the plane of the element that establishes its 3D rendering context. [CSS3-TRANSFORMS]
    if (!box.children_are_inline()) {
        box.for_each_child_of_type<Box>([&box, &scrollable_overflow_rect](Box const& child) {
            if (!child.paintable_box())
                return IterationDecision::Continue;

            auto child_border_box = child.paintable_box()->absolute_border_box_rect();
            // NOTE: Here we check that the child is not wholly in the negative scrollable overflow region.
            if (child_border_box.bottom() > 0 && child_border_box.right() > 0)
                scrollable_overflow_rect = scrollable_overflow_rect.united(child_border_box);

            // - The scrollable overflow areas of all of the above boxes
            //   (including zero-area boxes and accounting for transforms as described above),
            //   provided they themselves have overflow: visible (i.e. do not themselves trap the overflow)
            //   and that scrollable overflow is not already clipped (e.g. by the clip property or the contain property).
            if (is<Viewport>(box) || child.computed_values().overflow_x() == CSS::Overflow::Visible || child.computed_values().overflow_y() == CSS::Overflow::Visible) {
                auto child_scrollable_overflow = measure_scrollable_overflow(child);
                if (is<Viewport>(box) || child.computed_values().overflow_x() == CSS::Overflow::Visible)
                    scrollable_overflow_rect.unite_horizontally(child_scrollable_overflow);
                if (is<Viewport>(box) || child.computed_values().overflow_y() == CSS::Overflow::Visible)
                    scrollable_overflow_rect.unite_vertically(child_scrollable_overflow);
            }

            return IterationDecision::Continue;
        });
    }

    // FIXME: - The margin areas of grid item and flex item boxes for which the box establishes a containing block.

    // FIXME: - Additional padding added to the end-side of the scrollable overflow rectangle as necessary
    //          to enable a scroll position that satisfies the requirements of place-content: end alignment.

    paintable_box.set_overflow_data(Painting::PaintableBox::OverflowData {
        .scrollable_overflow_rect = scrollable_overflow_rect,
        .has_scrollable_overflow = !paintable_box.absolute_padding_box_rect().contains(scrollable_overflow_rect),
    });

    return scrollable_overflow_rect;
}

void LayoutState::commit()
{
    // Only the top-level LayoutState should ever be committed.
    VERIFY(!m_parent);

    HashTable<Layout::TextNode*> text_nodes;

    Vector<Painting::PaintableWithLines&> paintables_with_lines;

    for (auto& it : used_values_per_layout_node) {
        auto& used_values = *it.value;
        auto& node = const_cast<NodeWithStyleAndBoxModelMetrics&>(used_values.node());

        // Transfer box model metrics.
        node.box_model().inset = { used_values.inset_top, used_values.inset_right, used_values.inset_bottom, used_values.inset_left };
        node.box_model().padding = { used_values.padding_top, used_values.padding_right, used_values.padding_bottom, used_values.padding_left };
        node.box_model().border = { used_values.border_top, used_values.border_right, used_values.border_bottom, used_values.border_left };
        node.box_model().margin = { used_values.margin_top, used_values.margin_right, used_values.margin_bottom, used_values.margin_left };

        node.set_paintable(node.create_paintable());

        // For boxes, transfer all the state needed for painting.
        if (is<Layout::Box>(node)) {
            auto& box = static_cast<Layout::Box const&>(node);
            auto& paintable_box = const_cast<Painting::PaintableBox&>(*box.paintable_box());
            paintable_box.set_offset(used_values.offset);
            paintable_box.set_content_size(used_values.content_width(), used_values.content_height());
            paintable_box.set_containing_line_box_fragment(used_values.containing_line_box_fragment);
            if (used_values.override_borders_data().has_value()) {
                paintable_box.set_override_borders_data(used_values.override_borders_data().value());
            }
            if (used_values.table_cell_coordinates().has_value()) {
                paintable_box.set_table_cell_coordinates(used_values.table_cell_coordinates().value());
            }

            if (is<Layout::BlockContainer>(box)) {
                auto& paintable_with_lines = static_cast<Painting::PaintableWithLines&>(paintable_box);
                paintable_with_lines.set_line_boxes(move(used_values.line_boxes));
                paintables_with_lines.append(paintable_with_lines);
            }
        }
    }

    // Measure absolute rect of each line box. Also collect all text nodes.
    for (auto& paintable_with_lines : paintables_with_lines) {
        for (auto& line_box : paintable_with_lines.line_boxes()) {
            CSSPixelRect line_box_absolute_rect;
            for (auto const& fragment : line_box.fragments()) {
                line_box_absolute_rect = line_box_absolute_rect.united(fragment.absolute_rect());
                if (fragment.layout_node().is_text_node())
                    text_nodes.set(static_cast<Layout::TextNode*>(const_cast<Layout::Node*>(&fragment.layout_node())));
            }
            const_cast<LineBox&>(line_box).set_absolute_rect(line_box_absolute_rect);
        }
    }

    // Measure overflow in scroll containers.
    for (auto& it : used_values_per_layout_node) {
        auto& used_values = *it.value;
        if (!used_values.node().is_box())
            continue;
        auto const& box = static_cast<Layout::Box const&>(used_values.node());
        measure_scrollable_overflow(box);
    }

    for (auto* text_node : text_nodes)
        text_node->set_paintable(text_node->create_paintable());
}

void LayoutState::UsedValues::set_node(NodeWithStyleAndBoxModelMetrics& node, UsedValues const* containing_block_used_values)
{
    m_node = &node;

    // NOTE: In the code below, we decide if `node` has definite width and/or height.
    //       This attempts to cover all the *general* cases where CSS considers sizes to be definite.
    //       If `node` has definite values for min/max-width or min/max-height and a definite
    //       preferred size in the same axis, we clamp the preferred size here as well.
    //
    //       There are additional cases where CSS considers values to be definite. We model all of
    //       those by having our engine consider sizes to be definite *once they are assigned to
    //       the UsedValues by calling set_content_width() or set_content_height().

    auto const& computed_values = node.computed_values();

    auto adjust_for_box_sizing = [&](CSSPixels unadjusted_pixels, CSS::Size const& computed_size, bool width) -> CSSPixels {
        // box-sizing: content-box and/or automatic size don't require any adjustment.
        if (computed_values.box_sizing() == CSS::BoxSizing::ContentBox || computed_size.is_auto())
            return unadjusted_pixels;

        // box-sizing: border-box requires us to subtract the relevant border and padding from the size.
        CSSPixels border_and_padding;

        if (width) {
            border_and_padding = computed_values.border_left().width
                + computed_values.padding().left().to_px(*m_node, containing_block_used_values->content_width())
                + computed_values.border_right().width
                + computed_values.padding().right().to_px(*m_node, containing_block_used_values->content_width());
        } else {
            border_and_padding = computed_values.border_top().width
                + computed_values.padding().top().to_px(*m_node, containing_block_used_values->content_width())
                + computed_values.border_bottom().width
                + computed_values.padding().bottom().to_px(*m_node, containing_block_used_values->content_width());
        }

        return unadjusted_pixels - border_and_padding;
    };

    auto is_definite_size = [&](CSS::Size const& size, CSSPixels& resolved_definite_size, bool width) {
        // A size that can be determined without performing layout; that is,
        // a <length>,
        // a measure of text (without consideration of line-wrapping),
        // a size of the initial containing block,
        // or a <percentage> or other formula (such as the “stretch-fit” sizing of non-replaced blocks [CSS2]) that is resolved solely against definite sizes.

        auto containing_block_has_definite_size = containing_block_used_values ? (width ? containing_block_used_values->has_definite_width() : containing_block_used_values->has_definite_height()) : false;

        if (size.is_auto()) {
            // NOTE: The width of a non-flex-item block is considered definite if it's auto and the containing block has definite width.
            if (width
                && !node.is_floating()
                && !node.is_absolutely_positioned()
                && node.display().is_block_outside()
                && node.parent()
                && !node.parent()->is_floating()
                && (node.parent()->display().is_flow_root_inside()
                    || node.parent()->display().is_flow_inside())) {
                if (containing_block_has_definite_size) {
                    CSSPixels available_width = containing_block_used_values->content_width();
                    resolved_definite_size = available_width
                        - margin_left
                        - margin_right
                        - padding_left
                        - padding_right
                        - border_left
                        - border_right;
                    return true;
                }
                return false;
            }
            return false;
        }

        if (size.is_calculated()) {
            if (size.calculated().contains_percentage()) {
                if (!containing_block_has_definite_size)
                    return false;
                auto containing_block_size_as_length = width
                    ? CSS::Length::make_px(containing_block_used_values->content_width())
                    : CSS::Length::make_px(containing_block_used_values->content_height());
                resolved_definite_size = adjust_for_box_sizing(size.calculated().resolve_length_percentage(node, containing_block_size_as_length).value_or(CSS::Length::make_auto()).to_px(node), size, width);
                return true;
            }
            resolved_definite_size = adjust_for_box_sizing(size.calculated().resolve_length(node)->to_px(node), size, width);
            return true;
        }

        if (size.is_length()) {
            VERIFY(!size.is_auto()); // This should have been covered by the Size::is_auto() branch above.
            resolved_definite_size = adjust_for_box_sizing(size.length().to_px(node), size, width);
            return true;
        }
        if (size.is_percentage()) {
            if (containing_block_has_definite_size) {
                auto containing_block_size = width ? containing_block_used_values->content_width() : containing_block_used_values->content_height();
                resolved_definite_size = adjust_for_box_sizing(containing_block_size * static_cast<double>(size.percentage().as_fraction()), size, width);
                return true;
            }
            return false;
        }
        // FIXME: Determine if calc() value is definite.
        return false;
    };

    CSSPixels min_width = 0;
    bool has_definite_min_width = is_definite_size(computed_values.min_width(), min_width, true);
    CSSPixels max_width = 0;
    bool has_definite_max_width = is_definite_size(computed_values.max_width(), max_width, true);

    CSSPixels min_height = 0;
    bool has_definite_min_height = is_definite_size(computed_values.min_height(), min_height, false);
    CSSPixels max_height = 0;
    bool has_definite_max_height = is_definite_size(computed_values.max_height(), max_height, false);

    m_has_definite_width = is_definite_size(computed_values.width(), m_content_width, true);
    m_has_definite_height = is_definite_size(computed_values.height(), m_content_height, false);

    if (m_has_definite_width) {
        if (has_definite_min_width)
            m_content_width = max(min_width, m_content_width);
        if (has_definite_max_width)
            m_content_width = min(max_width, m_content_width);
    }

    if (m_has_definite_height) {
        if (has_definite_min_height)
            m_content_height = max(min_height, m_content_height);
        if (has_definite_max_height)
            m_content_height = min(max_height, m_content_height);
    }
}

void LayoutState::UsedValues::set_content_width(CSSPixels width)
{
    VERIFY(!width.might_be_saturated());
    if (width < 0) {
        // Negative widths are not allowed in CSS. We have a bug somewhere! Clamp to 0 to avoid doing too much damage.
        dbgln_if(LIBWEB_CSS_DEBUG, "FIXME: Layout calculated a negative width for {}: {}", m_node->debug_description(), width);
        width = 0;
    }
    m_content_width = width;
    m_has_definite_width = true;
}

void LayoutState::UsedValues::set_content_height(CSSPixels height)
{
    VERIFY(!height.might_be_saturated());
    if (height < 0) {
        // Negative heights are not allowed in CSS. We have a bug somewhere! Clamp to 0 to avoid doing too much damage.
        dbgln_if(LIBWEB_CSS_DEBUG, "FIXME: Layout calculated a negative height for {}: {}", m_node->debug_description(), height);
        height = 0;
    }
    m_content_height = height;
    m_has_definite_height = true;
}

void LayoutState::UsedValues::set_temporary_content_width(CSSPixels width)
{
    m_content_width = width;
}

void LayoutState::UsedValues::set_temporary_content_height(CSSPixels height)
{
    m_content_height = height;
}

AvailableSize LayoutState::UsedValues::available_width_inside() const
{
    if (width_constraint == SizeConstraint::MinContent)
        return AvailableSize::make_min_content();
    if (width_constraint == SizeConstraint::MaxContent)
        return AvailableSize::make_max_content();
    if (has_definite_width())
        return AvailableSize::make_definite(m_content_width);
    return AvailableSize::make_indefinite();
}

AvailableSize LayoutState::UsedValues::available_height_inside() const
{
    if (height_constraint == SizeConstraint::MinContent)
        return AvailableSize::make_min_content();
    if (height_constraint == SizeConstraint::MaxContent)
        return AvailableSize::make_max_content();
    if (has_definite_height())
        return AvailableSize::make_definite(m_content_height);
    return AvailableSize::make_indefinite();
}

AvailableSpace LayoutState::UsedValues::available_inner_space_or_constraints_from(AvailableSpace const& outer_space) const
{
    auto inner_width = available_width_inside();
    auto inner_height = available_height_inside();

    if (inner_width.is_indefinite() && outer_space.width.is_intrinsic_sizing_constraint())
        inner_width = outer_space.width;
    if (inner_height.is_indefinite() && outer_space.height.is_intrinsic_sizing_constraint())
        inner_height = outer_space.height;
    return AvailableSpace(inner_width, inner_height);
}

void LayoutState::UsedValues::set_content_offset(CSSPixelPoint new_offset)
{
    set_content_x(new_offset.x());
    set_content_y(new_offset.y());
}

void LayoutState::UsedValues::set_content_x(CSSPixels x)
{
    offset.set_x(x);
}

void LayoutState::UsedValues::set_content_y(CSSPixels y)
{
    offset.set_y(y);
}

void LayoutState::UsedValues::set_indefinite_content_width()
{
    m_has_definite_width = false;
}

void LayoutState::UsedValues::set_indefinite_content_height()
{
    m_has_definite_height = false;
}

void LayoutState::UsedValues::set_min_content_width()
{
    width_constraint = SizeConstraint::MinContent;
    m_content_width = 0;
    m_has_definite_height = false;
}

void LayoutState::UsedValues::set_max_content_width()
{
    width_constraint = SizeConstraint::MaxContent;
    m_content_width = INFINITY;
    m_has_definite_width = false;
}

}
