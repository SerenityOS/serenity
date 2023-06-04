/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibGfx/Point.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/LineBox.h>
#include <LibWeb/Painting/PaintableBox.h>

namespace Web::Layout {

enum class SizeConstraint {
    None,
    MinContent,
    MaxContent,
};

class AvailableSize;
class AvailableSpace;

struct LayoutState {
    LayoutState()
        : m_root(*this)
    {
    }

    explicit LayoutState(LayoutState const* parent);
    ~LayoutState();

    LayoutState const& find_root() const
    {
        LayoutState const* root = this;
        for (auto* state = m_parent; state; state = state->m_parent)
            root = state;
        return *root;
    }

    struct UsedValues {
        NodeWithStyleAndBoxModelMetrics const& node() const { return *m_node; }
        void set_node(NodeWithStyleAndBoxModelMetrics&, UsedValues const* containing_block_used_values);

        CSSPixels content_width() const { return m_content_width; }
        CSSPixels content_height() const { return m_content_height; }
        void set_content_width(CSSPixels);
        void set_content_height(CSSPixels);

        void set_indefinite_content_width();
        void set_indefinite_content_height();

        // NOTE: These are used by FlexFormattingContext to assign a temporary main size to items
        //       early on, so that descendants have something to resolve percentages against.
        void set_temporary_content_width(CSSPixels);
        void set_temporary_content_height(CSSPixels);

        bool has_definite_width() const { return m_has_definite_width && width_constraint == SizeConstraint::None; }
        bool has_definite_height() const { return m_has_definite_height && height_constraint == SizeConstraint::None; }

        // Returns the available space for content inside this layout box.
        // If the space in an axis is indefinite, and the outer space is an intrinsic sizing constraint,
        // the constraint is used in that axis instead.
        AvailableSpace available_inner_space_or_constraints_from(AvailableSpace const& outer_space) const;

        void set_content_offset(CSSPixelPoint);
        void set_content_x(CSSPixels);
        void set_content_y(CSSPixels);

        CSSPixelPoint offset;

        SizeConstraint width_constraint { SizeConstraint::None };
        SizeConstraint height_constraint { SizeConstraint::None };

        CSSPixels margin_left { 0 };
        CSSPixels margin_right { 0 };
        CSSPixels margin_top { 0 };
        CSSPixels margin_bottom { 0 };

        CSSPixels border_left { 0 };
        CSSPixels border_right { 0 };
        CSSPixels border_top { 0 };
        CSSPixels border_bottom { 0 };

        CSSPixels padding_left { 0 };
        CSSPixels padding_right { 0 };
        CSSPixels padding_top { 0 };
        CSSPixels padding_bottom { 0 };

        CSSPixels inset_left { 0 };
        CSSPixels inset_right { 0 };
        CSSPixels inset_top { 0 };
        CSSPixels inset_bottom { 0 };

        Vector<LineBox> line_boxes;

        CSSPixels margin_box_left() const { return margin_left + border_left + padding_left; }
        CSSPixels margin_box_right() const { return margin_right + border_right + padding_right; }
        CSSPixels margin_box_top() const { return margin_top + border_top + padding_top; }
        CSSPixels margin_box_bottom() const { return margin_bottom + border_bottom + padding_bottom; }

        CSSPixels margin_box_width() const { return margin_box_left() + content_width() + margin_box_right(); }
        CSSPixels margin_box_height() const { return margin_box_top() + content_height() + margin_box_bottom(); }

        CSSPixels border_box_left() const { return border_left + padding_left; }
        CSSPixels border_box_right() const { return border_right + padding_right; }
        CSSPixels border_box_top() const { return border_top + padding_top; }
        CSSPixels border_box_bottom() const { return border_bottom + padding_bottom; }

        CSSPixels border_box_width() const { return border_box_left() + content_width() + border_box_right(); }
        CSSPixels border_box_height() const { return border_box_top() + content_height() + border_box_bottom(); }

        Optional<LineBoxFragmentCoordinate> containing_line_box_fragment;

        void add_floating_descendant(Box const& box) { m_floating_descendants.set(&box); }
        auto const& floating_descendants() const { return m_floating_descendants; }

        void set_override_borders_data(Painting::BordersData const& override_borders_data) { m_override_borders_data = override_borders_data; };
        auto const& override_borders_data() const { return m_override_borders_data; }

    private:
        AvailableSize available_width_inside() const;
        AvailableSize available_height_inside() const;

        JS::GCPtr<Layout::NodeWithStyleAndBoxModelMetrics> m_node { nullptr };

        CSSPixels m_content_width { 0 };
        CSSPixels m_content_height { 0 };

        bool m_has_definite_width { false };
        bool m_has_definite_height { false };

        HashTable<JS::GCPtr<Box const>> m_floating_descendants;

        Optional<Painting::BordersData> m_override_borders_data;
    };

    void commit();

    // NOTE: get_mutable() will CoW the UsedValues if it's inherited from an ancestor state;
    UsedValues& get_mutable(NodeWithStyleAndBoxModelMetrics const&);

    // NOTE: get() will not CoW the UsedValues.
    UsedValues const& get(NodeWithStyleAndBoxModelMetrics const&) const;

    HashMap<Layout::Node const*, NonnullOwnPtr<UsedValues>> used_values_per_layout_node;

    // We cache intrinsic sizes once determined, as they will not change over the course of a full layout.
    // This avoids computing them several times while performing flex layout.
    struct IntrinsicSizes {
        Optional<CSSPixels> min_content_width;
        Optional<CSSPixels> max_content_width;

        // NOTE: Since intrinsic heights depend on the amount of available width, we have to cache
        //       three separate kinds of results, depending on the available width at the time of calculation.
        HashMap<CSSPixels, Optional<CSSPixels>> min_content_height_with_definite_available_width;
        HashMap<CSSPixels, Optional<CSSPixels>> max_content_height_with_definite_available_width;
        Optional<CSSPixels> min_content_height_with_min_content_available_width;
        Optional<CSSPixels> max_content_height_with_min_content_available_width;
        Optional<CSSPixels> min_content_height_with_max_content_available_width;
        Optional<CSSPixels> max_content_height_with_max_content_available_width;
    };

    HashMap<JS::GCPtr<NodeWithStyleAndBoxModelMetrics const>, NonnullOwnPtr<IntrinsicSizes>> mutable intrinsic_sizes;

    LayoutState const* m_parent { nullptr };
    LayoutState const& m_root;
};

}
