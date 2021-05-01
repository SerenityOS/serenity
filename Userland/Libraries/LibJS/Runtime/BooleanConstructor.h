/*
 * Copyright (c) 2020, Jack Karamanian <karamanian.jack@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace JS {

class BooleanConstructor final : public NativeFunction {
    JS_OBJECT(BooleanConstructor, NativeFunction);

public:
    explicit BooleanConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~BooleanConstructor() override;

    virtual Value call() override;
    virtual Value construct(Function& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }
};

}
