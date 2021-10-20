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

    virtual ThrowCompletionOr<Value> call() override;
    virtual ThrowCompletionOr<Object*> construct(FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }
};

#define DECLARE_NATIVE_ERROR_CONSTRUCTOR(ClassName, snake_name, PrototypeName, ConstructorName) \
    class ConstructorName final : public NativeFunction {                                       \
        JS_OBJECT(ConstructorName, NativeFunction);                                             \
                                                                                                \
    public:                                                                                     \
        explicit ConstructorName(GlobalObject&);                                                \
        virtual void initialize(GlobalObject&) override;                                        \
        virtual ~ConstructorName() override;                                                    \
        virtual ThrowCompletionOr<Value> call() override;                                       \
        virtual ThrowCompletionOr<Object*> construct(FunctionObject& new_target) override;      \
                                                                                                \
    private:                                                                                    \
        virtual bool has_constructor() const override { return true; }                          \
    };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    DECLARE_NATIVE_ERROR_CONSTRUCTOR(ClassName, snake_name, PrototypeName, ConstructorName)
JS_ENUMERATE_NATIVE_ERRORS
#undef __JS_ENUMERATE

}
