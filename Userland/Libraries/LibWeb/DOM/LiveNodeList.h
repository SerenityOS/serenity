/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibWeb/DOM/NodeList.h>

namespace Web::DOM {

// FIXME: Just like HTMLCollection, LiveNodeList currently does no caching.

class LiveNodeList final : public NodeList {
    WEB_PLATFORM_OBJECT(LiveNodeList, NodeList);

public:
    static JS::NonnullGCPtr<NodeList> create(JS::Realm&, Node& root, Function<bool(Node const&)> filter);
    virtual ~LiveNodeList() override;

    virtual u32 length() const override;
    virtual Node const* item(u32 index) const override;

    virtual bool is_supported_property_index(u32) const override;

private:
    LiveNodeList(JS::Realm&, Node& root, Function<bool(Node const&)> filter);

    virtual void visit_edges(Cell::Visitor&) override;

    JS::MarkedVector<Node*> collection() const;

    JS::NonnullGCPtr<Node> m_root;
    Function<bool(Node const&)> m_filter;
};

}
