/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Range.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::DOM {

Text::Text(Document& document, String const& data)
    : CharacterData(document, NodeType::TEXT_NODE, data)
{
}

// https://dom.spec.whatwg.org/#dom-text-text
NonnullRefPtr<Text> Text::create_with_global_object(Bindings::WindowObject& window, String const& data)
{
    return make_ref_counted<Text>(window.impl().associated_document(), data);
}

void Text::set_owner_input_element(Badge<HTML::HTMLInputElement>, HTML::HTMLInputElement& input_element)
{
    m_owner_input_element = input_element;
}

// https://dom.spec.whatwg.org/#dom-text-splittext
// https://dom.spec.whatwg.org/#concept-text-split
ExceptionOr<NonnullRefPtr<Text>> Text::split_text(size_t offset)
{
    // 1. Let length be node’s length.
    auto length = this->length();

    // 2. If offset is greater than length, then throw an "IndexSizeError" DOMException.
    if (offset > length)
        return DOM::IndexSizeError::create("Split offset is greater than length");

    // 3. Let count be length minus offset.
    auto count = length - offset;

    // 4. Let new data be the result of substringing data with node node, offset offset, and count count.
    auto new_data = TRY(substring_data(offset, count));

    // 5. Let new node be a new Text node, with the same node document as node. Set new node’s data to new data.
    auto new_node = adopt_ref(*new Text(document(), new_data));

    // 6. Let parent be node’s parent.
    RefPtr<Node> parent = this->parent();

    // 7. If parent is not null, then:
    if (parent) {
        // 1. Insert new node into parent before node’s next sibling.
        parent->insert_before(new_node, next_sibling());

        // 2. For each live range whose start node is node and start offset is greater than offset, set its start node to new node and decrease its start offset by offset.
        for (auto& range : Range::live_ranges()) {
            if (range->start_container() == this && range->start_offset() > offset)
                range->set_start(new_node, range->start_offset() - offset);
        }

        // 3. For each live range whose end node is node and end offset is greater than offset, set its end node to new node and decrease its end offset by offset.
        for (auto& range : Range::live_ranges()) {
            if (range->end_container() == this && range->end_offset() > offset)
                range->set_end(new_node, range->end_offset() - offset);
        }

        // 4. For each live range whose start node is parent and start offset is equal to the index of node plus 1, increase its start offset by 1.
        for (auto& range : Range::live_ranges()) {
            if (range->start_container() == this && range->start_offset() == index() + 1)
                range->set_start(*range->start_container(), range->start_offset() + 1);
        }

        // 5. For each live range whose end node is parent and end offset is equal to the index of node plus 1, increase its end offset by 1.
        for (auto& range : Range::live_ranges()) {
            if (range->end_container() == parent && range->end_offset() == index() + 1) {
                range->set_end(*range->end_container(), range->end_offset() + 1);
            }
        }
    }

    // 8. Replace data with node node, offset offset, count count, and data the empty string.
    TRY(replace_data(offset, count, ""));

    // 9. Return new node.
    return new_node;
}

}
