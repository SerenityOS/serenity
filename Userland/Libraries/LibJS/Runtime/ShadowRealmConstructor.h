/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace JS {

class ShadowRealmConstructor final : public NativeFunction {
    JS_OBJECT(ShadowRealmConstructor, NativeFunction);

public:
    explicit ShadowRealmConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~ShadowRealmConstructor() override = default;

    virtual ThrowCompletionOr<Value> call() override;
    virtual ThrowCompletionOr<Object*> construct(FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }
};

}
