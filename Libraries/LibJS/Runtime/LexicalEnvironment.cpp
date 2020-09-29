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

#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/LexicalEnvironment.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

LexicalEnvironment::LexicalEnvironment()
{
}

LexicalEnvironment::LexicalEnvironment(EnvironmentRecordType environment_record_type)
    : m_environment_record_type(environment_record_type)
{
}

LexicalEnvironment::LexicalEnvironment(HashMap<FlyString, Variable> variables, LexicalEnvironment* parent)
    : m_parent(parent)
    , m_variables(move(variables))
{
}

LexicalEnvironment::LexicalEnvironment(HashMap<FlyString, Variable> variables, LexicalEnvironment* parent, EnvironmentRecordType environment_record_type)
    : m_parent(parent)
    , m_variables(move(variables))
    , m_environment_record_type(environment_record_type)
{
}

LexicalEnvironment::~LexicalEnvironment()
{
}

void LexicalEnvironment::visit_children(Visitor& visitor)
{
    Cell::visit_children(visitor);
    visitor.visit(m_parent);
    visitor.visit(m_this_value);
    visitor.visit(m_home_object);
    visitor.visit(m_new_target);
    visitor.visit(m_current_function);
    for (auto& it : m_variables)
        visitor.visit(it.value.value);
}

Optional<Variable> LexicalEnvironment::get(const FlyString& name) const
{
    ASSERT(type() != EnvironmentRecordType::Global);
    return m_variables.get(name);
}

void LexicalEnvironment::set(GlobalObject& global_object, const FlyString& name, Variable variable)
{
    if (type() == EnvironmentRecordType::Global)
        global_object.put(name, variable.value);
    else
        m_variables.set(name, variable);
}

bool LexicalEnvironment::has_super_binding() const
{
    return m_environment_record_type == EnvironmentRecordType::Function && this_binding_status() != ThisBindingStatus::Lexical && m_home_object.is_object();
}

Value LexicalEnvironment::get_super_base()
{
    ASSERT(has_super_binding());
    if (m_home_object.is_object())
        return m_home_object.as_object().prototype();
    return {};
}

bool LexicalEnvironment::has_this_binding() const
{
    // More like "is_capable_of_having_a_this_binding".
    switch (m_environment_record_type) {
    case EnvironmentRecordType::Declarative:
    case EnvironmentRecordType::Object:
        return false;
    case EnvironmentRecordType::Function:
        return this_binding_status() != ThisBindingStatus::Lexical;
    case EnvironmentRecordType::Module:
    case EnvironmentRecordType::Global:
        return true;
    }
    ASSERT_NOT_REACHED();
}

Value LexicalEnvironment::get_this_binding(GlobalObject& global_object) const
{
    ASSERT(has_this_binding());
    if (this_binding_status() == ThisBindingStatus::Uninitialized) {
        vm().throw_exception<ReferenceError>(global_object, ErrorType::ThisHasNotBeenInitialized);
        return {};
    }
    return m_this_value;
}

void LexicalEnvironment::bind_this_value(GlobalObject& global_object, Value this_value)
{
    ASSERT(has_this_binding());
    if (m_this_binding_status == ThisBindingStatus::Initialized) {
        vm().throw_exception<ReferenceError>(global_object, ErrorType::ThisIsAlreadyInitialized);
        return;
    }
    m_this_value = this_value;
    m_this_binding_status = ThisBindingStatus::Initialized;
}

}
