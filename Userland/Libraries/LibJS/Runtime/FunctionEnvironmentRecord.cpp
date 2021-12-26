/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/FunctionEnvironmentRecord.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

FunctionEnvironmentRecord::FunctionEnvironmentRecord(EnvironmentRecord* parent_scope, HashMap<FlyString, Variable> variables)
    : DeclarativeEnvironmentRecord(variables, parent_scope)
{
}

FunctionEnvironmentRecord::~FunctionEnvironmentRecord()
{
}

void FunctionEnvironmentRecord::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_this_value);
    visitor.visit(m_new_target);
    visitor.visit(m_function_object);
}

// 9.1.1.3.5 GetSuperBase ( ), https://tc39.es/ecma262/#sec-getsuperbase
Value FunctionEnvironmentRecord::get_super_base() const
{
    VERIFY(m_function_object);
    auto home_object = m_function_object->home_object();
    if (home_object.is_undefined())
        return js_undefined();
    return home_object.as_object().prototype();
}

// 9.1.1.3.2 HasThisBinding ( ), https://tc39.es/ecma262/#sec-function-environment-records-hasthisbinding
bool FunctionEnvironmentRecord::has_this_binding() const
{
    if (this_binding_status() == ThisBindingStatus::Lexical)
        return false;
    return true;
}

// 9.1.1.3.3 HasSuperBinding ( ), https://tc39.es/ecma262/#sec-function-environment-records-hassuperbinding
bool FunctionEnvironmentRecord::has_super_binding() const
{
    if (this_binding_status() == ThisBindingStatus::Lexical)
        return false;
    if (function_object().home_object().is_undefined())
        return false;
    return true;
}

// 9.1.1.3.4 GetThisBinding ( ), https://tc39.es/ecma262/#sec-function-environment-records-getthisbinding
Value FunctionEnvironmentRecord::get_this_binding(GlobalObject& global_object) const
{
    VERIFY(has_this_binding());
    if (this_binding_status() == ThisBindingStatus::Uninitialized) {
        vm().throw_exception<ReferenceError>(global_object, ErrorType::ThisHasNotBeenInitialized);
        return {};
    }
    return m_this_value;
}

// 9.1.1.3.1 BindThisValue ( V ), https://tc39.es/ecma262/#sec-bindthisvalue
Value FunctionEnvironmentRecord::bind_this_value(GlobalObject& global_object, Value this_value)
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
