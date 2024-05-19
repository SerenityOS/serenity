/*
 * Copyright (c) 2024, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/IDBFactoryPrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/IndexedDB/IDBFactory.h>
#include <LibWeb/IndexedDB/IDBOpenDBRequest.h>

namespace Web::IndexedDB {

JS_DEFINE_ALLOCATOR(IDBFactory);

IDBFactory::IDBFactory(JS::Realm& realm)
    : Bindings::PlatformObject(realm)
{
}

IDBFactory::~IDBFactory() = default;

void IDBFactory::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE(IDBFactory);
}

// https://w3c.github.io/IndexedDB/#dom-idbfactory-open
JS::NonnullGCPtr<IDBOpenDBRequest> IDBFactory::open(String const&, Optional<WebIDL::UnsignedLongLong>)
{
    dbgln("FIXME: Implement IDBFactory::open");
    auto& realm = this->realm();
    return vm().heap().allocate<IDBOpenDBRequest>(realm, realm);
}

}
