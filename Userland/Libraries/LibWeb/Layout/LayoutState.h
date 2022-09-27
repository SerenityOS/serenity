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

    explicit LayoutState(LayoutState const* parent)
        : m_parent(parent)
        , m_root(find_root())
    {
        used_values_per_layout_node.resize(m_root.used_values_per_layout_node.size());
    }

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

        float content_width() const { return m_content_width; }
        float content_height() const { return m_content_height; }
        void set_content_width(float);
        void set_content_height(float);

        bool has_definite_width() const { return m_has_definite_width && width_constraint == SizeConstraint::None; }
        bool has_definite_height() const { return m_has_definite_height && height_constraint == SizeConstraint::None; }

        // Returns the available space for content inside this layout box.
        // If the space in an axis is indefinite, and the outer space is an intrinsic sizing constraint,
        // the constraint is used in that axis instead.
        AvailableSpace available_inner_space_or_constraints_from(AvailableSpace const& outer_space) const;

        void set_content_offset(Gfx::FloatPoint);
        void set_content_x(float);
        void set_content_y(float);

        Gfx::FloatPoint offset;

        SizeConstraint width_constraint { SizeConstraint::None };
        SizeConstraint height_constraint { SizeConstraint::None };

        float margin_left { 0 };
        float margin_right { 0 };
        float margin_top { 0 };
        float margin_bottom { 0 };

        float border_left { 0 };
        float border_right { 0 };
        float border_top { 0 };
        float border_bottom { 0 };

        float padding_left { 0 };
        float padding_right { 0 };
        float padding_top { 0 };
        float padding_bottom { 0 };

        float inset_left { 0 };
        float inset_right { 0 };
        float inset_top { 0 };
        float inset_bottom { 0 };

        Vector<LineBox> line_boxes;

        float margin_box_left() const { return margin_left + border_left + padding_left; }
        float margin_box_right() const { return margin_right + border_right + padding_right; }
        float margin_box_top() const { return margin_top + border_top + padding_top; }
        float margin_box_bottom() const { return margin_bottom + border_bottom + padding_bottom; }

        float margin_box_width() const { return margin_box_left() + content_width() + margin_box_right(); }
        float margin_box_height() const { return margin_box_top() + content_height() + margin_box_bottom(); }

        float border_box_left() const { return border_left + padding_left; }
        float border_box_right() const { return border_right + padding_right; }
        float border_box_top() const { return border_top + padding_top; }
        float border_box_bottom() const { return border_bottom + padding_bottom; }

        float border_box_width() const { return border_box_left() + content_width() + border_box_right(); }
        float border_box_height() const { return border_box_top() + content_height() + border_box_bottom(); }

        Optional<Painting::PaintableBox::OverflowData> overflow_data;

        Painting::PaintableBox::OverflowData& ensure_overflow_data()
        {
            if (!overflow_data.has_value())
                overflow_data = Painting::PaintableBox::OverflowData {};
            return *overflow_data;
        }

        Optional<LineBoxFragmentCoordinate> containing_line_box_fragment;

        void add_floating_descendant(Box const& box) { m_floating_descendants.set(&box); }
        auto const& floating_descendants() const { return m_floating_descendants; }

    private:
        AvailableSize available_width_inside() const;
        AvailableSize available_height_inside() const;

        Layout::NodeWithStyleAndBoxModelMetrics* m_node { nullptr };

        float m_content_width { 0 };
        float m_content_height { 0 };

        bool m_has_definite_width { false };
        bool m_has_definite_height { false };

        HashTable<Box const*> m_floating_descendants;
    };

    float resolved_definite_width(Box const&) const;
    float resolved_definite_height(Box const&) const;

    void commit();

    // NOTE: get_mutable() will CoW the UsedValues if it's inherited from an ancestor state;
    UsedValues& get_mutable(NodeWithStyleAndBoxModelMetrics const&);

    // NOTE: get() will not CoW the UsedValues.
    UsedValues const& get(NodeWithStyleAndBoxModelMetrics const&) const;

    Vector<OwnPtr<UsedValues>> used_values_per_layout_node;

    // We cache intrinsic sizes once determined, as they will not change over the course of a full layout.
    // This avoids computing them several times while performing flex layout.
    struct IntrinsicSizes {
        Optional<float> min_content_width;
        Optional<float> max_content_width;
        Optional<float> min_content_height;
        Optional<float> max_content_height;
    };

    HashMap<NodeWithStyleAndBoxModelMetrics const*, NonnullOwnPtr<IntrinsicSizes>> mutable intrinsic_sizes;

    LayoutState const* m_parent { nullptr };
    LayoutState const& m_root;
};

Gfx::FloatRect absolute_content_rect(Box const&, LayoutState const&);
Gfx::FloatRect margin_box_rect(Box const&, LayoutState const&);
Gfx::FloatRect margin_box_rect_in_ancestor_coordinate_space(Box const& box, Box const& ancestor_box, LayoutState const&);
Gfx::FloatRect border_box_rect(Box const&, LayoutState const&);
Gfx::FloatRect border_box_rect_in_ancestor_coordinate_space(Box const& box, Box const& ancestor_box, LayoutState const&);
Gfx::FloatRect content_box_rect(Box const&, LayoutState const&);
Gfx::FloatRect content_box_rect_in_ancestor_coordinate_space(Box const& box, Box const& ancestor_box, LayoutState const&);

}
