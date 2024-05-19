/*
 * Copyright (c) 2024, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/IDBOpenDBRequestPrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/IndexedDB/IDBOpenDBRequest.h>

namespace Web::IndexedDB {

JS_DEFINE_ALLOCATOR(IDBOpenDBRequest);

IDBOpenDBRequest::~IDBOpenDBRequest() = default;

IDBOpenDBRequest::IDBOpenDBRequest(JS::Realm& realm)
    : IDBRequest(realm)
{
}

void IDBOpenDBRequest::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE(IDBOpenDBRequest);
}

}
