/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/IDLEventListener.h>

namespace Web::DOM {

JS::NonnullGCPtr<IDLEventListener> IDLEventListener::create(JS::Realm& realm, JS::NonnullGCPtr<WebIDL::CallbackType> callback)
{
    return realm.heap().allocate<IDLEventListener>(realm, realm, move(callback)).release_allocated_value_but_fixme_should_propagate_errors();
}

IDLEventListener::IDLEventListener(JS::Realm& realm, JS::NonnullGCPtr<WebIDL::CallbackType> callback)
    : JS::Object(ConstructWithPrototypeTag::Tag, *realm.intrinsics().object_prototype())
    , m_callback(move(callback))
{
}

void IDLEventListener::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_callback.ptr());
}

}
