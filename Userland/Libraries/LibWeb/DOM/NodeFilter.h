/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/CallbackType.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/DOM/Node.h>

namespace Web::DOM {

class NodeFilter final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(NodeFilter, Bindings::PlatformObject);

public:
    static JS::NonnullGCPtr<NodeFilter> create(JS::Realm&, Bindings::CallbackType&);

    virtual ~NodeFilter() = default;

    Bindings::CallbackType& callback() { return m_callback; }

    enum Result {
        FILTER_ACCEPT = 1,
        FILTER_REJECT = 2,
        FILTER_SKIP = 3,
    };

    NodeFilter::Result accept_node(Node&) { return NodeFilter::FILTER_ACCEPT; }

private:
    NodeFilter(JS::Realm&, Bindings::CallbackType&);

    virtual void visit_edges(Cell::Visitor&) override;

    Bindings::CallbackType& m_callback;
};

}

WRAPPER_HACK(NodeFilter, Web::DOM)
