/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/NodeList.h>

namespace Web::DOM {

class StaticNodeList final : public NodeList {
    WEB_PLATFORM_OBJECT(StaticNodeList, NodeList);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<NodeList>> create(JS::Realm&, Vector<JS::Handle<Node>>);

    virtual ~StaticNodeList() override;

    virtual u32 length() const override;
    virtual Node const* item(u32 index) const override;

    virtual bool is_supported_property_index(u32) const override;

private:
    StaticNodeList(JS::Realm&, Vector<JS::Handle<Node>>);

    virtual void visit_edges(Cell::Visitor&) override;

    Vector<JS::NonnullGCPtr<Node>> m_static_nodes;
};

}
