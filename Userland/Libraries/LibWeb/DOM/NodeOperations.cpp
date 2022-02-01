/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibWeb/DOM/DocumentFragment.h>
#include <LibWeb/DOM/NodeOperations.h>
#include <LibWeb/DOM/Text.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#converting-nodes-into-a-node
ExceptionOr<NonnullRefPtr<Node>> convert_nodes_to_single_node(Vector<Variant<NonnullRefPtr<Node>, String>> const& nodes, DOM::Document& document)
{
    // 1. Let node be null.
    // 2. Replace each string in nodes with a new Text node whose data is the string and node document is document.
    // 3. If nodes contains one node, then set node to nodes[0].
    // 4. Otherwise, set node to a new DocumentFragment node whose node document is document, and then append each node in nodes, if any, to it.
    // 5. Return node.

    auto potentially_convert_string_to_text_node = [&document](Variant<NonnullRefPtr<Node>, String> const& node) -> NonnullRefPtr<Node> {
        if (node.has<NonnullRefPtr<Node>>())
            return node.get<NonnullRefPtr<Node>>();

        return adopt_ref(*new Text(document, node.get<String>()));
    };

    if (nodes.size() == 1)
        return potentially_convert_string_to_text_node(nodes.first());

    // This is NNRP<Node> instead of NNRP<DocumentFragment> to be compatible with the return type.
    NonnullRefPtr<Node> document_fragment = adopt_ref(*new DocumentFragment(document));
    for (auto& unconverted_node : nodes) {
        auto node = potentially_convert_string_to_text_node(unconverted_node);
        auto result = document_fragment->append_child(node);
        if (result.is_exception())
            return result.exception();
    }

    return document_fragment;
}

}
