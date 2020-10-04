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

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/LexicalEnvironment.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

NativeFunction* NativeFunction::create(GlobalObject& global_object, const FlyString& name, AK::Function<Value(VM&, GlobalObject&)> function)
{
    return global_object.heap().allocate<NativeFunction>(global_object, name, move(function), *global_object.function_prototype());
}

NativeFunction::NativeFunction(Object& prototype)
    : Function(prototype)
{
}

NativeFunction::NativeFunction(const FlyString& name, AK::Function<Value(VM&, GlobalObject&)> native_function, Object& prototype)
    : Function(prototype)
    , m_name(name)
    , m_native_function(move(native_function))
{
}

NativeFunction::NativeFunction(const FlyString& name, Object& prototype)
    : Function(prototype)
    , m_name(name)
{
}

NativeFunction::~NativeFunction()
{
}

Value NativeFunction::call()
{
    return m_native_function(vm(), global_object());
}

Value NativeFunction::construct(Function&)
{
    return {};
}

LexicalEnvironment* NativeFunction::create_environment()
{
    return heap().allocate<LexicalEnvironment>(global_object(), LexicalEnvironment::EnvironmentRecordType::Function);
}

bool NativeFunction::is_strict_mode() const
{
    return vm().in_strict_mode();
}

}
