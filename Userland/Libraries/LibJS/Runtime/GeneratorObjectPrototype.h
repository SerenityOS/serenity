/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

// 27.5.1 %GeneratorFunction.prototype.prototype%, https://tc39.es/ecma262/#sec-properties-of-generator-prototype
class GeneratorObjectPrototype final : public Object {
    JS_OBJECT(GeneratorObjectPrototype, Object);

public:
    explicit GeneratorObjectPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~GeneratorObjectPrototype() override;

private:
    JS_DECLARE_NATIVE_FUNCTION(next);
    JS_DECLARE_NATIVE_FUNCTION(return_);
    JS_DECLARE_NATIVE_FUNCTION(throw_);
};

}
