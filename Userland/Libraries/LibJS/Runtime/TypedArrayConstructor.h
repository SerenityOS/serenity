/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace JS {

class TypedArrayConstructor : public NativeFunction {
    JS_OBJECT(TypedArrayConstructor, NativeFunction);

public:
    TypedArrayConstructor(FlyString const& name, Object& prototype);
    explicit TypedArrayConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~TypedArrayConstructor() override = default;

    virtual ThrowCompletionOr<Value> call() override;
    virtual ThrowCompletionOr<Object*> construct(FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }

    JS_DECLARE_NATIVE_FUNCTION(from);
    JS_DECLARE_NATIVE_FUNCTION(of);
    JS_DECLARE_NATIVE_FUNCTION(symbol_species_getter);
};

}
