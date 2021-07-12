/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class AtomicsObject : public Object {
    JS_OBJECT(AtomicsObject, Object);

public:
    explicit AtomicsObject(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~AtomicsObject() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(add);
    JS_DECLARE_NATIVE_FUNCTION(and_);
    JS_DECLARE_NATIVE_FUNCTION(compare_exchange);
    JS_DECLARE_NATIVE_FUNCTION(exchange);
    JS_DECLARE_NATIVE_FUNCTION(is_lock_free);
    JS_DECLARE_NATIVE_FUNCTION(load);
    JS_DECLARE_NATIVE_FUNCTION(or_);
    JS_DECLARE_NATIVE_FUNCTION(store);
    JS_DECLARE_NATIVE_FUNCTION(sub);
    JS_DECLARE_NATIVE_FUNCTION(xor_);
};

}
