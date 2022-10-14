/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/WebIDL/CallbackType.h>

namespace Web::DOM {

class NodeFilter final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(NodeFilter, Bindings::PlatformObject);

public:
    static JS::NonnullGCPtr<NodeFilter> create(JS::Realm&, WebIDL::CallbackType&);

    virtual ~NodeFilter() = default;

    WebIDL::CallbackType& callback() { return m_callback; }

    enum Result {
        FILTER_ACCEPT = 1,
        FILTER_REJECT = 2,
        FILTER_SKIP = 3,
    };

private:
    NodeFilter(JS::Realm&, WebIDL::CallbackType&);

    virtual void visit_edges(Cell::Visitor&) override;

    WebIDL::CallbackType& m_callback;
};

}
