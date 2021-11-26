/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace JS {

class WeakMapConstructor final : public NativeFunction {
    JS_OBJECT(WeakMapConstructor, NativeFunction);

public:
    explicit WeakMapConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~WeakMapConstructor() override;

    virtual ThrowCompletionOr<Value> call() override;
    virtual ThrowCompletionOr<Object*> construct(FunctionObject&) override;

private:
    virtual bool has_constructor() const override { return true; }
};

}
