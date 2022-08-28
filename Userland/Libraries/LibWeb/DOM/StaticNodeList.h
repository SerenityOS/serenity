/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/NodeList.h>

namespace Web::DOM {

class StaticNodeList : public NodeList {
public:
    static NonnullRefPtr<NodeList> create(Vector<JS::Handle<Node>> static_nodes);

    virtual ~StaticNodeList() override = default;

    virtual u32 length() const override;
    virtual Node const* item(u32 index) const override;

    virtual bool is_supported_property_index(u32) const override;

private:
    StaticNodeList(Vector<JS::Handle<Node>> static_nodes);

    Vector<JS::Handle<Node>> m_static_nodes;
};

}
