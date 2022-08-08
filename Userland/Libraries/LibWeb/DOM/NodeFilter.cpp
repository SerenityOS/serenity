/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/DOM/NodeFilter.h>

namespace Web::DOM {

JS::NonnullGCPtr<NodeFilter> NodeFilter::create(JS::Realm& realm, Bindings::CallbackType& callback)
{
    return *realm.heap().allocate<NodeFilter>(realm, realm, callback);
}

NodeFilter::NodeFilter(JS::Realm& realm, Bindings::CallbackType& callback)
    : PlatformObject(*realm.intrinsics().object_prototype())
    , m_callback(callback)
{
}

void NodeFilter::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_callback);
}

}
