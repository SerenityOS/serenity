/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/FunctionEnvironment.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

FunctionEnvironment::FunctionEnvironment(Environment* parent_scope)
    : DeclarativeEnvironment(parent_scope)
{
}

FunctionEnvironment::~FunctionEnvironment()
{
}

void FunctionEnvironment::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_this_value);
    visitor.visit(m_new_target);
    visitor.visit(m_function_object);
}

// 9.1.1.3.5 GetSuperBase ( ), https://tc39.es/ecma262/#sec-getsuperbase
ThrowCompletionOr<Value> FunctionEnvironment::get_super_base() const
{
    VERIFY(m_function_object);

    // 1. Let home be envRec.[[FunctionObject]].[[HomeObject]].
    auto home_object = m_function_object->home_object();

    // 2. If home has the value undefined, return undefined.
    if (!home_object)
        return js_undefined();

    // 3. Assert: Type(home) is Object.

    // 4. Return ? home.[[GetPrototypeOf]]().
    return { TRY(home_object->internal_get_prototype_of()) };
}

// 9.1.1.3.2 HasThisBinding ( ), https://tc39.es/ecma262/#sec-function-environment-records-hasthisbinding
bool FunctionEnvironment::has_this_binding() const
{
    if (this_binding_status() == ThisBindingStatus::Lexical)
        return false;
    return true;
}

// 9.1.1.3.3 HasSuperBinding ( ), https://tc39.es/ecma262/#sec-function-environment-records-hassuperbinding
bool FunctionEnvironment::has_super_binding() const
{
    if (this_binding_status() == ThisBindingStatus::Lexical)
        return false;
    if (!function_object().home_object())
        return false;
    return true;
}

// 9.1.1.3.4 GetThisBinding ( ), https://tc39.es/ecma262/#sec-function-environment-records-getthisbinding
Value FunctionEnvironment::get_this_binding(GlobalObject& global_object) const
{
    VERIFY(has_this_binding());
    if (this_binding_status() == ThisBindingStatus::Uninitialized) {
        vm().throw_exception<ReferenceError>(global_object, ErrorType::ThisHasNotBeenInitialized);
        return {};
    }
    return m_this_value;
}

// 9.1.1.3.1 BindThisValue ( V ), https://tc39.es/ecma262/#sec-bindthisvalue
Value FunctionEnvironment::bind_this_value(GlobalObject& global_object, Value this_value)
{
    VERIFY(this_binding_status() != ThisBindingStatus::Lexical);
    if (this_binding_status() == ThisBindingStatus::Initialized) {
        vm().throw_exception<ReferenceError>(global_object, ErrorType::ThisIsAlreadyInitialized);
        return {};
    }
    m_this_value = this_value;
    m_this_binding_status = ThisBindingStatus::Initialized;
    return this_value;
}

}
