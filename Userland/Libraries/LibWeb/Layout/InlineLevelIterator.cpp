/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/BreakNode.h>
#include <LibWeb/Layout/InlineFormattingContext.h>
#include <LibWeb/Layout/InlineLevelIterator.h>
#include <LibWeb/Layout/InlineNode.h>
#include <LibWeb/Layout/ListItemMarkerBox.h>
#include <LibWeb/Layout/ReplacedBox.h>

namespace Web::Layout {

InlineLevelIterator::InlineLevelIterator(Layout::InlineFormattingContext& inline_formatting_context, Layout::FormattingState& formatting_state, Layout::BlockContainer const& container, LayoutMode layout_mode)
    : m_inline_formatting_context(inline_formatting_context)
    , m_formatting_state(formatting_state)
    , m_container(container)
    , m_next_node(container.first_child())
    , m_layout_mode(layout_mode)
{
    skip_to_next();
}

void InlineLevelIterator::enter_node_with_box_model_metrics(Layout::NodeWithStyleAndBoxModelMetrics const& node)
{
    if (!m_extra_leading_metrics.has_value())
        m_extra_leading_metrics = ExtraBoxMetrics {};

    // FIXME: It's really weird that *this* is where we assign box model metrics for these layout nodes..

    auto& node_state = m_formatting_state.get_mutable(node);
    auto const& container_state = m_formatting_state.get(m_container);
    auto const& computed_values = node.computed_values();

    node_state.margin_left = computed_values.margin().left.resolved(node, CSS::Length::make_px(container_state.content_width)).to_px(node);
    node_state.border_left = computed_values.border_left().width;
    node_state.padding_left = computed_values.padding().left.resolved(node, CSS::Length::make_px(container_state.content_width)).to_px(node);

    m_extra_leading_metrics->margin += node_state.margin_left;
    m_extra_leading_metrics->border += node_state.border_left;
    m_extra_leading_metrics->padding += node_state.padding_left;

    m_box_model_node_stack.append(node);
}

void InlineLevelIterator::exit_node_with_box_model_metrics()
{
    if (!m_extra_trailing_metrics.has_value())
        m_extra_trailing_metrics = ExtraBoxMetrics {};

    auto& node = m_box_model_node_stack.last();
    auto& node_state = m_formatting_state.get_mutable(node);
    auto const& container_state = m_formatting_state.get(m_container);
    auto const& computed_values = node.computed_values();

    node_state.margin_right = computed_values.margin().right.resolved(node, CSS::Length::make_px(container_state.content_width)).to_px(node);
    node_state.border_right = computed_values.border_right().width;
    node_state.padding_right = computed_values.padding().right.resolved(node, CSS::Length::make_px(container_state.content_width)).to_px(node);

    m_extra_trailing_metrics->margin += node_state.margin_right;
    m_extra_trailing_metrics->border += node_state.border_right;
    m_extra_trailing_metrics->padding += node_state.padding_right;

    m_box_model_node_stack.take_last();
}

// This is similar to Layout::Node::next_in_pre_order() but will not descend into inline-block nodes.
Layout::Node const* InlineLevelIterator::next_inline_node_in_pre_order(Layout::Node const& current, Layout::Node const* stay_within)
{
    if (current.first_child() && current.first_child()->is_inline() && !current.is_inline_block())
        return current.first_child();

    Layout::Node const* node = &current;
    Layout::Node const* next = nullptr;
    while (!(next = node->next_sibling())) {
        node = node->parent();

        // If node is the last node on the "box model node stack", pop it off.
        if (!m_box_model_node_stack.is_empty()
            && &m_box_model_node_stack.last() == node) {
            exit_node_with_box_model_metrics();
        }
        if (!node || node == stay_within)
            return nullptr;
    }

    // If node is the last node on the "box model node stack", pop it off.
    if (!m_box_model_node_stack.is_empty()
        && &m_box_model_node_stack.last() == node) {
        exit_node_with_box_model_metrics();
    }

    return next;
}

void InlineLevelIterator::compute_next()
{
    if (m_next_node == nullptr)
        return;
    do {
        m_next_node = next_inline_node_in_pre_order(*m_next_node, &m_container);
    } while (m_next_node && !m_next_node->is_inline());
}

void InlineLevelIterator::skip_to_next()
{
    if (m_next_node && is<Layout::NodeWithStyleAndBoxModelMetrics>(*m_next_node) && !m_next_node->is_inline_block())
        enter_node_with_box_model_metrics(static_cast<Layout::NodeWithStyleAndBoxModelMetrics const&>(*m_next_node));

    m_current_node = m_next_node;
    compute_next();
}

Optional<InlineLevelIterator::Item> InlineLevelIterator::next(float available_width)
{
    if (!m_current_node)
        return {};

    if (is<Layout::TextNode>(*m_current_node)) {
        auto& text_node = static_cast<Layout::TextNode const&>(*m_current_node);

        if (!m_text_node_context.has_value()) {
            auto& line_boxes = m_formatting_state.get(m_container).line_boxes;
            bool previous_is_empty_or_ends_in_whitespace = line_boxes.is_empty() || line_boxes.last().is_empty_or_ends_in_whitespace();
            enter_text_node(text_node, previous_is_empty_or_ends_in_whitespace);
        }

        auto chunk_opt = m_text_node_context->next_chunk;
        if (!chunk_opt.has_value()) {
            m_text_node_context = {};
            skip_to_next();
            return next(available_width);
        }

        m_text_node_context->next_chunk = m_text_node_context->chunk_iterator.next();
        if (!m_text_node_context->next_chunk.has_value())
            m_text_node_context->is_last_chunk = true;

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

        add_extra_box_model_metrics_to_item(item, m_text_node_context->is_first_chunk, m_text_node_context->is_last_chunk);
        return item;
    }

    if (m_current_node->is_positioned()) {
        skip_to_next();
        return next(available_width);
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
        auto& replaced_box = static_cast<Layout::ReplacedBox const&>(*m_current_node);
        // FIXME: This const_cast is gross.
        const_cast<Layout::ReplacedBox&>(replaced_box).prepare_for_replaced_layout();
    }

    auto& box = verify_cast<Layout::Box>(*m_current_node);
    auto& box_state = m_formatting_state.get(box);
    m_inline_formatting_context.dimension_box_on_line(box, m_layout_mode);

    skip_to_next();
    auto item = Item {
        .type = Item::Type::Element,
        .node = &box,
        .offset_in_node = 0,
        .length_in_node = 0,
        .width = box_state.content_width,
        .padding_start = box_state.padding_left,
        .padding_end = box_state.padding_right,
        .border_start = box_state.border_left,
        .border_end = box_state.border_right,
        .margin_start = box_state.margin_left,
        .margin_end = box_state.margin_right,
    };
    add_extra_box_model_metrics_to_item(item, true, true);
    return item;
}

void InlineLevelIterator::enter_text_node(Layout::TextNode const& text_node, bool previous_is_empty_or_ends_in_whitespace)
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

    // FIXME: The const_cast here is gross.
    const_cast<TextNode&>(text_node).compute_text_for_rendering(do_collapse, previous_is_empty_or_ends_in_whitespace);

    m_text_node_context = TextNodeContext {
        .do_collapse = do_collapse,
        .do_wrap_lines = do_wrap_lines,
        .do_respect_linebreaks = do_respect_linebreaks,
        .is_first_chunk = true,
        .is_last_chunk = false,
        .chunk_iterator = TextNode::ChunkIterator { text_node.text_for_rendering(), m_layout_mode, do_wrap_lines, do_respect_linebreaks },
    };
    m_text_node_context->next_chunk = m_text_node_context->chunk_iterator.next();
}

void InlineLevelIterator::add_extra_box_model_metrics_to_item(Item& item, bool add_leading_metrics, bool add_trailing_metrics)
{
    if (add_leading_metrics && m_extra_leading_metrics.has_value()) {
        item.margin_start += m_extra_leading_metrics->margin;
        item.border_start += m_extra_leading_metrics->border;
        item.padding_start += m_extra_leading_metrics->padding;
        m_extra_leading_metrics = {};
    }

    if (add_trailing_metrics && m_extra_trailing_metrics.has_value()) {
        item.margin_end += m_extra_trailing_metrics->margin;
        item.border_end += m_extra_trailing_metrics->border;
        item.padding_end += m_extra_trailing_metrics->padding;
        m_extra_trailing_metrics = {};
    }
}

}
