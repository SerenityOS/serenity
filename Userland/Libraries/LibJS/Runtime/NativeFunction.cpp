/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/FunctionEnvironment.h>
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
    , m_realm(vm().interpreter_if_exists() ? &vm().interpreter().realm() : nullptr)
{
}

NativeFunction::NativeFunction(FlyString name, Function<Value(VM&, GlobalObject&)> native_function, Object& prototype)
    : FunctionObject(prototype)
    , m_name(move(name))
    , m_native_function(move(native_function))
    , m_realm(vm().interpreter_if_exists() ? &vm().interpreter().realm() : nullptr)
{
}

NativeFunction::NativeFunction(FlyString name, Object& prototype)
    : FunctionObject(prototype)
    , m_name(move(name))
    , m_realm(vm().interpreter_if_exists() ? &vm().interpreter().realm() : nullptr)
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

FunctionEnvironment* NativeFunction::new_function_environment(Object* new_target)
{
    // Simplified version of 9.1.2.4 NewFunctionEnvironment ( F, newTarget )
    Environment* parent_scope = nullptr;
    if (!vm().execution_context_stack().is_empty())
        parent_scope = vm().lexical_environment();

    auto* environment = heap().allocate<FunctionEnvironment>(global_object(), parent_scope);
    environment->set_new_target(new_target ? new_target : js_undefined());

    return environment;
}

bool NativeFunction::is_strict_mode() const
{
    return true;
}

}
