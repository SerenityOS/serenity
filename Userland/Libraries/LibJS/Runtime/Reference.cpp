/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Reference.h>

namespace JS {

void Reference::put(GlobalObject& global_object, Value value)
{
    auto& vm = global_object.vm();

    if (is_unresolvable()) {
        throw_reference_error(global_object);
        return;
    }

    if (is_local_variable() || is_global_variable()) {
        if (is_local_variable())
            vm.set_variable(m_name.to_string(), value, global_object);
        else
            global_object.put(m_name, value);
        return;
    }

    auto base = this->base();

    if (!base.is_object() && vm.in_strict_mode()) {
        if (base.is_nullish())
            vm.throw_exception<TypeError>(global_object, ErrorType::ReferenceNullishSetProperty, m_name.to_value(vm).to_string_without_side_effects(), base.to_string_without_side_effects());
        else
            vm.throw_exception<TypeError>(global_object, ErrorType::ReferencePrimitiveSetProperty, m_name.to_value(vm).to_string_without_side_effects(), base.typeof(), base.to_string_without_side_effects());
        return;
    }

    if (base.is_nullish()) {
        // This will always fail the to_object() call below, let's throw the TypeError ourselves with a nice message instead.
        vm.throw_exception<TypeError>(global_object, ErrorType::ReferenceNullishSetProperty, m_name.to_value(vm).to_string_without_side_effects(), base.to_string_without_side_effects());
        return;
    }

    auto* object = base.to_object(global_object);
    if (!object)
        return;

    object->put(m_name, value);
}

void Reference::throw_reference_error(GlobalObject& global_object)
{
    auto& vm = global_object.vm();
    if (!m_name.is_valid())
        vm.throw_exception<ReferenceError>(global_object, ErrorType::ReferenceUnresolvable);
    else
        vm.throw_exception<ReferenceError>(global_object, ErrorType::UnknownIdentifier, m_name.to_string_or_symbol().to_display_string());
}

Value Reference::get(GlobalObject& global_object)
{
    auto& vm = global_object.vm();

    if (is_unresolvable()) {
        throw_reference_error(global_object);
        return {};
    }

    if (is_local_variable() || is_global_variable()) {
        Value value;
        if (is_local_variable())
            value = vm.get_variable(m_name.to_string(), global_object);
        else
            value = global_object.get(m_name);
        if (vm.exception())
            return {};
        if (value.is_empty()) {
            throw_reference_error(global_object);
            return {};
        }
        return value;
    }

    auto base = this->base();

    if (base.is_nullish()) {
        // This will always fail the to_object() call below, let's throw the TypeError ourselves with a nice message instead.
        vm.throw_exception<TypeError>(global_object, ErrorType::ReferenceNullishGetProperty, m_name.to_value(vm).to_string_without_side_effects(), base.to_string_without_side_effects());
        return {};
    }

    auto* object = base.to_object(global_object);
    if (!object)
        return {};

    return object->get(m_name).value_or(js_undefined());
}

}
