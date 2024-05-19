/*
 * Copyright (c) 2024, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/IndexedDB/IDBRequest.h>

namespace Web::IndexedDB {

// https://w3c.github.io/IndexedDB/#idbopendbrequest
class IDBOpenDBRequest : public IDBRequest {
    WEB_PLATFORM_OBJECT(IDBOpenDBRequest, IDBRequest);
    JS_DECLARE_ALLOCATOR(IDBOpenDBRequest);

public:
    virtual ~IDBOpenDBRequest();

protected:
    explicit IDBOpenDBRequest(JS::Realm&);

    virtual void initialize(JS::Realm&) override;
};

}
