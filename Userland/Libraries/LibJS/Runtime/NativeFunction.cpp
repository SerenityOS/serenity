/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
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

NativeFunction::NativeFunction(PropertyName const& name, AK::Function<Value(VM&, GlobalObject&)> native_function, Object& prototype)
    : Function(prototype)
    , m_name(name.as_string())
    , m_native_function(move(native_function))
{
}

NativeFunction::NativeFunction(PropertyName const& name, Object& prototype)
    : Function(prototype)
    , m_name(name.as_string())
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

FunctionEnvironmentRecord* NativeFunction::create_environment_record(Function&)
{
    return nullptr;
}

bool NativeFunction::is_strict_mode() const
{
    return vm().in_strict_mode();
}

}
