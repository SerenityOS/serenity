/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/BreakNode.h>
#include <LibWeb/Layout/InlineLevelIterator.h>
#include <LibWeb/Layout/InlineNode.h>
#include <LibWeb/Layout/ListItemMarkerBox.h>
#include <LibWeb/Layout/ReplacedBox.h>

namespace Web::Layout {

// This is similar to Layout::Node::next_in_pre_order() but will not descend into inline-block nodes.
static Layout::Node* next_inline_node_in_pre_order(Layout::Node& current, Layout::Node const* stay_within)
{
    if (current.first_child() && current.first_child()->is_inline() && !current.is_inline_block())
        return current.first_child();

    Layout::Node* node = &current;
    Layout::Node* next = nullptr;
    while (!(next = node->next_sibling())) {
        node = node->parent();
        if (!node || node == stay_within)
            return nullptr;
    }

    return next;
}

void InlineLevelIterator::skip_to_next()
{
    VERIFY(m_current_node);
    do {
        m_current_node = next_inline_node_in_pre_order(*m_current_node, &m_container);
    } while (m_current_node && !m_current_node->is_inline());
}

Optional<InlineLevelIterator::Item> InlineLevelIterator::next(float available_width)
{
    if (!m_current_node)
        return {};

    if (is<Layout::TextNode>(*m_current_node)) {
        auto& text_node = static_cast<Layout::TextNode&>(*m_current_node);

        if (!m_text_node_context.has_value()) {
            bool previous_is_empty_or_ends_in_whitespace = m_container.line_boxes().is_empty() || m_container.line_boxes().last().is_empty_or_ends_in_whitespace();
            enter_text_node(text_node, previous_is_empty_or_ends_in_whitespace);
        }

        auto chunk_opt = m_text_node_context->chunk_iterator.next();
        if (!chunk_opt.has_value()) {
            m_text_node_context = {};
            skip_to_next();
            return next(available_width);
        }

        auto& chunk = chunk_opt.value();
        float chunk_width = text_node.font().width(chunk.view) + text_node.font().glyph_spacing();
        Item item {
            .type = Item::Type::Text,
            .node = &text_node,
            .offset_in_node = chunk.start,
            .length_in_node = chunk.length,
            .width = chunk_width,
            .should_force_break = m_text_node_context->do_respect_linebreaks && chunk.has_breaking_newline,
            .is_collapsible_whitespace = m_text_node_context->do_collapse && chunk.is_all_whitespace,
        };

        return item;
    }

    if (is<Layout::BreakNode>(*m_current_node)) {
        skip_to_next();
        return Item {
            .type = Item::Type::ForcedBreak,
        };
    }

    if (is<Layout::ListItemMarkerBox>(*m_current_node)) {
        skip_to_next();
        return next(available_width);
    }

    if (!is<Layout::Box>(*m_current_node)) {
        skip_to_next();
        return next(available_width);
    }

    if (is<Layout::ReplacedBox>(*m_current_node)) {
        auto& replaced_box = static_cast<Layout::ReplacedBox&>(*m_current_node);
        replaced_box.prepare_for_replaced_layout();
    }

    auto& box = verify_cast<Layout::Box>(*m_current_node);

    skip_to_next();
    return Item {
        .type = Item::Type::Element,
        .node = &box,
        .offset_in_node = 0,
        .length_in_node = 0,
        .width = box.content_width(),
    };
}

void InlineLevelIterator::enter_text_node(Layout::TextNode& text_node, bool previous_is_empty_or_ends_in_whitespace)
{
    bool do_collapse = true;
    bool do_wrap_lines = true;
    bool do_respect_linebreaks = false;

    if (text_node.computed_values().white_space() == CSS::WhiteSpace::Nowrap) {
        do_collapse = true;
        do_wrap_lines = false;
        do_respect_linebreaks = false;
    } else if (text_node.computed_values().white_space() == CSS::WhiteSpace::Pre) {
        do_collapse = false;
        do_wrap_lines = false;
        do_respect_linebreaks = true;
    } else if (text_node.computed_values().white_space() == CSS::WhiteSpace::PreLine) {
        do_collapse = true;
        do_wrap_lines = true;
        do_respect_linebreaks = true;
    } else if (text_node.computed_values().white_space() == CSS::WhiteSpace::PreWrap) {
        do_collapse = false;
        do_wrap_lines = true;
        do_respect_linebreaks = true;
    }

    text_node.compute_text_for_rendering(do_collapse, previous_is_empty_or_ends_in_whitespace);

    m_text_node_context = TextNodeContext {
        .do_collapse = do_collapse,
        .do_wrap_lines = do_wrap_lines,
        .do_respect_linebreaks = do_respect_linebreaks,
        .chunk_iterator = TextNode::ChunkIterator { text_node.text_for_rendering(), m_layout_mode, do_wrap_lines, do_respect_linebreaks },
    };
}

}
