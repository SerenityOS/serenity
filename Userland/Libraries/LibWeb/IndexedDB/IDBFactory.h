/*
 * Copyright (c) 2024, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/WebIDL/Types.h>

namespace Web::IndexedDB {

// https://w3c.github.io/IndexedDB/#idbfactory
class IDBFactory : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(IDBFactory, Bindings::PlatformObject);
    JS_DECLARE_ALLOCATOR(IDBFactory);

public:
    virtual ~IDBFactory() override;

    JS::NonnullGCPtr<IDBOpenDBRequest> open(String const& name, Optional<WebIDL::UnsignedLongLong> version = {});

protected:
    explicit IDBFactory(JS::Realm&);

    virtual void initialize(JS::Realm&) override;
};

}
