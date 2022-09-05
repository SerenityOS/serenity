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

class StaticNodeList final : public NodeList {
    WEB_PLATFORM_OBJECT(StaticNodeList, NodeList);

public:
    static JS::NonnullGCPtr<NodeList> create(HTML::Window&, Vector<JS::Handle<Node>>);

    virtual ~StaticNodeList() override;

    virtual u32 length() const override;
    virtual Node const* item(u32 index) const override;

    virtual bool is_supported_property_index(u32) const override;

private:
    StaticNodeList(HTML::Window&, Vector<JS::Handle<Node>>);

    virtual void visit_edges(Cell::Visitor&) override;

    Vector<Node&> m_static_nodes;
};

}
