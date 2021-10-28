/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/AST.h>
#include <LibJS/Runtime/NativeFunction.h>

namespace JS {

class FunctionConstructor final : public NativeFunction {
    JS_OBJECT(FunctionConstructor, NativeFunction);

public:
    static ThrowCompletionOr<RefPtr<FunctionExpression>> create_dynamic_function_node(GlobalObject& global_object, FunctionObject& new_target, FunctionKind kind);

    explicit FunctionConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~FunctionConstructor() override;

    virtual ThrowCompletionOr<Value> call() override;
    virtual ThrowCompletionOr<Object*> construct(FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }
};

}
