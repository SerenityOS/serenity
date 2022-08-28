/*
 * Copyright (c) 2020-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/FunctionKind.h>
#include <LibJS/Runtime/NativeFunction.h>

namespace JS {

class FunctionConstructor final : public NativeFunction {
    JS_OBJECT(FunctionConstructor, NativeFunction);

public:
    static ThrowCompletionOr<ECMAScriptFunctionObject*> create_dynamic_function(VM&, FunctionObject& constructor, FunctionObject* new_target, FunctionKind kind, MarkedVector<Value> const& args);

    virtual void initialize(Realm&) override;
    virtual ~FunctionConstructor() override = default;

    virtual ThrowCompletionOr<Value> call() override;
    virtual ThrowCompletionOr<Object*> construct(FunctionObject& new_target) override;

private:
    explicit FunctionConstructor(Realm&);

    virtual bool has_constructor() const override { return true; }
};

}
