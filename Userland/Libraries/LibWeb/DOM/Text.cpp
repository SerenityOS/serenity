/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/TextPrototype.h>
#include <LibWeb/DOM/Range.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::DOM {

JS_DEFINE_ALLOCATOR(Text);

Text::Text(Document& document, String const& data)
    : CharacterData(document, NodeType::TEXT_NODE, data)
{
}

Text::Text(Document& document, NodeType type, String const& data)
    : CharacterData(document, type, data)
{
}

void Text::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE(Text);
}

void Text::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    SlottableMixin::visit_edges(visitor);
    visitor.visit(m_owner);
}

// https://dom.spec.whatwg.org/#dom-text-text
WebIDL::ExceptionOr<JS::NonnullGCPtr<Text>> Text::construct_impl(JS::Realm& realm, String const& data)
{
    // The new Text(data) constructor steps are to set this’s data to data and this’s node document to current global object’s associated Document.
    auto& window = verify_cast<HTML::Window>(HTML::current_global_object());
    return realm.heap().allocate<Text>(realm, window.associated_document(), data);
}

EditableTextNodeOwner* Text::editable_text_node_owner()
{
    if (!m_owner)
        return nullptr;
    EditableTextNodeOwner* owner = dynamic_cast<EditableTextNodeOwner*>(m_owner.ptr());
    VERIFY(owner);
    return owner;
}

// https://dom.spec.whatwg.org/#dom-text-splittext
// https://dom.spec.whatwg.org/#concept-text-split
WebIDL::ExceptionOr<JS::NonnullGCPtr<Text>> Text::split_text(size_t offset)
{
    // 1. Let length be node’s length.
    auto length = this->length();

    // 2. If offset is greater than length, then throw an "IndexSizeError" DOMException.
    if (offset > length)
        return WebIDL::IndexSizeError::create(realm(), "Split offset is greater than length"_fly_string);

    // 3. Let count be length minus offset.
    auto count = length - offset;

    // 4. Let new data be the result of substringing data with node node, offset offset, and count count.
    auto new_data = TRY(substring_data(offset, count));

    // 5. Let new node be a new Text node, with the same node document as node. Set new node’s data to new data.
    auto new_node = heap().allocate<Text>(realm(), document(), new_data);

    // 6. Let parent be node’s parent.
    JS::GCPtr<Node> parent = this->parent();

    // 7. If parent is not null, then:
    if (parent) {
        // 1. Insert new node into parent before node’s next sibling.
        parent->insert_before(*new_node, next_sibling());

        // 2. For each live range whose start node is node and start offset is greater than offset, set its start node to new node and decrease its start offset by offset.
        for (auto& range : Range::live_ranges()) {
            if (range->start_container() == this && range->start_offset() > offset)
                TRY(range->set_start(*new_node, range->start_offset() - offset));
        }

        // 3. For each live range whose end node is node and end offset is greater than offset, set its end node to new node and decrease its end offset by offset.
        for (auto& range : Range::live_ranges()) {
            if (range->end_container() == this && range->end_offset() > offset)
                TRY(range->set_end(*new_node, range->end_offset() - offset));
        }

        // 4. For each live range whose start node is parent and start offset is equal to the index of node plus 1, increase its start offset by 1.
        for (auto& range : Range::live_ranges()) {
            if (range->start_container() == this && range->start_offset() == index() + 1)
                TRY(range->set_start(*range->start_container(), range->start_offset() + 1));
        }

        // 5. For each live range whose end node is parent and end offset is equal to the index of node plus 1, increase its end offset by 1.
        for (auto& range : Range::live_ranges()) {
            if (range->end_container() == parent.ptr() && range->end_offset() == index() + 1) {
                TRY(range->set_end(*range->end_container(), range->end_offset() + 1));
            }
        }
    }

    // 8. Replace data with node node, offset offset, count count, and data the empty string.
    TRY(replace_data(offset, count, String {}));

    // 9. Return new node.
    return new_node;
}

}
