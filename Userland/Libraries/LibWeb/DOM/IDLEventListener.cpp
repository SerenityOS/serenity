/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/IDLEventListener.h>

namespace Web::DOM {

WebIDL::ExceptionOr<JS::NonnullGCPtr<IDLEventListener>> IDLEventListener::create(JS::Realm& realm, JS::NonnullGCPtr<WebIDL::CallbackType> callback)
{
    return MUST_OR_THROW_OOM(realm.heap().allocate<IDLEventListener>(realm, realm, move(callback)));
}

IDLEventListener::IDLEventListener(JS::Realm& realm, JS::NonnullGCPtr<WebIDL::CallbackType> callback)
    : JS::Object(ConstructWithPrototypeTag::Tag, realm.intrinsics().object_prototype())
    , m_callback(move(callback))
{
}

void IDLEventListener::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_callback.ptr());
}

}
