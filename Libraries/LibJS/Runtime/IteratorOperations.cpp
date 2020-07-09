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
#include <LibJS/Runtime/IteratorOperations.h>

namespace JS {

Object* get_iterator(Object& obj, String hint, Value method)
{
    auto& interpreter = obj.interpreter();
    ASSERT(hint == "sync" || hint == "async");
    if (method.is_empty()) {
        if (hint == "async")
            TODO();
        method = obj.get(obj.interpreter().get_well_known_symbol("iterator"));
        if (interpreter.exception())
            return {};
    }
    if (!method.is_function())
        TODO();
    auto iterator = interpreter.call(method.as_function(), &obj);
    if (interpreter.exception())
        return {};
    if (!iterator.is_object())
        TODO();
    return &iterator.as_object();
}

Value iterator_next(Object& iterator, Value value)
{
    auto& interpreter = iterator.interpreter();
    auto next_method = iterator.get("next");
    if (interpreter.exception())
        return {};

    ASSERT(next_method.is_function());

    Value result;
    if (value.is_empty()) {
        result = interpreter.call(next_method.as_function(), &iterator);
    } else {
        MarkedValueList arguments(iterator.heap());
        arguments.append(value);
        result = interpreter.call(next_method.as_function(), &iterator, move(arguments));
    }

    if (interpreter.exception())
        return {};
    if (!result.is_object())
        TODO();

    return result;
}

bool is_iterator_complete(Object& iterator_result)
{
    auto done = iterator_result.get("done");
    if (iterator_result.interpreter().exception())
        return false;
    return done.to_boolean();
}

Value iterator_value(Object& iterator_result)
{
    return iterator_result.get("value");
}

Value iterator_step(Object& iterator)
{
    auto& interpreter = iterator.interpreter();
    auto result = iterator_next(iterator);
    if (interpreter.exception())
        return {};
    auto done = is_iterator_complete(result.as_object());
    if (interpreter.exception())
        return {};
    if (done)
        return Value(false);
    return result;
}

void iterator_close(Object& iterator)
{
    (void)iterator;
    TODO();
}

Value create_iterator_result_object(Interpreter& interpreter, GlobalObject& global_object, Value value, bool done)
{
    auto* object = Object::create_empty(interpreter, global_object);
    object->put("value", value);
    object->put("done", Value(done));
    return object;
}

}
