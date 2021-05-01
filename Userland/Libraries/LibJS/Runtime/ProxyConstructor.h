/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace JS {

class ProxyConstructor final : public NativeFunction {
    JS_OBJECT(ProxyConstructor, NativeFunction);

public:
    explicit ProxyConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~ProxyConstructor() override;

    virtual Value call() override;
    virtual Value construct(Function& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }
};

}
