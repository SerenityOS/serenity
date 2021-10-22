/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace JS::Intl {

class NumberFormatConstructor final : public NativeFunction {
    JS_OBJECT(NumberFormatConstructor, NativeFunction);

public:
    explicit NumberFormatConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~NumberFormatConstructor() override = default;

    virtual ThrowCompletionOr<Value> call() override;
    virtual ThrowCompletionOr<Object*> construct(FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }

    JS_DECLARE_NATIVE_FUNCTION(supported_locales_of);
};

}
