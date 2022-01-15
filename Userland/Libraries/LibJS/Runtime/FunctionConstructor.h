/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
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
    static ThrowCompletionOr<ECMAScriptFunctionObject*> create_dynamic_function(GlobalObject& global_object, FunctionObject& constructor, FunctionObject* new_target, FunctionKind kind, MarkedValueList const& args);

    explicit FunctionConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~FunctionConstructor() override;

    virtual ThrowCompletionOr<Value> call() override;
    virtual ThrowCompletionOr<Object*> construct(FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }
};

}
