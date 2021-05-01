/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/NativeFunction.h>

namespace JS {

class ErrorConstructor final : public NativeFunction {
    JS_OBJECT(ErrorConstructor, NativeFunction);

public:
    explicit ErrorConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~ErrorConstructor() override = default;

    virtual Value call() override;
    virtual Value construct(Function& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }
};

#define DECLARE_ERROR_SUBCLASS_CONSTRUCTOR(ClassName, snake_name, PrototypeName, ConstructorName) \
    class ConstructorName final : public NativeFunction {                                         \
        JS_OBJECT(ConstructorName, NativeFunction);                                               \
                                                                                                  \
    public:                                                                                       \
        explicit ConstructorName(GlobalObject&);                                                  \
        virtual void initialize(GlobalObject&) override;                                          \
        virtual ~ConstructorName() override;                                                      \
        virtual Value call() override;                                                            \
        virtual Value construct(Function& new_target) override;                                   \
                                                                                                  \
    private:                                                                                      \
        virtual bool has_constructor() const override { return true; }                            \
    };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    DECLARE_ERROR_SUBCLASS_CONSTRUCTOR(ClassName, snake_name, PrototypeName, ConstructorName)
JS_ENUMERATE_ERROR_SUBCLASSES
#undef __JS_ENUMERATE

}
