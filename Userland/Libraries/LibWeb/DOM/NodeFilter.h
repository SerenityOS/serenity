/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/CallbackType.h>
#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::DOM {

class NodeFilter final : public Bindings::PlatformObject {
    JS_OBJECT(NodeFilter, Bindings::PlatformObject);

public:
    static JS::NonnullGCPtr<NodeFilter> create(JS::Realm&, Bindings::CallbackType&);
    NodeFilter(JS::Realm&, Bindings::CallbackType&);

    virtual ~NodeFilter() = default;

    NodeFilter& impl() { return *this; }

    Bindings::CallbackType& callback() { return m_callback; }

    enum Result {
        FILTER_ACCEPT = 1,
        FILTER_REJECT = 2,
        FILTER_SKIP = 3,
    };

private:
    virtual void visit_edges(Cell::Visitor&) override;

    Bindings::CallbackType& m_callback;
};

inline JS::Object* wrap(JS::Realm&, Web::DOM::NodeFilter& filter)
{
    return &filter.callback().callback;
}

}
