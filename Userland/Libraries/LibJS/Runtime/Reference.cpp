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
