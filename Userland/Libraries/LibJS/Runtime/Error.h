/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

class Error : public Object {
    JS_OBJECT(Error, Object);

public:
    static Error* create(GlobalObject&, const String& message = {});

    explicit Error(Object& prototype);
    virtual ~Error() override = default;
};

#define DECLARE_ERROR_SUBCLASS(ClassName, snake_name, PrototypeName, ConstructorName) \
    class ClassName final : public Error {                                            \
        JS_OBJECT(ClassName, Error);                                                  \
                                                                                      \
    public:                                                                           \
        static ClassName* create(GlobalObject&, const String& message = {});          \
                                                                                      \
        explicit ClassName(Object& prototype);                                        \
        virtual ~ClassName() override = default;                                      \
    };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    DECLARE_ERROR_SUBCLASS(ClassName, snake_name, PrototypeName, ConstructorName)
JS_ENUMERATE_ERROR_SUBCLASSES
#undef __JS_ENUMERATE
}
