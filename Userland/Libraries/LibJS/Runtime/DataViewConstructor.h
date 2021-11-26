/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace JS {

class DataViewConstructor final : public NativeFunction {
    JS_OBJECT(DataViewConstructor, NativeFunction);

public:
    explicit DataViewConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~DataViewConstructor() override;

    virtual ThrowCompletionOr<Value> call() override;
    virtual ThrowCompletionOr<Object*> construct(FunctionObject&) override;

private:
    virtual bool has_constructor() const override { return true; }
};

}
