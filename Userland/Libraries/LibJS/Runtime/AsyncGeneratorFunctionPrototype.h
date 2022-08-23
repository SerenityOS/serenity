/*
 * Copyright (c) 2021, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/PrototypeObject.h>

namespace JS {

class AsyncGeneratorFunctionPrototype final : public PrototypeObject<AsyncGeneratorFunctionPrototype, AsyncGeneratorFunction> {
    JS_PROTOTYPE_OBJECT(AsyncGeneratorFunctionPrototype, AsyncGeneratorFunction, AsyncGeneratorFunction);

public:
    explicit AsyncGeneratorFunctionPrototype(Realm&);
    virtual void initialize(Realm&) override;
    virtual ~AsyncGeneratorFunctionPrototype() override = default;
};

}
