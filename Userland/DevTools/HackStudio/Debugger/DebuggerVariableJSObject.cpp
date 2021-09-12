/*
 * Copyright (c) 2021, Matthew Olsson <matthewcolsson@gmail.com>
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DebuggerVariableJSObject.h"
#include "Debugger.h"
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/PropertyName.h>

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

bool DebuggerVariableJSObject::internal_set(const JS::PropertyName& property_name, JS::Value value, JS::Value)
{
    if (!property_name.is_string()) {
        vm().throw_exception<JS::TypeError>(global_object(), String::formatted("Invalid variable name {}", property_name.to_string()));
        return false;
    }

    auto name = property_name.as_string();
    auto it = m_variable_info.members.find_if([&](auto& variable) {
        return variable->name == name;
    });

    if (it.is_end()) {
        vm().throw_exception<JS::TypeError>(global_object(), String::formatted("Variable of type {} has no property {}", m_variable_info.type_name, property_name));
        return false;
    }

    auto& member = **it;
    auto new_value = debugger_object().js_to_debugger(value, member);
    if (!new_value.has_value()) {
        auto string_error = String::formatted("Cannot convert JS value {} to variable {} of type {}", value.to_string_without_side_effects(), name, member.type_name);
        vm().throw_exception<JS::TypeError>(global_object(), string_error);
        return false;
    }
    Debugger::the().session()->poke((u32*)member.location_data.address, new_value.value());
    return true;
}

DebuggerGlobalJSObject& DebuggerVariableJSObject::debugger_object() const
{
    return static_cast<DebuggerGlobalJSObject&>(global_object());
}

}
