/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace JS::Temporal {

class DurationConstructor final : public NativeFunction {
    JS_OBJECT(DurationConstructor, NativeFunction);

public:
    explicit DurationConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~DurationConstructor() override = default;

    virtual ThrowCompletionOr<Value> call() override;
    virtual ThrowCompletionOr<Object*> construct(FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }

    JS_DECLARE_OLD_NATIVE_FUNCTION(from);
};

}
