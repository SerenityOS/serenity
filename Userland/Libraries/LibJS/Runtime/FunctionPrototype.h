/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class FunctionPrototype final : public Object {
    JS_OBJECT(FunctionPrototype, Object);

public:
    explicit FunctionPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~FunctionPrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(apply);
    JS_DECLARE_NATIVE_FUNCTION(bind);
    JS_DECLARE_NATIVE_FUNCTION(call);
    JS_DECLARE_NATIVE_FUNCTION(to_string);
    JS_DECLARE_NATIVE_FUNCTION(symbol_has_instance);
};

}
