/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/FlyString.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

class Error : public Object {
    JS_OBJECT(Error, Object);

public:
    static Error* create(GlobalObject&, const FlyString& name, const String& message);

    Error(const FlyString& name, const String& message, Object& prototype);
    virtual ~Error() override;

    const FlyString& name() const { return m_name; }
    const String& message() const { return m_message; }

    void set_name(const FlyString& name) { m_name = name; }

private:
    virtual bool is_error() const final { return true; }

    FlyString m_name;
    String m_message;
};

#define DECLARE_ERROR_SUBCLASS(ClassName, snake_name, PrototypeName, ConstructorName) \
    class ClassName final : public Error {                                            \
        JS_OBJECT(ClassName, Error);                                                  \
                                                                                      \
    public:                                                                           \
        static ClassName* create(GlobalObject&, const String& message);               \
                                                                                      \
        ClassName(const String& message, Object& prototype);                          \
        virtual ~ClassName() override;                                                \
    };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName) \
    DECLARE_ERROR_SUBCLASS(ClassName, snake_name, PrototypeName, ConstructorName)
JS_ENUMERATE_ERROR_SUBCLASSES
#undef __JS_ENUMERATE
}
