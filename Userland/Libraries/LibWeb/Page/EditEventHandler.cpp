/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "EditEventHandler.h"
#include <AK/StringBuilder.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Position.h>
#include <LibWeb/DOM/Range.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/Layout/InitialContainingBlockBox.h>
#include <LibWeb/Layout/LayoutPosition.h>
#include <LibWeb/Page/Frame.h>

namespace Web {

// This method is quite convoluted but this is necessary to make editing feel intuitive.
void EditEventHandler::handle_delete(DOM::Range& range)
{
    auto* start = downcast<DOM::Text>(range.start_container());
    auto* end = downcast<DOM::Text>(range.end_container());

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
    m_frame.document()->force_layout();

    m_frame.did_edit({});
}

void EditEventHandler::handle_insert(DOM::Position position, u32 code_point)
{
    if (is<DOM::Text>(*position.node())) {
        auto& node = downcast<DOM::Text>(*position.node());

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
    m_frame.document()->force_layout();

    m_frame.did_edit({});
}
}
