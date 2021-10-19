/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace JS {

class NumberConstructor final : public NativeFunction {
    JS_OBJECT(NumberConstructor, NativeFunction);

public:
    explicit NumberConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~NumberConstructor() override;

    virtual Value call() override;
    virtual Value construct(FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }

    JS_DECLARE_OLD_NATIVE_FUNCTION(is_finite);
    JS_DECLARE_OLD_NATIVE_FUNCTION(is_integer);
    JS_DECLARE_OLD_NATIVE_FUNCTION(is_nan);
    JS_DECLARE_OLD_NATIVE_FUNCTION(is_safe_integer);
};

}
