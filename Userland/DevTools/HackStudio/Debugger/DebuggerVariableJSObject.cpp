/*
 * Copyright (c) 2021, Matthew Olsson <matthewcolsson@gmail.com>
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DebuggerVariableJSObject.h"
#include "Debugger.h"
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/PropertyKey.h>

namespace HackStudio {

DebuggerVariableJSObject* DebuggerVariableJSObject::create(DebuggerGlobalJSObject& global_object, const Debug::DebugInfo::VariableInfo& variable_info)
{
    return global_object.heap().allocate<DebuggerVariableJSObject>(global_object, variable_info, *global_object.object_prototype());
}

DebuggerVariableJSObject::DebuggerVariableJSObject(const Debug::DebugInfo::VariableInfo& variable_info, JS::Object& prototype)
    : JS::Object(prototype)
    , m_variable_info(variable_info)
{
}

DebuggerVariableJSObject::~DebuggerVariableJSObject()
{
}

JS::ThrowCompletionOr<bool> DebuggerVariableJSObject::internal_set(const JS::PropertyKey& property_key, JS::Value value, JS::Value)
{
    auto& vm = this->vm();

    if (!property_key.is_string())
        return vm.throw_completion<JS::TypeError>(global_object(), String::formatted("Invalid variable name {}", property_key.to_string()));

    auto name = property_key.as_string();
    auto it = m_variable_info.members.find_if([&](auto& variable) {
        return variable->name == name;
    });

    if (it.is_end())
        return vm.throw_completion<JS::TypeError>(global_object(), String::formatted("Variable of type {} has no property {}", m_variable_info.type_name, property_key));

    auto& member = **it;
    auto new_value = debugger_object().js_to_debugger(value, member);
    if (!new_value.has_value())
        return vm.throw_completion<JS::TypeError>(global_object(), String::formatted("Cannot convert JS value {} to variable {} of type {}", value.to_string_without_side_effects(), name, member.type_name));

    Debugger::the().session()->poke(member.location_data.address, new_value.value());
    return true;
}

DebuggerGlobalJSObject& DebuggerVariableJSObject::debugger_object() const
{
    return static_cast<DebuggerGlobalJSObject&>(global_object());
}

}
