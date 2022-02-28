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

namespace Web::Layout {

struct FormattingState {
    struct NodeState {
        float content_width { 0 };
        float content_height { 0 };
        Gfx::FloatPoint offset;

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

        float offset_left { 0 };
        float offset_right { 0 };
        float offset_top { 0 };
        float offset_bottom { 0 };

        Vector<LineBox> line_boxes;

        float margin_box_left() const { return margin_left + border_left + padding_left; }
        float margin_box_right() const { return margin_right + border_right + padding_right; }
        float margin_box_top() const { return margin_top + border_top + padding_top; }
        float margin_box_bottom() const { return margin_bottom + border_bottom + padding_bottom; }

        float border_box_left() const { return border_left + padding_left; }
        float border_box_right() const { return border_right + padding_right; }
        float border_box_top() const { return border_top + padding_top; }
        float border_box_bottom() const { return border_bottom + padding_bottom; }

        float border_box_width() const { return border_box_left() + content_width + border_box_right(); }
        float border_box_height() const { return border_box_top() + content_height + border_box_bottom(); }

        Optional<Layout::Box::OverflowData> overflow_data;

        Layout::Box::OverflowData& ensure_overflow_data()
        {
            if (!overflow_data.has_value())
                overflow_data = Layout::Box::OverflowData {};
            return *overflow_data;
        }

        Optional<LineBoxFragmentCoordinate> containing_line_box_fragment;

        // NOTE: NodeState is ref-counted and accessed via copy-on-write helpers below.
        size_t ref_count { 1 };
        void ref()
        {
            VERIFY(ref_count);
            ++ref_count;
        }
        void unref()
        {
            VERIFY(ref_count);
            if (!--ref_count)
                delete this;
        }
    };

    void commit();

    // NOTE: get_mutable() will CoW the NodeState if it's shared with another FormattingContext.
    NodeState& get_mutable(NodeWithStyleAndBoxModelMetrics const&);

    // NOTE: get() will not CoW the NodeState.
    NodeState const& get(NodeWithStyleAndBoxModelMetrics const&) const;

    HashMap<NodeWithStyleAndBoxModelMetrics const*, NonnullRefPtr<NodeState>> nodes;
};

Gfx::FloatRect absolute_content_rect(Box const&, FormattingState const&);
Gfx::FloatRect margin_box_rect(Box const&, FormattingState const&);
Gfx::FloatRect margin_box_rect_in_ancestor_coordinate_space(Box const& box, Box const& ancestor_box, FormattingState const&);

}
