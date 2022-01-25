/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace JS::Intl {

class RelativeTimeFormatConstructor final : public NativeFunction {
    JS_OBJECT(RelativeTimeFormatConstructor, NativeFunction);

public:
    explicit RelativeTimeFormatConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~RelativeTimeFormatConstructor() override = default;

    virtual ThrowCompletionOr<Value> call() override;
    virtual ThrowCompletionOr<Object*> construct(FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }
};

}
