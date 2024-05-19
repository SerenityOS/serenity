/*
 * Copyright (c) 2024, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/EventTarget.h>

namespace Web::IndexedDB {

class IDBRequest : public DOM::EventTarget {
    WEB_PLATFORM_OBJECT(IDBRequest, DOM::EventTarget);
    JS_DECLARE_ALLOCATOR(IDBRequest);

public:
    virtual ~IDBRequest() override;

protected:
    explicit IDBRequest(JS::Realm&);

    virtual void initialize(JS::Realm&) override;
};

}
