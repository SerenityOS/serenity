/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
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
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>

namespace JS {

Object* get_iterator(GlobalObject& global_object, Value value, String hint, Value method)
{
    auto& interpreter = global_object.interpreter();
    ASSERT(hint == "sync" || hint == "async");
    if (method.is_empty()) {
        if (hint == "async")
            TODO();
        auto object = value.to_object(interpreter, global_object);
        if (!object)
            return {};
        method = object->get(global_object.vm().well_known_symbol_iterator());
        if (interpreter.exception())
            return {};
    }
    if (!method.is_function()) {
        interpreter.throw_exception<TypeError>(ErrorType::NotIterable, value.to_string_without_side_effects().characters());
        return nullptr;
    }
    auto iterator = interpreter.call(method.as_function(), value);
    if (interpreter.exception())
        return {};
    if (!iterator.is_object()) {
        interpreter.throw_exception<TypeError>(ErrorType::NotIterable, value.to_string_without_side_effects().characters());
        return nullptr;
    }
    return &iterator.as_object();
}

Object* iterator_next(Object& iterator, Value value)
{
    auto& interpreter = iterator.interpreter();
    auto next_method = iterator.get("next");
    if (interpreter.exception())
        return {};

    if (!next_method.is_function()) {
        interpreter.throw_exception<TypeError>(ErrorType::IterableNextNotAFunction);
        return nullptr;
    }

    Value result;
    if (value.is_empty())
        result = interpreter.call(next_method.as_function(), &iterator);
    else
        result = interpreter.call(next_method.as_function(), &iterator, value);

    if (interpreter.exception())
        return {};
    if (!result.is_object()) {
        interpreter.throw_exception<TypeError>(ErrorType::IterableNextBadReturn);
        return nullptr;
    }

    return &result.as_object();
}

void iterator_close(Object& iterator)
{
    (void)iterator;
    TODO();
}

Value create_iterator_result_object(GlobalObject& global_object, Value value, bool done)
{
    auto* object = Object::create_empty(global_object);
    object->define_property("value", value);
    object->define_property("done", Value(done));
    return object;
}

void get_iterator_values(GlobalObject& global_object, Value value, AK::Function<IterationDecision(Value)> callback)
{
    auto& interpreter = global_object.interpreter();

    auto iterator = get_iterator(global_object, value);
    if (!iterator)
        return;

    while (true) {
        auto next_object = iterator_next(*iterator);
        if (!next_object)
            return;

        auto done_property = next_object->get("done");
        if (interpreter.exception())
            return;

        if (!done_property.is_empty() && done_property.to_boolean())
            return;

        auto next_value = next_object->get("value");
        if (interpreter.exception())
            return;

        auto result = callback(next_value);
        if (result == IterationDecision::Break)
            return;
        ASSERT(result == IterationDecision::Continue);
    }
}

}
