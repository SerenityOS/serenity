/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Error.h>

namespace JS {

class ErrorPrototype final : public Object {
    JS_OBJECT(ErrorPrototype, Object);

public:
    explicit ErrorPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~ErrorPrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(to_string);
};

#define DECLARE_NATIVE_ERROR_PROTOTYPE(ClassName, snake_name, PrototypeName, ConstructorName) \
    class PrototypeName final : public Object {                                               \
        JS_OBJECT(PrototypeName, Object);                                                     \
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
