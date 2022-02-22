/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/InlineNode.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::Layout {

// This class iterates over all the inline-level objects within an inline formatting context.
// By repeatedly calling next() with the remaining available width on the current line,
// it returns an "Item" representing the next piece of inline-level content to be placed on the line.
class InlineLevelIterator {
    AK_MAKE_NONCOPYABLE(InlineLevelIterator);
    AK_MAKE_NONMOVABLE(InlineLevelIterator);

public:
    struct Item {
        enum class Type {
            Text,
            Element,
            ForcedBreak,
        };
        Type type {};
        Layout::Node const* node { nullptr };
        size_t offset_in_node { 0 };
        size_t length_in_node { 0 };
        float width { 0.0f };
        float padding_start { 0.0f };
        float padding_end { 0.0f };
        float border_start { 0.0f };
        float border_end { 0.0f };
        float margin_start { 0.0f };
        float margin_end { 0.0f };
        bool should_force_break { false };
        bool is_collapsible_whitespace { false };

        float border_box_width() const
        {
            return border_start + padding_start + width + padding_end + border_end;
        }
    };

    InlineLevelIterator(Layout::InlineFormattingContext&, FormattingState&, Layout::BlockContainer const&, LayoutMode);

    Optional<Item> next(float available_width);

private:
    void skip_to_next();
    void compute_next();

    void enter_text_node(Layout::TextNode const&, bool previous_is_empty_or_ends_in_whitespace);

    void enter_node_with_box_model_metrics(Layout::NodeWithStyleAndBoxModelMetrics const&);
    void exit_node_with_box_model_metrics();

    void add_extra_box_model_metrics_to_item(Item&, bool add_leading_metrics, bool add_trailing_metrics);

    Layout::Node const* next_inline_node_in_pre_order(Layout::Node const& current, Layout::Node const* stay_within);

    Layout::InlineFormattingContext& m_inline_formatting_context;
    Layout::FormattingState& m_formatting_state;
    Layout::BlockContainer const& m_container;
    Layout::Node const* m_current_node { nullptr };
    Layout::Node const* m_next_node { nullptr };
    LayoutMode const m_layout_mode;

    struct TextNodeContext {
        bool do_collapse {};
        bool do_wrap_lines {};
        bool do_respect_linebreaks {};
        bool is_first_chunk {};
        bool is_last_chunk {};
        TextNode::ChunkIterator chunk_iterator;
        Optional<TextNode::Chunk> next_chunk {};
    };

    Optional<TextNodeContext> m_text_node_context;

    struct ExtraBoxMetrics {
        float margin { 0 };
        float border { 0 };
        float padding { 0 };
    };

    Optional<ExtraBoxMetrics> m_extra_leading_metrics;
    Optional<ExtraBoxMetrics> m_extra_trailing_metrics;

    Vector<NodeWithStyleAndBoxModelMetrics const&> m_box_model_node_stack;
};

}
