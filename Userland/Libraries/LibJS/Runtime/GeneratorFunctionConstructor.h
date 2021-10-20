/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace JS {

// 27.3.1 %GeneratorFunction%, https://tc39.es/ecma262/#sec-generatorfunction-constructor
class GeneratorFunctionConstructor final : public NativeFunction {
    JS_OBJECT(GeneratorFunctionConstructor, NativeFunction);

public:
    explicit GeneratorFunctionConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~GeneratorFunctionConstructor() override;

    virtual ThrowCompletionOr<Value> call() override;
    virtual ThrowCompletionOr<Object*> construct(FunctionObject& new_target) override;

private:
    bool has_constructor() const override { return true; }
};

}
