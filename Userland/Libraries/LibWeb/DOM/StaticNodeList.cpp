/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Error.h>
#include <LibWeb/DOM/StaticNodeList.h>

namespace Web::DOM {

JS::NonnullGCPtr<NodeList> StaticNodeList::create(JS::Realm& realm, Vector<JS::Handle<Node>> static_nodes)
{
    return realm.heap().allocate<StaticNodeList>(realm, realm, move(static_nodes));
}

StaticNodeList::StaticNodeList(JS::Realm& realm, Vector<JS::Handle<Node>> static_nodes)
    : NodeList(realm)
{
    for (auto& node : static_nodes)
        m_static_nodes.append(*node);
}

StaticNodeList::~StaticNodeList() = default;

void StaticNodeList::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto& node : m_static_nodes)
        visitor.visit(node);
}

// https://dom.spec.whatwg.org/#dom-nodelist-length
u32 StaticNodeList::length() const
{
    return m_static_nodes.size();
}

// https://dom.spec.whatwg.org/#dom-nodelist-item
Node const* StaticNodeList::item(u32 index) const
{
    // The item(index) method must return the indexth node in the collection. If there is no indexth node in the collection, then the method must return null.
    if (index >= m_static_nodes.size())
        return nullptr;
    return m_static_nodes[index];
}

// https://dom.spec.whatwg.org/#ref-for-dfn-supported-property-indices
bool StaticNodeList::is_supported_property_index(u32 index) const
{
    // The object’s supported property indices are the numbers in the range zero to one less than the number of nodes represented by the collection.
    // If there are no such elements, then there are no supported property indices.
    return index < m_static_nodes.size();
}

}
