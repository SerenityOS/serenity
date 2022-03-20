/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

// 27.3.3 %GeneratorFunction.prototype%, https://tc39.es/ecma262/#sec-properties-of-the-generatorfunction-prototype-object
class GeneratorFunctionPrototype final : public Object {
    JS_OBJECT(GeneratorFunctionPrototype, Object);

public:
    explicit GeneratorFunctionPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~GeneratorFunctionPrototype() override = default;
};

}
