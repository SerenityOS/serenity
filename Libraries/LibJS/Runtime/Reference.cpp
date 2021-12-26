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

#include <AK/StringBuilder.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Reference.h>

namespace JS {

void Reference::put(Interpreter& interpreter, GlobalObject& global_object, Value value)
{
    // NOTE: The caller is responsible for doing an exception check after assign().

    if (is_unresolvable()) {
        throw_reference_error(interpreter, global_object);
        return;
    }

    if (is_local_variable() || is_global_variable()) {
        if (is_local_variable())
            interpreter.set_variable(m_name.to_string(), value, global_object);
        else
            global_object.put(m_name, value);
        return;
    }

    if (!base().is_object() && interpreter.in_strict_mode()) {
        interpreter.throw_exception<TypeError>(ErrorType::ReferencePrimitiveAssignment, m_name.to_string().characters());
        return;
    }

    auto* object = base().to_object(interpreter, global_object);
    if (!object)
        return;

    object->put(m_name, value);
}

void Reference::throw_reference_error(Interpreter& interpreter, GlobalObject&)
{
    auto property_name = m_name.to_string();
    String message;
    if (property_name.is_empty()) {
        interpreter.throw_exception<ReferenceError>(ErrorType::ReferenceUnresolvable);
    } else {
        interpreter.throw_exception<ReferenceError>(ErrorType::UnknownIdentifier, property_name.characters());
    }
}

Value Reference::get(Interpreter& interpreter, GlobalObject& global_object)
{
    // NOTE: The caller is responsible for doing an exception check after fetch().

    if (is_unresolvable()) {
        throw_reference_error(interpreter, global_object);
        return {};
    }

    if (is_local_variable() || is_global_variable()) {
        Value value;
        if (is_local_variable())
            value = interpreter.get_variable(m_name.to_string(), global_object);
        else
            value = global_object.get(m_name);
        if (interpreter.exception())
            return {};
        if (value.is_empty()) {
            throw_reference_error(interpreter, global_object);
            return {};
        }
        return value;
    }

    auto* object = base().to_object(interpreter, global_object);
    if (!object)
        return {};

    return object->get(m_name).value_or(js_undefined());
}

}
