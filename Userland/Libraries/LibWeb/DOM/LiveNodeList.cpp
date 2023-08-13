/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Error.h>
#include <LibWeb/DOM/LiveNodeList.h>
#include <LibWeb/DOM/Node.h>

namespace Web::DOM {

JS::NonnullGCPtr<NodeList> LiveNodeList::create(JS::Realm& realm, Node& root, Scope scope, Function<bool(Node const&)> filter)
{
    return realm.heap().allocate<LiveNodeList>(realm, realm, root, scope, move(filter));
}

LiveNodeList::LiveNodeList(JS::Realm& realm, Node& root, Scope scope, Function<bool(Node const&)> filter)
    : NodeList(realm)
    , m_root(root)
    , m_filter(move(filter))
    , m_scope(scope)
{
}

LiveNodeList::~LiveNodeList() = default;

void LiveNodeList::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_root.ptr());
}

JS::MarkedVector<Node*> LiveNodeList::collection() const
{
    JS::MarkedVector<Node*> nodes(heap());
    if (m_scope == Scope::Descendants) {
        m_root->for_each_in_subtree([&](auto& node) {
            if (m_filter(node))
                nodes.append(const_cast<Node*>(&node));
            return IterationDecision::Continue;
        });
    } else {
        m_root->for_each_child([&](auto& node) {
            if (m_filter(node))
                nodes.append(const_cast<Node*>(&node));
            return IterationDecision::Continue;
        });
    }
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
    return nodes[index];
}

// https://dom.spec.whatwg.org/#ref-for-dfn-supported-property-indices
bool LiveNodeList::is_supported_property_index(u32 index) const
{
    // The object’s supported property indices are the numbers in the range zero to one less than the number of nodes represented by the collection.
    // If there are no such elements, then there are no supported property indices.
    return index < length();
}

}
