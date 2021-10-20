/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace JS {

class AggregateErrorConstructor final : public NativeFunction {
    JS_OBJECT(AggregateErrorConstructor, NativeFunction);

public:
    explicit AggregateErrorConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~AggregateErrorConstructor() override = default;

    virtual ThrowCompletionOr<Value> call() override;
    virtual ThrowCompletionOr<Object*> construct(FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }
};

}
