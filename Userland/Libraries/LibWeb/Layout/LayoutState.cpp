/*
 * Copyright (c) 2022-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibWeb/Layout/AvailableSpace.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/LayoutState.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Painting/InlinePaintable.h>
#include <LibWeb/Painting/SVGPathPaintable.h>

namespace Web::Layout {

LayoutState::LayoutState(LayoutState const* parent)
    : m_parent(parent)
    , m_root(find_root())
{
}

LayoutState::~LayoutState()
{
}

LayoutState::UsedValues& LayoutState::get_mutable(NodeWithStyle const& node)
{
    if (auto* used_values = used_values_per_layout_node.get(&node).value_or(nullptr))
        return *used_values;

    for (auto const* ancestor = m_parent; ancestor; ancestor = ancestor->m_parent) {
        if (auto* ancestor_used_values = ancestor->used_values_per_layout_node.get(&node).value_or(nullptr)) {
            auto cow_used_values = adopt_own(*new UsedValues(*ancestor_used_values));
            auto* cow_used_values_ptr = cow_used_values.ptr();
            used_values_per_layout_node.set(&node, move(cow_used_values));
            return *cow_used_values_ptr;
        }
    }

    auto const* containing_block_used_values = node.is_viewport() ? nullptr : &get(*node.containing_block());

    auto new_used_values = adopt_own(*new UsedValues);
    auto* new_used_values_ptr = new_used_values.ptr();
    new_used_values->set_node(const_cast<NodeWithStyle&>(node), containing_block_used_values);
    used_values_per_layout_node.set(&node, move(new_used_values));
    return *new_used_values_ptr;
}

LayoutState::UsedValues const& LayoutState::get(NodeWithStyle const& node) const
{
    if (auto const* used_values = used_values_per_layout_node.get(&node).value_or(nullptr))
        return *used_values;

    for (auto const* ancestor = m_parent; ancestor; ancestor = ancestor->m_parent) {
        if (auto const* ancestor_used_values = ancestor->used_values_per_layout_node.get(&node).value_or(nullptr))
            return *ancestor_used_values;
    }

    auto const* containing_block_used_values = node.is_viewport() ? nullptr : &get(*node.containing_block());

    auto new_used_values = adopt_own(*new UsedValues);
    auto* new_used_values_ptr = new_used_values.ptr();
    new_used_values->set_node(const_cast<NodeWithStyle&>(node), containing_block_used_values);
    const_cast<LayoutState*>(this)->used_values_per_layout_node.set(&node, move(new_used_values));
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
    if (is<Painting::PaintableWithLines>(box.paintable())) {
        for (auto const& fragment : static_cast<Painting::PaintableWithLines const&>(*box.paintable()).fragments()) {
            scrollable_overflow_rect = scrollable_overflow_rect.united(fragment.absolute_rect());
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

void LayoutState::resolve_relative_positions(Vector<Painting::PaintableWithLines&> const& paintables_with_lines)
{
    // This function resolves relative position offsets of all the boxes & fragments in the paint tree.
    // It runs *after* the paint tree has been constructed, so it modifies paintable node & fragment offsets directly.

    // Regular boxes (not line box fragments):
    for (auto& it : used_values_per_layout_node) {
        auto& used_values = *it.value;
        auto& node = const_cast<NodeWithStyle&>(used_values.node());

        if (!node.is_box())
            continue;

        auto& paintable = static_cast<Painting::PaintableBox&>(*node.paintable());
        CSSPixelPoint offset;

        if (used_values.containing_line_box_fragment.has_value()) {
            // Atomic inline case:
            // We know that `node` is an atomic inline because `containing_line_box_fragments` refers to the
            // line box fragment in the parent block container that contains it.
            auto const& containing_line_box_fragment = used_values.containing_line_box_fragment.value();
            auto const& containing_block = *node.containing_block();
            auto const& containing_block_used_values = get(containing_block);
            auto const& fragment = containing_block_used_values.line_boxes[containing_line_box_fragment.line_box_index].fragments()[containing_line_box_fragment.fragment_index];

            // The fragment has the final offset for the atomic inline, so we just need to copy it from there.
            offset = fragment.offset();
        } else {
            // Not an atomic inline, much simpler case.
            offset = used_values.offset;
        }
        // Apply relative position inset if appropriate.
        if (node.computed_values().position() == CSS::Positioning::Relative && is<NodeWithStyleAndBoxModelMetrics>(node)) {
            auto& inset = static_cast<NodeWithStyleAndBoxModelMetrics const&>(node).box_model().inset;
            offset.translate_by(inset.left, inset.top);
        }
        paintable.set_offset(offset);
    }

    // Line box fragments:
    for (auto const& paintable_with_lines : paintables_with_lines) {
        for (auto& fragment : paintable_with_lines.fragments()) {
            auto const& fragment_node = fragment.layout_node();
            if (!is<Layout::NodeWithStyleAndBoxModelMetrics>(*fragment_node.parent()))
                continue;
            // Collect effective relative position offset from inline-flow parent chain.
            CSSPixelPoint offset;
            for (auto* ancestor = fragment_node.parent(); ancestor; ancestor = ancestor->parent()) {
                if (!is<Layout::NodeWithStyleAndBoxModelMetrics>(*ancestor))
                    break;
                if (!ancestor->display().is_inline_outside() || !ancestor->display().is_flow_inside())
                    break;
                if (ancestor->computed_values().position() == CSS::Positioning::Relative) {
                    auto const& ancestor_node = static_cast<Layout::NodeWithStyleAndBoxModelMetrics const&>(*ancestor);
                    auto const& inset = ancestor_node.box_model().inset;
                    offset.translate_by(inset.left, inset.top);
                }
            }
            const_cast<Painting::PaintableFragment&>(fragment).set_offset(fragment.offset().translated(offset));
        }
    }
}

static void build_paint_tree(Node& node, Painting::Paintable* parent_paintable = nullptr)
{
    Painting::Paintable* paintable = nullptr;
    if (node.paintable()) {
        paintable = const_cast<Painting::Paintable*>(node.paintable());
        if (parent_paintable && !paintable->forms_unconnected_subtree()) {
            VERIFY(!paintable->parent());
            parent_paintable->append_child(*paintable);
        }
        paintable->set_dom_node(node.dom_node());
        if (node.dom_node())
            node.dom_node()->set_paintable(paintable);
    }
    for (auto* child = node.first_child(); child; child = child->next_sibling()) {
        build_paint_tree(*child, paintable);
    }
}

static Painting::BorderRadiiData normalized_border_radii_data(Layout::Node const& node, CSSPixelRect const& rect, CSS::BorderRadiusData top_left_radius, CSS::BorderRadiusData top_right_radius, CSS::BorderRadiusData bottom_right_radius, CSS::BorderRadiusData bottom_left_radius)
{
    Painting::BorderRadiusData bottom_left_radius_px {};
    Painting::BorderRadiusData bottom_right_radius_px {};
    Painting::BorderRadiusData top_left_radius_px {};
    Painting::BorderRadiusData top_right_radius_px {};

    bottom_left_radius_px.horizontal_radius = bottom_left_radius.horizontal_radius.to_px(node, rect.width());
    bottom_right_radius_px.horizontal_radius = bottom_right_radius.horizontal_radius.to_px(node, rect.width());
    top_left_radius_px.horizontal_radius = top_left_radius.horizontal_radius.to_px(node, rect.width());
    top_right_radius_px.horizontal_radius = top_right_radius.horizontal_radius.to_px(node, rect.width());

    bottom_left_radius_px.vertical_radius = bottom_left_radius.vertical_radius.to_px(node, rect.height());
    bottom_right_radius_px.vertical_radius = bottom_right_radius.vertical_radius.to_px(node, rect.height());
    top_left_radius_px.vertical_radius = top_left_radius.vertical_radius.to_px(node, rect.height());
    top_right_radius_px.vertical_radius = top_right_radius.vertical_radius.to_px(node, rect.height());

    // Scale overlapping curves according to https://www.w3.org/TR/css-backgrounds-3/#corner-overlap
    // Let f = min(Li/Si), where i ∈ {top, right, bottom, left},
    // Si is the sum of the two corresponding radii of the corners on side i,
    // and Ltop = Lbottom = the width of the box, and Lleft = Lright = the height of the box.
    auto l_top = rect.width();
    auto l_bottom = l_top;
    auto l_left = rect.height();
    auto l_right = l_left;
    auto s_top = (top_left_radius_px.horizontal_radius + top_right_radius_px.horizontal_radius);
    auto s_right = (top_right_radius_px.vertical_radius + bottom_right_radius_px.vertical_radius);
    auto s_bottom = (bottom_left_radius_px.horizontal_radius + bottom_right_radius_px.horizontal_radius);
    auto s_left = (top_left_radius_px.vertical_radius + bottom_left_radius_px.vertical_radius);
    CSSPixelFraction f = 1;
    f = min(f, l_top / s_top);
    f = min(f, l_right / s_right);
    f = min(f, l_bottom / s_bottom);
    f = min(f, l_left / s_left);

    // If f < 1, then all corner radii are reduced by multiplying them by f.
    if (f < 1) {
        top_left_radius_px.horizontal_radius *= f;
        top_left_radius_px.vertical_radius *= f;
        top_right_radius_px.horizontal_radius *= f;
        top_right_radius_px.vertical_radius *= f;
        bottom_right_radius_px.horizontal_radius *= f;
        bottom_right_radius_px.vertical_radius *= f;
        bottom_left_radius_px.horizontal_radius *= f;
        bottom_left_radius_px.vertical_radius *= f;
    }

    return Painting::BorderRadiiData { top_left_radius_px, top_right_radius_px, bottom_right_radius_px, bottom_left_radius_px };
}

void LayoutState::resolve_border_radii()
{
    Vector<Painting::InlinePaintable&> inline_paintables;

    for (auto& it : used_values_per_layout_node) {
        auto& used_values = *it.value;
        auto& node = const_cast<NodeWithStyle&>(used_values.node());

        auto* paintable = node.paintable();

        if (paintable && is<Painting::InlinePaintable>(*paintable)) {
            auto& inline_paintable = static_cast<Painting::InlinePaintable&>(*paintable);
            inline_paintables.append(inline_paintable);
        }

        if (paintable && is<Painting::PaintableBox>(*paintable)) {
            auto& paintable_box = static_cast<Painting::PaintableBox&>(*paintable);

            CSSPixelRect const border_rect { 0, 0, used_values.border_box_width(), used_values.border_box_height() };

            auto const& border_top_left_radius = node.computed_values().border_top_left_radius();
            auto const& border_top_right_radius = node.computed_values().border_top_right_radius();
            auto const& border_bottom_right_radius = node.computed_values().border_bottom_right_radius();
            auto const& border_bottom_left_radius = node.computed_values().border_bottom_left_radius();

            auto radii_data = normalized_border_radii_data(node, border_rect, border_top_left_radius, border_top_right_radius, border_bottom_right_radius, border_bottom_left_radius);
            paintable_box.set_border_radii_data(radii_data);
        }
    }

    for (auto& inline_paintable : inline_paintables) {
        Vector<Painting::PaintableFragment&> fragments;
        verify_cast<Painting::PaintableWithLines>(*inline_paintable.containing_block()->paintable_box()).for_each_fragment([&](auto& fragment) {
            if (inline_paintable.layout_node().is_inclusive_ancestor_of(fragment.layout_node()))
                fragments.append(const_cast<Painting::PaintableFragment&>(fragment));
            return IterationDecision::Continue;
        });

        auto const& top_left_border_radius = inline_paintable.computed_values().border_top_left_radius();
        auto const& top_right_border_radius = inline_paintable.computed_values().border_top_right_radius();
        auto const& bottom_right_border_radius = inline_paintable.computed_values().border_bottom_right_radius();
        auto const& bottom_left_border_radius = inline_paintable.computed_values().border_bottom_left_radius();

        auto containing_block_position_in_absolute_coordinates = inline_paintable.containing_block()->paintable_box()->absolute_position();
        for (size_t i = 0; i < fragments.size(); ++i) {
            auto is_first_fragment = i == 0;
            auto is_last_fragment = i == fragments.size() - 1;
            auto& fragment = fragments[i];

            CSSPixelRect absolute_fragment_rect { containing_block_position_in_absolute_coordinates.translated(fragment.offset()), fragment.size() };

            if (is_first_fragment) {
                auto extra_start_width = inline_paintable.box_model().padding.left;
                absolute_fragment_rect.translate_by(-extra_start_width, 0);
                absolute_fragment_rect.set_width(absolute_fragment_rect.width() + extra_start_width);
            }

            if (is_last_fragment) {
                auto extra_end_width = inline_paintable.box_model().padding.right;
                absolute_fragment_rect.set_width(absolute_fragment_rect.width() + extra_end_width);
            }

            auto border_radii_data = normalized_border_radii_data(inline_paintable.layout_node(), absolute_fragment_rect, top_left_border_radius, top_right_border_radius, bottom_right_border_radius, bottom_left_border_radius);
            fragment.set_border_radii_data(border_radii_data);
        }
    }
}

void LayoutState::resolve_box_shadow_data()
{
    for (auto& it : used_values_per_layout_node) {
        auto& used_values = *it.value;
        auto& node = const_cast<NodeWithStyle&>(used_values.node());
        auto* paintable = node.paintable();
        if (paintable && is<Painting::PaintableBox>(*paintable)) {
            auto box_shadow_data = node.computed_values().box_shadow();
            if (box_shadow_data.is_empty())
                continue;
            auto& paintable_box = static_cast<Painting::PaintableBox&>(*paintable);
            Vector<Painting::ShadowData> resolved_box_shadow_data;
            resolved_box_shadow_data.ensure_capacity(box_shadow_data.size());
            for (auto const& layer : box_shadow_data) {
                resolved_box_shadow_data.empend(
                    layer.color,
                    layer.offset_x.to_px(node),
                    layer.offset_y.to_px(node),
                    layer.blur_radius.to_px(node),
                    layer.spread_distance.to_px(node),
                    layer.placement == CSS::ShadowPlacement::Outer ? Painting::ShadowPlacement::Outer : Painting::ShadowPlacement::Inner);
            }
            paintable_box.set_box_shadow_data(move(resolved_box_shadow_data));
        }
    }
}

void LayoutState::commit(Box& root)
{
    // Only the top-level LayoutState should ever be committed.
    VERIFY(!m_parent);

    // NOTE: In case this is a relayout of an existing tree, we start by detaching the old paint tree
    //       from the layout tree. This is done to ensure that we don't end up with any old-tree pointers
    //       when text paintables shift around in the tree.
    root.for_each_in_inclusive_subtree_of_type<Layout::TextNode>([&](Layout::TextNode& text_node) {
        text_node.set_paintable(nullptr);
        return IterationDecision::Continue;
    });

    HashTable<Layout::TextNode*> text_nodes;

    Vector<Painting::PaintableWithLines&> paintables_with_lines;

    for (auto& it : used_values_per_layout_node) {
        auto& used_values = *it.value;
        auto& node = const_cast<NodeWithStyle&>(used_values.node());

        if (is<NodeWithStyleAndBoxModelMetrics>(node)) {
            // Transfer box model metrics.
            auto& box_model = static_cast<NodeWithStyleAndBoxModelMetrics&>(node).box_model();
            box_model.inset = { used_values.inset_top, used_values.inset_right, used_values.inset_bottom, used_values.inset_left };
            box_model.padding = { used_values.padding_top, used_values.padding_right, used_values.padding_bottom, used_values.padding_left };
            box_model.border = { used_values.border_top, used_values.border_right, used_values.border_bottom, used_values.border_left };
            box_model.margin = { used_values.margin_top, used_values.margin_right, used_values.margin_bottom, used_values.margin_left };
        }

        auto paintable = node.create_paintable();

        node.set_paintable(paintable);

        // For boxes, transfer all the state needed for painting.
        if (paintable && is<Painting::PaintableBox>(*paintable)) {
            auto& paintable_box = static_cast<Painting::PaintableBox&>(*paintable);
            paintable_box.set_offset(used_values.offset);
            paintable_box.set_content_size(used_values.content_width(), used_values.content_height());
            if (used_values.override_borders_data().has_value()) {
                paintable_box.set_override_borders_data(used_values.override_borders_data().value());
            }
            if (used_values.table_cell_coordinates().has_value()) {
                paintable_box.set_table_cell_coordinates(used_values.table_cell_coordinates().value());
            }

            if (is<Painting::PaintableWithLines>(paintable_box)) {
                auto& paintable_with_lines = static_cast<Painting::PaintableWithLines&>(paintable_box);
                for (auto& line_box : used_values.line_boxes) {
                    for (auto& fragment : line_box.fragments())
                        paintable_with_lines.add_fragment(fragment);
                }
                paintables_with_lines.append(paintable_with_lines);
            }

            if (used_values.computed_svg_transforms().has_value() && is<Painting::SVGGraphicsPaintable>(paintable_box)) {
                auto& svg_graphics_paintable = static_cast<Painting::SVGGraphicsPaintable&>(paintable_box);
                svg_graphics_paintable.set_computed_transforms(*used_values.computed_svg_transforms());
            }

            if (used_values.computed_svg_path().has_value() && is<Painting::SVGPathPaintable>(paintable_box)) {
                auto& svg_geometry_paintable = static_cast<Painting::SVGPathPaintable&>(paintable_box);
                svg_geometry_paintable.set_computed_path(move(*used_values.computed_svg_path()));
            }
        }
    }

    resolve_relative_positions(paintables_with_lines);

    // Make a pass over all the line boxes to:
    // - Measure absolute rect of each line box.
    // - Collect all text nodes, so we can create paintables for them later.
    for (auto& paintable_with_lines : paintables_with_lines) {
        for (auto& fragment : paintable_with_lines.fragments()) {
            if (fragment.layout_node().is_text_node())
                text_nodes.set(static_cast<Layout::TextNode*>(const_cast<Layout::Node*>(&fragment.layout_node())));
        }
    }

    for (auto* text_node : text_nodes)
        text_node->set_paintable(text_node->create_paintable());

    build_paint_tree(root);

    // Measure overflow in scroll containers.
    for (auto& it : used_values_per_layout_node) {
        auto& used_values = *it.value;
        if (!used_values.node().is_box())
            continue;
        auto const& box = static_cast<Layout::Box const&>(used_values.node());
        measure_scrollable_overflow(box);
    }

    resolve_border_radii();
    resolve_box_shadow_data();

    for (auto& it : used_values_per_layout_node) {
        auto& used_values = *it.value;
        auto& node = const_cast<NodeWithStyle&>(used_values.node());
        auto* paintable = node.paintable();
        if (paintable && is<Painting::InlinePaintable>(*paintable)) {
            auto& inline_paintable = static_cast<Painting::InlinePaintable&>(*paintable);
            // FIXME: Marking fragments contained by inline node is a hack required to skip them while painting
            //        PaintableWithLines content.
            inline_paintable.mark_contained_fragments();
        }
    }
}

void LayoutState::UsedValues::set_node(NodeWithStyle& node, UsedValues const* containing_block_used_values)
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
                auto containing_block_size_as_length = width ? containing_block_used_values->content_width() : containing_block_used_values->content_height();
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
                resolved_definite_size = adjust_for_box_sizing(containing_block_size.scaled(size.percentage().as_fraction()), size, width);
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

}
