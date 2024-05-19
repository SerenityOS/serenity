/*
 * Copyright (c) 2024, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/IDBRequestPrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/IndexedDB/IDBRequest.h>

namespace Web::IndexedDB {

JS_DEFINE_ALLOCATOR(IDBRequest);

IDBRequest::~IDBRequest() = default;

IDBRequest::IDBRequest(JS::Realm& realm)
    : EventTarget(realm)
{
}

void IDBRequest::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE(IDBRequest);
}

}
