/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Position.h>
#include <LibWeb/DOM/Range.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Layout/LayoutPosition.h>
#include <LibWeb/Page/EditEventHandler.h>

namespace Web {

void EditEventHandler::handle_delete_character_after(const DOM::Position& cursor_position)
{
    if (cursor_position.offset_is_at_end_of_node()) {
        // FIXME: Move to the next node and delete the first character there.
        return;
    }

    auto& node = *static_cast<DOM::Text*>(const_cast<DOM::Node*>(cursor_position.node()));
    auto& text = node.data();
    auto code_point_length = Utf8View(text).iterator_at_byte_offset(cursor_position.offset()).underlying_code_point_length_in_bytes();

    StringBuilder builder;
    builder.append(text.substring_view(0, cursor_position.offset()));
    builder.append(text.substring_view(cursor_position.offset() + code_point_length));
    node.set_data(builder.to_string());

    // FIXME: When nodes are removed from the DOM, the associated layout nodes become stale and still
    //        remain in the layout tree. This has to be fixed, this just causes everything to be recomputed
    //        which really hurts performance.
    m_browsing_context.active_document()->force_layout();

    m_browsing_context.did_edit({});
}

// This method is quite convoluted but this is necessary to make editing feel intuitive.
void EditEventHandler::handle_delete(DOM::Range& range)
{
    auto* start = verify_cast<DOM::Text>(range.start_container());
    auto* end = verify_cast<DOM::Text>(range.end_container());

    if (start == end) {
        StringBuilder builder;
        builder.append(start->data().substring_view(0, range.start_offset()));
        builder.append(end->data().substring_view(range.end_offset()));

        start->set_data(builder.to_string());
    } else {
        // Remove all the nodes that are fully enclosed in the range.
        HashTable<DOM::Node*> queued_for_deletion;
        for (auto* node = start->next_in_pre_order(); node; node = node->next_in_pre_order()) {
            if (node == end)
                break;

            queued_for_deletion.set(node);
        }
        for (auto* parent = start->parent(); parent; parent = parent->parent())
            queued_for_deletion.remove(parent);
        for (auto* parent = end->parent(); parent; parent = parent->parent())
            queued_for_deletion.remove(parent);
        for (auto* node : queued_for_deletion)
            node->remove();

        // Join the parent nodes of start and end.
        DOM::Node *insert_after = start, *remove_from = end, *parent_of_end = end->parent();
        while (remove_from) {
            auto* next_sibling = remove_from->next_sibling();

            remove_from->remove();
            insert_after->parent()->insert_before(*remove_from, *insert_after);

            insert_after = remove_from;
            remove_from = next_sibling;
        }
        if (!parent_of_end->has_children()) {
            if (parent_of_end->parent())
                parent_of_end->remove();
        }

        // Join the start and end nodes.
        StringBuilder builder;
        builder.append(start->data().substring_view(0, range.start_offset()));
        builder.append(end->data().substring_view(range.end_offset()));

        start->set_data(builder.to_string());
        end->remove();
    }

    // FIXME: When nodes are removed from the DOM, the associated layout nodes become stale and still
    //        remain in the layout tree. This has to be fixed, this just causes everything to be recomputed
    //        which really hurts performance.
    m_browsing_context.active_document()->force_layout();

    m_browsing_context.did_edit({});
}

void EditEventHandler::handle_insert(DOM::Position position, u32 code_point)
{
    if (is<DOM::Text>(*position.node())) {
        auto& node = verify_cast<DOM::Text>(*position.node());

        StringBuilder builder;
        builder.append(node.data().substring_view(0, position.offset()));
        builder.append_code_point(code_point);
        builder.append(node.data().substring_view(position.offset()));
        node.set_data(builder.to_string());

        node.invalidate_style();
    }

    // FIXME: When nodes are removed from the DOM, the associated layout nodes become stale and still
    //        remain in the layout tree. This has to be fixed, this just causes everything to be recomputed
    //        which really hurts performance.
    m_browsing_context.active_document()->force_layout();

    m_browsing_context.did_edit({});
}
}
