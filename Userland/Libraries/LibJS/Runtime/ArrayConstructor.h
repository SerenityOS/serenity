/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace JS {

class ArrayConstructor final : public NativeFunction {
    JS_OBJECT(ArrayConstructor, NativeFunction);

public:
    explicit ArrayConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~ArrayConstructor() override;

    virtual ThrowCompletionOr<Value> call() override;
    virtual ThrowCompletionOr<Object*> construct(FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }

    JS_DECLARE_NATIVE_FUNCTION(from);
    JS_DECLARE_NATIVE_FUNCTION(is_array);
    JS_DECLARE_NATIVE_FUNCTION(of);

    JS_DECLARE_NATIVE_FUNCTION(symbol_species_getter);
};

}
