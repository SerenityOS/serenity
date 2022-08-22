/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibJS/Heap/Handle.h>
#include <LibWeb/Bindings/CallbackType.h>
#include <LibWeb/Bindings/Wrappable.h>

namespace Web::DOM {

class NodeFilter
    : public RefCounted<NodeFilter>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::NodeFilterWrapper;

    explicit NodeFilter(Bindings::CallbackType callback)
        : m_callback(move(callback))
    {
    }

    virtual ~NodeFilter() = default;

    Bindings::CallbackType& callback() { return m_callback; }

    enum Result {
        FILTER_ACCEPT = 1,
        FILTER_REJECT = 2,
        FILTER_SKIP = 3,
    };

private:
    Bindings::CallbackType m_callback;
};

inline JS::Object* wrap(JS::Realm&, Web::DOM::NodeFilter& filter)
{
    return filter.callback().callback.cell();
}

}
