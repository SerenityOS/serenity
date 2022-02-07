/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/PrototypeObject.h>

namespace JS {

class ErrorPrototype final : public PrototypeObject<ErrorPrototype, Error> {
    JS_PROTOTYPE_OBJECT(ErrorPrototype, Error, Error);

public:
    explicit ErrorPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~ErrorPrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(to_string);
    JS_DECLARE_NATIVE_FUNCTION(stack);
};

#define DECLARE_NATIVE_ERROR_PROTOTYPE(ClassName, snake_name, PrototypeName, ConstructorName) \
    class PrototypeName final : public PrototypeObject<PrototypeName, ClassName> {            \
        JS_PROTOTYPE_OBJECT(PrototypeName, ClassName, ClassName);                             \
                                                                                              \
    public:                                                                                   \
        explicit PrototypeName(GlobalObject&);                                                \
        virtual void initialize(GlobalObject&) override;                                      \
        virtual ~PrototypeName() override = default;                                          \
    };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    DECLARE_NATIVE_ERROR_PROTOTYPE(ClassName, snake_name, PrototypeName, ConstructorName)
JS_ENUMERATE_NATIVE_ERRORS
#undef __JS_ENUMERATE

}
