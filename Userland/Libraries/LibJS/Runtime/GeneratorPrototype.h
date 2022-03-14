/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/GeneratorObject.h>
#include <LibJS/Runtime/PrototypeObject.h>

namespace JS {

// 27.5.1 Properties of the Generator Prototype Object, https://tc39.es/ecma262/#sec-properties-of-generator-prototype
class GeneratorPrototype final : public PrototypeObject<GeneratorPrototype, GeneratorObject> {
    JS_PROTOTYPE_OBJECT(GeneratorPrototype, GeneratorObject, Generator);

public:
    explicit GeneratorPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~GeneratorPrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(next);
    JS_DECLARE_NATIVE_FUNCTION(return_);
    JS_DECLARE_NATIVE_FUNCTION(throw_);
};

}
