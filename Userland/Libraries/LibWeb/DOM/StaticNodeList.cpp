/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/StaticNodeList.h>

namespace Web::DOM {

StaticNodeList::StaticNodeList(NonnullRefPtrVector<Node>&& static_nodes)
    : m_static_nodes(move(static_nodes))
{
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
    return &m_static_nodes[index];
}

// https://dom.spec.whatwg.org/#ref-for-dfn-supported-property-indices
bool StaticNodeList::is_supported_property_index(u32 index) const
{
    // The object’s supported property indices are the numbers in the range zero to one less than the number of nodes represented by the collection.
    // If there are no such elements, then there are no supported property indices.
    return index < m_static_nodes.size();
}

}
