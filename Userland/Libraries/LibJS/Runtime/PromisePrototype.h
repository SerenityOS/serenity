/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class PromisePrototype final : public Object {
    JS_OBJECT(PromisePrototype, Object);

public:
    PromisePrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~PromisePrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(then);
    JS_DECLARE_NATIVE_FUNCTION(catch_);
    JS_DECLARE_NATIVE_FUNCTION(finally);
};

}
