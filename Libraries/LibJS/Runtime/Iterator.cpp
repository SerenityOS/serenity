/*
 * Copyright (c) 2020, The SerenityOS developers.
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
#include <LibJS/Runtime/Iterator.h>

namespace JS {

Iterator::Iterator(Object& iterable, AK::Function<IteratorResult(Object&, const Vector<Value>&)>&& next_function)

    : Object(nullptr)
    , m_iterable(iterable)
    , m_next_function(move(next_function))
{
    put_native_function("next", next);
}

Iterator::~Iterator()
{
}

Value Iterator::next(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return {};
    if (!this_object->is_iterator())
        return interpreter.throw_exception<TypeError>("Not an iterator");

    auto* iterator = static_cast<Iterator*>(this_object);
    auto return_value = Object::create_empty(interpreter, interpreter.global_object());

    if (iterator->done()) {
        return_value->put("done", Value(true));
        return_value->put("value", js_undefined());
        return return_value;
    }

    auto next_value = iterator->next_function()(iterator->iterable(), interpreter.call_frame().arguments);

    return_value->put("done", Value(next_value.finished));
    return_value->put("value", next_value.value);

    return return_value;
}

void Iterator::visit_children(Visitor& visitor)
{
    Object::visit_children(visitor);
    visitor.visit(&m_iterable);
}

}
