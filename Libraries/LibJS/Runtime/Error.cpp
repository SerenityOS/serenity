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

#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

Error* Error::create(GlobalObject& global_object, const FlyString& name, const String& message)
{
    return global_object.heap().allocate<Error>(global_object, name, message, *global_object.error_prototype());
}

Error::Error(const FlyString& name, const String& message, Object& prototype)
    : Object(prototype)
    , m_name(name)
    , m_message(message)
{
}

Error::~Error()
{
}

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName)                                             \
    ClassName* ClassName::create(GlobalObject& global_object, const String& message)                                      \
    {                                                                                                                     \
        return global_object.heap().allocate<ClassName>(global_object, message, *global_object.snake_name##_prototype()); \
    }                                                                                                                     \
    ClassName::ClassName(const String& message, Object& prototype)                                                        \
        : Error(#ClassName, message, prototype)                                                                           \
    {                                                                                                                     \
    }                                                                                                                     \
    ClassName::~ClassName() { }

JS_ENUMERATE_ERROR_SUBCLASSES
#undef __JS_ENUMERATE

}
