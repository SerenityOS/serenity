/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace JS {

class WeakSetConstructor final : public NativeFunction {
    JS_OBJECT(WeakSetConstructor, NativeFunction);

public:
    explicit WeakSetConstructor(Realm&);
    virtual void initialize(Realm&) override;
    virtual ~WeakSetConstructor() override = default;

    virtual ThrowCompletionOr<Value> call() override;
    virtual ThrowCompletionOr<Object*> construct(FunctionObject&) override;

private:
    virtual bool has_constructor() const override { return true; }
};

}
