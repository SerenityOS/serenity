/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

NativeFunction* NativeFunction::create(GlobalObject& global_object, const FlyString& name, Function<Value(VM&, GlobalObject&)> function)
{
    return global_object.heap().allocate<NativeFunction>(global_object, name, move(function), *global_object.function_prototype());
}

// FIXME: m_realm is supposed to be the realm argument of CreateBuiltinFunction, or the current
//        Realm Record. The former is not something that's commonly used or we support, the
//        latter is impossible as no ExecutionContext exists when most NativeFunctions are created...

NativeFunction::NativeFunction(Object& prototype)
    : FunctionObject(prototype)
    , m_realm(&vm().interpreter().realm())
{
}

NativeFunction::NativeFunction(FlyString name, Function<Value(VM&, GlobalObject&)> native_function, Object& prototype)
    : FunctionObject(prototype)
    , m_name(move(name))
    , m_native_function(move(native_function))
    , m_realm(&vm().interpreter().realm())
{
}

NativeFunction::NativeFunction(FlyString name, Object& prototype)
    : FunctionObject(prototype)
    , m_name(move(name))
    , m_realm(&vm().interpreter().realm())
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
