/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

NativeFunction* NativeFunction::create(GlobalObject& global_object, FlyString const& name, Function<Value(VM&, GlobalObject&)> function)
{
    return global_object.heap().allocate<NativeFunction>(global_object, name, move(function), *global_object.function_prototype());
}

NativeFunction::NativeFunction(Object& prototype)
    : FunctionObject(prototype)
{
}

NativeFunction::NativeFunction(FlyString name, Function<Value(VM&, GlobalObject&)> native_function, Object& prototype)
    : FunctionObject(prototype)
    , m_name(move(name))
    , m_native_function(move(native_function))
{
}

NativeFunction::NativeFunction(FlyString name, Object& prototype)
    : FunctionObject(prototype)
    , m_name(move(name))
{
}

NativeFunction::~NativeFunction()
{
}

Value NativeFunction::call()
{
    return m_native_function(vm(), global_object());
}

Value NativeFunction::construct(FunctionObject&)
{
    return {};
}

FunctionEnvironment* NativeFunction::create_environment(FunctionObject&)
{
    return nullptr;
}

bool NativeFunction::is_strict_mode() const
{
    return vm().in_strict_mode();
}

}
