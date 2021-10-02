/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/LiveNodeList.h>
#include <LibWeb/DOM/Node.h>

namespace Web::DOM {

LiveNodeList::LiveNodeList(Node& root, Function<bool(Node const&)> filter)
    : m_root(root)
    , m_filter(move(filter))
{
}

NonnullRefPtrVector<Node> LiveNodeList::collection() const
{
    NonnullRefPtrVector<Node> nodes;
    m_root->for_each_in_inclusive_subtree_of_type<Node>([&](auto& node) {
        if (m_filter(node))
            nodes.append(node);

        return IterationDecision::Continue;
    });
    return nodes;
}

// https://dom.spec.whatwg.org/#dom-nodelist-length
u32 LiveNodeList::length() const
{
    return collection().size();
}

// https://dom.spec.whatwg.org/#dom-nodelist-item
Node const* LiveNodeList::item(u32 index) const
{
    // The item(index) method must return the indexth node in the collection. If there is no indexth node in the collection, then the method must return null.
    auto nodes = collection();
    if (index >= nodes.size())
        return nullptr;
    return &nodes[index];
}

// https://dom.spec.whatwg.org/#ref-for-dfn-supported-property-indices
bool LiveNodeList::is_supported_property_index(u32 index) const
{
    // The object’s supported property indices are the numbers in the range zero to one less than the number of nodes represented by the collection.
    // If there are no such elements, then there are no supported property indices.
    return index < length();
}

}
