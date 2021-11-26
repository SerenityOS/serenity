/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

class Error : public Object {
    JS_OBJECT(Error, Object);

public:
    static Error* create(GlobalObject&);
    static Error* create(GlobalObject&, String const& message);

    explicit Error(Object& prototype);
    virtual ~Error() override = default;

    ThrowCompletionOr<void> install_error_cause(Value options);
};

// NOTE: Making these inherit from Error is not required by the spec but
//       our way of implementing the [[ErrorData]] internal slot, which is
//       used in Object.prototype.toString().
#define DECLARE_NATIVE_ERROR(ClassName, snake_name, PrototypeName, ConstructorName) \
    class ClassName final : public Error {                                          \
        JS_OBJECT(ClassName, Error);                                                \
                                                                                    \
    public:                                                                         \
        static ClassName* create(GlobalObject&);                                    \
        static ClassName* create(GlobalObject&, String const& message);             \
                                                                                    \
        explicit ClassName(Object& prototype);                                      \
        virtual ~ClassName() override = default;                                    \
    };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    DECLARE_NATIVE_ERROR(ClassName, snake_name, PrototypeName, ConstructorName)
JS_ENUMERATE_NATIVE_ERRORS
#undef __JS_ENUMERATE
}
