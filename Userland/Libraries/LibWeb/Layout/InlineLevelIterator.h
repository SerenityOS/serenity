/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <LibWeb/Layout/BlockContainer.h>
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
        Layout::Node* node { nullptr };
        size_t offset_in_node { 0 };
        size_t length_in_node { 0 };
        float width { 0.0f };
        bool should_force_break { false };
        bool is_collapsible_whitespace { false };
    };

    explicit InlineLevelIterator(Layout::BlockContainer& container, LayoutMode layout_mode)
        : m_container(container)
        , m_current_node(container.first_child())
        , m_layout_mode(layout_mode)
    {
    }

    Optional<Item> next(float available_width);

private:
    void skip_to_next();

    void enter_text_node(Layout::TextNode&, bool previous_is_empty_or_ends_in_whitespace);

    Layout::BlockContainer& m_container;
    Layout::Node* m_current_node { nullptr };
    LayoutMode const m_layout_mode;

    struct TextNodeContext {
        bool do_collapse {};
        bool do_wrap_lines {};
        bool do_respect_linebreaks {};
        TextNode::ChunkIterator chunk_iterator;
    };

    Optional<TextNodeContext> m_text_node_context;
};

}
