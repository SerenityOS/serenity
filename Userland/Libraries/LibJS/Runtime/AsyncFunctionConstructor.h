/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/AST.h>
#include <LibJS/Runtime/NativeFunction.h>

namespace JS {

class AsyncFunctionConstructor final : public NativeFunction {
    JS_OBJECT(AsyncFunctionConstructor, NativeFunction);

public:
    explicit AsyncFunctionConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~AsyncFunctionConstructor() override = default;

    virtual ThrowCompletionOr<Value> call() override;
    virtual ThrowCompletionOr<Object*> construct(FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }
};

}
